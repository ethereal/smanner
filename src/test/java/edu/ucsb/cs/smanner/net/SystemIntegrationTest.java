package edu.ucsb.cs.smanner.net;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.ucsb.cs.smanner.protocol.paxos.PaxosFollower;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.Proposal;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction;
import edu.ucsb.cs.smanner.protocol.tpc.Transaction.TransactionState;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionListener;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitCoordinator;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;

public class SystemIntegrationTest {

	AbstractApplicationContext context1;
	AbstractApplicationContext context2;
	AbstractApplicationContext context3;
	AbstractApplicationContext contextConfig;

	PaxosLeader leaderA;
	PaxosFollower followerA;

	PaxosLeader leaderB;
	PaxosFollower followerB;

	TwoPhaseCommitCoordinator coordinator;
	TwoPhaseCommitParticipant participantA;
	TwoPhaseCommitParticipant participantB;
	
	volatile boolean committed = false;

	@Before
	public void setUp() throws Exception {
		committed = false;

		context1 = TestUtil.createContext("/META-INF/spring/node1.xml");
		context2 = TestUtil.createContext("/META-INF/spring/node2.xml");
		context3 = TestUtil.createContext("/META-INF/spring/node3.xml");
		contextConfig = TestUtil.createContext("/META-INF/spring/config.xml");
		
		// Paxos Group A
		leaderA = (PaxosLeader) context1.getBean("protocolAL");
		followerA = (PaxosFollower) context1.getBean("protocolA1");
		followerA.addListener(new ProposalListener() {
			@Override
			public void notifyCommit(Proposal proposal) {
				if(proposal.getId() % 10 == 0) {
					// prepare
					participantA.prepareTransaction(proposal.getId() / 10);
				} else {
					// commit
					participantA.commitTransaction(proposal.getId() / 10);
				}
			}
		});
		
		// Paxos Group B
		leaderB = (PaxosLeader) context2.getBean("protocolBL");
		followerB = (PaxosFollower) context2.getBean("protocolB2");
		followerB.addListener(new ProposalListener() {
			@Override
			public void notifyCommit(Proposal proposal) {
				if(proposal.getId() % 10 == 0) {
					// prepare
					participantB.prepareTransaction(proposal.getId() / 10);
				} else {
					// commit
					participantB.commitTransaction(proposal.getId() / 10);
				}
			}
		});
		
		// Two Phase Commit
		coordinator = (TwoPhaseCommitCoordinator) context2.getBean("protocolTpcL");
		
		participantA = (TwoPhaseCommitParticipant) context1.getBean("protocolTpcA");
		participantA.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(Transaction transaction) {
				leaderA.addProposal(transaction.getId() * 10);
			}
			public void notifyCommit(Transaction transaction) {
				leaderA.addProposal(transaction.getId() * 10 + 1);
			}
		});
		
		participantB = (TwoPhaseCommitParticipant) context2.getBean("protocolTpcB");
		participantB.addListener(new TransactionListener() {
			@Override
			public void notifyPrepare(Transaction transaction) {
				leaderB.addProposal(transaction.getId() * 10);
			}
			public void notifyCommit(Transaction transaction) {
				leaderB.addProposal(transaction.getId() * 10 + 1);
			}
		});
		

		followerA.addListener(new ProposalListener() {
			@Override
			public void notifyCommit(Proposal proposal) {
				committed = true;
			}
		});
		
		setupModerator(context1, "serverAL", "pgA");
		setupModerator(context1, "serverA1", "pgA");
		setupModerator(context1, "serverB1", "pgB");
		setupModerator(context1, "serverTpcA", "tpc");

		setupModerator(context2, "serverA2", "pgA");
		setupModerator(context2, "serverBL", "pgB");
		setupModerator(context2, "serverB2", "pgB");
		setupModerator(context2, "serverTpcB", "tpc");
		setupModerator(context2, "serverTpcL", "tpc");

		setupModerator(context3, "serverA3", "pgA");
		setupModerator(context3, "serverB3", "pgB");

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
		Set<String> participants = new HashSet<String>();
		participants.add("tpcA");
		participants.add("tpcB");
		
		coordinator.addTransaction(new Transaction(0, "tpcL", participants));
		while (coordinator.getTransaction(0).getState() != TransactionState.COMMITTED) {
			Thread.sleep(100);
		}
	}
	
	void setupModerator(ApplicationContext context, String moderatorName, String groupName) {
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) contextConfig.getBean(groupName);
		Moderator moderator = (Moderator) context.getBean(moderatorName);
		
		moderator.setNodes(endpoints);
		moderator.run();
	}

}
