package edu.ucsb.cs.smanner.net;

import java.net.URI;

public class Node {
	final String id;
	final URI host;

	public Node(String id, URI host) {
		this.id = id;
		this.host = host;
	}

	public Node(String id, String host) {
		this.id = id;
		this.host = URI.create(host);
	}

	public String getId() {
		return id;
	}

	public URI getHost() {
		return host;
	}
}
