package edu.ucsb.cs.smanner.protocol;

import java.util.Set;

public interface Protocol {

	public abstract void put(Message message) throws Exception;

	public abstract Message get() throws Exception;

	public abstract boolean hasMessage();

	public abstract boolean isDone();

	public abstract void setTime(long time);

	public abstract void setNodes(Set<String> nodes);

	public abstract void setSelf(String self);

}