package edu.ucsb.cs.smanner.protocol.paxos;

import edu.ucsb.cs.smanner.net.Node;

public class AcceptMessage extends PaxosMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9180704783970898612L;

	public AcceptMessage(Node source, Node destination, long id) {
		super(source, destination, id);
	}

}
