package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class Message {
	final Node sender;
	final Node receiver;
	public Message(Node sender, Node receiver) {
		this.sender = sender;
		this.receiver = receiver;
	}
}
