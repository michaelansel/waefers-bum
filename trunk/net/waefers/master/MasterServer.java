package net.waefers.master;

import java.io.IOException;
import java.util.ArrayList;

import net.waefers.GlobalControl;
import net.waefers.messaging.Message;
import net.waefers.messaging.MessageControl;

import static net.waefers.GlobalObjects.*;
import static net.waefers.messaging.Message.Response.*;

/**
 * 
 * The main server for receiving messages and processing them.
 * Should be extended upon for specific message functions, otherwise all will return ERROR.
 * 
 * @author Michael Ansel
 *
 */
class MasterServer {
	
	/**
	 * Listen for incoming UDP packets and respond accordingly
	 */
	public void receiveAndProcess() {
		log.finest("Master server run method starting");

		while(true) {
			Message msg = MessageControl.receive();
			Message rmsg = null;

			if(msg == null) continue;
			
			switch(msg.type) {
			case HEARTBEAT: //Heartbeat packet
				rmsg = heartbeat(msg);
				break;
			case BLOCK: //Packet concerning actual blocks
				rmsg = block(msg);
				break;
			case METADATA: //Packet concerning file system information
				rmsg = meta(msg);
				break;
			case BLOCK_LOCATION: //Packet concerning block replica locations
				rmsg = blockLocation(msg);
				break;
			case NODE_LOCATION: //Packet concerning the direct connection addresses for nodes
				rmsg = nodeLocation(msg);
				break;
			default:
				rmsg = MessageControl.createReply(msg,ERROR,null);
				break;
			}
			
			MessageControl.send(rmsg,false);
		}
	}
	
	protected Message heartbeat(Message msg) {
		return error(msg);
	}
	
	protected Message block(Message msg) {
		return error(msg);
	}
	
	protected Message meta(Message msg) {
		return error(msg);
	}
	
	protected Message blockLocation(Message msg) {
		return error(msg);
	}
	
	protected Message nodeLocation(Message msg) {
		return error(msg);
	}
	
	private Message error(Message msg) {
		log.finest("Default processing method. Returning ERROR!");
		return MessageControl.createReply(msg,ERROR,null);
	}
	
	protected void start() {
		log.finest("MS-Start");
		MessageControl.init();
		receiveAndProcess();
	}
	
	protected void processArgs(String[] args) throws IOException {
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
	}
	
	/**
	 * Start a MasterServer
	 * @param args [-d filename.log]
	 */
	public static void main(String[] args) throws IOException {
		MasterServer ms = new MasterServer();
		ms.processArgs(args);
		ms.start();
	}
}
