/**
 * 
 */
package net.waefers.client;

import static net.waefers.GlobalControl.log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;

import net.waefers.GlobalControl;
import net.waefers.block.Block;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node;

/**
 * 
 * Client for testing infrastructure
 * 
 * @author Michael Ansel
 *
 */
public class TestClient extends Thread{

	
	/**
	 * @param args [-d logfile.txt] listenhost[:port]
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) {
		try {
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
		
		String s,host;
		int port;
		
		SocketAddress addr;
		Node node = new Node();
		node.uri = new URI("testpeer@waefers");
		log.finest("Local node uri:"+node.uri.toString());
		node.type = Node.Type.PEER;
		Block b = new Block();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(new String("Test string!").getBytes());
			b.id = md.digest();
			log.finest(String.valueOf(b.id));
		} catch (Exception e) {
			log.throwing("TestClient", "main", e);
		}
		Block block = new Block();
		b.id = b.id;
		node.dataStored.add(block);
		Message msg = new Message(node, new Node(URI.create("nodemaster@waefers")), node);
		
		
		if((s=argList.remove(0)).indexOf(":")>0) {
			host = s.substring(0, s.indexOf(":"));
			port = Integer.parseInt(s.substring(s.indexOf(":")+1));
			addr = (SocketAddress) new InetSocketAddress(host,port);
		} else {
			host = s;
			addr = (SocketAddress) new InetSocketAddress(host,(int)(Math.random() * 64511)+1024); //Randomly select a port between 1024 and 65535
		}
		
		MessageControl.init(addr);

		while(true) {
			MessageControl.send(msg, false);
			Thread.sleep( (long) 10 * 1000 );
		}
		
		} catch(Exception e) {
			log.throwing("TestClient", "main", e);
		}
	}
	
}