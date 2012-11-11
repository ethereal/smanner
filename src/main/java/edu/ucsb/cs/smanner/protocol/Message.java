package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class Message {
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
}
