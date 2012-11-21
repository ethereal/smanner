package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;

public class TwoPhaseCommitCoordinator extends AbstractProtocol {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitCoordinator.class);
	volatile boolean active = true;

	Queue<Message> outQueue = new LinkedList<Message>();
	
	Map<Long, Transaction> transactions = new HashMap<Long, Transaction>();
	
	@Override
	public void put(Message message) throws Exception {
		log.trace("TwoPhaseCommitCoordinator::put({})", message);
		if(message instanceof IsPreparedMessage) {
			IsPreparedMessage msg = (IsPreparedMessage)message;
			Transaction t = transactions.get(msg.id);

			log.debug("received prepare for transaction {} from {}", t.id, msg.getSource());
			t.prepare(msg.getSource());
			
			if(t.getState() == TransactionState.PREPARED) {
				log.debug("transaction {} prepared", t.id);
				
				t.setState(TransactionState.COMMITTED);
				for(String follower : t.followers) {
					outQueue.add(new CommitMessage(self, follower, msg.id));
				}
			}
		} else {
			throw new Exception(String.format("unexpected message type %s", message.getClass()));
		}
	}

	@Override
	public Message get() throws Exception {
		log.trace("TwoPhaseCommitCoordinator::get()");
		return outQueue.poll();
	}

	@Override
	public boolean hasMessage() {
		log.trace("TwoPhaseCommitCoordinator::hasMessage()");
		return !outQueue.isEmpty();
	}

	@Override
	public boolean isDone() {
		return !active;
	}
	
	public void cancel() {
		active = false;
	}
	
	public void addTransaction(Transaction t) {
		log.debug("add transaction {}", t.id);
		transactions.put(t.id, t);
		for(String follower : t.followers) {
			outQueue.add(new PrepareMessage(self, follower, t.id));
		}
	}
	
	public Transaction getTransaction(long id) {
		return transactions.get(id);
	}

}
