package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.Set;

import edu.ucsb.cs.smanner.protocol.Operation;

public class ProposeMessage extends PaxosMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5301232227010536003L;

	final Set<String> acceptors;
	final Operation operation;

	public ProposeMessage(String source, String destination, long id, Set<String> acceptors, Operation operation) {
		super(source, destination, id);
		this.acceptors = acceptors;
		this.operation = operation;
	}

	public Set<String> getAcceptors() {
		return acceptors;
	}

	public Operation getOperation() {
		return operation;
	}

}
