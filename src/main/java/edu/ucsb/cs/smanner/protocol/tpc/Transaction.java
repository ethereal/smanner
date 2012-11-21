package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashSet;
import java.util.Set;

public class Transaction {
	public enum TransactionState {
		NEW,
		PREPARED,
		COMMITTED,
		ABORTED
	}
	
	final long id;
	final String coordinator;
	final Set<String> followers;
	
	TransactionState state;
	Set<String> preparedFollowers = new HashSet<String>();
	
	public Transaction(long id, String coordinator) {
		this.id = id;
		this.coordinator = coordinator;
		this.followers = null;
		this.state = TransactionState.NEW;
	}

	public Transaction(long id, String coordinator, Set<String> followers) {
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

	public void prepare(String follower) {
		if(state != TransactionState.NEW)
			throw new IllegalStateException("Transaction must be in state NEW");
		
		preparedFollowers.add(follower);
		if(preparedFollowers.containsAll(followers)) {
			state = TransactionState.PREPARED;
		}
	}
}
