package edu.ucsb.cs.smanner;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.net.MessageEndpoint;
import edu.ucsb.cs.smanner.net.Moderator;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.store.FileStore;

public abstract class AbstractFollower {
	private static Logger log = LoggerFactory.getLogger(AbstractFollower.class);

	AbstractApplicationContext context;
	AbstractApplicationContext contextConfig;

	PaxosFollower follower;

	FileStore store;
	
	public AbstractFollower(String localConfig, String fileName) {
		context = NodeTool.createContext(localConfig);
		store = new FileStore(new File(fileName));
	}

	public void runMain(String nodeId, String args[]) throws Exception {
		log.info("Node {} starting.", nodeId);
		setUp();
		
		log.info("Node prepared, press enter to run.");
		NodeTool.readLine();
		run();

		log.info("Node running, press enter to stop.");
		NodeTool.readLine();
		tearDown();
	}
	
	public void setUp() throws Exception {
		log.trace("AbstractFollower::setUp()");
		
		follower = (PaxosFollower) context.getBeansOfType(PaxosFollower.class).values().iterator().next();
		follower.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosWriteOperation) {
					log.debug("write");
					// commit
					try {
						store.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
				}
			}
		});
	}

	public void run() {
		log.trace("AbstractFollower::run()");
		// set up servers
		contextConfig = NodeTool.createContext("/META-INF/spring/nodes.xml");
		setupModerator(context);
	}
	
	public void tearDown() throws Exception {
		log.trace("AbstractFollower::tearDown()");
		context.close();
		contextConfig.close();
	}

	void setupModerator(ApplicationContext context) {
		log.trace("AbstractFollower::setupModerator()");
		Moderator moderator = (Moderator) context.getBeansOfType(Moderator.class).values().iterator().next();
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(moderator.getGroupName());
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

}
