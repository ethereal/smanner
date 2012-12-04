package edu.ucsb.cs.smanner;

import edu.ucsb.cs.smanner.protocol.Operation;

public class PaxosReadOperation extends Operation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4104799222076149180L;
	
	final long transactionId;

	public PaxosReadOperation(String id, long transactionId) {
		super(id);
		this.transactionId = transactionId;
	}

	public long getTransactionId() {
		return transactionId;
	}

}
