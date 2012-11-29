package edu.ucsb.cs.smanner.net;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.OperationResult;

public class StringOperationResult extends OperationResult {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3905953866334915306L;

	final String string;
	
	public StringOperationResult(Operation operation, String string) {
		super(operation);
		this.string = string;
	}

	public String getString() {
		return string;
	}

}
