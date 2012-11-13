package edu.ucsb.cs.smanner.net;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.tpc.Transaction;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitFollower;

public class TwoPhaseCommitTest {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitTest.class);

	Node nodeA = new Node("A", "A");
	Node nodeB = new Node("B", "B");
	Node nodeC = new Node("C", "C");

	PipedInputStream inAB;
	PipedOutputStream outAB;
	PipedInputStream inBA;
	PipedOutputStream outBA;
	PipedInputStream inAC;
	PipedOutputStream outAC;
	PipedInputStream inCA;
	PipedOutputStream outCA;
	PipedInputStream inBC;
	PipedOutputStream outBC;
	PipedInputStream inCB;
	PipedOutputStream outCB;

	Moderator senderA;
	Moderator receiverB;
	Moderator receiverC;
	
	TwoPhaseCommitCoordinator coordA;
	TwoPhaseCommitFollower followB;
	TwoPhaseCommitFollower followC;

	@Before
	public void setUp() throws Exception {
		inAB = new PipedInputStream();
		outAB = new PipedOutputStream();
		inBA = new PipedInputStream();
		outBA = new PipedOutputStream();
		inAB.connect(outBA);
		inBA.connect(outAB);

		inAC = new PipedInputStream();
		outAC = new PipedOutputStream();
		inCA = new PipedInputStream();
		outCA = new PipedOutputStream();
		inAC.connect(outCA);
		inCA.connect(outAC);

		inBC = new PipedInputStream();
		outBC = new PipedOutputStream();
		inCB = new PipedInputStream();
		outCB = new PipedOutputStream();
		inBC.connect(outCB);
		inCB.connect(outBC);
		
		coordA = new TwoPhaseCommitCoordinator();
		followB = new TwoPhaseCommitFollower();
		followC = new TwoPhaseCommitFollower();

		senderA = new Moderator(nodeA, coordA);
		senderA.addNode(nodeB, inAB, outAB);
		senderA.addNode(nodeC, inAC, outAC);

		receiverB = new Moderator(nodeB, followB);
		receiverB.addNode(nodeA, inBA, outBA);

		receiverC = new Moderator(nodeC, followC);
		receiverC.addNode(nodeA, inCA, outCA);
	}

	@After
	public void tearDown() throws Exception {
		senderA.cancel();
		receiverB.cancel();
		receiverC.cancel();
		
		inAB.close();
		outAB.close();
		inAC.close();
		outAC.close();
		inBC.close();
		outBC.close();
		inBA.close();
		outBA.close();
		inCA.close();
		outCA.close();
		inCB.close();
		outCB.close();
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("TwoPhaseCommitTest::testCommit()");
		Set<Node> nodes = new HashSet<Node>();
		nodes.add(nodeB);
		nodes.add(nodeC);
		
		Transaction t = new Transaction(0, nodes);
		
		senderA.run();
		receiverB.run();
		receiverC.run();

		coordA.addTransaction(t);
		
		while(coordA.getTransaction(0).getState() != TransactionState.COMMITTED ||
			  followB.getTransaction(0).getState() != TransactionState.COMMITTED ||
			  followC.getTransaction(0).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
}
