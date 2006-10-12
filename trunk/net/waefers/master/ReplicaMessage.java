//TODO:Shift to {Replica,Block}Message and kill
package net.waefers.master;

import java.net.URI;

public class ReplicaMessage {

	public enum RMessageType {ADD,REMOVE};
	public RMessageType rMessageType;
	
	public int block;
	public URI uri;
	
	public ReplicaMessage(RMessageType type,int block, URI uri) {
		this.rMessageType = type;
		this.block = block;
		this.uri = uri;
	}
}
