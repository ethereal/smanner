package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class CountProtocol implements Protocol {
	Node self;
	Node destination;
	int index;
	int maxCount;

	public CountProtocol(Node self, Node destination, int maxCount) {
		this.self = self;
		this.destination = destination;
		this.index = 0;
		this.maxCount = maxCount;
	}

	@Override
	public Message get() throws Exception {
		Message message = new CountMessage(self, destination, index);
		index++;
		return message;
	}

	@Override
	public boolean hasMessage() {
		return (index < maxCount);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	public void put(Message message) throws Exception {
		// ignore
	}
}
