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
import java.util.HashMap;
import java.util.HashSet;

import net.waefers.block.Block;
import net.waefers.directory.DirectoryCleaner;
import net.waefers.messaging.LocationMessage;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node;

public class ReplicaMaster extends MasterServer {

//Specific variable for this Server
	
	/**
	 * Contains block->Node mapping
	 * HashMap<blockID,Node>
	 */
	private HashMap<String,HashSet<Node>> blockLocs;
	
	/**
	 * Current nodes registered on the NodeMaster
	 */
	private HashSet<Node> curNodes;
	
	
//Constructors
	
	public ReplicaMaster(SocketAddress addr) throws SocketException {
		super(addr);
		blockLocs = new HashMap<String,HashSet<Node>>();
	}
	public ReplicaMaster(int port) throws SocketException {
		this(new InetSocketAddress(port));
	}
	public ReplicaMaster(String hostname,int port) throws SocketException {
		this(new InetSocketAddress(hostname,port));
	}
	public ReplicaMaster(String hostname) throws SocketException {
		this(new InetSocketAddress(hostname,DEFAULT_PORT));
	}
	public ReplicaMaster() throws SocketException {
		this(new InetSocketAddress(DEFAULT_PORT));
	}

//Methods specific to this Server	
	
	/**
	 * Process heartbeat message and add/update node in the directory
	 * @param msg
	 * @return
	 */
	protected Message location(Message msg) {
		Message rmsg = null;
		LocationMessage lMsg = (LocationMessage) msg.getPayload();
		
		switch(lMsg.action) {
		case ADD:
			Message.Response[] status = null;
			int x = 0;
			//For every block on the peer
			for( Block block : (Block[]) lMsg.blocks.toArray() ) {
				log.finest("Adding block:" + block.id + " to directory\n");
				boolean success = false;
				//If the block is already in the directory
				if( blockLocs.containsKey(block) ) {
					//Add the node to the end of the set of nodes for this block
					success = blockLocs.get(block).add(lMsg.node);
					log.finest("Node added to existing block list");
				} else {
					//Create a new node set
					HashSet<Node> hs = new HashSet<Node>();
					//Add the node to the new set
					hs.add(lMsg.node);
					//Add the new set to the directory
					if(blockLocs.put(block.id, hs)!=null) success=true;
					log.finest("Node added to new block list");
				}
				//Set rmsg to action status
				if(success) status[x] = Message.Response.SUCCESS;
				status[x] = Message.Response.ERROR;
				x++;
			}
			break;
		}

		return rmsg;
	}
	
	public static void begin() throws SocketException {
		new ReplicaMaster().run();
	}

}