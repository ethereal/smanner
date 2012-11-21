package edu.ucsb.cs.smanner.protocol.tpc;


public class CommitMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -382142840985402528L;

	public CommitMessage(String source, String destination, long id) {
		super(source, destination, id);
	}
}
