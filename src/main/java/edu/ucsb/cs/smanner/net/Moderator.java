package edu.ucsb.cs.smanner.net;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
	InputThread inputThread;
	OutputThread outputThread;

	ExecutorService executor;

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

	void cancel() {
		log.trace("Moderator::cancel()");
		if(inputThread != null)
			inputThread.cancel();
		
		if(outputThread != null)
			outputThread.cancel();
		
		if(executor != null)
			executor.shutdown();
	}

	void run() {
		log.trace("Moderator::run()");
		
		log.trace("Moderator::createQueues");
		inQueue = new LinkedBlockingQueue<Message>();

		log.trace("Moderator::createThreads");
		inputThread = new InputThread();
		outputThread = new OutputThread();

		log.trace("Moderator::createThreads");
		protocol.setTime(0);
		protocol.setNodes(Collections.unmodifiableSet(endpoints.keySet()));
		protocol.setSelf(self);

		log.trace("Moderator::createThreads");
		executor = Executors.newFixedThreadPool(2);
		executor.execute(inputThread);
		executor.execute(outputThread);
	}

	boolean isDone() {
		log.trace("Moderator::isDone()");
		return protocol.isDone();
	}

	class InputThread implements Runnable {
		volatile boolean active = true;

		@Override
		public void run() {
			log.trace("InputThread::run()");
			try {
				while (active) {
					Message message = inQueue.poll(POLL_INTERVAL,
							TimeUnit.MILLISECONDS);
					if (message != null) {
						log.debug("Receiving {}", message);
						synchronized (protocol) {
							protocol.put(message);
						}
					}
				}
			} catch (Exception e) {
				log.error("InputThread {} encountered exception: {}", self, e);
			}
		}

		public void cancel() {
			log.trace("InputThread::cancel()");
			active = false;
		}
	}

	class OutputThread implements Runnable {
		volatile boolean active = true;

		@Override
		public void run() {
			log.trace("OutputThread::run()");
			try {
				long startTime = System.nanoTime();
				long currentTime = 0;

				while (active) {
					synchronized (protocol) {
						protocol.setTime(currentTime);
						
						while (protocol.hasMessage()) {
							Message message = protocol.get();
							log.debug("sending {}", message);
							MessageEndpoint remote = endpoints.get(message
									.getDestination());
							
							if(remote == null)
								throw new Exception(String.format("Endpoint %s not known.", message.getDestination()));

							remote.put(message);
						}
					}

					Thread.sleep(POLL_INTERVAL);

					currentTime = System.nanoTime() - startTime;
				}
			} catch (Exception e) {
				log.error("OutputThread {} encountered exception: {}", self, e);
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

}
