package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.Set;

import edu.ucsb.cs.smanner.net.Node;

public class ProposeMessage extends PaxosMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5301232227010536003L;
	
	final Set<Node> acceptors;
	
	public ProposeMessage(Node source, Node destination, long id, Set<Node> acceptors) {
		super(source, destination, id);
		this.acceptors = acceptors;
	}

	public Set<Node> getAcceptors() {
		return acceptors;
	}

}
