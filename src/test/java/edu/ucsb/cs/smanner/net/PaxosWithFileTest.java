package edu.ucsb.cs.smanner.net;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.store.FileStore;

public class PaxosWithFileTest {
	private static Logger log = LoggerFactory.getLogger(PaxosWithFileTest.class);

	final String nodeL = "L";
	final String nodeA = "A";
	final String nodeB = "B";
	final String nodeC = "C";
	
	final File fileA = new File(nodeA);
	final File fileB = new File(nodeB);
	final File fileC = new File(nodeC);
	
	FileStore storeA;
	FileStore storeB;
	FileStore storeC;

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
		fileA.createNewFile();
		fileB.createNewFile();
		fileC.createNewFile();
		
		storeA = new FileStore(fileA);
		storeB = new FileStore(fileB);
		storeC = new FileStore(fileC);
		
		leader = new PaxosLeader();
		followA = new PaxosFollower();
		followA.addListener(createListener(storeA));
		followB = new PaxosFollower();
		followB.addListener(createListener(storeB));
		followC = new PaxosFollower();
		followC.addListener(createListener(storeC));
		
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
		
		fileA.delete();
		fileB.delete();
		fileC.delete();
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("PaxosTest::testCommit()");

		serverL.run();
		serverA.run();
		serverB.run();
		serverC.run();
		
		leader.addProposal(new WriteOperation("one", "operation"));
		
		while(! storeA.read().equals("operation\n") ||
			  ! storeB.read().equals("operation\n") ||
			  ! storeC.read().equals("operation\n")) {
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
		
		leader.addProposal(new WriteOperation("one", "Hello"));
		leader.addProposal(new WriteOperation("two", "World"));
		leader.addProposal(new WriteOperation("three", "!"));
		
		while(! storeA.read().equals("Hello\nWorld\n!\n") ||
			  ! storeB.read().equals("Hello\nWorld\n!\n") ||
			  ! storeC.read().equals("Hello\nWorld\n!\n")) {
			Thread.sleep(100);
		}
	}
	
	ProposalListener createListener(final FileStore store) {
		return new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				WriteOperation wop = (WriteOperation) operation;
				
				try {
					store.append(wop.line);
				} catch (Exception e) {
					log.error("error while writing to file: {}", e);
				}
			}
		};
	}
	
	@SuppressWarnings("serial")
	static class WriteOperation extends Operation {
		final String line;

		public WriteOperation(String id, String line) {
			super(id);
			this.line = line;
		}
	}
}
