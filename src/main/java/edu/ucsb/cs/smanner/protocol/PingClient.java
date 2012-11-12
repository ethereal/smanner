package edu.ucsb.cs.smanner.protocol;

import java.util.ArrayList;
import java.util.List;

public class PingClient extends Protocol {

	long intervalInNs;
	long time;

	long seqnum;
	long lastTimestamp;

	List<Long> responses = new ArrayList<Long>();

	public PingClient(long intervalInNs) {
		this.intervalInNs = intervalInNs;
	}

	@Override
	public void put(Message message) throws Exception {
		if (message instanceof PongMessage) {
			PongMessage pong = (PongMessage) message;
			responses.add((int) pong.seqnum, time - pong.timestamp);
		} else {
			throw new Exception("unknown message");
		}
	}

	@Override
	public Message get() throws Exception {
		Message message = new PingMessage(self, null, seqnum, lastTimestamp); // FIXME null dest
		seqnum++;
		lastTimestamp = lastTimestamp + intervalInNs;
		return message;
	}

	@Override
	public boolean hasMessage() {
		return (lastTimestamp + intervalInNs <= time);
	}

	@Override
	public boolean isDone() {
		return false;
	}
}
