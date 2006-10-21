package net.waefers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static net.waefers.GlobalObjects.*;

public class GlobalControl {
	
	/**
	 * Save log to a file
	 * @param file file to log to
	 * @throws IOException
	 */
	static public void logToFile(String file) throws IOException {
        FileHandler ch = new FileHandler(file+"Log.txt");
        SimpleFormatter sf = new SimpleFormatter();
        ch.setFormatter(sf);
        log.addHandler(ch);
        ch.setLevel(Level.FINEST);
        log.setLevel(Level.FINEST);
        
        statusFile(file);
    }
	
	/**
	 * Print log to console
	 * @throws IOException
	 */
	static public void logToConsole() throws IOException {
        ConsoleHandler ch = new ConsoleHandler();
        SimpleFormatter sf = new SimpleFormatter();
        ch.setFormatter(sf);
        log.addHandler(ch);
        ch.setLevel(Level.FINEST);
        log.setLevel(Level.FINEST);
    }
	
	public static void statusFile(String file) throws IOException {
		status = new PrintWriter(new FileOutputStream(new File(file+"Status.txt"),false));
	}
	
	/**
	* Convert a byte[] array to readable string format. This makes the "hex" readable!
	* @return result String buffer in String format
	* @param in byte[] buffer to convert to string format
	*/
	public static String byteArrayToHexString(byte in[]) {
	    byte ch = 0x00;
	    int i = 0; 
	    if (in == null || in.length <= 0)
	        return null;
	    String pseudo[] = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
	    StringBuffer out = new StringBuffer(in.length * 2);
	    while (i < in.length) {
/* Strip off high nibble */
	    	ch = (byte) (in[i] & 0xF0);
/* shift the bits down */
	        ch = (byte) (ch >>> 4);
/* must do this is high order bit is on! */
	        ch = (byte) (ch & 0x0F); 
/* convert the nibble to a String Character */
	        out.append(pseudo[ (int) ch]);
/* Strip off low nibble */ 
	        ch = (byte) (in[i] & 0x0F);
/* convert the nibble to a String Character */
	        out.append(pseudo[ (int) ch]);
	        i++;
	    }
	    String rslt = new String(out);
	    return rslt;

	}    
}
