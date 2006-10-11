package net.waefers.messaging;

import static net.waefers.GlobalControl.log;
import static net.waefers.master.ReplicaControl.replicaList;
import static net.waefers.messaging.Message.ResponseType.ERROR;
import static net.waefers.messaging.Message.ResponseType.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Date;

import net.waefers.directory.NodeEntry;
import net.waefers.node.Node;
import net.waefers.node.NodeControl;



/**
 * 
 * Creates an interface for managing messages.
 * 
 * @author Michael Ansel
 *
 */
public class MessageControl {
	
	/**
	 * DatagramChannel to be used for message communications
	 */
	static DatagramChannel server;
	
	/**
	 * Receive buffer
	 */
	static ByteBuffer rbuf = ByteBuffer.wrap(new byte[1472]);
	
	/**
	 * Selector and selectionKey
	 */
	static Selector selector;
	static SelectionKey key;
	
	static ByteArrayOutputStream bos;
	static ObjectOutputStream oos;
	static ByteBuffer bbuff;
	
	/**
	 * Register with the selector
	 * @param selector selector to register with
	 * @return 
	 */
	public static boolean init() {
		try {
			server.configureBlocking(false);
			key = server.register(selector, SelectionKey.OP_READ);
			open();
			
			
			return true;
		}catch(Exception e) {
			log.throwing("MessagingContext","registerChannel",e);
			return false;
		}
	}
	
	/**
	 * Send a message
	 * @param msg message to send
	 * @throws IOException
	 */
	public static void send(Message msg) throws IOException {
		if (msg.bbuf == null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(msg);
			oos.close();
			msg.bbuf = ByteBuffer.wrap(bos.toByteArray());
		}
		msg.bbuf.mark();
		server.send(msg.bbuf, NodeControl.getSocketAddress(msg.getDestination()));
		msg.bbuf.reset();
		log.fine("Sending to "+server.socket().getLocalSocketAddress()+": msg="+msg);
	}

	/**
	 * Gets the next incoming message
	 * @param skey Selection key
	 * @return Next incoming message
	 * @throws IOException
	 */
	public static Message receive() {
		Message msg = null;
		
		try {
			bos = new ByteArrayOutputStream(1472);
			bos.reset();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			synchronized(bbuff) {
				server.receive(bbuff);
				bbuff.flip();
				DatagramPacket pkt = new DatagramPacket(bbuff.array(),bbuff.limit());
				if(pkt.getLength() == 0) return null;
				
				/* Convert packet back into a Message */
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(pkt.getData()));
				msg = (Message) ois.readObject();
				
				msg.srcAddr = pkt.getSocketAddress();
				msg.dstAddr = server.socket().getLocalSocketAddress();
				
				log.finer(String.format("RECEIVED: addr=%s size=%d msg=%s", pkt.getSocketAddress(), pkt.getLength(), msg));
				
				return msg;
			}
		} catch(Exception e) {
			log.throwing("MasterServer", "run", e);
			return null;
		}
	}
	
	/**
	 * Creates a new Message object
	 * @param source local address
	 * @param destination remote address
	 * @param payload message data
	 * @return new Message object
	 */
	public static Message createMessage(URI source,URI destination,Object payload) {
		return new Message(source,destination,payload);
		
	}
	
	/**
	 * Creates a new reply by reversing the source and destination
	 * URIs and giving the new Message the same ID as the old
	 * @param msg Message to reply to
	 * @param payload Payload for reply
	 * @return new reply message
	 */
	public static Message createReply(Message msg, Object payload) {
		Message rmsg = new Message(msg.getDestination(),msg.getSource(),payload);
		rmsg.id = msg.id;
		return rmsg;
	}
	
	/**
	 * Modify an existing message and erase
	 * the cached serialized version
	 * @param msg Message to modify
	 * @param payload Payload for message
	 * @return modified messge
	 */
	public static Message setMessage(Message msg,Object payload) {
		msg.payload = payload;
		msg.bbuf = null;
		return msg;
	}
	
	/**
	 * Open the messaging interface
	 *
	 */
	public static void open() {
		try {
			DatagramChannel.open();
		}catch (IOException e) {
			log.throwing("MessagingContext", "open", e);
		}
		
	}
	
	/**
	 * Close the messaging interface
	 *
	 */
	public static void close() {
		try {
			server.close();
		}catch (IOException e) {
			log.throwing("MessagingContext", "close", e);
		}
		
	}
}