package net.waefers;

import java.net.URI;
import java.nio.ByteBuffer;

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
	private URI srcURI,dstURI;
	
	/**
	 * Message data
	 */
	Object payload;
	
	/**
	 * Possible message types
	 */
	public enum MessageType {REGISTER,GETHASHES,GETLOCATIONS,GETDATA,FORWARD,ROUTE};
	
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
		
	public Message(URI source,URI destination,Object payload) {
		this.srcURI = source;
		this.dstURI = destination;
		this.payload = payload;
		this.id = last_id++;
		this.type = MessageType.REGISTER;
	}
	
	public URI getSource() {
		return srcURI;
	}
	
	public URI getDestination() {
		return dstURI;
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
		return srcURI.hashCode() ^ dstURI.hashCode() ^ id;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Message) {
        	Message msg = (Message) o;
            return ((srcURI.equals(msg.getSource()) && dstURI.equals(msg.getDestination())) ||
            		(srcURI.equals(msg.getDestination()) && dstURI.equals(msg.getSource())))
            		&& id == msg.id;
        } else {
        	return false;
        }
	}
	
	public String toString() {
		return String.format("{type=%s response=%s src=%s dst=%s id=%d payload=%s}",
				type, response, srcURI, dstURI, id, payload);
	}
}
