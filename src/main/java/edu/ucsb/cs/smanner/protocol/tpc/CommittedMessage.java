package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.protocol.OperationResult;

public class CommittedMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -200427074678798839L;

	final OperationResult result;

	public CommittedMessage(String source, String destination, long id,	OperationResult result) {
		super(source, destination, id);
		this.result = result;
	}

	public OperationResult getResult() {
		return result;
	}

}
