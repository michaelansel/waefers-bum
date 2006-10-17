package net.waefers.master;

import static net.waefers.GlobalControl.log;
import static net.waefers.messaging.Message.Response.ERROR;
import static net.waefers.messaging.Message.Response.SUCCESS;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.waefers.GlobalControl;
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
	
	public void addBlocks(Node node) {
		log.fine("Adding blocks to ReplicaMaster for "+node);
		LocationMessage lMsg = new LocationMessage();
		lMsg.action = LocationMessage.Action.ADD;
		lMsg.blocks = node.dataStored;
		lMsg.node = node;
		
		Message msg = new Message(node.uri,URI.create("replicamaster@waefers"),lMsg);
		msg.type = Message.Type.BLOCK_LOCATION;
		
		MessageControl.send(msg,true);
	}
	
	/**
	 * Process heartbeat message and add/update node in the directory
	 * @param msg
	 * @return
	 */
	protected Message heartbeat(Message msg) {
		if(msg.response!=null) {
			log.finest("Killing incoming message with a response already msg="+msg);
			return null;
		}
		Message rmsg;
		NodeEntry src;
		src = nodeDirectory.get(msg.getSource());
		log.finest("Heartbeat message received from "+msg.getSource().uri);
		
		//If node is not in the directory or if it has expired
		if( src == null || src.expires.before(new Date()) ) {
			NodeEntry ne = new NodeEntry((Node)msg.getPayload());
			nodeDirectory.put(((Node)msg.getPayload()).uri,ne);
			ne.updateExpiryTime();
			nodeExpiry.put(ne.expires,ne);
			log.fine(msg.getSource() + " registered as " + msg.srcSAddr);
			if(ne.node.isPeer()) addBlocks(ne.node);
			rmsg = MessageControl.createReply(msg,SUCCESS,null);
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
			log.fine(ne.node.uri + " registered as " + ne.node.address);
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
		new DirectoryCleaner(this).start();
		MessageControl.init();
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
