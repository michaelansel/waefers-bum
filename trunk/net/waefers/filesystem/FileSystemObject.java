package net.waefers.filesystem;

/**
 * 
 * Abstract class representing an item in the file system
 * 
 * @author Michael Ansel
 *
 */

public abstract class FileSystemObject {
	
	public String name;
	public String path;
	//Other metadata similar to files and folders
	
	/**
	 * Returns true if two FileSystemObjects have the same path and name
	 */
	public boolean equals(Object o) {
		try {
			FileSystemObject fso = (FileSystemObject) o;
			if(this.name.equals(fso.name) && this.path.equals(fso.path)) {
				return true;
			} else { return false; }
		} catch(Exception e) {
			return false;
		}
	}
	
	public String toString() {
		return path+"/"+name;
	}
}
