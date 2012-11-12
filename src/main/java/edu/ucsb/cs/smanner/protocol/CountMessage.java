package edu.ucsb.cs.smanner.protocol;

import edu.ucsb.cs.smanner.net.Node;

public class CountMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4742755615382316699L;

	final int index;

	public CountMessage(Node source, Node destination, int index) {
		super(source, destination);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

}
