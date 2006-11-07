package net.waefers.master;

import static net.waefers.GlobalObjects.*;
import static net.waefers.GlobalControl.*;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;

import net.waefers.GlobalControl;
import net.waefers.GlobalObjects;
import net.waefers.PrintQueue;
import net.waefers.PrintStatus;
import net.waefers.block.Block;
import net.waefers.messaging.Heartbeater;
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
	private static HashSet<Node> curNodes;
	
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
			Message.Response[] status = new Message.Response[lMsg.blocks.size()];
			int x = 0;
			curNodes.add(lMsg.node);
			
			Object[] blocks = lMsg.blocks.toArray();
			
			//For every block on the peer
			for( Object objBlock : blocks ) {
				Block block = new Block();
				if(objBlock instanceof Block)
					block = (Block) objBlock;
				if(block.id == null) continue;
				log.finest("Adding block:" + byteArrayToHexString(block.id) + " to directory");
				boolean success = false;
				//If the block is already in the directory
				if( blockLocs.containsKey(block) ) {
					//Add the node to the end of the set of nodes for this block
					blockLocs.get(block).add(lMsg.node);
					success = (blockLocs.get(block.id).contains(lMsg.node));
					if(success)
						log.finest("Node added to existing block list");
				} else {
					//Create a new node set
					HashSet<Node> hs = new HashSet<Node>();
					//Add the node to the new set
					hs.add(lMsg.node);
					//Add the new set to the directory
					blockLocs.put(block.id, hs);
					success = (blockLocs.get(block.id).contains(lMsg.node));
					if(success) {
						log.finest("Node added to new block list");
					}
				}
				//Set rmsg to action status
				status[x] = Message.Response.ERROR;
				if(success) status[x] = Message.Response.SUCCESS;
				x++;
			}
			
			rmsg = MessageControl.createReply(msg, Message.Response.SUCCESS, status);
			
			for(Message.Response status2 : status) {
				if(status2 != Message.Response.SUCCESS)
					rmsg.response = Message.Response.ERROR;
			}
			
			break;
		}

		return rmsg;
	}
	
	public void start() {
		blockLocs = new HashMap<byte[],HashSet<Node>>();
		GlobalObjects.blockLocs = blockLocs;
		curNodes = new HashSet<Node>();
		GlobalObjects.curNodes = curNodes;
		MessageControl.initRand();
		Node node = new Node(URI.create("replicamaster@waefers"));
		node.type = Node.Type.MASTER;
		Timer t = new Timer();
		t.schedule(new Heartbeater(node), 0, 4*60*1000); //Update ReplicaMaster on NodeMaster every 4 minutes
		Timer printer = new Timer();
		printer.schedule(new PrintQueue(), 10*1000, 30*1000);
		printer.schedule(new PrintStatus(), 0, 10*1000);
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