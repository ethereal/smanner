package edu.ucsb.cs.smanner.protocol;

import java.util.Set;

import edu.ucsb.cs.smanner.net.Node;

public abstract class Protocol {
	long time;
	Set<Node> nodes;
	Node self;
	
	public abstract void put(Message message) throws Exception;
	public abstract Message get() throws Exception;
	public abstract boolean hasMessage();
	public abstract boolean isDone();
	
	public final void setTime(long time) {
		this.time = time;
	}
	
	public final void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}
	
	public final void setSelf(Node self) {
		this.self = self;
	}
}
