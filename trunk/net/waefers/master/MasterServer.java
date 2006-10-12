package net.waefers.master;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.waefers.GlobalControl;
import net.waefers.filesystem.FileSystemObject;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;

import static net.waefers.GlobalControl.log;
import static net.waefers.messaging.Message.Response.*;
import static net.waefers.GlobalControl.DEFAULT_PORT;

/**
 * 
 * The main server for receiving messages and processing them.
 * Should be extended upon for specific message functions, otherwise all will return ERROR.
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
	 * Initialize listening socket and directories
	 * @throws SocketException
	 */
	public MasterServer(SocketAddress addr) throws SocketException {
		server = new DatagramSocket(addr);
		//fileDirectory = new HashMap<String,FileSystemObject>();
		//blockDirectory = new HashMap<Integer,HashSet<URI>>(); //Move to replica server
		//replicaList = new LinkedList<Node>();
		MessageControl.init();
	}
	
	public MasterServer(int port) throws SocketException {
		this(new InetSocketAddress(port));
	}
	
	public MasterServer(String hostname,int port) throws SocketException {
		this(new InetSocketAddress(hostname,port));
	}
	
	public MasterServer(String hostname) throws SocketException {
		this(new InetSocketAddress(hostname,DEFAULT_PORT));
	}
	
	public MasterServer() throws SocketException {
		this(new InetSocketAddress(DEFAULT_PORT));
	}
	
	
	/**
	 * Listen for incoming UDP packets and respond accordingly
	 */
	public void run() {
		log.finest("Master server run method starting");

		while(true) {
			Message msg = MessageControl.receive();
			Message rmsg = null;

			if(msg == null) continue;
			
			switch(msg.type) {
			case HEARTBEAT: //Heartbeat packet
				rmsg = heartbeat(msg);
				break;
			case BLOCK: //Packet concerning actual blocks
				rmsg = block(msg);
				break;
			case METADATA: //Packet concerning file system information
				rmsg = meta(msg);
				break;
			case LOCATION: //Packet concerning block replica locations
				rmsg = location(msg);
				break;
			default:
				rmsg = MessageControl.createReply(msg,ERROR,null);
				break;
			}
			
			MessageControl.send(rmsg);
		}
		
		//log.finest("Master server run method ending");
	}
	
	private Message heartbeat(Message msg) {
		return MessageControl.createReply(msg,ERROR,null);
	}
	
	private Message block(Message msg) {
		return MessageControl.createReply(msg,ERROR,null);
	}
	
	private Message meta(Message msg) {
		return MessageControl.createReply(msg,ERROR,null);
	}
	
	private Message location(Message msg) {
		return MessageControl.createReply(msg,ERROR,null);
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
