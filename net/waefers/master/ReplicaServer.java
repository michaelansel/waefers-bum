package net.waefers.master;

import static net.waefers.GlobalControl.log;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 
 * Replica management server
 * 
 * @author Michael Ansel
 *
 */
public class ReplicaServer extends Thread {

	URI uri;
	
	InetSocketAddress addr;
	
	ReplicaServer(String uri, String host, String port) {
		try {
			this.uri = new URI(uri);
		} catch (URISyntaxException e) {
			log.throwing("ReplicaServer", "ReplicaServer", e);
		}
		addr = new InetSocketAddress(host,Integer.parseInt(port));
	}
	
	public void run() {
		log.finest("ReplicaServer thread starting");
		
		
		
		log.finest("ReplicaServer thread ending");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
