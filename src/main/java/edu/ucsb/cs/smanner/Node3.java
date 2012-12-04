package edu.ucsb.cs.smanner;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.net.MessageEndpoint;
import edu.ucsb.cs.smanner.net.Moderator;
import edu.ucsb.cs.smanner.net.TestUtil;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.store.FileStore;

public class Node3 {
	private static Logger log = LoggerFactory.getLogger(Node3.class);

	AbstractApplicationContext context;
	AbstractApplicationContext contextConfig;

	PaxosFollower followerA;
	PaxosFollower followerB;

	FileStore gradesStore = new FileStore(new File("grades.txt"));
	FileStore statsStore = new FileStore(new File("stats.txt"));

	public static void main(String args[]) throws Exception {
		Node3 node = new Node3();
		node.setUp();
		
		log.info("Node prepared, press enter to run.");
		UITool.readLine();
		node.run();

		log.info("Node running, press enter to stop.");
		UITool.readLine();
		node.tearDown();
	}
	
	public void setUp() throws Exception {
		log.trace("Node3::setUp()");
		context = TestUtil.createContext("/META-INF/spring/node3.xml");
		
		followerA = (PaxosFollower) context.getBean("protocolA3");
		followerA.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosWriteOperation) {
					// commit
					try {
						gradesStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
				}
			}
		});

		followerB = (PaxosFollower) context.getBean("protocolB3");
		followerB.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				if(operation instanceof PaxosWriteOperation) {
					// commit
					try {
						statsStore.append(((PaxosWriteOperation) operation).getString());
					} catch (Exception e) {
						// should not happen
						log.error("could not write to file store");
					}
				}
			}
		});

	}

	public void run() {
		log.trace("Node3::run()");
		// set up servers
		contextConfig = TestUtil.createContext("/META-INF/spring/config.xml");
		setupModerator(context, "serverA3", "pgA");
		setupModerator(context, "serverB3", "pgB");
	}
	
	public void tearDown() throws Exception {
		log.trace("Node3::tearDown()");
		context.close();
		contextConfig.close();
	}

	void setupModerator(ApplicationContext context, String moderatorName, String groupName) {
		log.trace("Node3::setupModerator()");
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(groupName);
		Moderator moderator = (Moderator) context.getBean(moderatorName);
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

}
