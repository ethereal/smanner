package edu.ucsb.cs.smanner;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.net.MessageEndpoint;
import edu.ucsb.cs.smanner.net.Moderator;
import edu.ucsb.cs.smanner.net.TestUtil;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;
import edu.ucsb.cs.smanner.store.FileStore;
import edu.ucsb.cs.smanner.store.LockManager;

public class Node2 {
	private static Logger log = LoggerFactory.getLogger(Node2.class);

	AbstractApplicationContext context;
	AbstractApplicationContext contextConfig;

	PaxosLeader leaderB;
	PaxosFollower followerA;
	PaxosFollower followerB;

	TwoPhaseCommitParticipant participantB;
	
	FileStore gradesStore = new FileStore(new File("grades.txt"));
	FileStore statsStore = new FileStore(new File("stats.txt"));

	public static void main(String args[]) throws Exception {
		Node2 node = new Node2();
		node.setUp();
		
		log.info("Node prepared, press enter to run.");
		UITool.readLine();
		node.run();

		log.info("Node running, press enter to stop.");
		UITool.readLine();
		node.tearDown();
	}
	
	public void setUp() throws Exception {
		log.trace("Node2::setUp()");
		context = TestUtil.createContext("/META-INF/spring/node2.xml");
		
		// Paxos Group B
		leaderB = (PaxosLeader) context.getBean("protocolBL");
		followerB = (PaxosFollower) context.getBean("protocolB2");
		followerB.addListener(new ProposalListener() {
			LockManager lock = new LockManager();
			
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group B");
				if(operation instanceof PaxosPrepareOperation) {
					log.debug("prepare PGB");
					// prepare
					try {
						lock.lock(operation.getId());
						participantB.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
					} catch(Exception e) {
						participantB.declinePrepare(((PaxosPrepareOperation) operation).transactionId);
					}
				}  else if(operation instanceof PaxosAbortOperation) {
					log.debug("abort PGB");
					// abort
					lock.unlock();
					participantB.abortTransaction(((PaxosAbortOperation) operation).transactionId);
				} else if(operation instanceof PaxosWriteOperation) {
					log.debug("write PGB");
					// commit
					try {
						statsStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
					lock.unlock();
					participantB.commitTransaction(((PaxosWriteOperation) operation).transactionId);
				} else if(operation instanceof PaxosReadOperation) {
					log.debug("read PGB");
					// commit
					String result;
					try {
						result = statsStore.read();
					} catch (Exception e) {
						// should not happen
						log.error("could not read file store");
						result = "[ERROR]";
					}
					lock.unlock();
					participantB.commitTransaction(((PaxosReadOperation) operation).transactionId, new ReadOperationResult(operation, result));
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		followerA = (PaxosFollower) context.getBean("protocolA2");
		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosWriteOperation) {
					log.debug("write PGA");
					// commit
					try {
						gradesStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
				}
			}
		});
		
		// Two Phase Commit
		participantB = (TwoPhaseCommitParticipant) context.getBean("protocolTpcB");
		participantB.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				leaderB.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				if(operation instanceof WriteOperation) {
					leaderB.addProposal(new PaxosWriteOperation(operation.getId(), id, ((WriteOperation) operation).getString()));
				} else if(operation instanceof ReadOperation) {
					leaderB.addProposal(new PaxosReadOperation(operation.getId(), id));
				}
			}
			@Override
			public void abort(long id, Operation operation) {
				leaderB.addProposal(new PaxosAbortOperation(operation.getId(), id));
			}
		});
		
	}

	public void run() {
		log.trace("Node2::run()");
		// set up servers
		contextConfig = TestUtil.createContext("/META-INF/spring/config.xml");
		setupModerator(context, "serverA2", "pgA");
		setupModerator(context, "serverBL", "pgB");
		setupModerator(context, "serverB2", "pgB");
		setupModerator(context, "serverTpcB", "tpc");
		setupModerator(context, "serverTpcL", "tpc");
	}
	
	public void tearDown() throws Exception {
		log.trace("Node2::tearDown()");
		context.close();
		contextConfig.close();
	}

	void setupModerator(ApplicationContext context, String moderatorName, String groupName) {
		log.trace("Node2::setupModerator()");
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(groupName);
		Moderator moderator = (Moderator) context.getBean(moderatorName);
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

}
