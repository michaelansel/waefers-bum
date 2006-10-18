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
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashSet;

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
	private static DatagramChannel server = null;
	
	/**
	 * Receive buffer
	 */
	private static ByteBuffer rbuf = ByteBuffer.wrap(new byte[1472]);
	
	/**
	 * Incoming message queue
	 * <msg.id,msg>
	 */
	protected static HashMap<Integer,Message> queue = new HashMap<Integer,Message>();
	
	/**
	 * List of incoming messages
	 * <msg.id>
	 */
	protected static LinkedList<Integer> queueList = new LinkedList<Integer>();
	
	/**
	 * HashMap of all messages received sans payload indexed by ID
	 */
	private static HashMap<Integer,Message> msgLog = new HashMap<Integer,Message>();
	
	/**
	 * HashSet of all messages waiting for responses
	 */
	private static HashSet<Integer> waiting = new HashSet<Integer>();
	
	/**
	 * Selector and selectionKey
	 */
	private static Selector selector;
	private static SelectionKey key;
	
	private static ByteArrayOutputStream bos = null;
	private static ObjectOutputStream oos = null;
	
	/**
	 * Whether or not MessageControl has been initialized yet
	 */
	public static boolean initialized = false;
	
	/**
	 * Register with the selector
	 * @param selector selector to register with
	 * @return 
	 */
	public static boolean init(SocketAddress localAddr) {
		try {
			server = DatagramChannel.open();
			selector = Selector.open();
			server.configureBlocking(false);
			key = server.register(selector, SelectionKey.OP_READ);
			server.socket().bind(localAddr);
			bos = new ByteArrayOutputStream(1472);
			initialized = true;
			
			log.finest("MessageControl initialized on "+localAddr);
			
			return true;
		}catch(Exception e) {
			log.throwing("MessageControl","registerChannel",e);
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
		return init(new InetSocketAddress("localhost",DEFAULT_PORT));
	}
	
	public static boolean initRand() {
		return init((int)(Math.random() * 64511)+1024);
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
	 * @param verify whether or not to wait for reply
	 * @return Message verification message received or null if verify=false
	 * @throws IOException
	 */
	public static Message send(Message msg,boolean verify) {
		try {
			if(msg.bbuf != null) msg.bbuf.clear();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(msg);
			oos.close();
			msg.bbuf = ByteBuffer.wrap(bos.toByteArray());
			
			msg.bbuf.mark();
			Message rmsg = null;

			Timer timer = new Timer();
			new SendTimerTask(msg).run();
			timer.schedule( new SendTimerTask(msg), (long) 10*1000, (long) 10*1000 ); //Schedule the resender

			if(!verify) {
				timer.cancel();
				return null;
			}
			/* Add this message to the list of messages being waited for */
			waiting.add(msg.id);
			
			/* Wait for a response */
			while(rmsg == null) {
				//log.finest("Waiting for the reply to message id="+msg.id);
				rmsg = receive(msg.id,true);
			}
			/* Message sent and received, kill cached version */
			msg.bbuf.clear();
			log.finest("Response received for id="+msg.id);
			
			/* Response recieved, kill the resend thread */
			timer.cancel();
			
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
		/* The ONLY way to send a message */
		public void run() {
			try {
				SocketAddress sendTo;
				if( msg.dstSAddr == null && msg.getDestination().address == null ) {
					sendTo = NodeControl.getSocketAddress(msg.getDestination());
					server.send(msg.bbuf, sendTo);
				} else if(msg.dstSAddr == null){
					sendTo = msg.getDestination().address;
					server.send(msg.bbuf, sendTo);
				} else {
					sendTo = msg.dstSAddr;
					server.send(msg.bbuf, sendTo);
				}
				/* Reset the cached version of the message back to the mark so it is ready to be sent again */
				msg.bbuf.reset();
				log.finer(String.format("SENT: addr=%s size=%d msg=%s", sendTo, bos.size(), msg));
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
		return receive(0,false);
	}
	
	public static Message receive(int id) {
		return receive(id,true);
	}
	
	public static Message receive(int id,boolean checkID) {
		try {
			Thread.sleep( (long) 10 );
		} catch(Exception e) {
			log.throwing("MessageControl", "receive", e);
		}
		
		/* If looking for a message that has already been received and processed */
		if(msgLog.containsKey(id)) {
			log.finest("Message already received. Returning noPayload cached version.\nmsg="+msgLog.get(id).toString());
			/* Return message without the payload */
			return msgLog.get(id);
		}
		
		/* If looking for a message in the queue */
		if(queue.containsKey(id)) {
			Message msg = queue.remove(id);
			queueList.remove(id);
			log.finest("Returning message from queue msg="+msg);
			msgLog.put(msg.id,msg.noPayload());
			return msg;
		}
		
		/* If looking for first available message and there are messages waiting in the queue */
		if(!checkID && !queueList.isEmpty()) {
			/* Remove the first message from the queue */
			Message msg = queue.remove(queueList.removeFirst());
			
			/* If there are people waiting for messages, but not the first one in the queue */
			if(!waiting.isEmpty() && !waiting.contains(queueList.getFirst())) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Waiting, but not for first in queueList msg="+msg.toString());
				return msg;
			/* If there is nobody waiting for a message */
			} else if(waiting.isEmpty()) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Not waiting msg="+msg.toString());
				return msg;
			}
			log.finest("Waiting for first message in queueList msg="+msg.toString());
			/* Someone is waiting for the first message in the queue, so move it to the end */
			queueList.addLast(msg.id);
			queue.put(msg.id, msg);
			/* Try again! */
			receive(id,checkID);
		}
		
		Message msg = null;

		/* Receive the next incoming message waiting at the socket layer */
		try {
			synchronized(rbuf) {
				/* Clear the receive buffer before doing anything */
				rbuf.clear();
				/* Get the next packet */
				SocketAddress addr = server.receive(rbuf);
				if(addr == null) return null;
				/* Get the buffer ready for reading */
				rbuf.flip();
				/* Turn the data into a packet */
				DatagramPacket pkt = new DatagramPacket(rbuf.array(),rbuf.limit());
				/* Packet made, clear the buffer */
				rbuf.clear();
				if(pkt.getLength() == 0) {
					log.finest("Null packet");
					return null;
				}
				
				/* Convert packet back into a Message */
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(pkt.getData()));
				msg = (Message) ois.readObject();
				
				//If bad message or identifier
				if(msg==null || msg.id==0) {
					log.finest("Bad message or id=0 msg="+msg);
					return null;
				}
				/* Show that we received a valid packet and successfully converted it back into a Message */
				log.finer(String.format("RECEIVED: addr=%s size=%d msg=%s", addr, pkt.getLength(), msg));
				
				/* Destroy the cached version of the message, if there is one (Shouldn't be, it's transient) */
				msg.bbuf = null;
				
				/* Set message return path */
				msg.srcSAddr = addr;
				msg.dstSAddr = server.socket().getLocalSocketAddress();
				
				//If message has already been received and processed
				if(msgLog.containsKey(msg.id) && msgLog.get(msg.id).equals(msg.noPayload())) {
					log.finest("Message already recieved, ignoring msg=" + msg.toString());
					return null;
				}
				
				/* If we are looking for this message */
				if(msg.id==id) {
					msgLog.put(msg.id,msg.noPayload());
					log.finest("Found requested message msg="+msg);
					return msg;
				}
				
				/* If somebody else is waiting for this message
				 * Put it in the queue and return a recursive receive() call
				 */
				if(waiting.contains(msg.id)) {
					queue.put(msg.id, msg);
					queueList.add(msg.id);
					log.finest("Somebody else is looking for this message; queueing msg="+msg);
					return receive(id,checkID);
				}
				
				
				if(!checkID) {
					msgLog.put(msg.id,msg.noPayload());
					log.finest("Not checking for a certain message id msg="+msg);
					return msg;
				}
				/* Nobody gets this message, just add it to the queue 
				 * Means we are looking for a specific message, but not this one or any one in the queue
				 */
				queue.put(msg.id, msg);
				queueList.addLast(msg.id);
			}
		} catch(Exception e) {
			log.throwing("MasterServer", "run", e);
			return null;
		}
		log.finest("We are looking for a specific message, but it was not found. Sorry!");
		return null;
	}
	
	/**
	 * Creates a new reply by reversing the source and destination
	 * URIs and giving the new Message the same ID as the old
	 * @param msg Message to reply to
	 * @param payload Payload for reply
	 * @return new reply message
	 */
	public static Message createReply(Message msg, Response response, Object payload) {
		if(msg.response!=null) {
			log.finest("Trying to a reply! Returning null; msg="+msg);
			return null;
		}
		Message rmsg = new Message(msg.getDestination(),msg.getSource(),payload);
		rmsg.id = msg.id;
		rmsg.type = msg.type;
		rmsg.response = response;
		rmsg.srcSAddr = msg.dstSAddr;
		rmsg.dstSAddr = msg.srcSAddr;
		return rmsg;
	}
	
	/**
	 * Get listening address
	 * @return SocketAddress we are listening on
	 */
	public static SocketAddress getAddress() {
		return server.socket().getLocalSocketAddress();
	}
	
	/**
	 * Close the messaging interface
	 *
	 */
	public static void close() {
		try {
			server.close();
		}catch (IOException e) {
			log.throwing("MessageControl", "close", e);
		}
		
	}
}