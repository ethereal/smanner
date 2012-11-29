package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.OperationResult;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;

public class TwoPhaseCommitParticipant extends AbstractProtocol {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitParticipant.class);

	volatile boolean active = true;

	Map<Long, Transaction> transactions = new HashMap<Long, Transaction>();

	Queue<Message> outQueue = new LinkedList<Message>();

	TransactionExecutor executor;

	@Override
	public void put(Message message) throws Exception {
		log.trace("TwoPhaseCommitParticipant::put({})", message);
		if (message instanceof DoPrepareMessage) {
			DoPrepareMessage msg = (DoPrepareMessage) message;

			if (transactions.containsKey(msg.id))
				throw new Exception("Transaction ID already exists");

			Transaction t = new Transaction(msg.id, msg.getSource());
			t.setOperation(self, msg.operation);

			log.debug("{}: Try preparing transaction {}", self, t.id);
			transactions.put(msg.id, t);
			executePrepare(t.id, msg.operation);

		} else if (message instanceof DoCommitMessage) {
			DoCommitMessage msg = (DoCommitMessage) message;

			if (!transactions.containsKey(msg.id))
				throw new Exception("Transaction ID does not exist");

			Transaction t = transactions.get(msg.id);

			log.debug("{}: Try committing transaction {}", self, t.id);
			executeCommit(t.id, t.getOperation(self));
			
		} else if (message instanceof DoAbortMessage) {
			DoAbortMessage msg = (DoAbortMessage) message;

			if (!transactions.containsKey(msg.id))
				throw new Exception("Transaction ID does not exist");

			Transaction t = transactions.get(msg.id);

			log.debug("{}: Try aborting transaction {}", self, t.id);
			executeAbort(t.id, t.getOperation(self));
			
		} else {
			throw new Exception(String.format("unexpected message type %s", message.getClass()));
		}
	}

	@Override
	public Message get() throws Exception {
		log.trace("TwoPhaseCommitParticipant::get()");
		return outQueue.poll();
	}

	@Override
	public boolean hasMessage() {
		log.trace("TwoPhaseCommitParticipant::hasMessage()");
		return !outQueue.isEmpty();
	}

	@Override
	public boolean isDone() {
		return !active;
	}

	void cancel() {
		active = false;
	}

	public Transaction getTransaction(long id) {
		return transactions.get(id);
	}

	public void acceptPrepare(long id) {
		log.debug("{}: Prepared transaction {}", self, id);
		Transaction t = transactions.get(id);
		t.setState(TransactionState.PREPARED);
		outQueue.add(new PreparedMessage(self, t.coordinator, t.id));
	}

	public void declinePrepare(long id) {
		log.debug("{}: Aborted transaction prepare {}", self, id);
		Transaction t = transactions.get(id);
		t.setState(TransactionState.ABORTED);
		outQueue.add(new AbortedMessage(self, t.coordinator, t.id));
	}

	public void commitTransaction(long id) {
		commitTransaction(id, null);
	}

	public void commitTransaction(long id, OperationResult result) {
		log.debug("{}: Committed transaction {} with result {}", new Object[] { self, id, result });
		Transaction t = transactions.get(id);
		t.setState(TransactionState.COMMITTED);
		outQueue.add(new CommittedMessage(self, t.coordinator, id, result));
	}

	public void abortTransaction(long id) {
		log.debug("{}: Aborted transaction {}", self, id);
		Transaction t = transactions.get(id);
		t.setState(TransactionState.ABORTED);
	}

	public void setExecutor(TransactionExecutor executor) {
		this.executor = executor;
		this.executor.setParticipant(this);
	}

	void executePrepare(long id, Operation operation) {
		if (executor != null)
			executor.prepare(id, operation);
	}

	void executeCommit(long id, Operation operation) {
		if (executor != null)
			executor.commit(id, operation);
	}

	void executeAbort(long id, Operation operation) {
		if (executor != null)
			executor.abort(id, operation);
	}

}
