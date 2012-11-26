package edu.ucsb.cs.smanner.protocol;

import java.io.Serializable;

public abstract class Operation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3098044702929684256L;

	final String id;

	public Operation(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
