package edu.ucsb.cs.smanner.protocol.paxos;


public class AcceptMessage extends PaxosMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9180704783970898612L;

	public AcceptMessage(String source, String destination, long id) {
		super(source, destination, id);
	}

}
