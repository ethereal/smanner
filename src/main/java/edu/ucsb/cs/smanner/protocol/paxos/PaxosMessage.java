package edu.ucsb.cs.smanner.protocol.paxos;

import edu.ucsb.cs.smanner.net.Node;
import edu.ucsb.cs.smanner.protocol.Message;

public class PaxosMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5244090965463632299L;
	
	final long id;

	public PaxosMessage(Node source, Node destination, long id) {
		super(source, destination);
		this.id = id;
	}

}
