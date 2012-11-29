package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.OperationResult;

public class Transaction {
	public enum TransactionState {
		NEW, PREPARED, COMMITTED, ABORTED
	}

	final long id;
	final String coordinator;
	final Set<String> followers;

	TransactionState state;
	Set<String> preparedFollowers = new HashSet<String>();
	Set<String> committedFollowers = new HashSet<String>();

	Map<String, Operation> operations = new HashMap<String, Operation>();
	Map<String, OperationResult> results = new HashMap<String, OperationResult>();

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
		if (state != TransactionState.NEW)
			throw new IllegalStateException("Transaction must be in state NEW");

		preparedFollowers.add(follower);
		if (preparedFollowers.containsAll(followers)) {
			state = TransactionState.PREPARED;
		}
	}

	public void commit(String follower, OperationResult result) {
		if (state != TransactionState.PREPARED)
			throw new IllegalStateException("Transaction must be in state PREPARED");

		committedFollowers.add(follower);
		results.put(follower, result);
		
		if (committedFollowers.containsAll(followers)) {
			state = TransactionState.COMMITTED;
		}
	}
	
	public void abort() {
		state = TransactionState.ABORTED;
	}

	public Map<String, Operation> getOperations() {
		return operations;
	}

	public void setOperations(Map<String, Operation> operations) {
		this.operations = operations;
	}
	
	public Map<String, OperationResult> getResults() {
		return results;
	}

	public void setResults(Map<String, OperationResult> results) {
		this.results = results;
	}

	public void setResult(String node, OperationResult result) {
		results.put(node, result);
	}

	public void setOperation(String node, Operation operation) {
		operations.put(node, operation);
	}

	public Operation getOperation(String node) {
		return operations.get(node);
	}

	public Object getResult(String node) {
		return results.get(node);
	}

}
