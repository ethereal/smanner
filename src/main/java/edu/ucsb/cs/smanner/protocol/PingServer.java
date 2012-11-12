package edu.ucsb.cs.smanner.protocol;

import java.util.LinkedList;
import java.util.Queue;

public class PingServer extends Protocol {

	Queue<Message> outgoing = new LinkedList<Message>();

	public void put(Message message) throws Exception {
		if (message instanceof PingMessage) {
			PingMessage ping = (PingMessage) message;
			outgoing.add(new PongMessage(self, message.source, ping.seqnum, ping.timestamp));
		} else {
			throw new Exception("unknown message");
		}
	}

	@Override
	public Message get() throws Exception {
		return outgoing.remove();
	}

	@Override
	public boolean hasMessage() {
		return !outgoing.isEmpty();
	}

	@Override
	public boolean isDone() {
		return false;
	}
}
