package edu.ucsb.cs.smanner.store;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileStoreTest {

	final File file = new File("fileStoreTest.txt");
	
	FileStore store;
	
	@Before
	public void setUp() throws Exception {
		FileWriter writer = new FileWriter(file);
		writer.append("Hello\n");
		writer.append("World\n");
		writer.flush();
		writer.close();
		
		store = new FileStore(file);
	}

	@After
	public void tearDown() throws Exception {
		file.delete();
	}

	@Test
	public void testRead()throws Exception {
		assertEquals("Hello\nWorld\n", store.read());
	}

	@Test
	public void testAppend()throws Exception {
		store.append("I'm fine");
		store.append("ey!");
		assertEquals("Hello\nWorld\nI'm fine\ney!\n", store.read());
	}

}
