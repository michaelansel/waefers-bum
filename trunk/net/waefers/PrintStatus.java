package net.waefers;

import java.net.URI;

import static net.waefers.GlobalControl.*;
import static net.waefers.GlobalObjects.*;

import java.util.Date;
import java.util.TimerTask;

public class PrintStatus extends TimerTask {
	
	public void run() {
		String s=new Date().toString()+"\n";

		if(nodeDirectory!=null) {
			s+="Node Directory\n";
			for(URI uri : nodeDirectory.keySet() ) {
				s += "{uri="+uri+" nodeEntry="+nodeDirectory.get(uri)+"}\n";
			}
		}
		
		if(blockLocs!=null) {
			s+="Block Directory\n";
			for(byte[] id : blockLocs.keySet() ) {
				s += "{id="+byteArrayToHexString(id)+" block="+blockLocs.get(id)+"}\n";
			}
		}
		
		log.finest("Writing status to file\n"+s);
		status.write(s+"\n\n");
		status.flush();
	}
}
