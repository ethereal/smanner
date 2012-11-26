package edu.ucsb.cs.smanner.protocol.paxos;

import edu.ucsb.cs.smanner.protocol.Operation;

public interface ProposalListener {
	void notify(long id, Operation operation);
}
