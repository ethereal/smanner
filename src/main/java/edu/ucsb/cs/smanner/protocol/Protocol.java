package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Message;

public interface Protocol {
	void put(Message message) throws Exception;
	Message get() throws Exception;
	boolean hasMessage();
	boolean isDone();
}
