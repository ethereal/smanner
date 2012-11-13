package edu.ucsb.cs.smanner.net;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

public class TestUtil {
	public static void connect(Moderator a, Moderator b) throws IOException {
		PipedInputStream inAB = new PipedInputStream();
		PipedOutputStream outAB = new PipedOutputStream();
		PipedInputStream inBA = new PipedInputStream();
		PipedOutputStream outBA = new PipedOutputStream();
		
		inAB.connect(outBA);
		inBA.connect(outAB);
		
		a.addNode(b.self, inAB, outAB);
		b.addNode(a.self, inBA, outBA);
	}
	
	public static void connectAll(List<Moderator> moderators) throws IOException {
		for(int i=0; i<moderators.size(); i++) {
			for(int j=i; j<moderators.size(); j++) {
				connect(moderators.get(i), moderators.get(j));
			}
		}
	}
}
