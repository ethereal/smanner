package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashSet;
import java.util.Set;

import edu.ucsb.cs.smanner.net.Node;

public class Transaction {
	public enum TransactionState {
		NEW,
		PREPARED,
		COMMITTED,
		ABORTED
	}
	
	final long id;
	final Node coordinator;
	final Set<Node> followers;
	
	TransactionState state;
	Set<Node> preparedFollowers = new HashSet<Node>();
	
	public Transaction(long id, Node coordinator) {
		this.id = id;
		this.coordinator = coordinator;
		this.followers = null;
		this.state = TransactionState.NEW;
	}

	public Transaction(long id, Node coordinator, Set<Node> followers) {
		this.id = id;
		this.coordinator = coordinator;
		this.followers = followers;
		this.state = TransactionState.NEW;
	}

	public TransactionState getState() {
		return state;
	}

	public void setState(TransactionState state) {
		this.state = state;
	}

	public long getId() {
		return id;
	}

	public void prepare(Node follower) {
		if(state != TransactionState.NEW)
			throw new IllegalStateException("Transaction must be in state NEW");
		
		preparedFollowers.add(follower);
		if(preparedFollowers.containsAll(followers)) {
			state = TransactionState.PREPARED;
		}
	}
}
