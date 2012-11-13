package edu.ucsb.cs.smanner.protocol;

import java.io.Serializable;

import edu.ucsb.cs.smanner.net.Node;

public class Message implements Serializable {
	/**
	 * SerialVersionUID 
	 */
	private static final long serialVersionUID = 1151213061714710185L;
	
	final Node source;
	final Node destination;

	public Message(Node source, Node destination) {
		this.source = source;
		this.destination = destination;
	}

	public Node getSource() {
		return source;
	}

	public Node getDestination() {
		return destination;
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s->%s)", getClass().getSimpleName(), source, destination);
	}
}
