package net.waefers.master;

import static net.waefers.GlobalObjects.*;
import static net.waefers.messaging.Message.Response.ERROR;
import static net.waefers.messaging.Message.Response.SUCCESS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TreeMap;

import net.waefers.GlobalControl;
import net.waefers.GlobalObjects;
import net.waefers.PrintQueue;
import net.waefers.PrintStatus;
import net.waefers.directory.DirectoryCleaner;
import net.waefers.directory.NodeEntry;
import net.waefers.messaging.LocationMessage;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node;

public class NodeMaster extends MasterServer {

//Specific variable for this Server
	
	/**
	 * Directory of all up-to-date peers
	 */
	private HashMap<URI,NodeEntry> nodeDirectory = null;
	
	/**
	 * Map of expiration times to nodes for easy cleaning
	 */
	private TreeMap<Date,NodeEntry> nodeExpiry = null;

//Methods specific to this Server	
	
	/**
	 * Get all expired nodes
	 * @return Map of all expired nodes
	 */
	public Map<Date,NodeEntry> getExpired() {
		return nodeExpiry.headMap(new Date());
	}
	
	/**
	 * Remove and return the entry specified by key
	 * @param key
	 * @return removed entry
	 */
	public NodeEntry removeExpiredEntry(Date key) {
		return nodeExpiry.remove(key);
	}
	
	public Message addBlocks(Node node) {
		log.fine("Adding blocks to ReplicaMaster for "+node);
		LocationMessage lMsg = new LocationMessage();
		lMsg.action = LocationMessage.Action.ADD;
		lMsg.blocks = node.dataStored;
		lMsg.node = node;
		
		Message msg = new Message(node,new Node(URI.create("replicamaster@waefers")),lMsg);
		msg.type = Message.Type.BLOCK_LOCATION;
		
		return MessageControl.send(msg,true);
	}
	
	/**
	 * Process heartbeat message and add/update node in the directory
	 * @param msg
	 * @return
	 */
	protected Message heartbeat(Message msg) {
		if(msg.response!=null) {
			log.finest("Killing incoming heartbeat message with a response already msg="+msg);
			return null;
		}
		Message rmsg;
		NodeEntry src;
		src = nodeDirectory.get(msg.getSource());
		log.finest("Heartbeat message received from "+msg.getSource());
		
		//If node is not in the directory or if it has expired
		if( src == null || src.expires.before(new Date()) ) {
			if(msg.getSource().address==null) {
				msg.getSource().address=(InetSocketAddress)msg.srcSAddr;
				log.finest("Node has no address; using incoming address="+msg.srcSAddr);
			}
			NodeEntry ne = new NodeEntry(msg.getSource());
			nodeDirectory.put(msg.getSource().uri,ne);
			ne.updateExpiryTime();
			nodeExpiry.put(ne.expires,ne);
			log.fine(nodeDirectory.get(msg.getSource().uri).node + " registered as " + msg.srcSAddr);
			Message rab;
			if(ne.node.isPeer() && (rab = addBlocks(ne.node))!=null && rab.response==SUCCESS) { 
				rmsg = MessageControl.createReply(msg,SUCCESS,null);
			} else if(!ne.node.isPeer()) {
				rmsg = MessageControl.createReply(msg,SUCCESS,null);
			} else {
				log.finest("Failed to add blocks for node="+ne.node);
				rmsg = MessageControl.createReply(msg,ERROR,null);
			}
		} else if(src.node.address.equals(msg.srcSAddr)) { //If the node is really who it says it is
			src.updateExpiryTime();
			log.finer(src.node.uri + " expiration time updated");
				if(!src.node.address.equals(msg.srcSAddr)) //If the directory is out of sync with the node address
				log.fine(msg.getSource() + " is now " + msg.srcSAddr);

			rmsg = MessageControl.createReply(msg,SUCCESS,null);
		} else {
			log.finer("Unable to work with heartbeat");
			rmsg = MessageControl.createReply(msg,ERROR,null);
		}
		log.finest("Returning reply msg="+rmsg);
		return rmsg;
	}
	
	/**
	 * Process node address requests
	 * @param msg Raw incoming message to process
	 * @return Reply to incoming message
	 */
	protected Message nodeLocation(Message msg) {
		Message rmsg;
		NodeEntry ne;
		log.finest("Node location request received from:"+msg.getSource()+" for "+((Node)msg.getPayload()).uri);
		ne = nodeDirectory.get(((Node)msg.getPayload()).uri);
		
		//If node is in the directory and has not expired
		if(!(ne == null || ne.expires.before(new Date()))) {
			log.fine(ne.node.uri + " is currently registered as " + ne.node.address);
			rmsg = MessageControl.createReply(msg,SUCCESS,ne.node);
		} else {
			rmsg = MessageControl.createReply(msg,ERROR,null);
		}
		return rmsg;
	}
	
	protected void start() {
		log.finest("NM-Start");
		nodeDirectory = new HashMap<URI,NodeEntry>();
		nodeExpiry = new TreeMap<Date,NodeEntry>();
		GlobalObjects.nodeDirectory = nodeDirectory;
		new DirectoryCleaner(this).start();
		MessageControl.init();
		Timer printer = new Timer();
		printer.schedule(new PrintQueue(), 10*1000, 30*1000);
		printer.schedule(new PrintStatus(), 0, 10*1000);
		receiveAndProcess();
	}
	
	/**
	 * Start a NodeMaster
	 * @param args [-d filename.log]
	 */
	public static void main(String[] args) throws IOException {
		NodeMaster nm = new NodeMaster();
		nm.processArgs(args);
		GlobalControl.logToConsole();
		nm.start();
	}
}
