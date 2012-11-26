package edu.ucsb.cs.smanner.protocol.tpc;


public class DoCommitMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -382142840985402528L;

	public DoCommitMessage(String source, String destination, long id) {
		super(source, destination, id);
	}
}
