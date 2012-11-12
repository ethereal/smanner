package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertFalse;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.CountReceiver;
import edu.ucsb.cs.smanner.protocol.CountSender;

public class ModeratorMultiNodeTest {
	private static Logger log = LoggerFactory.getLogger(ModeratorMultiNodeTest.class);

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

		senderA = new Moderator(nodeA, new CountSender(20, 10000000));
		senderA.addNode(nodeB, inAB, outAB);
		senderA.addNode(nodeC, inAC, outAC);

		receiverB = new Moderator(nodeB, new CountReceiver(20));
		receiverB.addNode(nodeA, inBA, outBA);

		receiverC = new Moderator(nodeC, new CountReceiver(20));
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
	public void testIsDoneActive() throws Exception {
		log.trace("ModeratorTest::testIsDoneActive()");
		senderA.run();
		receiverB.run();
		receiverC.run();
		Thread.sleep(100);
		assertFalse(senderA.isDone());
		assertFalse(receiverB.isDone());
		assertFalse(receiverC.isDone());
	}

	@Test(timeout = 2000)
	public void testIsDoneComplete() throws Exception {
		log.trace("ModeratorTest::testIsDoneTerminated()");
		senderA.run();
		receiverB.run();
		receiverC.run();
		
		while (!senderA.isDone() || !receiverB.isDone() || !receiverC.isDone()) {
			Thread.sleep(100);
		}
	}
}
