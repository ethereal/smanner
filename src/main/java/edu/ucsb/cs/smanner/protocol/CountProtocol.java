package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class CountProtocol implements Protocol {
	Node self;
	Node destination;
	int index;
	int maxCount;
	long intervalInNs;
	
	long time;
	long lastTime;

	public CountProtocol(Node self, Node destination, int maxCount, long intervalInNs) {
		this.self = self;
		this.destination = destination;
		this.index = 0;
		this.maxCount = maxCount;
		this.intervalInNs = intervalInNs;
		this.time = 0;
		this.lastTime = -intervalInNs;
	}

	@Override
	public Message get() throws Exception {
		Message message = new CountMessage(self, destination, index);
		lastTime += intervalInNs;
		index++;
		return message;
	}

	@Override
	public boolean hasMessage() {
		return (index < maxCount) && (lastTime + intervalInNs <= time);
	}

	@Override
	public boolean isDone() {
		return (index >= maxCount);
	}

	public void put(Message message) throws Exception {
		// ignore
	}

	@Override
	public void setTime(long time) {
		this.time = time;
	}
}
