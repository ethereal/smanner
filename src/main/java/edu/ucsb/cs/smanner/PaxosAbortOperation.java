package edu.ucsb.cs.smanner;

import edu.ucsb.cs.smanner.protocol.Operation;

public class PaxosAbortOperation extends Operation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4104799222076149180L;
	
	final long transactionId;

	public PaxosAbortOperation(String id, long transactionId) {
		super(id);
		this.transactionId = transactionId;
	}

	public long getTransactionId() {
		return transactionId;
	}

}
