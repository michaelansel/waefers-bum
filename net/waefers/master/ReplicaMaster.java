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
	private HashMap<Integer,HashSet<Node>> blockLocs;
	
	/**
	 * Current nodes registered on the NodeMaster
	 */
	private HashSet<Node> curNodes;
	
	
//Constructors
	
	public ReplicaMaster(SocketAddress addr) throws SocketException {
		super(addr);
		blockLocs = new HashMap<Integer,HashSet<Node>>();
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
		Message rmsg;
		LocationMessage lMsg = (LocationMessage) msg.getPayload();
		
		switch(lMsg.action) {
		case ADD:
			for( Block block : (Block[]) lMsg.blocks.toArray() ) {
				if( blockLocs.containsKey(block) ) {
					blockLocs.get(block).add(lMsg.node);
				} else {
					blockLocs.put(block, (new HashSet<Node>).);
				}
			}
		}
		
		rmsg=null;
		return rmsg;
	}
	
	public static void begin() throws SocketException {
		new ReplicaMaster().run();
	}

}