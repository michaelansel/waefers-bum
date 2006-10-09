package net.waefers.filesystem;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a directory in the file system
 * 
 * @author Michael Ansel
 *
 */

public class Directory extends FileSystemObject {
	public String name;
	public String path;
	public TreeMap<String,FileSystemObject> contents = null;
	//Other directory metadata goes here
	
	Directory(String path, String name) {
		this.path = path;
		this.name = name;
		contents = new TreeMap<String,FileSystemObject>();
	}
	
	public TreeSet directoryContents() {
		return (TreeSet) contents.keySet();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	public String toString() {
		return super.toString();
	}
}
