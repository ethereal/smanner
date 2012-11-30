package edu.ucsb.cs.smanner.protocol.tpc;

import java.util.Map;

import edu.ucsb.cs.smanner.protocol.OperationResult;

public class ClientResponseMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -20042723478798839L;

	final Map<String, OperationResult> results;
	final boolean successful;

	public ClientResponseMessage(String source, String destination, long id, Map<String, OperationResult> results) {
		super(source, destination, id);
		this.results = results;
		this.successful = true;
	}

	public ClientResponseMessage(String source, String destination, long id, boolean successful) {
		super(source, destination, id);
		this.results = null;
		this.successful = successful;
	}

	public Map<String, OperationResult> getResults() {
		return results;
	}

	public boolean isSuccessful() {
		return successful;
	}

}
