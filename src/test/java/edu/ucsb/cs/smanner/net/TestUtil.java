package edu.ucsb.cs.smanner.net;

import java.io.IOException;
import java.util.List;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestUtil {
	public static void connect(Moderator a, Moderator b) throws IOException {
		a.addNode(b);
		b.addNode(a);
	}

	public static void connectAll(List<Moderator> moderators)
			throws IOException {
		for (int i = 0; i < moderators.size(); i++) {
			for (int j = i; j < moderators.size(); j++) {
				connect(moderators.get(i), moderators.get(j));
			}
		}
	}

	public static AbstractApplicationContext createContext(String resName) {
		return new ClassPathXmlApplicationContext(resName);
	}
}
