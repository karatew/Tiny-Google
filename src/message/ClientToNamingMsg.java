package message;

import java.io.Serializable;

import util.Address;

public class ClientToNamingMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;

	public ClientToNamingMsg(Address senderAddress, Address receiverAddress) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
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

}
