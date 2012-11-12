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
	final Set<Node> followers;
	
	TransactionState state;
	Set<Node> preparedFollowers = new HashSet<Node>();
	
	public Transaction(long id) {
		this.id = id;
		this.followers = null;
		this.state = TransactionState.NEW;
	}

	public Transaction(long id, Set<Node> followers) {
		this.id = id;
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
