package net.waefers.master;

import static net.waefers.GlobalControl.log;
import static net.waefers.GlobalControl.DEFAULT_PORT;
import static net.waefers.master.ReplicaControl.replicaList;
import static net.waefers.messaging.Message.Response.ERROR;
import static net.waefers.messaging.Message.Response.SUCCESS;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import net.waefers.directory.DirectoryCleaner;
import net.waefers.directory.NodeEntry;
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
	
	
//Constructors
	
	public NodeMaster(SocketAddress addr) throws SocketException {
		super(addr);
		nodeDirectory = new HashMap<URI,NodeEntry>();
		nodeExpiry = new TreeMap<Date,NodeEntry>();
		new DirectoryCleaner(nodeExpiry).start();
	}
	public NodeMaster(int port) throws SocketException {
		this(new InetSocketAddress(port));
	}
	public NodeMaster(String hostname,int port) throws SocketException {
		this(new InetSocketAddress(hostname,port));
	}
	public NodeMaster(String hostname) throws SocketException {
		this(new InetSocketAddress(hostname,DEFAULT_PORT));
	}
	public NodeMaster() throws SocketException {
		this(new InetSocketAddress(DEFAULT_PORT));
	}

//Methods specific to this Server	
	
	/**
	 * Process heartbeat message and add/update node in the directory
	 * @param msg
	 * @return
	 */
	protected Message heartbeat(Message msg) {
		Message rmsg;
		NodeEntry src;
		src = nodeDirectory.get(msg.getSource());
		
		if(src == null || src.expires.before(new Date())) {
			NodeEntry ne = new NodeEntry((Node)msg.getPayload());
			nodeDirectory.put(((Node)msg.getPayload()).uri,ne);
			ne.updateExpiryTime();
			nodeExpiry.put(ne.expires,ne);
			log.fine(msg.getSource() + " registered as " + msg.srcAddr);
			rmsg = MessageControl.createReply(msg,SUCCESS,replicaList);
		} else if(src.node.address.equals(msg.srcAddr)) { //If IP address is equal
			src.updateExpiryTime();
			if(!src.node.address.equals(msg.srcAddr)) //If IP:port is not equal
				log.fine(msg.getSource() + " is now " + msg.srcAddr);

			rmsg = MessageControl.createReply(msg,SUCCESS,replicaList);
		} else {
			rmsg = MessageControl.createReply(msg,ERROR,replicaList);
		}
		return rmsg;
	}
	
	public static void begin() throws SocketException {
		new NodeMaster().run();
	}

}
