package edu.ucsb.cs.smanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UITool {
	
	private UITool() {
		// left blank
	}
	
	public static void sleep(long timeMillis) {
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() < start + timeMillis) {
			try {
				Thread.sleep(timeMillis);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	public static String readLine() {
		try {
			return new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			return null;
		}
	}

}
