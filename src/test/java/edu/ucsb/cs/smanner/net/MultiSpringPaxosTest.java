package edu.ucsb.cs.smanner.net;

import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;

public class MultiSpringPaxosTest {

	AbstractApplicationContext context1;
	AbstractApplicationContext context2;
	AbstractApplicationContext context3;
	AbstractApplicationContext contextConfig;

	PaxosLeader leader;
	PaxosFollower follower;

	volatile boolean committed = false;

	@Before
	public void setUp() throws Exception {
		committed = false;

		context1 = TestUtil.createContext("/META-INF/spring/pgA1.xml");
		context2 = TestUtil.createContext("/META-INF/spring/pgA2.xml");
		context3 = TestUtil.createContext("/META-INF/spring/pgA3.xml");
		contextConfig = TestUtil.createContext("/META-INF/spring/pgA-config.xml");
		
		leader = (PaxosLeader) context1.getBean("protocolAL");
		follower = (PaxosFollower) context1.getBean("protocolA1");

		follower.addListener(new ProposalListener() {
			@Override
			public void notifyCommit(Proposal proposal) {
				committed = true;
			}
		});
		
		setupMods(context1);
		setupMods(context2);
		setupMods(context3);

	}

	@After
	public void tearDown() throws Exception {
		context1.close();
		context2.close();
		context3.close();
	}

	@Test(timeout = 5000)
	public void testStartup() {
		// nothing here
	}

	@Test(timeout = 5000)
	public void testCommit() throws Exception {
		leader.addProposal(0);
		while (!committed) {
			Thread.sleep(100);
		}
	}
	
	void setupMods(ApplicationContext context) {
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean("pgA");
		Map<String, Moderator> moderators = (Map<String, Moderator>) context.getBeansOfType(Moderator.class);

		for (Moderator mod : moderators.values()) {
			mod.setNodes(endpoints);
			mod.run();
		}
	}

}
