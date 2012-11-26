package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashSet;
import java.util.Set;

import edu.ucsb.cs.smanner.protocol.Operation;


public class Proposal {
	public enum ProposalState {
		NEW,
		ACCEPTED
	}
	
	final long id;
	final Set<String> acceptors;
	
	ProposalState state;
	Set<String> acceptedNodes = new HashSet<String>();
	
	Operation operation;

	public Proposal(long id, Set<String> acceptors) {
		this.id = id;
		this.acceptors = acceptors;
		this.state = ProposalState.NEW;
	}
	
	public void accept(String acceptor) {
		if(acceptors.contains(acceptor))
			acceptedNodes.add(acceptor);
		
		if(acceptedNodes.size() > acceptors.size() / 2) {
			state = ProposalState.ACCEPTED;
		}
	}

	public ProposalState getState() {
		return state;
	}

	public void setState(ProposalState state) {
		this.state = state;
	}

	public Set<String> getAcceptedNodes() {
		return acceptedNodes;
	}

	public void setAcceptedNodes(Set<String> acceptedNodes) {
		this.acceptedNodes = acceptedNodes;
	}

	public long getId() {
		return id;
	}

	public Set<String> getAcceptors() {
		return acceptors;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	
}
