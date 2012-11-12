package edu.ucsb.cs.smanner.protocol;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountReceiver extends Protocol {
	private Logger log = LoggerFactory.getLogger(CountReceiver.class);
	
	int maxCount;
	int count;
	
	Queue<Message> outQueue = new LinkedList<Message>();

	public CountReceiver(int maxCount) {
		this.maxCount = maxCount;
		this.count = 0;
	}

	@Override
	public Message get() throws Exception {
		return null;
	}

	@Override
	public boolean hasMessage() {
		return false;
	}

	@Override
	public boolean isDone() {
		return (count >= maxCount);
	}

	public void put(Message message) throws Exception {
		log.debug("Received message {} from {}", count, message.getSource());
		count++;
	}

	public int getCount() {
		return count;
	}
}
