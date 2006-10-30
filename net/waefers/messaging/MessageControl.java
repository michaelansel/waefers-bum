package net.waefers.messaging;

import static net.waefers.GlobalObjects.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashSet;

import net.waefers.messaging.Message.Response;
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
	protected static HashMap<Integer,Message> msgLog = new HashMap<Integer,Message>();
	
	/**
	 * HashSet of all messages waiting for responses
	 */
	protected static HashSet<Integer> waiting = new HashSet<Integer>();
	
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
			
			/* Set the time for the message to die if we haven't received a response yet */
			Date msgDie = new Date(System.currentTimeMillis() + MESSAGE_TTL*1000);
			
			/* Wait for a response */
			while(rmsg == null && msgDie.after(new Date(System.currentTimeMillis()))) {
				rmsg = receive(msg.id,true);
			}
			
			/* Message sent and received, kill cached version */
			msg.bbuf.clear();
			log.finest("Response received for id="+msg.id);
			
			/* Response received, kill the resend thread */
			timer.cancel();
			
			/* Response received, remove from waiting list */
			waiting.remove(msg.id);
			
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
		
		Message queued;
		if(!queueList.isEmpty() 
				&& (queued = checkQueue(id,checkID,queueList.getFirst())) != null)
			return queued;
		
		Message msg = null;

		/* Receive the next incoming message waiting at the socket layer */
		try {
			/* Get the next waiting packet from the socket */
			DatagramPacket pkt = receiveFromSocket();
			
			/* If the packet is empty or null, ignore it */
			if(pkt == null || pkt.getLength() == 0) {
				if(!(pkt == null)) log.finest("Null packet");
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
			log.finer(String.format("RECEIVED: addr=%s size=%d msg=%s", pkt.getSocketAddress(), pkt.getLength(), msg));
			
			/* Destroy the cached version of the message, if there is one (Shouldn't be, it's transient) */
			msg.bbuf = null;
			
			/* Set message return path */
			msg.srcSAddr = pkt.getSocketAddress();
			msg.dstSAddr = server.socket().getLocalSocketAddress();
			
			//If message has already been received and processed
			if(msgLog.containsKey(msg.id) && msgLog.get(msg.id).equals(msg)) {
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
				log.finest("Not checking for a certain message; msg="+msg);
				return msg;
			}
			/* Nobody gets this message, just add it to the queue 
			 * Means we are looking for a specific message, but not this one or any one in the queue
			 */
			queue.put(msg.id, msg);
			queueList.addLast(msg.id);
		} catch(Exception e) {
			log.throwing("MasterServer", "run", e);
			return null;
		}
		log.finest("We are looking for a specific message, but it was not found. Sorry!");
		return null;
	}
	
	private static DatagramPacket receiveFromSocket() throws IOException {
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
			pkt.setSocketAddress(addr);
			
			return pkt;
		}
	}
	
	/**
	 * Checks the message queue for specific message id and returns it if found;
	 * if not looking for a specific message, returns the first message in the queue not being waited upon
	 * @param id Message we are looking for
	 * @param checkID Whether or not to check for a specific message id
	 * @param breakOn The first message in the queue, so we know when we have run all the way through the queue
	 * @return Message, if found; otherwise, null
	 */
	private static Message checkQueue(Integer id, boolean checkID, Integer breakOn) {
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
			if(breakOn==queueList.getFirst()) return null;
			/* Remove the first message from the queue */
			Message msg = queue.remove(queueList.removeFirst());
			
			/* If there are people waiting for messages, but not the first one in the queue */
			if(!waiting.isEmpty() && !waiting.contains(msg.id)) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Others are waiting, but not for first in queueList msg="+msg.toString());
				return msg;
			/* If there is nobody waiting for a message */
			} else if(waiting.isEmpty()) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Not waiting msg="+msg.toString());
				return msg;
			}
			log.finest("Others are waiting for first message in queueList msg="+msg.toString());
			/* Someone is waiting for the first message in the queue, so move it to the end */
			queueList.addLast(msg.id);
			queue.put(msg.id, msg);
			/* Try again! */
			//TODO: This is creating an endless loop if there are ANY messages in the queue
			return checkQueue(id,checkID,breakOn);
		}
		/* Nothing in the queue to be returned */
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
			log.finest("Trying to reply to a reply! Returning null; msg="+msg);
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