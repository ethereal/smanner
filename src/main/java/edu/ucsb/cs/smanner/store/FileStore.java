package edu.ucsb.cs.smanner.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStore {
	private static Logger log = LoggerFactory.getLogger(FileStore.class);
	
	final File file;
	
	public FileStore(File file) {
		this.file = file;
	}

	public void append(String line) throws Exception {
		FileWriter writer = new FileWriter(file, true);
		writer.append(line);
		writer.append("\n");
		writer.flush();
		writer.close();
	}
	
	public String read() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		
		String line = reader.readLine();
		while(line != null) {
			log.trace("appending line '{}'", line);
			sb.append(line);
			sb.append("\n");
			line = reader.readLine();
		}
		
		reader.close();
		
		log.debug("read '{}'", sb.toString());
		return sb.toString();
	}
}
