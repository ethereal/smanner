package edu.ucsb.cs.smanner.net;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;
import edu.ucsb.cs.smanner.store.LockManager;

public class TwoPhaseCommitWithLockTest {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitWithLockTest.class);

	final String nodeA = "A";
	final String nodeB = "B";
	final String nodeC = "C";

	Moderator senderA;
	Moderator receiverB;
	Moderator receiverC;

	TwoPhaseCommitCoordinator coordA;
	TwoPhaseCommitParticipant followB;
	TwoPhaseCommitParticipant followC;
	
	LockManager lockB;
	LockManager lockC;

	@Before
	public void setUp() throws Exception {
		lockB = new LockManager();
		lockC = new LockManager();
		
		coordA = new TwoPhaseCommitCoordinator();
		followB = new TwoPhaseCommitParticipant();
		followC = new TwoPhaseCommitParticipant();

		followB.setExecutor(new LockTransactionExecutor(lockB));
		followC.setExecutor(new LockTransactionExecutor(lockC));

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
		
		senderA.run();
		receiverB.run();
		receiverC.run();

		long id1 = coordA.addTransaction(new NullOperation("one"));
		long id2 = coordA.addTransaction(new NullOperation("two"));

		while (coordA.getTransaction(id1).getState() != TransactionState.COMMITTED ||
			   followB.getTransaction(id1).getState() != TransactionState.COMMITTED ||
			   followC.getTransaction(id1).getState() != TransactionState.COMMITTED ||
			   coordA.getTransaction(id2).getState() != TransactionState.ABORTED ||
			   followB.getTransaction(id2).getState() != TransactionState.ABORTED ||
			   followC.getTransaction(id2).getState() != TransactionState.ABORTED) {
			Thread.sleep(100);
		}
	}

	static class LockTransactionExecutor extends TransactionExecutor {
		final LockManager lock;

		public LockTransactionExecutor(LockManager lock) {
			this.lock = lock;
		}
		
		@Override
		public void abort(long id, Operation operation) {
			try { Thread.sleep(200); } catch(Exception ignore) {};
			lock.unlock();
			participant.abortTransaction(id);
		}
		
		@Override
		public void commit(long id, Operation operation) {
			try { Thread.sleep(200); } catch(Exception ignore) {};
			lock.unlock();
			participant.commitTransaction(id);
		}
		
		@Override
		public void prepare(long id, Operation operation) {
			try {
				lock.lock(operation.getId());
				participant.acceptPrepare(id);
			} catch (Exception e) {
				participant.declinePrepare(id);
			}
		}
	}
}
