package net.waefers.directory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.TreeMap;

import net.waefers.master.NodeMaster;
import net.waefers.messaging.LocationMessage;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;

import static net.waefers.GlobalControl.log;



/**
 * 
 * Background thread to police directory and remove obsolete entries
 * 
 * @author Michael Ansel
 *
 */
public class DirectoryCleaner extends Thread {
	
	static final int POLL_INTERVAL = 10/60; //How often to check for expired entries (minutes)
	
	NodeMaster nodeMaster;
	
	public DirectoryCleaner(NodeMaster nm) {
		this.setPriority(this.getPriority()-1);
		this.nodeMaster = nm;
		
	}
	
	public void run() {
		for(Date key : nodeMaster.getExpired().keySet()) {
			NodeEntry deadNode = nodeMaster.removeExpiredEntry(key);
			MessageControl.initRand();
			
			LocationMessage lMsg = new LocationMessage();
			lMsg.action = LocationMessage.Action.REMOVE_FULL;
			lMsg.blocks = deadNode.node.dataStored;
			lMsg.node = deadNode.node;
			
			Message msg = new Message(URI.create("directorycleaner@waefers"),URI.create("replicamaster@waefers"),lMsg);
			msg.type = Message.Type.BLOCK_LOCATION;
			
			MessageControl.send(msg,false);
		}
	}

}
