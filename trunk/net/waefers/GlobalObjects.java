package net.waefers;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.waefers.directory.NodeEntry;
import net.waefers.messaging.Message;
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
	 * Reference to curNodes in ReplicaMaster
	 * Initialized by ReplicaMaster
	 */
	public static HashSet<Node> curNodes;
	
	/**
	 * Incoming message queue
	 * <msg.id,msg>
	 * Initialized by MessageControl
	 */
	protected static HashMap<Integer,Message> queue = new HashMap<Integer,Message>();
	
	/**
	 * List of incoming messages
	 * <msg.id>
	 * Initialized by MessageControl
	 */
	protected static LinkedList<Integer> queueList = new LinkedList<Integer>();
	
	/**
	 * HashMap of all messages received sans payload indexed by ID
	 * Initialized by MessageControl
	 */
	protected static HashMap<Integer,Message> msgLog = new HashMap<Integer,Message>();
	
	/**
	 * HashSet of all messages waiting for responses
	 * Initialized by MessageControl
	 */
	protected static HashSet<Integer> waiting = new HashSet<Integer>();

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
	public static final Node baseNode = new Node( URI.create("nodemaster@waefers"), new InetSocketAddress("baseNode",DEFAULT_PORT), Type.MASTER);
	
	/**
	 * Global status file
	 * Contains most major variables
	 */
	public static PrintWriter status;
	
	/**
	 * Time between node heartbeats (minutes)
	 */
	public static final int HEARTBEAT_TIME = 5;
	
	/**
	 * Maximum single Message size [1472(single packet size) * #ofPackets]
	 * TODO: Split large messages to multiple packets instead of relying on packet fragmenting
	 */
	public static final int MAX_MESSAGE_SIZE = 1472 * 10; 
}
