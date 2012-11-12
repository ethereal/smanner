package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class PongMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7453390053837726024L;
	
	final long seqnum;
	final long timestamp;

	public PongMessage(Node sender, Node receiver, long seqnum, long timestamp) {
		super(sender, receiver);
		this.seqnum = seqnum;
		this.timestamp = timestamp;
	}
}
