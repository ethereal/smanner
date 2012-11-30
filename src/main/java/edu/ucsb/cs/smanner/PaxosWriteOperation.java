package edu.ucsb.cs.smanner;

import edu.ucsb.cs.smanner.protocol.Operation;

public class PaxosWriteOperation extends Operation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4104799222076149180L;
	
	final long transactionId;
	final String string;

	public PaxosWriteOperation(String id, long transactionId, String string) {
		super(id);
		this.transactionId = transactionId;
		this.string = string;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public String getString() {
		return string;
	}

}
