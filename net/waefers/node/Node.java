package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
	public HashSet<byte[]> dataStored = new HashSet<byte[]>();
	
	/**
	 * Node external InetSocketAddress
	 */
	public InetSocketAddress address;
	
	public Node() {
		this(null,null,null);
	}
	
	public Node(URI uri) {
		this(uri,null,null);
	}
	
	public Node(NodeType type) {
		this(null,null,type);
	}
	
	public Node(SocketAddress addr) {
		this(null,addr,null);
	}
	
	public Node(URI uri, SocketAddress addr) {
		this(uri,addr,null);
	}
	
	public Node(URI uri, SocketAddress addr, NodeType type) {
		this.uri = uri;
		this.address = (InetSocketAddress) addr;
		this.type = type;
	}
	
	public boolean isPeer() {
		return type == NodeType.PEER;
	}
	
	public int hashCode() {
		return uri.hashCode() ^ dataStored.hashCode();
	}
	
	public String toString() {
		return String.format("{type=%s uri=%s #ofblocks=%s}",
				type, uri, dataStored.size());
	}

}
