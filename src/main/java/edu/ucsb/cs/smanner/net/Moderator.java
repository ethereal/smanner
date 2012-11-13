package edu.ucsb.cs.smanner.net;

import java.io.EOFException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
	
	Node self;
	Protocol protocol;
	
	Set<Node> nodes = new HashSet<Node>();
	Map<Node, BlockingQueue<Message>> inQueues = new HashMap<Node, BlockingQueue<Message>>();
	Map<Node, BlockingQueue<Message>> outQueues = new HashMap<Node, BlockingQueue<Message>>();
	
	Map<Node, InputThread> inputThreads = new HashMap<Node, InputThread>();
	Map<Node, OutputThread> outputThreads = new HashMap<Node, OutputThread>();
	ModeratorThread moderatorThread = new ModeratorThread();
	
	ExecutorService executor = Executors.newCachedThreadPool();;
	
	public Moderator(Node self, Protocol protocol) {
		log.trace("Moderator::Moderator({}, {})", self,  protocol);
		this.self = self;
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
		protocol.setTime(0);
		protocol.setNodes(Collections.unmodifiableSet(nodes));
		protocol.setSelf(self);
		
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
						if(! self.equals(message.getDestination())) {
							log.error("Received {} with invalid destination at {}", message, self);
							return;
						}
						
						if(! nodes.contains(message.getSource())) {
							log.error("Received {} from unknown source at {}", message, self);
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
						
						if(! self.equals(message.getSource())) {
							log.error("Sent {} with invalid source at {}", message, self);
							return;
						}
						
						if(! nodes.contains(message.getDestination())) {
							log.error("Sent {} to unknown destination at {}", message, self);
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
					Message message = (Message) inObj.readObject();
					log.debug("Receiving {}", message);
					inQueues.get(node).add(message);
				}
			} catch (EOFException e) {
				log.info("InputThread {} exiting due to EOF");
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
					Message message = outQueues.get(node).take();
					log.debug("Sending {}", message);
					outObj.writeObject(message);
					outObj.flush();
				}
			} catch (EOFException e) {
				log.info("OutputThread {} exiting due to EOF");
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
