package edu.ucsb.cs.smanner;

public class Node32 extends AbstractFollower {

	public static void main(String[] args) throws Exception {
		new Node32("/META-INF/spring/node32.xml", "stats.txt").runMain("Node32", args);
	}

	public Node32(String localConfig, String fileName) {
		super(localConfig, fileName);
	}

}
