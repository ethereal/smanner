package edu.ucsb.cs.smanner.net;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionListener;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;

public class TwoPhaseCommitTest {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitTest.class);

	final String nodeA = "A";
	final String nodeB = "B";
	final String nodeC = "C";

	Moderator senderA;
	Moderator receiverB;
	Moderator receiverC;
	
	TwoPhaseCommitCoordinator coordA;
	TwoPhaseCommitParticipant followB;
	TwoPhaseCommitParticipant followC;

	@Before
	public void setUp() throws Exception {
		coordA = new TwoPhaseCommitCoordinator();
		followB = new TwoPhaseCommitParticipant();
		followC = new TwoPhaseCommitParticipant();
		
		followB.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(long id, Operation operation) {
				followB.prepareTransaction(id);
			}
			@Override
			public void notifyCommit(long id, Operation operation) {
				followB.commitTransaction(id);
			}
		});

		followC.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(long id, Operation operation) {
				followC.prepareTransaction(id);
			}
			@Override
			public void notifyCommit(long id, Operation operation) {
				followC.commitTransaction(id);
			}
		});

		senderA = new Moderator(nodeA, coordA);
		receiverB = new Moderator(nodeB, followB);
		receiverC = new Moderator(nodeC, followC);

		TestUtil.connectAll(Arrays.asList(new Moderator[] { senderA, receiverB, receiverC }));
	}

	@After
	public void tearDown() throws Exception {
		senderA.cancel();
		receiverB.cancel();
		receiverC.cancel();	
	}

	@Test(timeout = 1000)
	public void testCommit() throws Exception {
		log.trace("TwoPhaseCommitTest::testCommit()");
		senderA.run();
		receiverB.run();
		receiverC.run();

		long id = coordA.addTransaction(new NullOperation("one"));
		
		while(coordA.getTransaction(id).getState() != TransactionState.COMMITTED ||
			  followB.getTransaction(id).getState() != TransactionState.COMMITTED ||
			  followC.getTransaction(id).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
}
