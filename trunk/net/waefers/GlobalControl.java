package net.waefers;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GlobalControl {
	
	/**
	 * Default port for all communications
	 */
	public static final int DEFAULT_PORT = 51951;
	
	/**
	 * Main logging object
	 */
	public static final Logger log = Logger.getLogger("global");

	/**
	 * Save log to a file
	 * @param file file to log to
	 * @throws IOException
	 */
	static public void logToFile(String file) throws IOException {
        FileHandler ch = new FileHandler(file);
        SimpleFormatter sf = new SimpleFormatter();
        ch.setFormatter(sf);
        log.addHandler(ch);
        ch.setLevel(Level.FINEST);
        log.setLevel(Level.FINEST);
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
}
