package edu.ucsb.cs.smanner.protocol.tpc;

import edu.ucsb.cs.smanner.protocol.Operation;

public abstract class TransactionExecutor {
	protected TwoPhaseCommitParticipant participant;
	
	public abstract void prepare(long id, Operation operation);
	public abstract void commit(long id, Operation operation);
	public abstract void abort(long id, Operation operation);
	
	public void setParticipant(TwoPhaseCommitParticipant participant) {
		this.participant = participant;
	}
	
}
