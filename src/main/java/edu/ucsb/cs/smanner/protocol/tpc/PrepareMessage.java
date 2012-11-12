package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.net.Node;

public class PrepareMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3862420759509617358L;

	public PrepareMessage(Node source, Node destination, long id) {
		super(source, destination, id);
	}
}
