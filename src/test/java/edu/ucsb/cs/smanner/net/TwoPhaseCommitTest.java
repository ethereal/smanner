package edu.ucsb.cs.smanner.net;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
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

		followB.setExecutor(new NullTransactionExecutor());
		followC.setExecutor(new NullTransactionExecutor());

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

		while (coordA.getTransaction(id).getState() != TransactionState.COMMITTED ||
			   followB.getTransaction(id).getState() != TransactionState.COMMITTED ||
			   followC.getTransaction(id).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}

	@Test(timeout = 1000)
	public void testAbort() throws Exception {
		log.trace("TwoPhaseCommitTest::testAbort()");
		
		followC.setExecutor(new FaultyTransactionExecutor());
		
		senderA.run();
		receiverB.run();
		receiverC.run();

		long id = coordA.addTransaction(new NullOperation("one"));

		while (coordA.getTransaction(id).getState() != TransactionState.ABORTED ||
			   followB.getTransaction(id).getState() != TransactionState.ABORTED ||
			   followC.getTransaction(id).getState() != TransactionState.ABORTED) {
			Thread.sleep(100);
		}
	}

	static class FaultyTransactionExecutor extends NullTransactionExecutor {
		@Override
		public void prepare(long id, Operation operation) {
			participant.declinePrepare(id);
		}
	}
}
