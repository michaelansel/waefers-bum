package net.waefers.block;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * Represents a data block
 * 
 * @author Michael Ansel
 */

import net.waefers.block.BlockControl;
import static net.waefers.GlobalControl.log;

public class Block {
	
	/**
	 * Possible block types
	 * DATA: Block contains the actual data
	 * REFERENCE: Block contains a reference to a local file containing the data
	 */
	private enum BlockType {DATA,REFERENCE};
	
	/**
	 * Type of block
	 */
	private BlockType type;
	
	/**
	 * Block data
	 */
	private ByteBuffer data;
	
	/**
	 * Data hash (id)
	 */
	public String id;
	
	
	public Block() {
		type = BlockType.REFERENCE;
		id = "";
		data = null;
	}
	
	/**
	 * 
	 * @return Whether or not the toggle was successful
	 */
	public boolean toggleType() {
		try {
			if(type==BlockType.DATA) {
				type = BlockType.REFERENCE;
				boolean work = BlockControl.writeFile(BlockControl.dataFile(id),data);
				if(work) data=null;
				return work;
			}
			if(type==BlockType.REFERENCE) {
				type = BlockType.DATA;
				data = BlockControl.readFile(BlockControl.dataFile(id));
				return data!=null;
			}
			return false;
		} catch(IOException e) {
			log.throwing("Block", "toggleType", e);
			return false;
		}
	}
	
	public ByteBuffer getData() {
		if(type==BlockType.REFERENCE) this.toggleType();
		
		return data;
	}

}
