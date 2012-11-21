package edu.ucsb.cs.smanner.protocol.tpc;


public class IsPreparedMessage extends TpcMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2004270735053798839L;

	public IsPreparedMessage(String source, String destination, long id) {
		super(source, destination, id);
	}
}
