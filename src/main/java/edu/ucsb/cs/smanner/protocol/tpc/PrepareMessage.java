package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.protocol.Operation;


public class PrepareMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3862420759509617358L;

	final Operation operation;
	
	public PrepareMessage(String source, String destination, long id, Operation operation) {
		super(source, destination, id);
		this.operation = operation;
	}

	public Operation getOperation() {
		return operation;
	}
}
