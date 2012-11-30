package edu.ucsb.cs.smanner;

import edu.ucsb.cs.smanner.protocol.Operation;

public class WriteOperation extends Operation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2646098416145690550L;
	
	final String string;

	public WriteOperation(String id, String string) {
		super(id);
		this.string = string;
	}

	public String getString() {
		return string;
	}
	
}
