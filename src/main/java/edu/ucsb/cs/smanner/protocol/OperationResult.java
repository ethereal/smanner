package edu.ucsb.cs.smanner.protocol;

import java.io.Serializable;

public abstract class OperationResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5238099081995175797L;
	
	final Operation operation;

	public OperationResult(Operation operation) {
		this.operation = operation;
	}
}
