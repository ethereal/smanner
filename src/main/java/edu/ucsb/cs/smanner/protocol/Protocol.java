package edu.ucsb.cs.smanner.protocol;


public interface Protocol {
	void put(Message message) throws Exception;
	Message get() throws Exception;
	boolean hasMessage();
	boolean isDone();
}
