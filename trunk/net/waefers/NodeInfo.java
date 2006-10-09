package net.waefers;

import java.net.URI;
import java.util.ArrayList;

/**
 * 
 * Node information object
 * 
 * @author Michael Ansel
 *
 */
public class NodeInfo {
	
	/**
	 * Node URI
	 */
	URI uri;
	
	/**
	 * Possible node types
	 */
	public enum NodeType {PEER,REPLICA};
	
	/**
	 * Node type
	 */
	private NodeType type;
	
	/**
	 * ArrayList of data stored on peer
	 */
	ArrayList<Integer> dataStored = new ArrayList<Integer>();
	
	public boolean isPeer() {
		return type == NodeType.PEER;
	}
	
	public void setURI(URI uri) {
		this.uri = uri;
	}
	
	public URI getURI() {
		return uri;
	}
	
	public String toString() {
		return String.format("{type=%s uri=%s #ofblocks=%n}",
				type, uri, dataStored.size());
	}

}
