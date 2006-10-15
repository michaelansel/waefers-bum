package net.waefers.messaging;

import java.util.HashSet;
import net.waefers.block.Block;
import net.waefers.node.Node;

public class LocationMessage extends Message {
	
	static final long serialVersionUID = 0;

	/**
	 * Possible actions to perform on replica directory
	 * ADD: Add a block and its location to the directory
	 * GET: Get a blocks location from the directory
	 * REMOVE_PART: Remove some of the block->node mappings, but leave the node in the directory
	 * REMOVE_FULL: Remove a node and all its mappings from the directory
	 */
	public enum LocationAction {ADD,GET,REMOVE_PART,REMOVE_FULL};
	
	/**
	 * Action to perform on replica directory
	 */
	public LocationAction action;
	
	/**
	 * Set with all local data in it, or references to location on disk
	 */
	public HashSet<Block> blocks = new HashSet<Block>();
	
	/**
	 * Node to associate/associated with blocks
	 */
	public Node node;
	
}
