package edu.ucsb.cs.smanner.net;

import java.util.Map;

import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.OperationResult;

public interface TransactionEndpoint {
	long create(Map<String, Operation> operations);
	TransactionState getState(long transactionId);
	Map<String, OperationResult> getResult(long transactionId);
}
