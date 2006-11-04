package net.waefers;

import static net.waefers.GlobalObjects.DEFAULT_PORT;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import net.waefers.directory.NodeEntry;
import net.waefers.node.Node;
import net.waefers.node.Node.Type;

public class GlobalObjects {

	/**
	 * Reference to nodeDirectory in NodeMaster
	 * Initialized by NodeMaster
	 */
	public static HashMap<URI, NodeEntry> nodeDirectory;
	
	/**
	 * Reference to blockLocs in ReplicaMaster
	 * Initialized by ReplicaMaster
	 */
	public static HashMap<byte[], HashSet<Node>> blockLocs;

	/**
	 * Default port for all communications
	 */
	public static final int DEFAULT_PORT = 51951;
	
	/**
	 * Whether or not peer to peer system is active
	 */
	public static boolean peer2peer = false;
	
	/**
	 * Main logging object
	 */
	public static final Logger log = Logger.getLogger("global");
	
	/**
	 * Message time to live (seconds)
	 */
	public static final int MESSAGE_TTL = 60;
	
	/**
	 * Static node to be used as a starting point for all new peers
	 */
	public static final Node baseNode = new Node( URI.create("nodemaster@waefers"), new InetSocketAddress("localhost",DEFAULT_PORT), Type.MASTER);
	
	/**
	 * Global status file
	 * Contains most major variables
	 */
	public static PrintWriter status;
	
	/**
	 * Time between node heartbeats (minutes)
	 */
	public static final int HEARTBEAT_TIME = 5;
}
