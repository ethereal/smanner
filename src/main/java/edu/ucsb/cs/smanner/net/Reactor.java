package edu.ucsb.cs.smanner.net;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.Protocol;

public class Reactor {
	private static Logger log = LoggerFactory.getLogger(Reactor.class);
	
	Node node;
	Protocol protocol;
	
	Set<Node> nodes = new HashSet<Node>();
	Map<Node, BlockingQueue<Message>> inQueues = new HashMap<Node, BlockingQueue<Message>>();
	Map<Node, BlockingQueue<Message>> outQueues = new HashMap<Node, BlockingQueue<Message>>();
	
	Map<Node, InputThread> inputThreads = new HashMap<Node, InputThread>();
	Map<Node, OutputThread> outputThreads = new HashMap<Node, OutputThread>();
	ModeratorThread moderatorThread = new ModeratorThread();
	
	ExecutorService executor = Executors.newCachedThreadPool();;
	
	public Reactor(Node node, Protocol protocol) {
		this.node = node;
		this.protocol = protocol;
	}

	void addNode(Node node, InputStream in, OutputStream out) throws Exception {
		nodes.add(node);
		
		inQueues.put(node, new LinkedBlockingQueue<Message>());
		outQueues.put(node, new LinkedBlockingQueue<Message>());
		
		inputThreads.put(node, new InputThread(node, new ObjectInputStream(in)));
		outputThreads.put(node, new OutputThread(node, new ObjectOutputStream(out)));
	}
	
	void cancel() {
		for(InputThread t : inputThreads.values())
			t.cancel();
		
		for(OutputThread t : outputThreads.values())
			t.cancel();
		
		moderatorThread.cancel();
		
		executor.shutdown();
	}
	
	void run() {
		for(InputThread t : inputThreads.values())
			executor.execute(t);
		
		for(OutputThread t : outputThreads.values())
			executor.execute(t);
		
		executor.execute(moderatorThread);
	}
	
	boolean isDone() {
		return !executor.isTerminated();
	}
	
	class ModeratorThread implements Runnable {
		long pollIntervalInMs = 10;
		volatile boolean active = true;

		@Override
		public void run() {
			while (active) {
				Collection<Message> incoming = new ArrayList<Message>();

				// check protocol status
				if(protocol.isDone()) {
					log.info("Protocol completed");
					return;
				}
				
				// collect pending messages
				for (Node node : nodes) {
					inQueues.get(node).drainTo(incoming);
				}
				
				// deliver messages
				try {
					for (Message message : incoming) {
						if(! node.equals(message.getDestination())) {
							log.error("Received message for {} at {}", message.getDestination(), node);
							return;
						}
						
						if(nodes.contains(message.getSource())) {
							log.error("Received message from unknown node {}", message.getSource());
							return;
						}
						
						protocol.put(message);
					}
				} catch (Exception e) {
					log.error("Protocol error during delivery: {}", e);
					return;
				}
				
				// queue messages for sending
				int messageCount = 0;
				try {
					while (protocol.hasMessage()) {
						Message message = protocol.get();
						
						if(! node.equals(message.getSource())) {
							log.error("Sending message from {} at {}", message.getDestination(), node);
							return;
						}
						
						if(nodes.contains(message.getDestination())) {
							log.error("Sending message to unknown node {}", message.getSource());
							return;
						}

						outQueues.get(message.getDestination()).add(message);
						messageCount++;
					}
				} catch (Exception e) {
					log.error("Protocol error during sending: {}", e);
					return;
				}
				
				// poll if no activity
				if (messageCount <= 0) {
					try {
						Thread.sleep(pollIntervalInMs);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}
		
		void cancel() {
			active = false;
		}
	}
	
	class InputThread implements Runnable {
		volatile boolean active = true;
		
		ObjectInputStream in;
		Node node;

		public InputThread(Node node, ObjectInputStream in) {
			this.in = in;
			this.node = node;
		}

		@Override
		public void run() {
			try {
				while (active) {
					log.debug("Waiting for message from {}", node);
					Message message = (Message) in.readObject();
					
					log.debug("Enqueueing message from {}", node);
					inQueues.get(node).add(message);
				}
			} catch (Exception e) {
				log.error("InputThread {} encountered exception: {}", node, e);
			}
		}

		public void cancel() {
			active = false;
		}
	}

	class OutputThread implements Runnable {
		volatile boolean active = true;
		
		ObjectOutputStream out;
		Node node;

		public OutputThread(Node node, ObjectOutputStream out) {
			this.out = out;
			this.node = node;
		}

		@Override
		public void run() {
			try {
				while (active) {
					log.debug("Sending message to {}", node);
					Message message = outQueues.get(node).take();
					out.writeObject(message);
				}
			} catch (Exception e) {
				log.error("OutputThread {} encountered exception: {}", node, e);
			}
		}

		public void cancel() {
			active = false;
		}
	}

}
