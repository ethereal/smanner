package edu.ucsb.cs.smanner.protocol;

import java.util.ArrayList;
import java.util.List;

import edu.ucsb.cs.smanner.net.Node;

public class PingClient implements Protocol {
	
	Node self;
	Node dest;
	long intervalInNs;
	
	long seqnum;
	long lastTimestamp;
	
	List<Long> responses = new ArrayList<Long>();
	
	public PingClient(Node self, Node dest, long intervalInNs) {
		this.self = self;
		this.dest = dest;
		this.intervalInNs = intervalInNs;
	}

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
		Message message = new PingMessage(self, dest, seqnum, lastTimestamp);
		seqnum++;
		lastTimestamp = lastTimestamp + intervalInNs;
		return message;
	}

	@Override
	public boolean hasMessage() {
		return (lastTimestamp + intervalInNs <= System.nanoTime());
	}

	@Override
	public boolean isDone() {
		return false;
	}

}
