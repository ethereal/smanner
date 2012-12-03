package edu.ucsb.cs.smanner.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Protocol;

public class Moderator implements MessageEndpoint {
	public static final long POLL_INTERVAL = 10;

	private static Logger log = LoggerFactory.getLogger(Moderator.class);

	String self;
	Protocol protocol;
	Map<String, MessageEndpoint> endpoints = new HashMap<String, MessageEndpoint>();

	BlockingQueue<Message> inQueue;
	ModeratorThread moderatorThread;

	ExecutorService executor;
	
	/** associative group name for XML config */
	String groupName;

	public Moderator(String self, Protocol protocol) {
		log.trace("Moderator::Moderator({}, {})", self, protocol);
		this.self = self;
		this.protocol = protocol;
	}

	void addNode(MessageEndpoint endpoint) {
		log.trace("Moderator::addIdentifier({}, {})", new Object[] {
				endpoint.getIdentifier(), endpoint });
		endpoints.put(endpoint.getIdentifier(), endpoint);
	}

	public void cancel() {
		log.trace("Moderator::cancel()");
		if(moderatorThread != null)
			moderatorThread.cancel();
		
		if(executor != null)
			executor.shutdown();
	}

	public void run() {
		log.trace("Moderator::run()");
		
		log.trace("Moderator::createQueues");
		inQueue = new LinkedBlockingQueue<Message>();

		log.trace("Moderator::createThreads");
		moderatorThread = new ModeratorThread();

		log.trace("Moderator::createThreads");
		protocol.setTime(0);
		protocol.setNodes(Collections.unmodifiableSet(endpoints.keySet()));
		protocol.setSelf(self);

		log.trace("Moderator::createThreads");
		executor = Executors.newSingleThreadExecutor();
		executor.execute(moderatorThread);
	}

	boolean isDone() {
		log.trace("Moderator::isDone()");
		return protocol.isDone();
	}

	class ModeratorThread implements Runnable {
		volatile boolean active = true;

		@Override
		public void run() {
			log.trace("ModeratorThread::run()");
			try {
				long startTime = System.nanoTime();
				long currentTime = 0;
				
				log.debug("Thread started for node {}", self);

				while (active) {
					protocol.setTime(currentTime);
					
					Collection<Message> messages = new ArrayList<Message>();
					inQueue.drainTo(messages);
					
					for(Message message : messages) {
						log.debug("receiving {}", message);
						protocol.put(message);
					}
					
					while (protocol.hasMessage()) {
						Message message = protocol.get();
						log.debug("sending {}", message);
						
						MessageEndpoint remote = endpoints.get(message
								.getDestination());
						
						if(remote == null)
							throw new Exception(String.format("Endpoint %s not known.", message.getDestination()));

						remote.put(message);
					}

					Thread.sleep(POLL_INTERVAL);

					currentTime = System.nanoTime() - startTime;
				}
			} catch (Exception e) {
				log.error("ModeratorThread {} encountered exception: {}", self, e);
			}
		}

		public void cancel() {
			log.trace("OutputThread::cancel()");
			active = false;
		}
	}

	@Override
	public void put(Message message) throws Exception {
		inQueue.put(message);
	}

	@Override
	public String getIdentifier() {
		return self;
	}
	
	public void setNodes(Collection<MessageEndpoint> endpoints) {
		for(MessageEndpoint endpoint : endpoints) {
			String identifier = endpoint.getIdentifier();
			log.debug("adding at {} endpoint {}", self, identifier);
			this.endpoints.put(identifier, endpoint);
		}
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
