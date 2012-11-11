package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class CountMessage extends Message {
	final int index;

	public CountMessage(Node source, Node destination, int index) {
		super(source, destination);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

}
