package net.waefers.block;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BlockControl {
	
	/**
	 * 
	 * Returns the local data file for a selected hash
	 * 
	 * @param id data hash/id
	 * @return Local data file
	 */
	public static File dataFile(String id) {
		return null;
	}
	
	/**
	 * 
	 * Reads data from local file with verification
	 * 
	 * @param file File to read data from
	 * @return Data from file
	 */
	public static ByteBuffer readFile(File file)  throws IOException {
		return null;
	}
	
	/**
	 * 
	 * Writes data to local File with verification
	 * 
	 * @param file File to write data to
	 * @param data Data to write to file
	 * @return Whether or not the write action was successful
	 * @throws IOException
	 */
	public static boolean writeFile(File file, ByteBuffer data) throws IOException {
		return false;
	}
}
