package net.waefers.master;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import net.waefers.GlobalControl;
import net.waefers.Message;
import net.waefers.NodeInfo.NodeType;
import static net.waefers.GlobalControl.log;

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
	 * Node time till expiration in minutesf
	 */
	private static final int NODE_TTL = 5;
	
	/**
	 * Contains filename->block[] mapping
	 * HashMap<filename,FileEntry(contains block[])>
	 */
	private HashMap<String,Entry> fileDirectory = null;
	
	/**
	 * Contains block->URI mapping
	 * HashMap<blockID,URI>
	 * 
	 * Move to replica server
	 */
	private HashMap<Integer,URI> blockDirectory = null;
	
	/**
	 * Contains directory of all up-to-date peers
	 */
	private HashMap<URI,Entry> nodeDirectory = null;
	
	/**
	 * Contains a list of all registered replica masters
	 * Treated as a circular buffer, as each RM is referenced
	 * it is thrown to the back of the list (by incrementing the
	 * current position)
	 */
	private LinkedList<URI> replicaList = null;
	
	/**
	 * Abstract class representing a directory entry
	 */
	abstract class Entry {
				
	}
	
	/**
	 * Represents a file->block[] mapping
	 */
	private class FileEntry extends Entry {
		ArrayList<Integer> blocks = new ArrayList<Integer>();
		int md5;
		String filename;
		String path;
		//Other file metadata goes here
		
		FileEntry(String path,String filename,int md5) {
			this.path = path;
			this.filename = filename;
			this.md5 = md5;
		}
	}
	
	/**
	 * Represents a directory->directory mapping
	 */
	private class DirectoryEntry extends Entry {
		String directoryName;
		String path;
		TreeMap<String,TreeMap> directory = null;
		//Other directory metadata goes here
		
		DirectoryEntry(String path, String name) {
			this.path = path;
			this.directoryName = name;
		}
	}
	
	/**
	 * Represents a registered node
	 */
	private class NodeEntry extends Entry {

		URI id;
		
		InetSocketAddress address;
		
		Date expires;
		
		NodeType type;
		
		NodeEntry(URI id,InetSocketAddress address,NodeType type) {
			this.id = id;
			this.address = address;
			Calendar tempCal = Calendar.getInstance();
			tempCal.add(Calendar.MINUTE, NODE_TTL);
			this.expires = tempCal.getTime();
			this.type = type;
		}
	}
	
	/**
	 * Initialize listening socket and directories
	 * @throws SocketException
	 */
	MasterServer() throws SocketException {
		server = new DatagramSocket(51951);
		fileDirectory = new HashMap<String,Entry>();
		blockDirectory = new HashMap<Integer,URI>(); //Move to replica server
		nodeDirectory = new HashMap<URI,Entry>();
		replicaList = new LinkedList<URI>();
	}
	
	
	/**
	 * Listen for incoming UDP packets and respond accordingly
	 */
	public void run() {
		log.finest("Master server run method starting");
		
		
		
		log.finest("Master server run method ending");
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
