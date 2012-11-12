package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class PingMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1829715387440866671L;

	final long seqnum;
	final long timestamp;

	public PingMessage(Node sender, Node receiver, long seqnum, long timestamp) {
		super(sender, receiver);
		this.seqnum = seqnum;
		this.timestamp = timestamp;
	}
}
