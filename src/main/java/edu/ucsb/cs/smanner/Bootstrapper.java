package edu.ucsb.cs.smanner;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.ucsb.cs.smanner.net.MessageEndpoint;
import edu.ucsb.cs.smanner.net.Moderator;
import edu.ucsb.cs.smanner.protocol.Operation;
import edu.ucsb.cs.smanner.protocol.paxos.PaxosLeader;
import edu.ucsb.cs.smanner.protocol.paxos.ProposalListener;
import edu.ucsb.cs.smanner.protocol.tpc.TransactionExecutor;
import edu.ucsb.cs.smanner.protocol.tpc.TwoPhaseCommitParticipant;
import edu.ucsb.cs.smanner.store.FileStore;
import edu.ucsb.cs.smanner.store.LockManager;

public class Bootstrapper {
	private static Logger log = LoggerFactory.getLogger(Bootstrapper.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	AbstractApplicationContext localContext;
	AbstractApplicationContext globalContext;

	void init(String localConfig, String globalConfig) {
		localContext = new ClassPathXmlApplicationContext(localConfig);
		globalContext = new ClassPathXmlApplicationContext(globalConfig);
	}

	void run() {
		@SuppressWarnings("unchecked")
		Collection<Moderator> moderators = (Collection<Moderator>) localContext
				.getBeansOfType(Moderator.class);
		for (Moderator moderator : moderators) {
			setupGroup(moderator);
			moderator.run();
		}
	}

	void cancel() {
		localContext.close();
		globalContext.close();
	}

	void setupGroup(Moderator moderator) {
		@SuppressWarnings("unchecked")
		Collection<MessageEndpoint> endpoints = (Collection<MessageEndpoint>) globalContext
				.getBean(moderator.getGroupName());
		moderator.setNodes(endpoints);
	}

	static class TpcFollowerExecutor extends TransactionExecutor {
		final PaxosLeader leader;
		final TwoPhaseCommitParticipant participant;
		final FileStore localStore;
		
		public TpcFollowerExecutor(PaxosLeader leader,
				TwoPhaseCommitParticipant participant, FileStore localStore) {
			this.leader = leader;
			this.participant = participant;
			this.localStore = localStore;
		}

		@Override
		public void prepare(long id, Operation operation) {
			leader.addProposal(new PaxosPrepareOperation(operation.getId(), id));
		}

		@Override
		public void commit(long id, Operation operation) {
			if (operation instanceof ReadOperation) {
				try {
					String result = localStore.read();
					participant.commitTransaction(id, new ReadOperationResult(operation, result));
					
				} catch (Exception e) {
					// should not happen
					log.error("error reading file store {}", e);
					participant.commitTransaction(id, new ReadOperationResult(operation, "[ERROR]"));
				}
				
			} else if (operation instanceof WriteOperation) {
				leader.addProposal(new PaxosWriteOperation(operation.getId(),
						id, ((WriteOperation) operation).string));
				
			} else {
				log.error("unknown operation {}", operation);
			}
		}

		@Override
		public void abort(long id, Operation operation) {
			// left blank
		}
	}

	static class PaxosListener implements ProposalListener {
		final TwoPhaseCommitParticipant participant;
		final LockManager manager;
		final FileStore localStore;
		
		public PaxosListener(TwoPhaseCommitParticipant participant,
				LockManager manager, FileStore localStore) {
			this.participant = participant;
			this.manager = manager;
			this.localStore = localStore;
		}

		@Override
		public void notify(long id, Operation operation) {
			log.debug("notified Paxos Group B");
			if(operation instanceof PaxosPrepareOperation) {
				long transactionId = ((PaxosPrepareOperation) operation).transactionId;
				
				try {
					manager.lock(operation.getId());
					participant.acceptPrepare(transactionId);
				} catch (Exception e) {
					log.warn("locking failed");
					participant.declinePrepare(transactionId);
				}
			} else if(operation instanceof PaxosWriteOperation) {
				PaxosWriteOperation wop = (PaxosWriteOperation)operation;
				try {
					localStore.append(wop.string);
					manager.unlock();
					participant.commitTransaction(wop.transactionId);
				} catch (Exception e) {
					// should not happen
					log.error("error writing file store {}", e);
					manager.unlock();
					participant.commitTransaction(wop.transactionId);
				}
			} else {
				log.error("unknown operation {}", operation.getId());
			}
		}
	}}
