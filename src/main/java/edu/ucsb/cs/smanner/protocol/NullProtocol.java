package edu.ucsb.cs.smanner.protocol;

public class NullProtocol extends AbstractProtocol {
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
}
