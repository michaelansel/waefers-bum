/**
 * 
 */
package net.waefers.master;

import java.util.HashMap;

import net.waefers.filesystem.FileSystemObject;

/**
 * @author Michael Ansel
 *
 */
public class MetaMaster {

	/**
	 * Contains filename->block[] mapping
	 * HashMap<filename,FileEntry(contains block[])>
	 */
	private HashMap<String,FileSystemObject> fileDirectory = null;
	
	
}
