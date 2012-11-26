package edu.ucsb.cs.smanner.store;

public class LockManager {
	String operationId;
	
	public void lock(String operationId) throws Exception {
		if(this.operationId != null)
			throw new Exception(String.format("Lock is held by %s already", this.operationId));
		this.operationId = operationId;
	}
	
	public boolean hasLock(String operationId) {
		return (operationId.equals(this.operationId));
	}
	
	public void unlock() {
		this.operationId = null;
	}

}
