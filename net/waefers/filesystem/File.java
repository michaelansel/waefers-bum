package net.waefers.filesystem;

import java.util.ArrayList;

/**
 * 
 * Represents a file in the filesystem
 * 
 * @author Michael Ansel
 *
 */

public class File extends FileSystemObject {
	ArrayList<Integer> blocks = new ArrayList<Integer>();
	int md5;
	public String name;
	public String path;
	//Other file metadata goes here
	
	File(String path,String name,int md5) {
		this.path = path;
		this.name = name;
		this.md5 = md5;
	}
	
	void setMD5(int md5) {
		this.md5 = md5;
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	public String toString() {
		return super.toString();
	}
}
