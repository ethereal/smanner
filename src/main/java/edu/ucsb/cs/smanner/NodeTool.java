package edu.ucsb.cs.smanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class NodeTool {
	
	private NodeTool() {
		// left blank
	}
	
	public static String readLine() {
		try {
			return new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			return null;
		}
	}

	public static AbstractApplicationContext createContext(String resName) {
		GenericXmlApplicationContext context = new GenericXmlApplicationContext();
		context.setValidating(true);
		context.load(resName);
		context.refresh();
		return context;
	}
	
}
