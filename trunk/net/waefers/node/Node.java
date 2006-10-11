package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
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
	 * Node URI
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
	
	public String toString() {
		return String.format("{type=%s uri=%s #ofblocks=%n}",
				type, uri, dataStored.size());
	}

}
