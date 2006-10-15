package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node.NodeType;
import static net.waefers.GlobalControl.*;
import static net.waefers.messaging.Message.Type;



/**
 * 
 * Static methods for dealing with nodes
 * 
 * @author Michael Ansel
 *
 */

public class NodeControl {
	
	/**
	 * Static node to be used as a starting point for all new peers
	 */
	static final Node baseNode = new Node( URI.create("nodemaster@waefers"), new InetSocketAddress("192.168.1.101",51951), NodeType.PEER);
	
	public static SocketAddress getSocketAddress(Node node) {
		//If looking for baseNode
		if(node.uri.equals(baseNode.uri)) return baseNode.address;
		
		//If peer to peer system is in use
		if(peer2peer) {
			//check local finger table
			//find closest node and have them search for the node
		}
		
		//If master system is in use
		if(!peer2peer) {
			Message msg = new Message(new Node(URI.create("nodecontrol@waefers")),baseNode,node);
			msg.type = Type.NODE_LOCATION;
			MessageControl.init((int)(Math.random() * 64511)+1024); //Randomly select a port between 1024 and 65535
			Message reply = MessageControl.send(msg,true);
			if(reply.response==Message.Response.SUCCESS) return ((Node)reply.getPayload()).address;
		}
		return new InetSocketAddress("0.0.0.0",0);
	}

}
