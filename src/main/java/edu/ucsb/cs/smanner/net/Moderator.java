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

public class Moderator {
	private static Logger log = LoggerFactory.getLogger(Moderator.class);
	
	Node node;
	Protocol protocol;
	
	Set<Node> nodes = new HashSet<Node>();
	Map<Node, BlockingQueue<Message>> inQueues = new HashMap<Node, BlockingQueue<Message>>();
	Map<Node, BlockingQueue<Message>> outQueues = new HashMap<Node, BlockingQueue<Message>>();
	
	Map<Node, InputThread> inputThreads = new HashMap<Node, InputThread>();
	Map<Node, OutputThread> outputThreads = new HashMap<Node, OutputThread>();
	ModeratorThread moderatorThread = new ModeratorThread();
	
	ExecutorService executor = Executors.newCachedThreadPool();;
	
	public Moderator(Node node, Protocol protocol) {
		log.trace("Moderator::Moderator({}, {})", node,  protocol);
		this.node = node;
		this.protocol = protocol;
	}

	void addNode(Node node, InputStream in, OutputStream out) {
		log.trace("Moderator::addNode({}, {}, {})", new Object[] {node,  in, out});
		nodes.add(node);
		
		log.trace("Moderator::addNode::createQueues");
		inQueues.put(node, new LinkedBlockingQueue<Message>());
		outQueues.put(node, new LinkedBlockingQueue<Message>());
		
		log.trace("Moderator::addNode::createThreads");
		inputThreads.put(node, new InputThread(node, in));
		outputThreads.put(node, new OutputThread(node, out));
	}
	
	void cancel() {
		log.trace("Moderator::cancel()");
		for(InputThread t : inputThreads.values())
			t.cancel();
		
		for(OutputThread t : outputThreads.values())
			t.cancel();
		
		moderatorThread.cancel();
		
		executor.shutdown();
	}
	
	void run() {
		log.trace("Moderator::run()");
		for(InputThread t : inputThreads.values())
			executor.execute(t);
		
		for(OutputThread t : outputThreads.values())
			executor.execute(t);
		
		executor.execute(moderatorThread);
	}
	
	boolean isDone() {
		log.trace("Moderator::isDone()");
		return protocol.isDone();
	}
	
	class ModeratorThread implements Runnable {
		long pollIntervalInMs = 10;
		volatile boolean active = true;

		@Override
		public void run() {
			log.trace("ModeratorThread::run()");
			
			long timeStart = System.nanoTime();
			
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
					long timeCurrent = System.nanoTime() - timeStart;
					protocol.setTime(timeCurrent);
					
					for (Message message : incoming) {
						if(! node.equals(message.getDestination())) {
							log.error("Received message for {} at {}", message.getDestination(), node);
							return;
						}
						
						if(! nodes.contains(message.getSource())) {
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
						
						if(! nodes.contains(message.getDestination())) {
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
			log.trace("ModeratorThread::cancel()");
			active = false;
		}
	}
	
	class InputThread implements Runnable {
		volatile boolean active = true;
		
		InputStream in;
		Node node;

		public InputThread(Node node, InputStream in) {
			log.trace("InputThread::InputThread({}, {})", node, in);
			this.in = in;
			this.node = node;
		}

		@Override
		public void run() {
			log.trace("InputThread::run()");
			try {
				ObjectInputStream inObj = new ObjectInputStream(in);
				
				while (active) {
					log.debug("Waiting for message from {}", node);
					Message message = (Message) inObj.readObject();
					
					log.debug("Enqueueing message from {}", node);
					inQueues.get(node).add(message);
				}
			} catch (Exception e) {
				log.error("InputThread {} encountered exception: {}", node, e);
			}
		}

		public void cancel() {
			log.trace("InputThread::cancel()");
			active = false;
		}
	}

	class OutputThread implements Runnable {
		volatile boolean active = true;
		
		OutputStream out;
		Node node;

		public OutputThread(Node node, OutputStream out) {
			log.trace("OutputThread::OutputThread({}, {})", node, out);
			this.out = out;
			this.node = node;
		}

		@Override
		public void run() {
			log.trace("OutputThread::run()");
			try {
				ObjectOutputStream outObj = new ObjectOutputStream(out);
				
				while (active) {
					log.debug("Sending message to {}", node);
					Message message = outQueues.get(node).take();
					outObj.writeObject(message);
				}
			} catch (Exception e) {
				log.error("OutputThread {} encountered exception: {}", node, e);
			}
		}

		public void cancel() {
			log.trace("OutputThread::cancel()");
			active = false;
		}
	}

}
