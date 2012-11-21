package edu.ucsb.cs.smanner.net;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionListener;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitFollower;

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
	TwoPhaseCommitFollower tpcFA;
	TwoPhaseCommitFollower tpcFB;
	
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
			public void notifyCommit(Proposal proposal) {
				if(proposal.getId() % 10 == 0) {
					// prepare
					tpcFA.prepareTransaction(proposal.getId() / 10);
				} else {
					// commit
					tpcFA.commitTransaction(proposal.getId() / 10);
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
		
		pfA.addListener(new ProposalListener() {
			@Override
			public void notifyCommit(Proposal proposal) {
				if(proposal.getId() % 10 == 0) {
					// prepare
					tpcFB.prepareTransaction(proposal.getId() / 10);
				} else {
					// commit
					tpcFB.commitTransaction(proposal.getId() / 10);
				}
			}
		});
		
		// 2PC Group
		tpcCoord = new TwoPhaseCommitCoordinator();
		tpcFA = new TwoPhaseCommitFollower();
		tpcFB = new TwoPhaseCommitFollower();
		mCoord = new Moderator(nodeCoord, tpcCoord);
		mFollowA = new Moderator(nodeFollowA, tpcFA);
		mFollowB = new Moderator(nodeFollowB, tpcFB);
		
		tpcFA.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(Transaction transaction) {
				plA.addProposal(transaction.getId() * 10);
			}
			public void notifyCommit(Transaction transaction) {
				plA.addProposal(transaction.getId() * 10 + 1);
			}
		});
		tpcFB.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(Transaction transaction) {
				plB.addProposal(transaction.getId() * 10);
			}
			public void notifyCommit(Transaction transaction) {
				plB.addProposal(transaction.getId() * 10 + 1);
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

	@Test
	public void testCommit() throws Exception {
		log.trace("SmallSpannerTest::testCommit()");
		
		Set<String> followers = new HashSet<String>();
		followers.add(nodeFollowA);
		followers.add(nodeFollowB);
		
		tpcCoord.addTransaction(new Transaction(0, nodeCoord, followers));
		
		while(tpcCoord.getTransaction(0).getState() != TransactionState.COMMITTED ||
			  tpcFA.getTransaction(0) == null || tpcFA.getTransaction(0).getState() != TransactionState.COMMITTED ||
			  tpcFA.getTransaction(0) == null || tpcFB.getTransaction(0).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}

}
