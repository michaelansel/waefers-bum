package net.waefers.messaging;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import net.waefers.node.Node;
import static net.waefers.GlobalControl.log;

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
	 * Source and Destination Nodes for the message
	 */
	private Node source = new Node();
	private Node destination = new Node();
	
	/**
	 * Source and Destination SocketAddresses
	 * Retrieved from the socket when message received
	 * Return path for message reply
	 */
	public SocketAddress srcSAddr,dstSAddr;
	
	
	
	/**
	 * Message data
	 */
	private Object payload;
	
	/**
	 * Possible message types
	 * HEARTBEAT - Register/Update node on the network
	 * BLOCK - Get/Put block data
	 * METADATA - Get/Put information concerning the file system
	 * BLOCK_LOCATION - (Master Only) Get node to request block from
	 * NODE_LOCATION - Get the direct connect address for a node
	 */
	public enum Type {HEARTBEAT,BLOCK,METADATA,BLOCK_LOCATION,NODE_LOCATION};
	
	/**
	 * Message type
	 */
	public Type type;
	
	/**
	 * Possible response types
	 */
	public enum Response {SUCCESS,ERROR};
	
	/**
	 * Message response type
	 */
	public Response response;
	
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
		this.type = null;
	}
	
	public Message(URI source,URI destination,Object payload) {
		log.finest("new message source uri:"+source.toString());
		this.source.uri = source;
		this.destination.uri = destination;
		this.payload = payload;
		this.id = last_id++;
		this.type = null;
	}
	
	public Message(URI source,URI destination) {
		this(source,destination,null);
	}
	
	public Message(Node source,Node destination) {
		this(source,destination,null);
	}
	
	public Message() {
		this((Node)null,(Node)null,(Object)null);
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
	
	public void killPayload() {
		payload = null;
	}
	
	/**
	 * Modify the payload of an existing message and kill the cached serialized version
	 * @param msg Message to modify
	 * @param payload Payload for message
	 * @return modified messge
	 */
	public static Message setPayload(Message msg,Object payload) {
		msg.payload = payload;
		msg.bbuf = null;
		return msg;
	}
	
	public Message noPayload() {
		Message msg;
		msg = (Message) this.clone();
		msg.killPayload();
		return msg;
	}
	
	public boolean isError() {
		return (response == Response.ERROR);
	}
	
	/**
	 * Creates an exact clone of this message with the same message id
	 * @return Cloned message
	 */
	public Message clone() {
		Message msg = new Message(this.source,this.destination,this.payload);
		msg.id=this.id;
		msg.type=this.type;
		msg.response=this.response;
		return msg;
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
