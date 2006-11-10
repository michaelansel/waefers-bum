package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.HashSet;

import net.waefers.block.Block;

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
	public enum Type {PEER,MASTER};
	
	/**
	 * Node type
	 */
	public Type type;
	
	/**
	 * ArrayList of data stored on peer
	 */
	public HashSet<Block> dataStored = new HashSet<Block>();
	
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
	
	public Node(Type type) {
		this(null,null,type);
	}
	
	public Node(SocketAddress addr) {
		this(null,addr,null);
	}
	
	public Node(URI uri, SocketAddress addr) {
		this(uri,addr,null);
	}
	
	public Node(URI uri, SocketAddress addr, Type type) {
		this.uri = uri;
		this.address = (InetSocketAddress) addr;
		this.type = type;
	}
	
	public boolean isPeer() {
		return type == Type.PEER;
	}
	
	/**
	 * Clones the Node and returns the clone with only URI and routing information
	 * @return Cloned Node with only routing info
	 */
	public Node routing() {
		Node n = new Node(this.uri,this.address,this.type);
		return n;
	}
	
	/**
	 * Clones the Node and returns the clone with only URI and block information
	 * @return Cloned Node with only block info
	 */
	public Node blocks() {
		Node n = new Node(this.uri);
		n.dataStored = this.dataStored;
		return n;
	}
	
	public int hashCode() {
		return uri.hashCode() ^ dataStored.hashCode();
	}
	
	public String toString() {
		return String.format("{type=%s uri=%s #ofblocks=%s address=%s}",
				type, uri, dataStored.size(), address);
	}
	
	/**
	 * Returns true if the node type and uri of both nodes are equal
	 */
	public boolean equals(Object o) {
		if(o instanceof Node) {
			Node n = (Node) o;
			return (
					(type == n.type) &&
					(uri.equals(n.uri))
					);
		}
		else
			return false;
	}

}
