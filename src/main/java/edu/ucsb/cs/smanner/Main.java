package edu.ucsb.cs.smanner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.cs.smanner.protocol.Message;
import edu.ucsb.cs.smanner.protocol.PingClient;
import edu.ucsb.cs.smanner.protocol.PingServer;
import edu.ucsb.cs.smanner.protocol.Protocol;

public class Main {
	private static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		if(args.length != 2) {
			log.error("use 'client 23456' or 'server 23456' arguments.");
			return;
		}
		
		if("server".equals(args[0])) {
			serverMode(Integer.valueOf(args[1]));
			
		} else if("client".equals(args[0])) {
			clientMode("localhost", Integer.valueOf(args[1]));
		}
	}
	
	public static void serverMode(int port) {
		Collection<Main> servers = new ArrayList<Main>();
		ServerSocket serverSocket;
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			log.error("Could not bind to port: {}", e);
			return;
		}

		try {
			while (serverSocket.isBound()) {
				Socket socket = serverSocket.accept();

				Main server = new Main(socket, new PingServer());
				server.start();
				
				servers.add(server);
			}
		} catch (Exception e) {
			log.info("Stopped accepting connections: {}", e);
		}
		
		for(Main server : servers)
			server.cancel();
	}
	
	public static void clientMode(String host, int port) {
		Socket socket;
		
		try {
			socket = new Socket(host, port);
		} catch (Exception e) {
			log.error("Could not connect to host: {}", e);
			return;
		}
		
		Main client = new Main(socket, new PingClient());
		try {
			client.start();
		} catch (Exception e) {
			log.error("Could not start client: {}", e);
			return;
		}
		
		try {
			System.out.println("Press any key to quit.");
			System.in.read();
		} catch (Exception e) {
			// ignore
		}
		
		client.cancel();
		
	}

	Socket socket;
	Protocol protocol;

	ObjectInputStream in;
	ObjectOutputStream out;
	InputThread tIn;
	OutputThread tOut;
	Executor exec;
	Object monitor = new Object();

	public Main(Socket socket, Protocol protocol) {
		this.socket = socket;
		this.protocol = protocol;
	}

	void start() throws Exception {
		log.info("Starting node");
		in = new ObjectInputStream(socket.getInputStream());
		out = new ObjectOutputStream(socket.getOutputStream());

		tIn = new InputThread();
		tOut = new OutputThread();

		exec = Executors.newFixedThreadPool(2);
		exec.execute(tIn);
		exec.execute(tOut);
	}
	
	void cancel() {
		log.info("Stopping node");
		tIn.cancel();
		tOut.cancel();
		
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException ignore) {}
	}

	class InputThread implements Runnable {
		volatile boolean active = true;

		@Override
		public void run() {
			try {
				while (active) {
					log.debug("Receiving message");
					Message message = (Message) in.readObject();

					synchronized (monitor) {
						protocol.put(message);
					}
				}
			} catch (Exception e) {
				log.error("InputThread encountered exception: {}", e);
			}
		}

		public void cancel() {
			active = false;
		}
	}

	class OutputThread implements Runnable {
		long interval = 100;
		volatile boolean active = true;

		@Override
		public void run() {
			try {
				while (active) {
					long currentTime = System.nanoTime();
					long nextTime = currentTime + interval;

					while (currentTime < nextTime) {
						Thread.sleep(nextTime - currentTime);
						currentTime = System.nanoTime();
					}

					synchronized (monitor) {
						while (protocol.hasMessage()) {
							log.debug("Sending message");
							out.writeObject(protocol.get());
						}
					}

					nextTime += interval;
				}
			} catch (Exception e) {
				log.error("OutputThread encountered exception: {}", e);
			}
		}

		public void cancel() {
			active = false;
		}
	}

}
