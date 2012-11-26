package edu.ucsb.cs.smanner.net;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;

public class SmallSpannerTest {
	private static Logger log = LoggerFactory.getLogger(SmallSpannerTest.class);
	
	// First Paxos Group
	final String nodeAL = "AL";
	final String nodeA1 = "A1";
	final String nodeA2 = "A2";
	final String nodeA3 = "A3";
	
	Moderator mAL;
	Moderator mA1;
	Moderator mA2;
	Moderator mA3;
	
	PaxosLeader plA;
	PaxosFollower pfA;
	
	// Second Paxos Group
	final String nodeBL = "BL";
	final String nodeB1 = "B1";
	final String nodeB2 = "B2";
	final String nodeB3 = "B3";
	
	Moderator mBL;
	Moderator mB1;
	Moderator mB2;
	Moderator mB3;
	
	PaxosLeader plB;
	PaxosFollower pfB;
	
	// 2PC group
	final String nodeCoord = "COORD";
	final String nodeFollowA = "FA";
	final String nodeFollowB = "FB";

	Moderator mCoord;
	Moderator mFollowA;
	Moderator mFollowB;
	
	TwoPhaseCommitCoordinator tpcCoord;
	TwoPhaseCommitParticipant tpcFA;
	TwoPhaseCommitParticipant tpcFB;
	
	@Before
	public void setUp() throws Exception {
		
		// First Paxos Group
		plA = new PaxosLeader();
		pfA = new PaxosFollower();
		mAL = new Moderator(nodeAL, plA);
		mA1 = new Moderator(nodeA1, pfA);
		mA2 = new Moderator(nodeA2, new PaxosFollower());
		mA3 = new Moderator(nodeA3, new PaxosFollower());
		
		pfA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group A");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					tpcFA.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					tpcFA.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		// Second Paxos Group
		plB = new PaxosLeader();
		pfB = new PaxosFollower();
		mBL = new Moderator(nodeBL, plB);
		mB1 = new Moderator(nodeB1, pfB);
		mB2 = new Moderator(nodeB2, new PaxosFollower());
		mB3 = new Moderator(nodeB3, new PaxosFollower());
		
		pfB.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				log.debug("notified Paxos Group B");
				if(operation instanceof PaxosPrepareOperation) {
					// prepare
					tpcFB.acceptPrepare(((PaxosPrepareOperation) operation).transactionId);
				} else if(operation instanceof PaxosCommitOperation) {
					// commit
					tpcFB.commitTransaction(((PaxosCommitOperation) operation).transactionId);
				} else {
					log.error("unknown operation {}", operation.getId());
				}
			}
		});
		
		// 2PC Group
		tpcCoord = new TwoPhaseCommitCoordinator();
		tpcFA = new TwoPhaseCommitParticipant();
		tpcFB = new TwoPhaseCommitParticipant();
		mCoord = new Moderator(nodeCoord, tpcCoord);
		mFollowA = new Moderator(nodeFollowA, tpcFA);
		mFollowB = new Moderator(nodeFollowB, tpcFB);
		
		tpcFA.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				plA.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				plA.addProposal(new PaxosCommitOperation(operation.getId(), id));
			}
			@Override
			public void abort(long id, Operation operation) {
				// left blank	
			}
		});
		tpcFB.setExecutor(new TransactionExecutor() {
			@Override
			public void prepare(long id, Operation operation) {
				plB.addProposal(new PaxosPrepareOperation(operation.getId(), id));
			}
			public void commit(long id, Operation operation) {
				plB.addProposal(new PaxosCommitOperation(operation.getId(), id));
			}
			@Override
			public void abort(long id, Operation operation) {
				// left blank	
			}
		});
		
		TestUtil.connectAll(Arrays.asList(new Moderator[] {mAL, mA1, mA2, mA3}));
		TestUtil.connectAll(Arrays.asList(new Moderator[] {mBL, mB1, mB2, mB3}));
		TestUtil.connectAll(Arrays.asList(new Moderator[] {mCoord, mFollowA, mFollowB}));
		
		// run
		mAL.run();
		mA1.run();
		mA2.run();
		mA3.run();
		
		mBL.run();
		mB1.run();
		mB2.run();
		mB3.run();
		
		mCoord.run();
		mFollowA.run();
		mFollowB.run();
	}

	@After
	public void tearDown() throws Exception {
		// run
		mAL.cancel();
		mA1.cancel();
		mA2.cancel();
		mA3.cancel();
		
		mBL.cancel();
		mB1.cancel();
		mB2.cancel();
		mB3.cancel();
		
		mCoord.cancel();
		mFollowA.cancel();
		mFollowB.cancel();
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("SmallSpannerTest::testCommit()");
		
		Set<String> followers = new HashSet<String>();
		followers.add(nodeFollowA);
		followers.add(nodeFollowB);
		
		long id = tpcCoord.addTransaction(new NullOperation("one"));
		
		while(tpcCoord.getTransaction(id).getState() != TransactionState.COMMITTED ||
			  tpcFA.getTransaction(id) == null || tpcFA.getTransaction(id).getState() != TransactionState.COMMITTED ||
			  tpcFA.getTransaction(id) == null || tpcFB.getTransaction(id).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
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
