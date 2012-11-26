package edu.ucsb.cs.smanner.net;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;

public class SystemIntegrationTest {
	private static Logger log = LoggerFactory.getLogger(SystemIntegrationTest.class);

	AbstractApplicationContext context1;
	AbstractApplicationContext context2;
	AbstractApplicationContext context3;
	AbstractApplicationContext contextConfig;

	PaxosLeader leaderA;
	PaxosFollower followerA;

	PaxosLeader leaderB;
	PaxosFollower followerB;

	TwoPhaseCommitCoordinator coordinator;
	TwoPhaseCommitParticipant participantA;
	TwoPhaseCommitParticipant participantB;
	
	volatile boolean committed = false;

	@Before
	public void setUp() throws Exception {
		committed = false;

		context1 = TestUtil.createContext("/META-INF/spring/node1.xml");
		context2 = TestUtil.createContext("/META-INF/spring/node2.xml");
		context3 = TestUtil.createContext("/META-INF/spring/node3.xml");
		contextConfig = TestUtil.createContext("/META-INF/spring/config.xml");
		
		// Paxos Group A
		leaderA = (PaxosLeader) context1.getBean("protocolAL");
		followerA = (PaxosFollower) context1.getBean("protocolA1");
		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group B");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					participantA.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					participantA.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		// Paxos Group B
		leaderB = (PaxosLeader) context2.getBean("protocolBL");
		followerB = (PaxosFollower) context2.getBean("protocolB2");
		followerB.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group B");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					participantB.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					participantB.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		// Two Phase Commit
		coordinator = (TwoPhaseCommitCoordinator) context2.getBean("protocolTpcL");
		
		participantA = (TwoPhaseCommitParticipant) context1.getBean("protocolTpcA");
		participantA.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				leaderA.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				leaderA.addProposal(new PaxosCommitOperation(operation.getId(), id));
			}
			@Override
			public void abort(long id, Operation operation) {
				// left blank	
			}
		});
		
		participantB = (TwoPhaseCommitParticipant) context2.getBean("protocolTpcB");
		participantB.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				leaderB.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				leaderB.addProposal(new PaxosCommitOperation(operation.getId(), id));
			}
			@Override
			public void abort(long id, Operation operation) {
				// left blank	
			}
		});
		

		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				committed = true;
			}
		});
		
		setupModerator(context1, "serverAL", "pgA");
		setupModerator(context1, "serverA1", "pgA");
		setupModerator(context1, "serverB1", "pgB");
		setupModerator(context1, "serverTpcA", "tpc");

		setupModerator(context2, "serverA2", "pgA");
		setupModerator(context2, "serverBL", "pgB");
		setupModerator(context2, "serverB2", "pgB");
		setupModerator(context2, "serverTpcB", "tpc");
		setupModerator(context2, "serverTpcL", "tpc");

		setupModerator(context3, "serverA3", "pgA");
		setupModerator(context3, "serverB3", "pgB");

	}

	@After
	public void tearDown() throws Exception {
		context1.close();
		context2.close();
		context3.close();
	}

	@Test(timeout = 5000)
	public void testStartup() {
		// nothing here
	}

	@Test(timeout = 5000)
	public void testCommit() throws Exception {
		long id = coordinator.addTransaction(new NullOperation("one"));
		while (coordinator.getTransaction(id).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
	
	void setupModerator(ApplicationContext context, String moderatorName, String groupName) {
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(groupName);
		Moderator moderator = (Moderator) context.getBean(moderatorName);
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

	@SuppressWarnings("serial")
	static class PaxosCommitOperation extends Operation {
		final long transactionId;
		
		public PaxosCommitOperation(String id, long transactionId) {
			super(id);
			this.transactionId = transactionId;
		}

		public long getTransactionId() {
			return transactionId;
		}
	}

	@SuppressWarnings("serial")
	static class PaxosPrepareOperation extends Operation {
		final long transactionId;

		public PaxosPrepareOperation(String id, long transactionId) {
			super(id);
			this.transactionId = transactionId;
		}

		public long getTransactionId() {
			return transactionId;
		}
	}

}
