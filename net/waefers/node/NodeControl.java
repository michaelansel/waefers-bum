package net.waefers.node;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;

import net.waefers.directory.NodeEntry;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node.Type;
import static net.waefers.GlobalControl.*;
import static net.waefers.GlobalObjects.*;



/**
 * 
 * Static methods for dealing with nodes
 * 
 * @author Michael Ansel
 *
 */

public class NodeControl {
	
	public static SocketAddress getSocketAddress(Node node) {
		log.finest("Finding socket address for node="+node);
		
		/* If looking for baseNode */
		if(node.uri.equals(baseNode.uri)) {
			log.finest("Returning baseNode="+baseNode);
			return baseNode.address;
		}
		
		/* If peer to peer system is in use */
		if(peer2peer) {
			//check local finger table
			//find closest node and have them search for the node
		}
		
		/* If master system is in use */
		if(!peer2peer) {
			Message msg = new Message(new Node(URI.create("nodecontrol@waefers")),baseNode,node);
			msg.type = Message.Type.NODE_LOCATION;
			if(!MessageControl.initialized) { 
				MessageControl.initRand(); //Randomly select a port between 1024 and 65535
			}
			
			/* If we are the baseNode, get from the directory */
			//TODO: Could fail if bound to same address as baseNode, but not really baseNode
			if( baseNode.address.equals(MessageControl.getAddress()) ) {
				Node foundNode = getFromDirectory(((Node)msg.getPayload()).uri).node;
				log.finest("We are the baseNode, getting from directory; node="+node);
				return foundNode.address;
			}
			
			/* If we are not the baseNode, request from baseNode */
			Message reply = MessageControl.send(msg,true);
			if(reply.response==Message.Response.SUCCESS) {
				log.finest("Node found. Returning node="+(Node)reply.getPayload());
				return ((Node)reply.getPayload()).address;
			}
		}
		log.finest("Node not found. Returning 0.0.0.0:0");
		return new InetSocketAddress("0.0.0.0",0);
	}

	
	public static NodeEntry getFromDirectory(URI uri) {
		if(!nodeDirectory.containsKey(uri)) {
			log.finest("URI not in the nodeDirectory; uri="+uri);
			return null;
		}
		return nodeDirectory.get(uri);
	}
}
