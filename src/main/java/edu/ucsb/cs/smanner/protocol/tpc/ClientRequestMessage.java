package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.Map;

import edu.ucsb.cs.smanner.protocol.Operation;


public class ClientRequestMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3862456059509617358L;

	final Map<String, Operation> operations;
	
	public ClientRequestMessage(String source, String destination, long id, Map<String, Operation> operations) {
		super(source, destination, id);
		this.operations = operations;
	}

	public Map<String, Operation> getOperations() {
		return operations;
	}
}
