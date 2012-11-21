package edu.ucsb.cs.smanner.protocol;


public interface MessageEndpoint {
	void put(Message message) throws Exception;
	String getIdentifier();
}
