package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.protocol.Message;

public class TpcMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6927114703474133065L;
	final long id;
	
	public TpcMessage(String source, String destination, long id) {
		super(source, destination);
		this.id = id;
	}

	public long getId() {
		return id;
	}
}
