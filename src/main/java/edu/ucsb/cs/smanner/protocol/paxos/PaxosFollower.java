package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.AbstractProtocol;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal.ProposalState;

public class PaxosFollower extends AbstractProtocol {
	private static Logger log = LoggerFactory.getLogger(PaxosFollower.class);
	
	volatile boolean active = true;
	
	Map<Long, Proposal> proposals = new HashMap<Long, Proposal>();

	Queue<Message> outQueue = new LinkedList<Message>();
	Queue<AcceptMessage> acceptQueue = new LinkedList<AcceptMessage>();
	
	Collection<ProposalListener> listeners = new ArrayList<ProposalListener>();
	
	@Override
	public void put(Message message) throws Exception {
		if(message instanceof DoAccept) {
			handlePropose((DoAccept)message);
		} else if (message instanceof AcceptMessage) {
			handleAccept((AcceptMessage)message);
		} else {
			throw new Exception(String.format("Unknown message type %s", message.getClass()));
		}
	}
	
	void handlePropose(DoAccept msg) throws Exception {
		if(proposals.containsKey(msg.id))
			throw new Exception(String.format("Proposal %d already exists", msg.id));
		
		Proposal p = new Proposal(msg.id, msg.acceptors);
		p.setOperation(msg.operation);
		proposals.put(msg.id, p);
		
		log.debug("{}: voting for proposal {}", self, p.id);
		for(String node : nodes) {
			outQueue.add(new AcceptMessage(self, node, p.id));
		}
		
		handleQueuedAccepts();
	}
	
	void handleAccept(AcceptMessage msg) {
		if(! proposals.containsKey(msg.id)) {
			log.warn("{}: Proposal {} does not exists", self, msg.id);
			acceptQueue.add(msg);
			return;
		}
		
		Proposal p = proposals.get(msg.id);
		if(p.getState() == ProposalState.ACCEPTED)
			return;
		
		p.accept(msg.getSource());
		
		if(p.getState() == ProposalState.ACCEPTED) {
			log.debug("{}: accepted proposal {}", self, p.id);
			notifyListeners(p.id, p.operation);
		}
	}
	
	private void handleQueuedAccepts() {
		Iterator<AcceptMessage> it = acceptQueue.iterator();
		acceptQueue = new LinkedList<AcceptMessage>();
		
		while(it.hasNext()) {
			AcceptMessage msg = it.next();
			log.debug("{}: handling queued request {}", self, msg);
			handleAccept(msg);
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

	public Proposal getProposal(long id) {
		return proposals.get(id);
	}
	
	public void addListener(ProposalListener listener) {
		listeners.add(listener);
	}
	
	void notifyListeners(long id, Operation operation) {
		for(ProposalListener l : listeners) {
			l.notify(id, operation);
		}
	}
}
