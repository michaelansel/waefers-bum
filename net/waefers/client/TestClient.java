/**
 * 
 */
package net.waefers.client;

import static net.waefers.GlobalObjects.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Timer;

import net.waefers.GlobalControl;
import net.waefers.PrintQueue;
import net.waefers.PrintStatus;
import net.waefers.block.Block;
import net.waefers.messaging.Heartbeater;
import net.waefers.messaging.MessageControl;
import net.waefers.node.Node;

/**
 * 
 * Client for testing infrastructure
 * 
 * @author Michael Ansel
 *
 */
public class TestClient extends Thread {

	
	/**
	 * @param args [-d logfile.txt] [listenhost[:port]]
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
			GlobalControl.logToConsole();
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

		if(!argList.isEmpty()) {
			if((s=argList.remove(0)).indexOf(":")>0) {
				host = s.substring(0, s.indexOf(":"));
				port = Integer.parseInt(s.substring(s.indexOf(":")+1));
				addr = (SocketAddress) new InetSocketAddress(host,port);
			} else {
				host = s;
				addr = (SocketAddress) new InetSocketAddress(host,(int)(Math.random() * 64511)+1024); //Randomly select a port between 1024 and 65535
			}
		} else {
			host = "localhost";
			addr = (SocketAddress) new InetSocketAddress(host,(int)(Math.random() * 64511)+1024); //Randomly select a port between 1024 and 65535 
		}

		MessageControl.init(addr);
		Timer printer = new Timer();
		printer.schedule(new PrintQueue(), 10*1000, 30*1000);
		printer.schedule(new PrintStatus(), 0, 10*1000);

		Timer t = new Timer();
		t.schedule(new Heartbeater(node),0,HEARTBEAT_TIME*60*1000);
		
		} catch(Exception e) {
			log.throwing("TestClient", "main", e);
		}
	}
	
}