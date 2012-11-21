package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;

public class PaxosLeader extends AbstractProtocol {

	volatile boolean active = true;
	
	Queue<Message> outQueue = new LinkedList<Message>();
	
	@Override
	public void put(Message message) throws Exception {
		// ignore
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
	
	public void addProposal(long id) {
		Set<String> acceptors = new HashSet<String>(nodes);
		acceptors.remove(self);
		
		for(String node : nodes) {
			outQueue.add(new ProposeMessage(self, node, id, acceptors));
		}
	}

}
