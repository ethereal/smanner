package edu.ucsb.cs.smanner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

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
		
		log.info("Client running.");
		client.execute(args);

		log.info("Client shutting down.");
		client.tearDown();
	}
	
	public void execute(String args[]) {
		if("read".equals(args[0])) {
			String opId = UUID.randomUUID().toString();
			Map<String, Operation> operations = new HashMap<String, Operation>();
			operations.put("tpcA", new ReadOperation(opId));
			operations.put("tpcB", new ReadOperation(opId));
			runTransaction(operations);
		} else if("write".equals(args[0])) {
			String opId = UUID.randomUUID().toString();
			Map<String, Operation> operations = new HashMap<String, Operation>();
			operations.put("tpcA", new WriteOperation(opId, args[1]));
			operations.put("tpcB", new WriteOperation(opId, args[2]));
			runTransaction(operations);
		}  else if("multi".equals(args[0])) {
			int runs = Integer.valueOf(args[1]);
			for(int i=0; i<runs; i++) {
				String opId = UUID.randomUUID().toString();
				Map<String, Operation> operations = new HashMap<String, Operation>();
				operations.put("tpcA", new WriteOperation(opId, args[2] + i));
				operations.put("tpcB", new WriteOperation(opId, args[3] + i));
				runTransaction(operations);
			}
		} else {
			log.error("Unknown command");
			return;
		}
	}
	
	void runTransaction(Map<String, Operation> operations) {
		long tid = endpoint.create(operations);
		
		try {
			TransactionState state = endpoint.getState(tid);
			while(state != TransactionState.COMMITTED) {
				// retry externally
				if(state == TransactionState.ABORTED) {
					log.info("transaction {} failed. Retrying.", tid);
					tid = endpoint.create(operations);
				}
				
				Thread.sleep(100);
				state = endpoint.getState(tid);
			}

			log.info("transaction {} completed with state {}", tid, state);
			if(state == TransactionState.COMMITTED && endpoint.getResult(tid).get("tpcA") instanceof ReadOperationResult) {
				String a[] = ((ReadOperationResult)endpoint.getResult(tid).get("tpcA")).getResult().split("\n");
				String b[] = ((ReadOperationResult)endpoint.getResult(tid).get("tpcB")).getResult().split("\n");
				
				StringBuilder sb = new StringBuilder();
				for(int i=0; i<a.length; i++) {
					sb.append(a[i]);
					sb.append('\t');
					sb.append(b[i]);
					sb.append('\n');
				}
				
				log.info("result:\n{}", sb);
			}
		} catch (InterruptedException e) {
			log.warn("Interrupted. Exisiting.");
		}
	}
	
	public void setUp() throws Exception {
		log.trace("Client::setUp()");
		context = NodeTool.createContext("/META-INF/spring/awsclient.xml");
		endpoint = (TransactionEndpoint)context.getBean("smanner");
	}

	public void tearDown() throws Exception {
		log.trace("Client::tearDown()");
		context.close();
	}

}
