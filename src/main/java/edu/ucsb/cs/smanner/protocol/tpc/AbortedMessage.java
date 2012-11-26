package edu.ucsb.cs.smanner.protocol.tpc;


public class AbortedMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2004270735053798839L;

	public AbortedMessage(String source, String destination, long id) {
		super(source, destination, id);
	}
}
