package edu.ucsb.cs.smanner.net;

import java.io.Serializable;
import java.net.URI;

public class Node implements Serializable {
	/**
	 * SerialVersionUID 
	 */
	private static final long serialVersionUID = 3835957217574210599L;
	
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
	
	@Override
	public String toString() {
		return String.format("%s", id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
