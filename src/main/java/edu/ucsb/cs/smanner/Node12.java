package edu.ucsb.cs.smanner;

public class Node12 extends AbstractFollower {

	public static void main(String[] args) throws Exception {
		new Node12("/META-INF/spring/node12.xml", "stats.txt").runMain("Node12", args);
	}

	public Node12(String localConfig, String fileName) {
		super(localConfig, fileName);
	}

}
