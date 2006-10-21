package net.waefers.messaging;

import static net.waefers.GlobalObjects.*;
import static net.waefers.messaging.MessageControl.*;

import java.util.TimerTask;

public class PrintQueue extends TimerTask {
	
	public void run() {
		String s;
		s = "Message Queue\n";
		for(Integer id : queue.keySet() ) {
			s += "{id="+id+" msg="+queue.get(id)+"}\n";
		}
		s += "Message QueueList\n";
		for(Integer id : queueList ) {
			s += "{id="+id+" msg="+queue.get(id)+"}\n";
		}
		s += "Message Log\n";
		for(Integer id : msgLog.keySet() ) {
			s += "{id="+id+" msg="+msgLog.get(id)+"}\n";
		}
		s += "Waiting List\n";
		for(Integer id : waiting ) {
			s += "{id="+id+"}\n";
		}
		log.finest(s);
	}
}
