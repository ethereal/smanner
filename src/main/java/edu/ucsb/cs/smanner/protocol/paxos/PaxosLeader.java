package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Operation;

public class PaxosLeader extends AbstractProtocol {

	long nextId = 0;
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
	
	public long addProposal(Operation operation) {
		Set<String> acceptors = new HashSet<String>(nodes);
		acceptors.remove(self);

		long id = nextId;
		nextId++;
		
		for(String node : nodes) {
			outQueue.add(new DoAccept(self, node, id, acceptors, operation));
		}
		
		return id;
	}

}
