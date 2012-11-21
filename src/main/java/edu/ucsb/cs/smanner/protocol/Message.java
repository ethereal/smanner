package edu.ucsb.cs.smanner.protocol;

import java.io.Serializable;

public class Message implements Serializable {
	/**
	 * SerialVersionUID 
	 */
	private static final long serialVersionUID = 1151213061714710185L;
	
	final String source;
	final String destination;

	public Message(String source, String destination) {
		this.source = source;
		this.destination = destination;
	}

	public String getSource() {
		return source;
	}

	public String getDestination() {
		return destination;
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s->%s)", getClass().getSimpleName(), source, destination);
	}
}
