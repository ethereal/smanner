package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.CountSender;

public class ModeratorTest {
	private static Logger log = LoggerFactory.getLogger(ModeratorTest.class);
	
	final String node = "node";

	Moderator moderator;

	@Before
	public void setUp() throws Exception {
		moderator = new Moderator(node, new CountSender(20, 10000000));
		moderator.addNode(moderator);
	}

	@After
	public void tearDown() throws Exception {
		moderator.cancel();
	}

	@Test(timeout = 1000)
	public void testIsDonePassive() throws Exception {
		log.trace("ModeratorTest::testIsDonePassive()");
		assertFalse(moderator.isDone());
	}
	
	@Test(timeout = 1000)
	public void testIsDoneActive() throws Exception {
		log.trace("ModeratorTest::testIsDoneActive()");
		moderator.run();
		Thread.sleep(100);
		assertFalse(moderator.isDone());
	}
	
	@Test(timeout = 1000)
	public void testIsDoneComplete() throws Exception {
		log.trace("ModeratorTest::testIsDoneTerminated()");
		moderator.run();
		while(! moderator.isDone()) {
			Thread.sleep(100);
		}
	}
}
