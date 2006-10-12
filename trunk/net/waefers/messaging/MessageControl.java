package net.waefers.messaging;

import static net.waefers.GlobalControl.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import net.waefers.messaging.Message.Response;
import net.waefers.node.NodeControl;
import static net.waefers.GlobalControl.DEFAULT_PORT;



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
	 * Incoming message queue
	 */
	static HashMap<Integer,Message> queue = new HashMap<Integer,Message>();
	
	/**
	 * List of incoming messages
	 */
	static LinkedList<Integer> queueList = new LinkedList<Integer>();
	
	/**
	 * Array of all messages received sans payload indexed by ID
	 */
	static HashMap<Integer,Message> msgLog = new HashMap<Integer,Message>();
	
	/**
	 * Selector and selectionKey
	 */
	static Selector selector;
	static SelectionKey key;
	
	static ByteArrayOutputStream bos;
	static ObjectOutputStream oos;
	
	/**
	 * Register with the selector
	 * @param selector selector to register with
	 * @return 
	 */
	public static boolean init(SocketAddress addr) {
		try {
			server.configureBlocking(false);
			key = server.register(selector, SelectionKey.OP_READ);
			DatagramChannel.open();
			server.socket().bind(addr);
			bos = new ByteArrayOutputStream(1472);
			
			
			return true;
		}catch(Exception e) {
			log.throwing("MessagingContext","registerChannel",e);
			return false;
		}
	}
	
	public static boolean init(int port) {
		return init(new InetSocketAddress(port));
	}
	
	public static boolean init(String hostname, int port) {
		return init(new InetSocketAddress(hostname,port));
	}
	
	public static boolean init(String hostname) {
		return init(new InetSocketAddress(hostname,DEFAULT_PORT));
	}
	
	public static boolean init() {
		return init(new InetSocketAddress(DEFAULT_PORT));
	}
	
	/**
	 * Send a message, no verification
	 * @param msg message to send
	 * @throws IOException
	 */
	public static void send(Message msg) {
		send(msg,false);
	}
	
	/**
	 * Send a message with verification (returns reply)
	 * @param msg message to send
	 * @param verify whether or not to guarantee reply
	 * @return Message verification message received or null if verify=false
	 * @throws IOException
	 */
	public static Message send(Message msg,boolean verify) {
		try {
			if (msg.bbuf == null) {
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(msg);
				oos.close();
				msg.bbuf = ByteBuffer.wrap(bos.toByteArray());
			}
			msg.bbuf.mark();
			Message rmsg = null;
			while(rmsg == null) {
				Timer timer = new Timer();
				timer.schedule(new SendTimerTask(msg), 0, 10*1000);
				if(!verify) break;
				rmsg = receive(msg.id,true); //Check for instantaneous response
				Thread.sleep( (long) 1000 ); //Give message time to go back and forth
				rmsg = receive(msg.id,true); //Due to recursiveness of receive method, has possibility of never returning. Need a way to prevent this and a) kill request; and b) resend message then retry receive
			}
			return rmsg;
		} catch (Exception e) {
			log.throwing("MessageControl", "send", e);
			return null;
		}
	}
	
	static class SendTimerTask extends TimerTask {
		
		Message msg;
		
		SendTimerTask(Message msg) {
			this.msg = msg;
		}
		
		public void run() {
			try {
				server.send(msg.bbuf, NodeControl.getSocketAddress(msg.getDestination()));
				msg.bbuf.reset();
				log.fine("Sending to "+server.socket().getLocalSocketAddress()+": msg="+msg);
			} catch(IOException e) {
				log.throwing("MessageControl", "SendTimerTask.run", e);
			}
		}
	}

	/**
	 * Gets the next incoming message
	 * @param skey Selection key
	 * @return Next incoming message
	 * @throws IOException
	 */
	public static Message receive() {
		if(!queue.isEmpty()) {
			Message msg = queue.remove(queueList.removeFirst());
			msgLog.put(msg.id,msg.noPayload()); //msg.noPayload() might kill the payload on the original message
			return msg;
		}
		return receive(0,false);
	}
	
	public static Message receive(int id) {
		return receive(id,true);
	}
	
	public static Message receive(int id,boolean checkID) {
		Message msg = null;
		try {
			synchronized(rbuf) {
				server.receive(rbuf);
				rbuf.flip();
				DatagramPacket pkt = new DatagramPacket(rbuf.array(),rbuf.limit());
				if(pkt.getLength() == 0) return null;
				
				/* Convert packet back into a Message */
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(pkt.getData()));
				msg = (Message) ois.readObject();
				
				if(msg.id==0) return null;
				
				msg.srcAddr = pkt.getSocketAddress();
				msg.dstAddr = server.socket().getLocalSocketAddress();
				
				log.finer(String.format("RECEIVED: addr=%s size=%d msg=%s", pkt.getSocketAddress(), pkt.getLength(), msg));
				
				if(!checkID) {
					msgLog.put(msg.id,msg.noPayload());
					return msg;
				}
				if(msg.id==id) {
					msgLog.put(msg.id,msg.noPayload());
					return msg;
				}
				queue.put(msg.id, msg);
				queueList.addLast(msg.id);
			}
		} catch(Exception e) {
			log.throwing("MasterServer", "run", e);
			return null;
		}
		return receive(id,checkID);
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
	public static Message createReply(Message msg, Response response, Object payload) {
		Message rmsg = new Message(msg.getDestination(),msg.getSource(),payload);
		rmsg.id = msg.id;
		rmsg.response = response;
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