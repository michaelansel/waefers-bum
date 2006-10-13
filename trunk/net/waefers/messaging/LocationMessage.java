package net.waefers.messaging;

import java.util.HashSet;
import net.waefers.block.Block;
import net.waefers.node.Node;

public class LocationMessage {
	
	/**
	 * Possible actions to perform on replica directory
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
