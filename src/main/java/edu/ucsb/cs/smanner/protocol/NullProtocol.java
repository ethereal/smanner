package edu.ucsb.cs.smanner.protocol;

public class NullProtocol implements Protocol {
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
		return false;
	}

	public void put(Message message) throws Exception {
		// ignore
	}

	@Override
	public void setTime(long time) {
		// ignore
	}
}
