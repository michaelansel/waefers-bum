package net.waefers.messaging;

import java.util.HashSet;
import net.waefers.block.Block;

public class LocationMessage {
	
	/**
	 * Possible actions to perform on replica directory
	 */
	public enum LocationAction {ADD,GET,REMOVE};
	
	/**
	 * Action to perform on replica directory
	 */
	public LocationAction action;
	
	/**
	 * Set with all local data in it, or references to location on disk
	 */
	HashSet<Block> blocks = new HashSet<Block>();
	
	
	
}
