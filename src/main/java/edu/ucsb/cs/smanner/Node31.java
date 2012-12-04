package edu.ucsb.cs.smanner;

public class Node31 extends AbstractFollower {

	public static void main(String[] args) throws Exception {
		new Node31("/META-INF/spring/node31.xml", "grades.txt").runMain("Node31", args);
	}

	public Node31(String localConfig, String fileName) {
		super(localConfig, fileName);
	}

}
