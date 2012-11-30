package edu.ucsb.cs.smanner;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.OperationResult;

public class ReadOperationResult extends OperationResult {
	/**
	 * 
	 */
	private static final long serialVersionUID = 627886925025870369L;
	
	final String result;

	public ReadOperationResult(Operation operation, String result) {
		super(operation);
		this.result = result;
	}

	public String getResult() {
		return result;
	}

}
