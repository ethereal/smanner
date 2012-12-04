package edu.ucsb.cs.smanner;

public class Node21 extends AbstractFollower {

	public static void main(String[] args) throws Exception {
		new Node21("/META-INF/spring/node21.xml", "grades.txt").runMain("Node21", args);
	}

	public Node21(String localConfig, String fileName) {
		super(localConfig, fileName);
	}

}
