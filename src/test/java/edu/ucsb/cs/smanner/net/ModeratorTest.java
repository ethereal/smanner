package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertFalse;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.CountProtocol;

public class ModeratorTest {
	private static Logger log = LoggerFactory.getLogger(ModeratorTest.class);
	
	Node node = new Node("id", "localhost");

	PipedInputStream in;
	PipedOutputStream out;
	Moderator moderator;

	@Before
	public void setUp() throws Exception {
		in = new PipedInputStream();
		out = new PipedOutputStream();
		in.connect(out);
		
		moderator = new Moderator(node, new CountProtocol(node, node, 20, 10000000));
		moderator.addNode(node, in, out);
	}

	@After
	public void tearDown() throws Exception {
		moderator.cancel();
		in.close();
		out.close();
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
