package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal.ProposalState;

public class PaxosTest {
	private static Logger log = LoggerFactory.getLogger(PaxosTest.class);

	final String nodeL = "L";
	final String nodeA = "A";
	final String nodeB = "B";
	final String nodeC = "C";

	Moderator serverL;
	Moderator serverA;
	Moderator serverB;
	Moderator serverC;
	
	PaxosLeader leader;
	PaxosFollower followA;
	PaxosFollower followB;
	PaxosFollower followC;

	@Before
	public void setUp() throws Exception {
		leader = new PaxosLeader();
		followA = new PaxosFollower();
		followB = new PaxosFollower();
		followC = new PaxosFollower();
		
		serverL = new Moderator(nodeL, leader);
		serverA = new Moderator(nodeA, followA);
		serverB = new Moderator(nodeB, followB);
		serverC = new Moderator(nodeC, followC);
		
		TestUtil.connectAll(Arrays.asList(new Moderator[] { serverL, serverA, serverB, serverC }));
	}

	@After
	public void tearDown() throws Exception {
		leader.cancel();
		followA.cancel();
		followB.cancel();
		followC.cancel();
	}

	@Test(timeout = 1000)
	public void testOperationHanddown() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		serverC.run();
		
		long id = leader.addProposal(new NullOperation("one"));
		
		while(isProposalPending(id)) {
			Thread.sleep(100);
		}
		
		assertNotNull(followA.getProposal(id).getOperation());
		assertNotNull(followB.getProposal(id).getOperation());
		assertNotNull(followC.getProposal(id).getOperation());
	}
	
	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		serverC.run();
		
		long id = leader.addProposal(new NullOperation("one"));
		
		while(isProposalPending(id)) {
			Thread.sleep(100);
		}
	}
	
	@Test(timeout = 1000)
	public void testMultiCommit() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		serverC.run();
		
		long id1 = leader.addProposal(new NullOperation("one"));
		long id2 = leader.addProposal(new NullOperation("two"));
		long id3 = leader.addProposal(new NullOperation("three"));
		
		while(isProposalPending(id1) || isProposalPending(id2) || isProposalPending(id3)) {
			Thread.sleep(100);
		}
	}
	
	boolean isProposalPending(long id) {
		return (followA.getProposal(id) == null || followA.getProposal(id).getState() != ProposalState.ACCEPTED ||
				followB.getProposal(id) == null || followB.getProposal(id).getState() != ProposalState.ACCEPTED ||
				followC.getProposal(id) == null || followC.getProposal(id).getState() != ProposalState.ACCEPTED);
	}
	
	// TODO fix test
	// @Test(timeout = 1000)
	public void testCommitWithFailure() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		// C missing
		
		long id = leader.addProposal(new NullOperation("one"));
		
		while(followA.getProposal(id) == null || followA.getProposal(id).getState() != ProposalState.ACCEPTED ||
			  followB.getProposal(id) == null || followB.getProposal(id).getState() != ProposalState.ACCEPTED) {
			Thread.sleep(100);
		}
	}
}
