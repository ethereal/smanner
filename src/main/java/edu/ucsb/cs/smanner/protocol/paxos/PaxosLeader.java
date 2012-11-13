package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.ucsb.cs.smanner.net.Node;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Protocol;

public class PaxosLeader extends Protocol {

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
		Set<Node> acceptors = new HashSet<Node>(nodes);
		acceptors.remove(self);
		
		for(Node node : nodes) {
			outQueue.add(new ProposeMessage(self, node, id, acceptors));
		}
	}

}
