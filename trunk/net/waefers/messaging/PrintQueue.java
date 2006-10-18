package net.waefers.messaging;

import static net.waefers.GlobalControl.log;
import static net.waefers.messaging.MessageControl.*;

import java.util.TimerTask;

public class PrintQueue extends TimerTask {
	
	public void run() {
		String s=null;
		s += "Message Queue\n";
		for(Integer id : queue.keySet() ) {
			s += "{id="+id+" msg="+queue.get(id)+"}\n";
		}
		s += "Message QueueList\n";
		for(Integer id : queueList ) {
			s += "{id="+id+" msg="+queue.get(id)+"}\n";
		}
		log.finest(s);
	}
}
