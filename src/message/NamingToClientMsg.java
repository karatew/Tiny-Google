package message;

import java.io.Serializable;

import util.Address;

public class NamingToClientMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	private Address tinyGoogleServerAddress;

	public NamingToClientMsg(Address senderAddress, Address receiverAddress,
			Address tinyGoogleServerAddress) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.tinyGoogleServerAddress = tinyGoogleServerAddress;
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

	public Address getTinyGoogleServerAddress() {
		return tinyGoogleServerAddress;
	}

	public void setTinyGoogleServerAddress(Address tinyGoogleServerAddress) {
		this.tinyGoogleServerAddress = tinyGoogleServerAddress;
	}

}
