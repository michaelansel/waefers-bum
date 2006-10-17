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
import java.util.TreeSet;

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
	static DatagramChannel server = null;
	
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
	 * HashMap of all messages received sans payload indexed by ID
	 */
	static HashMap<Integer,Message> msgLog = new HashMap<Integer,Message>();
	
	/**
	 * TreeSet of all messages waiting for responses
	 */
	static TreeSet<Integer> waiting = new TreeSet<Integer>();
	
	/**
	 * Selector and selectionKey
	 */
	static Selector selector;
	static SelectionKey key;
	
	static ByteArrayOutputStream bos = null;
	static ObjectOutputStream oos = null;
	
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
		return init(new InetSocketAddress(DEFAULT_PORT));
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
			
			//Wait for a response
			while(rmsg == null) {
				log.finest("Waiting for the reply to message id="+msg.id);
				rmsg = receive(msg.id,true);
			}
			msg.bbuf.clear();
			log.finest("Response received for id="+msg.id);
			
			//Response recieved, kill the resend thread
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
		
		if(msgLog.containsKey(id)) {
			log.finest("Message already received. Returning noPayload cached version.\nmsg="+msgLog.get(id).toString());
			return msgLog.get(id);
		}
		
		if(queue.containsKey(id)) {
			Message msg = queue.remove(id);
			queueList.remove(id);
			log.finest("Returning message from queue msg="+msg);
			msgLog.put(msg.id,msg.noPayload());
			return msg;
		}
		
		if(!checkID && !queueList.isEmpty()) {
			Message msg = queue.remove(queueList.removeFirst());
			if(!waiting.isEmpty() && !waiting.contains(queueList.getFirst())) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Waiting, but not for first in queueList msg="+msg.toString());
				return msg;
			} else if(waiting.isEmpty()) {
				msgLog.put(msg.id,msg.noPayload());
				log.finest("Not waiting msg="+msg.toString());
				return msg;
			}
			log.finest("Waiting for first message in queueList msg="+msg.toString());
			queueList.addLast(queueList.removeFirst());
			receive(id,checkID);
		}
		
		Message msg = null;

		try {
			synchronized(rbuf) {
				rbuf.clear();
				SocketAddress addr = server.receive(rbuf);
				if(addr == null) return null;
				rbuf.flip();
				DatagramPacket pkt = new DatagramPacket(rbuf.array(),rbuf.limit());
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
				
				//If message has already been received and processed
				if(msgLog.containsKey(msg.id) && msgLog.get(msg.id)==msg.noPayload()) {
					log.finest("Message already recieved, ignoring msg=" + msg.toString());
					return null;
				}
				
				//If somebody else is waiting for the message
				if(waiting.contains(msg.id)) {
					queue.put(msg.id, msg);
				}
				
				msg.srcSAddr = addr;
				msg.dstSAddr = server.socket().getLocalSocketAddress();
				
				log.finer(String.format("RECEIVED: addr=%s size=%d msg=%s", addr, pkt.getLength(), msg));
				
				if(!checkID) {
					msgLog.put(msg.id,msg.noPayload());
					log.finest("Not checking for a certain message id msg="+msg);
					return msg;
				}
				if(msg.id==id) {
					msgLog.put(msg.id,msg.noPayload());
					log.finest("Found requested message msg="+msg);
					return msg;
				}
				queue.put(msg.id, msg);
				queueList.addLast(msg.id);
			}
		} catch(Exception e) {
			log.throwing("MasterServer", "run", e);
			return null;
		}
		log.finest("Uhh, yeah, something happened, so we aren't going to return anything!");
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
		Message rmsg = new Message(msg.getDestination(),msg.getSource(),payload);
		rmsg.id = msg.id;
		rmsg.type = msg.type;
		rmsg.response = response;
		rmsg.srcSAddr = msg.dstSAddr;
		rmsg.dstSAddr = msg.srcSAddr;
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