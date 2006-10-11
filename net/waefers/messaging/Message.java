package net.waefers.messaging;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import net.waefers.node.Node;

/**
 * 
 * The basic message format to be used for all
 * information transfers.
 * 
 * @author Michael Ansel
 *
 */
public class Message implements java.io.Serializable {
	
	/**
	 * Source and Destination URIs for the message
	 */
	private Node source,destination;
	
	/**
	 * Source and Destination SocketAddresses
	 * Retrieved from the socket when message received
	 */
	public SocketAddress srcAddr,dstAddr;
	
	
	/**
	 * Message data
	 */
	Object payload;
	
	/**
	 * Possible message types
	 * HEARTBEAT - Register/Update node on the network
	 * BLOCK - Get/Put block data
	 * METADATA - Get/Put information concerning the file system
	 * LOCATION - (Master Only) Get node to request block from
	 */
	public enum MessageType {HEARTBEAT,BLOCK,METADATA,LOCATION};
	
	/**
	 * Message type
	 */
	public MessageType type;
	
	/**
	 * Possible response types
	 */
	public enum ResponseType {SUCCESS,ERROR};
	
	/**
	 * Message response type
	 */
	public ResponseType response;
	
	/**
	 * Message ID
	 */
	public int id;
	
	/**
	 * Last message ID
	 */
	transient private static int last_id = (int)(Math.random() * Integer.MAX_VALUE); 
	
	static final long serialVersionUID = 0;
	
	/**
	 * Serialized version of the message
	 */
	transient ByteBuffer bbuf;
		
	public Message(Node source,Node destination,Object payload) {
		this.source = source;
		this.destination = destination;
		this.payload = payload;
		this.id = last_id++;
		this.type = MessageType.HEARTBEAT;
	}
	
	public Message(URI source,URI destination,Object payload) {
		this.source.uri = source;
		this.destination.uri = destination;
		this.payload = payload;
		this.id = last_id++;
		this.type = MessageType.HEARTBEAT;
	}
	
	public Message(URI source,URI destination) {
		this(source,destination,null);
	}
	
	public Message(Node source,Node destination) {
		this(source,destination,null);
	}
	
	public Node getSource() {
		return source;
	}
	
	public Node getDestination() {
		return destination;
	}
	
	public boolean hasPayload() {
		return (payload != null);
	}
	
	public Object getPayload() {
		return payload;
	}
	
	public boolean isError() {
		return (response == ResponseType.ERROR);
	}
	
	public int hashCode() {
		return source.hashCode() ^ destination.hashCode() ^ id;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Message) {
        	Message msg = (Message) o;
            return ((source.equals(msg.getSource()) && destination.equals(msg.getDestination())) ||
            		(source.equals(msg.getDestination()) && destination.equals(msg.getSource())))
            		&& id == msg.id;
        } else {
        	return false;
        }
	}
	
	public String toString() {
		return String.format("{type=%s response=%s src=%s dst=%s id=%d payload=%s}",
				type, response, source, destination, id, payload);
	}
}
