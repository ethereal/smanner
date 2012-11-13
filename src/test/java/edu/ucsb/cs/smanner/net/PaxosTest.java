package edu.ucsb.cs.smanner.net;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;

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

	Node nodeL = new Node("L", "A:1234");
	Node nodeA = new Node("A", "A");
	Node nodeB = new Node("B", "B");
	Node nodeC = new Node("C", "C");

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
		
		connectAll(Arrays.asList(new Moderator[] { serverL, serverA, serverB, serverC }));
	}

	@After
	public void tearDown() throws Exception {
		leader.cancel();
		followA.cancel();
		followB.cancel();
		followC.cancel();
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		serverC.run();
		
		leader.addProposal(0);
		
		while(followA.getProposal(0) == null || followA.getProposal(0).getState() != ProposalState.ACCEPTED ||
			  followB.getProposal(0) == null || followB.getProposal(0).getState() != ProposalState.ACCEPTED ||
			  followC.getProposal(0) == null || followC.getProposal(0).getState() != ProposalState.ACCEPTED) {
			Thread.sleep(100);
		}
	}
	
	@Test(timeout = 1000)
	public void testCommitWithFailure() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		// C missing
		
		leader.addProposal(0);
		
		while(followA.getProposal(0) == null || followA.getProposal(0).getState() != ProposalState.ACCEPTED ||
			  followB.getProposal(0) == null || followB.getProposal(0).getState() != ProposalState.ACCEPTED) {
			Thread.sleep(100);
		}
	}
	
	private void connect(Moderator a, Moderator b) throws IOException {
		PipedInputStream inAB = new PipedInputStream();
		PipedOutputStream outAB = new PipedOutputStream();
		PipedInputStream inBA = new PipedInputStream();
		PipedOutputStream outBA = new PipedOutputStream();
		
		inAB.connect(outBA);
		inBA.connect(outAB);
		
		a.addNode(b.self, inAB, outAB);
		b.addNode(a.self, inBA, outBA);
	}
	
	private void connectAll(List<Moderator> moderators) throws IOException {
		for(int i=0; i<moderators.size(); i++) {
			for(int j=i; j<moderators.size(); j++) {
				connect(moderators.get(i), moderators.get(j));
			}
		}
	}
	
}
