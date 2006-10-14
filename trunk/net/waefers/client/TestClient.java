/**
 * 
 */
package net.waefers.client;

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
	 * @param args [-d logfile.txt] MasterIP[:port]
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
		
		DatagramSocket server = new DatagramSocket(1472);
		SocketAddress addr;
		ByteArrayOutputStream bos;
		ObjectOutputStream oos;
		Node node = new Node();
		node.uri = new URI("nodemaster@waefers");
		log.finest("Local node uri:"+node.uri.toString());
		node.type = Node.NodeType.PEER;
		Block b = new Block();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(new String("Test string!").getBytes());
			b.id = md.digest().toString();
			log.finest(String.valueOf(b.id));
		} catch (Exception e) {
			log.throwing("TestClient", "main", e);
		}
		node.dataStored.add(b.id);
		Message msg = MessageControl.createMessage(node.uri, new URI("filemaster@waefer"), node);
		
		
		if((s=argList.remove(0)).indexOf(":")>0) {
			host = s.substring(0, s.indexOf(":"));
			port = Integer.parseInt(s.substring(s.indexOf(":")+1));
			addr = (SocketAddress) new InetSocketAddress(host,port);
		} else {
			host = s;
			addr = (SocketAddress) new InetSocketAddress(host,51951);
		}
		
		try {
			bos = new ByteArrayOutputStream(1472);
			bos.reset();
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		oos = new ObjectOutputStream(bos);
		oos.writeObject(msg);
		oos.flush();
		
		while(true) {
			server.send(new DatagramPacket(bos.toByteArray(), bos.size(), addr));
			log.finer(String.format("SENT: addr=%s size=%d msg=%s", addr, bos.size(), msg));
			Thread.sleep( (long) 10*1000 );
		}
		
		} catch(Exception e) {
			log.throwing("TestClient", "main", e);
		}
	}
	
}