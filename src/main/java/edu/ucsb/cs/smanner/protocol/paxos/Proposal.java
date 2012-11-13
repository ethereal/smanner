package edu.ucsb.cs.smanner.protocol.paxos;

import java.util.HashSet;
import java.util.Set;

import edu.ucsb.cs.smanner.net.Node;


public class Proposal {
	public enum ProposalState {
		NEW,
		ACCEPTED
	}
	
	final long id;
	final Set<Node> acceptors;
	
	ProposalState state;
	Set<Node> acceptedNodes = new HashSet<Node>();

	public Proposal(long id, Set<Node> acceptors) {
		this.id = id;
		this.acceptors = acceptors;
		this.state = ProposalState.NEW;
	}
	
	public void accept(Node acceptor) {
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

	public Set<Node> getAcceptedNodes() {
		return acceptedNodes;
	}

	public void setAcceptedNodes(Set<Node> acceptedNodes) {
		this.acceptedNodes = acceptedNodes;
	}

	public long getId() {
		return id;
	}

	public Set<Node> getAcceptors() {
		return acceptors;
	}
	
	
	
}
