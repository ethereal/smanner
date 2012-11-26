package edu.ucsb.cs.smanner.net;

import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;

public class SpringPaxosTest {

	AbstractApplicationContext context;

	PaxosLeader leader;
	PaxosFollower follower;

	volatile boolean committed = false;

	@Before
	public void setUp() throws Exception {
		committed = false;

		context = TestUtil.createContext("/META-INF/spring/pgA-all.xml");
		leader = (PaxosLeader) context.getBean("protocolAL");
		follower = (PaxosFollower) context.getBean("protocolA3");

		follower.addListener(new ProposalListener() {
			@Override
			public void notify(long id, Operation operation) {
				committed = true;
			}
		});

		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) context.getBean("pgA");
		Map<String, Moderator> moderators = (Map<String, Moderator>) context.getBeansOfType(Moderator.class);

		for (Moderator mod : moderators.values()) {
			mod.setNodes(endpoints);
			mod.run();
		}

	}

	@After
	public void tearDown() throws Exception {
		context.close();
	}

	@Test(timeout = 5000)
	public void testStartup() {
		// nothing here
	}

	@Test(timeout = 5000)
	public void testCommit() throws Exception {
		leader.addProposal(new NullOperation("null"));
		while (!committed) {
			Thread.sleep(100);
		}
	}

}
