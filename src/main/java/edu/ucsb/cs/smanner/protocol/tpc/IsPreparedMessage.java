package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.net.Node;

public class IsPreparedMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2004270735053798839L;

	public IsPreparedMessage(Node source, Node destination, long id) {
		super(source, destination, id);
	}
}
