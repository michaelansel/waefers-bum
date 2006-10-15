package net.waefers.master;

import static net.waefers.GlobalControl.log;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import net.waefers.GlobalControl;
import net.waefers.block.Block;
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
	private static HashMap<byte[],HashSet<Node>> blockLocs;
	
	/**
	 * Current nodes registered on the NodeMaster
	 */
	private HashSet<Node> curNodes;
	
//Methods specific to this Server	
	
	/**
	 * Process location message and return requested information/success-fail
	 * @param msg
	 * @return reply message
	 */
	protected Message blockLocation(Message msg) {
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
	
	public void start() {
		blockLocs = new HashMap<byte[],HashSet<Node>>();
		MessageControl.init();
		receiveAndProcess();
	}
	
	/**
	 * Start a ReplicaMaster
	 * @param args [-d filename.log]
	 */
	public static void main(String[] args) throws IOException {
		ReplicaMaster rm = new ReplicaMaster();
		rm.processArgs(args);
		GlobalControl.logToConsole();
		rm.start();
	}

}