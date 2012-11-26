package edu.ucsb.cs.smanner.store;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LockManagerTest {
	final static String LOCKNAME = "operation";
	
	LockManager manager;

	@Before
	public void setUp() throws Exception {
		manager = new LockManager();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLock() throws Exception {
		manager.lock(LOCKNAME);
		assertTrue(manager.hasLock(LOCKNAME));
	}

	@Test
	public void testHasLock() throws Exception {
		manager.lock(LOCKNAME);
		assertTrue(manager.hasLock(LOCKNAME));
		assertFalse(manager.hasLock("false"));
	}

	@Test(expected = Exception.class)
	public void testLockFailure() throws Exception {
		manager.lock(LOCKNAME);
		manager.lock("false");
	}

}
