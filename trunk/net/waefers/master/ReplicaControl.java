package net.waefers.master;

import static net.waefers.GlobalControl.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import net.waefers.Message;
import net.waefers.MessagingContext;
import net.waefers.Node;

public class ReplicaControl {
	
	/**
	 * Contains a list of all registered replica masters
	 * Treated as a circular buffer, as each RM is referenced
	 * it is thrown to the back of the list (by incrementing the
	 * current position)
	 */
	public static LinkedList<Node> replicaList = null;
	
	
	
	public static void sendToReplicas(ReplicaMessage msg) throws URISyntaxException, IOException {
		for(Node replica : replicaList) {
			Message msg1 = new Message(new URI("replicacontrol@waefer"),replica.uri,msg);
			MessagingContext.send(msg1);
		}
	}

}
