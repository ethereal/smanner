package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.CountReceiver;
import edu.ucsb.cs.smanner.protocol.CountSender;

public class ModeratorMultiNodeTest {
	private static Logger log = LoggerFactory.getLogger(ModeratorMultiNodeTest.class);

	final String nodeA = "A";
	final String nodeB = "B";
	final String nodeC = "C";

	Moderator senderA;
	Moderator receiverB;
	Moderator receiverC;

	@Before
	public void setUp() throws Exception {
		senderA = new Moderator(nodeA, new CountSender(20, 10000000));
		receiverB = new Moderator(nodeB, new CountReceiver(20));
		receiverC = new Moderator(nodeC, new CountReceiver(20));

		senderA.addNode(receiverB);
		senderA.addNode(receiverC);

		receiverB.addNode(senderA);

		receiverC.addNode(senderA);
	}

	@After
	public void tearDown() throws Exception {
		senderA.cancel();
		receiverB.cancel();
		receiverC.cancel();
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
