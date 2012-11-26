package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.protocol.Operation;

public interface TransactionListener {
	void notifyPrepare(long id, Operation operation);
	void notifyCommit(long id, Operation operation);
}
