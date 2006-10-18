package net.waefers.messaging;

import java.net.URI;
import java.util.TimerTask;

import net.waefers.node.Node;

public class Heartbeater extends TimerTask {
	
	Node node;
	
	public Heartbeater(Node node) {
		this.node = node;
	}
	
	public void run() {
		Message msg = new Message(node.uri,URI.create("nodemaster@waefers"),node);
		msg.type=Message.Type.HEARTBEAT;
		MessageControl.send(msg,true);
	}
}
