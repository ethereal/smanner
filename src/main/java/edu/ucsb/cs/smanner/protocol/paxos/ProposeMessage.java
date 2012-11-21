package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.Set;

public class ProposeMessage extends PaxosMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5301232227010536003L;
	
	final Set<String> acceptors;
	
	public ProposeMessage(String source, String destination, long id, Set<String> acceptors) {
		super(source, destination, id);
		this.acceptors = acceptors;
	}

	public Set<String> getAcceptors() {
		return acceptors;
	}

}
