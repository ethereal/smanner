package edu.ucsb.cs.smanner;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.net.MessageEndpoint;
import edu.ucsb.cs.smanner.net.Moderator;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;
import edu.ucsb.cs.smanner.store.FileStore;
import edu.ucsb.cs.smanner.store.LockManager;

public class Node1 {
	private static Logger log = LoggerFactory.getLogger(Node1.class);

	AbstractApplicationContext context;
	AbstractApplicationContext contextConfig;

	PaxosLeader leaderA;
	PaxosFollower followerA;
	PaxosFollower followerB;

	TwoPhaseCommitParticipant participantA;
	
	FileStore gradesStore = new FileStore(new File("grades.txt"));
	FileStore statsStore = new FileStore(new File("stats.txt"));

	public static void main(String args[]) throws Exception {
		Node1 node = new Node1();
		node.setUp();
		
		log.info("Node prepared, press enter to run.");
		NodeTool.readLine();
		node.run();

		log.info("Node running, press enter to stop.");
		NodeTool.readLine();
		node.tearDown();
	}
	
	public void setUp() throws Exception {
		log.trace("Node1::setUp()");
		context = NodeTool.createContext("/META-INF/spring/node1.xml");
		
		// Paxos Group A
		leaderA = (PaxosLeader) context.getBean("protocolAL");
		followerA = (PaxosFollower) context.getBean("protocolA1");
		followerA.addListener(new ProposalListener() {
			LockManager lock = new LockManager();
			
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosPrepareOperation) {
					log.debug("prepare PGA");
					// prepare
					try {
						lock.lock(operation.getId());
						participantA.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
					} catch(Exception e) {
						participantA.declinePrepare(((PaxosPrepareOperation) operation).transactionId);
					}
				}  else if(operation instanceof PaxosAbortOperation) {
					log.debug("abort PGA");
					// abort
					lock.unlock();
					participantA.abortTransaction(((PaxosAbortOperation) operation).transactionId);
				} else if(operation instanceof PaxosWriteOperation) {
					log.debug("write PGA");
					// commit
					try {
						gradesStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
					lock.unlock();
					participantA.commitTransaction(((PaxosWriteOperation) operation).transactionId);
				} else if(operation instanceof PaxosReadOperation) {
					log.debug("read PGA");
					// commit
					String result;
					try {
						result = gradesStore.read();
					} catch (Exception e) {
						// should not happen
						log.error("could not read file store");
						result = "[ERROR]";
					}
					lock.unlock();
					participantA.commitTransaction(((PaxosReadOperation) operation).transactionId, new ReadOperationResult(operation, result));
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		followerB = (PaxosFollower) context.getBean("protocolB1");
		followerB.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosWriteOperation) {
					log.debug("write PGB");
					// commit
					try {
						statsStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
				}
			}
		});

		// Two Phase Commit
		participantA = (TwoPhaseCommitParticipant) context.getBean("protocolTpcA");
		participantA.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				leaderA.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				if(operation instanceof WriteOperation) {
					leaderA.addProposal(new PaxosWriteOperation(operation.getId(), id, ((WriteOperation) operation).getString()));
				} else if(operation instanceof ReadOperation) {
					leaderA.addProposal(new PaxosReadOperation(operation.getId(), id));
				}
			}
			@Override
			public void abort(long id, Operation operation) {
				leaderA.addProposal(new PaxosAbortOperation(operation.getId(), id));
			}
		});
		
	}

	public void run() {
		log.trace("Node1::run()");
		// set up servers
		contextConfig = NodeTool.createContext("/META-INF/spring/config.xml");
		setupModerator(context, "serverAL", "pgA");
		setupModerator(context, "serverA1", "pgA");
		setupModerator(context, "serverB1", "pgB");
		setupModerator(context, "serverTpcA", "tpc");
	}
	
	public void tearDown() throws Exception {
		log.trace("Node1::tearDown()");
		context.close();
		contextConfig.close();
	}

	void setupModerator(ApplicationContext context, String moderatorName, String groupName) {
		log.trace("Node1::setupModerator()");
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(groupName);
		Moderator moderator = (Moderator) context.getBean(moderatorName);
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

}
