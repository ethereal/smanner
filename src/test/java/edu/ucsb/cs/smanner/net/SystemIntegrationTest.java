package edu.ucsb.cs.smanner.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;
import edu.ucsb.cs.smanner.store.LockManager;

public class SystemIntegrationTest {
	private static Logger log = LoggerFactory.getLogger(SystemIntegrationTest.class);

	AbstractApplicationContext context1;
	AbstractApplicationContext context2;
	AbstractApplicationContext context3;
	AbstractApplicationContext contextConfig;
	AbstractApplicationContext contextClient1;
	AbstractApplicationContext contextClient2;

	PaxosLeader leaderA;
	PaxosFollower followerA;
	LockManager lockA;

	PaxosLeader leaderB;
	PaxosFollower followerB;
	LockManager lockB;

	TwoPhaseCommitCoordinator coordinator;
	TwoPhaseCommitParticipant participantA;
	TwoPhaseCommitParticipant participantB;
	
	TransactionEndpoint client1;
	TransactionEndpoint client2;
	
	volatile boolean committed = false;

	@Before
	public void setUp() throws Exception {
		committed = false;

		context1 = TestUtil.createContext("/META-INF/spring/node1.xml");
		context2 = TestUtil.createContext("/META-INF/spring/node2.xml");
		context3 = TestUtil.createContext("/META-INF/spring/node3.xml");
		contextConfig = TestUtil.createContext("/META-INF/spring/config.xml");
		contextClient1 = TestUtil.createContext("/META-INF/spring/client.xml");
		contextClient2 = TestUtil.createContext("/META-INF/spring/client.xml");
		
		// Paxos Group A
		leaderA = (PaxosLeader) context1.getBean("protocolAL");
		followerA = (PaxosFollower) context1.getBean("protocolA1");
		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group A");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					try {
						lockA.lock(operation.getId());
						participantA.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
					} catch(Exception e) {
						participantA.declinePrepare(((PaxosPrepareOperation) operation).transactionId);
					}
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					sleep(200);
					lockA.unlock();
					participantA.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				}  else if(operation instanceof PaxosAbortOperation) {
					// abort
					lockA.unlock();
					participantA.abortTransaction(((PaxosAbortOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		lockA = new LockManager();
		
		// Paxos Group B
		leaderB = (PaxosLeader) context2.getBean("protocolBL");
		followerB = (PaxosFollower) context2.getBean("protocolB2");
		followerB.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group B");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					try {
						lockB.lock(operation.getId());
						participantB.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
					} catch(Exception e) {
						participantB.declinePrepare(((PaxosPrepareOperation) operation).transactionId);
					}
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					sleep(200);
					lockB.unlock();
					participantB.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				}  else if(operation instanceof PaxosAbortOperation) {
					// abort
					lockB.unlock();
					participantB.abortTransaction(((PaxosAbortOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		lockB = new LockManager();
		
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
				leaderA.addProposal(new PaxosAbortOperation(operation.getId(), id));
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
				leaderB.addProposal(new PaxosAbortOperation(operation.getId(), id));
			}
		});
		

		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				committed = true;
			}
		});
		
		// set up servers
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
		
		// set up clients
		client1 = (TransactionEndpoint)contextClient1.getBean("smanner");
		client2 = (TransactionEndpoint)contextClient2.getBean("smanner");

	}

	@After
	public void tearDown() throws Exception {
		context1.close();
		context2.close();
		context3.close();
		contextConfig.close();
		contextClient1.close();
		contextClient2.close();
	}

	@Test(timeout = 1000)
	public void testStartup() {
		// nothing here
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		long id = coordinator.addTransaction(new NullOperation("one"));
		while (coordinator.getTransaction(id).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
	
	@Test(timeout = 1000)
	public void testSingleClient() throws Exception {
		Map<String, Operation> operations = new HashMap<String, Operation>();
		operations.put("tpcA", new NullOperation("A"));
		operations.put("tpcB", new NullOperation("B"));
		
		long id = client1.create(operations);
		
		while(client1.getState(id) != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
	
	@Test(timeout = 1000)
	public void testMultiClientAbort() throws Exception {
		Map<String, Operation> operations = new HashMap<String, Operation>();
		operations.put("tpcA", new NullOperation("A"));
		operations.put("tpcB", new NullOperation("B"));
		
		long id1 = client1.create(operations);
		long id2 = client2.create(operations);
		
		while(client1.getState(id1) != TransactionState.COMMITTED ||
			  client2.getState(id2) != TransactionState.ABORTED) {
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

	@SuppressWarnings("serial")
	static class PaxosAbortOperation extends Operation {
		final long transactionId;

		public PaxosAbortOperation(String id, long transactionId) {
			super(id);
			this.transactionId = transactionId;
		}

		public long getTransactionId() {
			return transactionId;
		}
	}
	
	static void sleep(long timeMillis) {
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() < start + timeMillis) {
			try {
				Thread.sleep(timeMillis);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
}
