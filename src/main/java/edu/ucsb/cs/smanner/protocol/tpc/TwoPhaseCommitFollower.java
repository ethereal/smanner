package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;

public class TwoPhaseCommitFollower extends AbstractProtocol {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitFollower.class);
	
	volatile boolean active = true;
	
	Map<Long, Transaction> transactions = new HashMap<Long, Transaction>();
	
	Queue<Message> outQueue = new LinkedList<Message>();
	
	Collection<TransactionListener> listeners = new ArrayList<TransactionListener>();

	@Override
	public void put(Message message) throws Exception {
		log.trace("TwoPhaseCommitFollower::put({})", message);
		if(message instanceof PrepareMessage) {
			PrepareMessage msg = (PrepareMessage)message;
			
			if(transactions.containsKey(msg.id))
				throw new Exception("Transaction ID already exists");
			
			Transaction t = new Transaction(msg.id, msg.getSource());
			
			log.debug("{}: Try prepare transaction {}", self, t.id);
			transactions.put(msg.id, t);
			notifyListenersPrepare(t);
			
		} else if(message instanceof CommitMessage) {
			CommitMessage msg = (CommitMessage)message;
			
			if(!transactions.containsKey(msg.id))
				throw new Exception("Transaction ID does not exist");
			
			Transaction t = transactions.get(msg.id);
			
			log.debug("{}: Try commit transaction {}", self, t.id);
			notifyListenersCommit(t);
		} else {
			throw new Exception(String.format("unexpected message type %s", message.getClass()));
		}
	}

	@Override
	public Message get() throws Exception {
		log.trace("TwoPhaseCommitFollower::get()");
		return outQueue.poll();
	}

	@Override
	public boolean hasMessage() {
		log.trace("TwoPhaseCommitFollower::hasMessage()");
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
	
	public void prepareTransaction(long id) {
		log.debug("{}: Prepared transaction {}", self, id);
		Transaction t = transactions.get(id);
		t.setState(TransactionState.PREPARED);
		outQueue.add(new IsPreparedMessage(self, t.coordinator, t.id));
	}
	
	public void commitTransaction(long id) {
		log.debug("{}: Committed transaction {}", self, id);
		Transaction t = transactions.get(id);
		t.setState(TransactionState.COMMITTED);
	}
	
	public void addListener(TransactionListener listener) {
		listeners.add(listener);
	}
	
	void notifyListenersPrepare(Transaction t) {
		for(TransactionListener l : listeners) {
			l.notifyPrepare(t);
		}
	}

	void notifyListenersCommit(Transaction t) {
		for(TransactionListener l : listeners) {
			l.notifyCommit(t);
		}
	}

}
