package edu.ucsb.cs.smanner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.net.TestUtil;
import edu.ucsb.cs.smanner.net.TransactionEndpoint;
import edu.ucsb.cs.smanner.net.TransactionState;
import edu.ucsb.cs.smanner.protocol.Operation;

public class Client {
	private static Logger log = LoggerFactory.getLogger(Client.class);

	AbstractApplicationContext context;
	
	TransactionEndpoint endpoint;
	
	ExecutorService executor;

	public static void main(String args[]) throws Exception {
		Client client = new Client();
		client.setUp();
		
		log.info("Client prepared.");
		client.execute(args);

		log.info("Client shutting down.");
		client.tearDown();
	}
	
	public void execute(String args[]) {
		String opId = UUID.randomUUID().toString();
		Map<String, Operation> operations = new HashMap<String, Operation>();
		
		if("read".equals(args[0])) {
			operations.put("tpcA", new ReadOperation(opId));
			operations.put("tpcB", new ReadOperation(opId));
		} else if("write".equals(args[0])) {
			operations.put("tpcA", new WriteOperation(opId, args[1]));
			operations.put("tpcB", new WriteOperation(opId, args[2]));
		} else {
			log.error("Unknown command");
			return;
		}

		long tid = endpoint.create(operations);
		
		try {
			while(endpoint.getState(tid) != TransactionState.COMMITTED) {
				Thread.sleep(100);
			}
			log.info("result A:\n{}", ((ReadOperationResult)endpoint.getResult(tid).get("tpcA")).getResult());
			log.info("result B:\n{}", ((ReadOperationResult)endpoint.getResult(tid).get("tpcB")).getResult());
		} catch (InterruptedException e) {
			log.warn("Interrupted. Exisiting.");
		}
	}
	
	public void setUp() throws Exception {
		log.trace("Client::setUp()");
		context = TestUtil.createContext("/META-INF/spring/client.xml");
		endpoint = (TransactionEndpoint)context.getBean("smanner");
	}

	public void tearDown() throws Exception {
		log.trace("Client::tearDown()");
		context.close();
	}

}
