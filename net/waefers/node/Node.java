package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;

/**
 * 
 * Node information object
 * 
 * @author Michael Ansel
 *
 */
public class Node implements java.io.Serializable {
	
	static final long serialVersionUID = 0;

	/**
	 * Unique node identifier
	 */
	public URI uri;
	
	/**
	 * Possible node types
	 */
	public enum NodeType {PEER,REPLICA};
	
	/**
	 * Node type
	 */
	public NodeType type;
	
	/**
	 * ArrayList of data stored on peer
	 */
	public HashSet<Integer> dataStored = new HashSet<Integer>();
	
	/**
	 * Node external InetSocketAddress
	 */
	public InetSocketAddress address;
	
	public boolean isPeer() {
		return type == NodeType.PEER;
	}
	
	public int hashCode() {
		return uri.hashCode() ^ dataStored.hashCode();
	}
	
	public String toString() {
		return String.format("{type=%s uri=%s #ofblocks=%n}",
				type, uri, dataStored.size());
	}

}
