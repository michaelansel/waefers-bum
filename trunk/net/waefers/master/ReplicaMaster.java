package net.waefers.master;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import static net.waefers.GlobalControl.DEFAULT_PORT;

import net.waefers.messaging.Message;

public class ReplicaMaster extends MasterServer {

	//Constructors
	
	public ReplicaMaster(SocketAddress addr) throws SocketException {
		super(addr);
	}
	public ReplicaMaster(int port) throws SocketException {
		this(new InetSocketAddress(port));
	}
	public ReplicaMaster(String hostname,int port) throws SocketException {
		this(new InetSocketAddress(hostname,port));
	}
	public ReplicaMaster(String hostname) throws SocketException {
		this(new InetSocketAddress(hostname,DEFAULT_PORT));
	}
	public ReplicaMaster() throws SocketException {
		this(new InetSocketAddress(DEFAULT_PORT));
	}
	
	public void run() {
		super.run();
	}
	
	private Message block(Message msg) {
		return null;
	}

}