package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.net.Node;
import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Protocol;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal.ProposalState;

public class PaxosFollower extends Protocol {
	private static Logger log = LoggerFactory.getLogger(PaxosFollower.class);
	
	volatile boolean active = true;
	
	Map<Long, Proposal> proposals = new HashMap<Long, Proposal>();

	Queue<Message> outQueue = new LinkedList<Message>();
	Queue<AcceptMessage> acceptQueue = new LinkedList<AcceptMessage>();
	
	@Override
	public void put(Message message) throws Exception {
		if(message instanceof ProposeMessage) {
			handlePropose((ProposeMessage)message);
		} else if (message instanceof AcceptMessage) {
			handleAccept((AcceptMessage)message);
		} else {
			throw new Exception(String.format("Unknown message type %s", message.getClass()));
		}
	}
	
	void handlePropose(ProposeMessage msg) throws Exception {
		if(proposals.containsKey(msg.id))
			throw new Exception(String.format("Proposal %d already exists", msg.id));
		
		Proposal p = new Proposal(msg.id, msg.acceptors);
		proposals.put(msg.id, p);
		
		log.debug("{}: voting for proposal {}", self, p.id);
		for(Node node : p.acceptors) {
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
		p.accept(msg.getSource());
		
		if(p.getState() == ProposalState.ACCEPTED) {
			log.debug("accepted proposal {}", p.id);
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
}