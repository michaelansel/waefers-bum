package net.waefers;

import static net.waefers.GlobalControl.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;


/**
 * 
 * Creates an interface for managing messages.
 * 
 * @author Michael Ansel
 *
 */
public class MessagingContext {
	
	/**
	 * DatagramChannel to be used for message communications
	 */
	private DatagramChannel socket;
	
	/**
	 * Receive buffer
	 */
	ByteBuffer rbuf = ByteBuffer.wrap(new byte[1472]);
	
	/**
	 * Register with the selector
	 * @param selector selector to register with
	 * @return 
	 */
	public SelectionKey registerChannel(Selector selector) {
		try {
			socket.configureBlocking(false);
			return socket.register(selector, SelectionKey.OP_READ);
		}catch(Exception e) {
			log.throwing("MessagingContext","registerChannel",e);
			return null;
		}
	}
	
	/**
	 * Send a message
	 * @param msg message to send
	 * @throws IOException
	 */
	public void send(Message msg) throws IOException {
		if (msg.bbuf == null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(msg);
			oos.close();
			msg.bbuf = ByteBuffer.wrap(bos.toByteArray());
		}
		msg.bbuf.mark();
		socket.send(msg.bbuf, socket.socket().getLocalSocketAddress());
		msg.bbuf.reset();
		log.fine("Sending to "+socket.socket().getLocalSocketAddress()+": msg="+msg);
	}
	
	/**
	 * Gets the next incoming message
	 * @param skey Selection key
	 * @return Next incoming message
	 * @throws IOException
	 */
	public Message receive(SelectionKey skey) throws IOException {
		try {
			synchronized(rbuf) {
				socket.receive(rbuf);
				ByteArrayInputStream bis = new ByteArrayInputStream(rbuf.array());
				ObjectInputStream ois = new ObjectInputStream(bis);
				Message msg = (Message) ois.readObject();
				ois.close();
				rbuf.clear();
				return msg;
			}
		} catch (Exception e) {
			log.throwing("MessagingContext", "receive", e);
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
	public Message createMessage(URI source,URI destination,Object payload) {
		return new Message(source,destination,payload);
		
	}
	
	/**
	 * Creates a new reply by reversing the source and destination
	 * URIs and giving the new Message the same ID as the old
	 * @param msg Message to reply to
	 * @param payload Payload for reply
	 * @return new reply message
	 */
	public Message createReply(Message msg, Object payload) {
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
	public Message setMessage(Message msg,Object payload) {
		msg.payload = payload;
		msg.bbuf = null;
		return msg;
	}
	
	/**
	 * Close the messaging interface
	 *
	 */
	public void close() {
		try {
			socket.close();
		}catch (IOException e) {
			log.throwing("MessagingContext", "close", e);
		}
		
	}
}