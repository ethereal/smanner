package edu.ucsb.cs.smanner.net;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;

public class NullTransactionExecutor extends TransactionExecutor {

	@Override
	public void prepare(long id, Operation operation) {
		participant.acceptPrepare(id);
	}

	@Override
	public void commit(long id, Operation operation) {
		participant.commitTransaction(id);
	}

	@Override
	public void abort(long id, Operation operation) {
		participant.abortTransaction(id);
	}

}
