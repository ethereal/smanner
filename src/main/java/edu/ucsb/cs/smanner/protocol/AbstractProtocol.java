package edu.ucsb.cs.smanner.protocol;

import java.util.Set;

public abstract class AbstractProtocol implements Protocol {
	protected long time;
	protected Set<String> nodes;
	protected String self;
	
	@Override
	public final void setTime(long time) {
		this.time = time;
	}
	
	@Override
	public final void setNodes(Set<String> nodes) {
		this.nodes = nodes;
	}
	
	@Override
	public final void setSelf(String self) {
		this.self = self;
	}
}
