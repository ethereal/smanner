package edu.ucsb.cs.smanner.protocol.tpc;

public interface TransactionListener {
	void notifyPrepare(Transaction transaction);
	void notifyCommit(Transaction transaction);
}
