package edu.ucsb.cs.smanner.protocol.tpc;

public interface TransactionListener {
	void notifyCommit(Transaction transaction);
}
