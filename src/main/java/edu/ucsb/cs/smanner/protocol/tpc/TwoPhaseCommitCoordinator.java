package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import edu.ucsb.cs.smanner.net.Node;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Protocol;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;

public class TwoPhaseCommitCoordinator extends Protocol {
	volatile boolean active = true;

	Queue<Message> outQueue = new LinkedList<Message>();
	
	Map<Long, Transaction> transactions = new HashMap<Long, Transaction>();
	
	@Override
	public void put(Message message) throws Exception {
		if(message instanceof IsPreparedMessage) {
			IsPreparedMessage msg = (IsPreparedMessage)message;
			Transaction t = transactions.get(msg.id);
			t.prepare(msg.getSource());
			
			if(t.getState() == TransactionState.PREPARED) {
				for(Node follower : t.followers) {
					outQueue.add(new CommitMessage(self, follower, msg.id));
				}
			}
		} else {
			throw new Exception(String.format("unexpected message type %s", message.getClass()));
		}
	}

	@Override
	public Message get() throws Exception {
		return outQueue.poll();
	}

	@Override
	public boolean hasMessage() {
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
		transactions.put(t.id, t);
		for(Node follower : t.followers) {
			outQueue.add(new PrepareMessage(self, follower, t.id));
		}
	}

}
