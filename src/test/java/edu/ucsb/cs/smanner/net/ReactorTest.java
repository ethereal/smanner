package edu.ucsb.cs.smanner.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.cs.smanner.protocol.CountProtocol;

public class ReactorTest {
	
	Node node = new Node("id", "localhost");

	PipedInputStream in;
	PipedOutputStream out;
	Reactor reactor;

	@Before
	public void setUp() throws Exception {
		in = new PipedInputStream();
		out = new PipedOutputStream();
		in.connect(out);
		
		reactor = new Reactor(node, new CountProtocol(node, node, 10));
		reactor.addNode(node, in, out);
	}

	@After
	public void tearDown() throws Exception {
		reactor.cancel();
		in.close();
		out.close();
	}

	@Test(timeout = 1000)
	public void testIsDonePassive() throws Exception {
		assertTrue(reactor.isDone());
	}
	
	@Test(timeout = 1000)
	public void testIsDoneActive() throws Exception {
		reactor.run();		
		assertFalse(reactor.isDone());
	}
	
	@Test(timeout = 1000)
	public void testIsDoneTerminated() throws Exception {
		reactor.cancel();
		while(! reactor.isDone()) {
			Thread.sleep(100);
		}
	}
}
