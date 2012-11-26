package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;

public class TwoPhaseCommitCoordinator extends AbstractProtocol {
	private static Logger log = LoggerFactory.getLogger(TwoPhaseCommitCoordinator.class);
	
	int nextId = 0;
	volatile boolean active = true;

	Queue<Message> outQueue = new LinkedList<Message>();
	
	Map<Long, Transaction> transactions = new HashMap<Long, Transaction>();
	
	@Override
	public void put(Message message) throws Exception {
		log.trace("TwoPhaseCommitCoordinator::put({})", message);
		if(message instanceof PreparedMessage) {
			PreparedMessage msg = (PreparedMessage)message;
			Transaction t = transactions.get(msg.id);

			log.debug("received prepare for transaction {} from {}", t.id, msg.getSource());
			t.prepare(msg.getSource());
			
			if(t.getState() == TransactionState.PREPARED) {
				log.debug("transaction {} prepared", t.id);
				
				t.setState(TransactionState.COMMITTED);
				for(String follower : t.followers) {
					outQueue.add(new DoCommitMessage(self, follower, msg.id));
				}
			}
		} else if(message instanceof AbortedMessage) {
			AbortedMessage msg = (AbortedMessage)message;
			Transaction t = transactions.get(msg.id);

			log.debug("received abort for transaction {} from {}", t.id, msg.getSource());

			log.debug("transaction {} aborted", t.id);
			t.setState(TransactionState.ABORTED);
			for(String follower : t.followers) {
				outQueue.add(new DoAbortMessage(self, follower, msg.id));
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
	
	public int addTransaction(Operation operation) {
		int id = nextId;
		nextId++;
		
		Set<String> followers = new HashSet<String>();
		followers.addAll(nodes);
		followers.remove(self);
		
		log.debug("add transaction {}", id);
		transactions.put((long)id, new Transaction(id, self, followers));
		
		for(String follower : followers) {
			outQueue.add(new DoPrepareMessage(self, follower, id, operation));
		}
		
		return id;
	}
	
	public Transaction getTransaction(long id) {
		return transactions.get(id);
	}

}
