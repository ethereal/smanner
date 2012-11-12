package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.net.Node;

public class CommitMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -382142840985402528L;

	public CommitMessage(Node source, Node destination, long id) {
		super(source, destination, id);
	}
}
