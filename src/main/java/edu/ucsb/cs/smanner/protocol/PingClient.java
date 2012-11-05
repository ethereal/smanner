package edu.ucsb.cs.smanner.protocol;

import java.util.ArrayList;
import java.util.List;

import edu.ucsb.cs.smanner.net.Node;

public class PingClient implements Protocol {
	
	final static long INTERVAL = 1000000000;
	
	Node self;
	Node dest;

	long seqnum;
	long lastTimestamp;
	
	List<Long> responses = new ArrayList<Long>();
	
	@Override
	public void put(Message message) throws Exception {
		if(message instanceof PongMessage) {
			PongMessage pong = (PongMessage)message;
			responses.add((int)pong.seqnum, System.nanoTime() - pong.timestamp);
		} else {
			throw new Exception("unknown message");
		}
	}

	@Override
	public Message get() throws Exception {
		if(lastTimestamp + INTERVAL <= System.nanoTime()) {
			seqnum++;
			lastTimestamp = lastTimestamp + INTERVAL;
			return new PingMessage(self, dest, seqnum, lastTimestamp);
		}
		return null;
	}

	@Override
	public boolean hasMessage() {
		return (lastTimestamp + INTERVAL <= System.nanoTime());
	}

	@Override
	public boolean isDone() {
		return false;
	}

}
