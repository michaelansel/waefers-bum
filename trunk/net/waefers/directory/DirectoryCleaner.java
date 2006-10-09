package net.waefers.directory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.TreeMap;

import net.waefers.master.ReplicaControl;
import net.waefers.master.ReplicaMessage;
import static net.waefers.master.ReplicaMessage.RMessageType.*;
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
	
	TreeMap<Date,NodeEntry> nodeExpiry = null;
	
	public DirectoryCleaner(TreeMap<Date,NodeEntry> nodeExpiry) {
		this.setPriority(this.getPriority()-1);
		this.nodeExpiry = nodeExpiry;
		
	}
	
	public void run() {
		for(Date key : nodeExpiry.headMap(new Date()).keySet()) {
			NodeEntry deadNode = nodeExpiry.get(key);
			for(int block : deadNode.node.dataStored) {
				//Remove <block,<node>> from replica master
				ReplicaMessage msg = new ReplicaMessage(REMOVE,block,deadNode.node.uri);
				try {
					ReplicaControl.sendToReplicas(msg);
				} catch (Exception e) {
					log.throwing("DirectoryCleaner","run",e);
				}
				//blockDirectory.get(block).remove(deadNode.node.uri);
			}
		}
	}

}
