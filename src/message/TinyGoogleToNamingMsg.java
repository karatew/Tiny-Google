package message;

import java.io.Serializable;
import java.util.Date;

import util.Address;

/**
 * Tiny_Google server sends request messages to Name_Server 
 * for two purposes:
 * (1) to register itself to the server tables in name_server;
 * (2) to look up for available helper servers. 
 */
public class TinyGoogleToNamingMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	// requestType
	// 0: Tiny_Google registration at the naming server
	// 1: Tiny_Google looking up available helper servers
	private int requestType;

	public TinyGoogleToNamingMsg(Address senderAddress, Address receiverAddress, int requestType) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.requestType = requestType;
	}

	public Address getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(Address senderAddress) {
		this.senderAddress = senderAddress;
	}

	public Address getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(Address receiverAddress) {
		this.receiverAddress = receiverAddress;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		if (requestType == 0 || requestType == 1) {
			this.requestType = requestType;
		} else {
			return;
		}
	}
}
