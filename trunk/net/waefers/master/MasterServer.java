package net.waefers.master;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import net.waefers.GlobalControl;
import net.waefers.directory.DirectoryCleaner;
import net.waefers.directory.NodeEntry;
import net.waefers.filesystem.FileSystemObject;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node;
import static net.waefers.GlobalControl.log;
import static net.waefers.master.ReplicaControl.replicaList;
import static net.waefers.messaging.Message.ResponseType.*;

/**
 * 
 * The main server for managing filename to hash/block mappings.
 * Can also start one or more replica servers and the STUNT Rendezvous server
 * 
 * @author Michael Ansel
 *
 */
public class MasterServer extends Thread{
	
	DatagramSocket server;
	
	/**
	 * Contains filename->block[] mapping
	 * HashMap<filename,FileEntry(contains block[])>
	 */
	private HashMap<String,FileSystemObject> fileDirectory = null;
	
	/**
	 * Contains block->URI mapping
	 * HashMap<blockID,URI>
	 * 
	 * Move to replica server
	 */
	private HashMap<Integer,HashSet<URI>> blockDirectory = null;
	
	/**
	 * Contains directory of all up-to-date peers
	 */
	private HashMap<URI,NodeEntry> nodeDirectory = null;
	
	private TreeMap<Date,NodeEntry> nodeExpiry = null;
	

	/**
	 * Initialize listening socket and directories
	 * @throws SocketException
	 */
	MasterServer() throws SocketException {
		server = new DatagramSocket(51951);
		fileDirectory = new HashMap<String,FileSystemObject>();
		blockDirectory = new HashMap<Integer,HashSet<URI>>(); //Move to replica server
		nodeDirectory = new HashMap<URI,NodeEntry>(); //Move to separate node directory server??
		nodeExpiry = new TreeMap<Date,NodeEntry>();
		replicaList = new LinkedList<Node>();
		new DirectoryCleaner(nodeExpiry).start();
		MessageControl.init();
	}
	
	
	/**
	 * Listen for incoming UDP packets and respond accordingly
	 */
	public void run() {
		log.finest("Master server run method starting");

		while(true) {
			Message msg = MessageControl.receive();
			NodeEntry src;

			if(msg == null) continue;
			
			switch(msg.type) {
			case HEARTBEAT:
				src = nodeDirectory.get(msg.getSource());
				
				if(src == null || src.expires.before(new Date())) {
					NodeEntry ne = new NodeEntry((Node)msg.getPayload());
					nodeDirectory.put(((Node)msg.getPayload()).uri,ne);
					ne.updateExpiryTime();
					nodeExpiry.put(ne.expires,ne);
					log.fine(msg.getSource() + " registered as " + msg.srcAddr);
					msg = MessageControl.createReply(msg,replicaList);
					msg.response = SUCCESS;
				} else if(src.node.address.equals(msg.srcAddr)) { //If IP address is equal
					src.updateExpiryTime();
					if(!src.node.address.equals(msg.srcAddr)) //If IP:port is not equal
						log.fine(msg.getSource() + " is now " + msg.srcAddr);
	
					msg = MessageControl.createReply(msg,replicaList);
					msg.response = SUCCESS;
				} else {
					msg = MessageControl.createReply(msg,replicaList);
					msg.response = ERROR;
				}
				
				break;
			case BLOCK: //Packet concerning actual blocks
				break;
			case METADATA: //Packet concerning file system information
				break;
			case LOCATION: //Packet concerning block replica locations
				break;
			default:
				break;
			}
			
		}
		
		//log.finest("Master server run method ending");
	}

	/**
	 * Start a MasterServer
	 * @param args [-d filename.log] <Replica URI:hostname:port> <ReplicaURI:hostname:port>...
	 */
	public static void main(String[] args) throws IOException {
		ArrayList<String> argList = new ArrayList<String>();
		for (String arg : args) {
			log.finer("argument:"+arg);
			argList.add(arg);
		}
		if(args[0].equalsIgnoreCase("-d")) {
			GlobalControl.logToFile(args[1]);
			argList.remove(0);
			argList.remove(0);
		}

		new MasterServer().start();
		
		Iterator stepper = argList.iterator();
		while(stepper.hasNext()) {
			String uri,host,port,next;
			next = (String) stepper.next();
			uri = next.substring(0,next.indexOf(":"));
			host = next.substring(next.indexOf(":"),next.lastIndexOf(":"));
			port = next.substring(next.indexOf(":"),next.length());
			new ReplicaServer(uri,host,port).start();
		}
		
	}

}
