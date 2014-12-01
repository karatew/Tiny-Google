package message;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import util.Address;

/**
 * Name_Server sends only one type of message to Tiny_Google 
 * server: the addresses of available helpers.
 */
public class NamingToTinyGoogleMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	private List<Address> helperServerAddressList;

	public NamingToTinyGoogleMsg(Address senderAddress, Address receiverAddress,
			List<Address> helperServerAddressList) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.helperServerAddressList = helperServerAddressList;
	}

	public int getAvailableServerCount() {
		if (helperServerAddressList == null)
			return 0;
		return helperServerAddressList.size();
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

	public List<Address> getHelperServerAddressList() {
		return helperServerAddressList;
	}

	public void setHelperServerAddressList(List<Address> helperServerAddressList) {
		this.helperServerAddressList = helperServerAddressList;
	}

}
