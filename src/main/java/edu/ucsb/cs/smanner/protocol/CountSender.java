package edu.ucsb.cs.smanner.protocol;

import java.util.LinkedList;
import java.util.Queue;

import edu.ucsb.cs.smanner.net.Node;

public class CountSender extends Protocol {
	int index;
	int maxCount;
	long intervalInNs;
	
	long lastTime;
	
	Queue<Message> outQueue = new LinkedList<Message>();

	public CountSender(int maxCount, long intervalInNs) {
		this.index = 0;
		this.maxCount = maxCount;
		this.intervalInNs = intervalInNs;
		this.lastTime = -intervalInNs;
	}

	@Override
	public Message get() throws Exception {
		if(outQueue.isEmpty()) {
			for(Node node : nodes) {
				outQueue.add(new CountMessage(self, node, index));
			}
			lastTime += intervalInNs;
			index++;
		}
		return outQueue.poll();
	}

	@Override
	public boolean hasMessage() {
		return (!outQueue.isEmpty()) || ((index < maxCount) && (lastTime + intervalInNs <= time));
	}

	@Override
	public boolean isDone() {
		return outQueue.isEmpty() && (index >= maxCount);
	}

	public void put(Message message) throws Exception {
		// ignore
	}
}
