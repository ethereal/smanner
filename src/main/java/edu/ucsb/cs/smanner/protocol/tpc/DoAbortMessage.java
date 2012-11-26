package edu.ucsb.cs.smanner.protocol.tpc;


public class DoAbortMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3862420759509617358L;

	public DoAbortMessage(String source, String destination, long id) {
		super(source, destination, id);
	}
}
