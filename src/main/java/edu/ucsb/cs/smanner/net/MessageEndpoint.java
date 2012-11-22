package edu.ucsb.cs.smanner.net;

import edu.ucsb.cs.smanner.protocol.Message;


public interface MessageEndpoint {
	void put(Message message) throws Exception;
	String getIdentifier();
}
