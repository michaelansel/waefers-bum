package net.waefers.directory;

import java.util.Date;

import net.waefers.node.Node;

/**
 * 
 * Represents a registered node
 * 
 * @author Michael Ansel
 *
 */

public class NodeEntry {

	public Node node;
	public Date expires;
	static final int NODE_TTL = 30/60; //Node expiration time (minutes)
	
	public NodeEntry(Node node) {
		this.node = node;
		this.updateExpiryTime();
	}

	public void updateExpiryTime() {
		this.expires = new Date(System.currentTimeMillis() + NODE_TTL*60*1000);
		
	}
	
	public String toString() {
		return "{node="+node+" expires="+expires+"}";
	}
}