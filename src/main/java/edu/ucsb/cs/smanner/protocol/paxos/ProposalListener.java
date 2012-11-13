package edu.ucsb.cs.smanner.protocol.paxos;

public interface ProposalListener {
	void notifyCommit(Proposal proposal);
}
