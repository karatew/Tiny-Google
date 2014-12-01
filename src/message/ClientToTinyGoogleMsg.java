package message;

import java.io.Serializable;
import java.util.List;

import util.Address;

/**
 * Please be noted that the integer field 'requestType' cannot changed 
 * once the message is created.
 * A client uses different constructor to create two different types of 
 * messages, while TinyGoogle Server uses 'requestType' to identify which 
 * kind of request did the client send.
 */
public class ClientToTinyGoogleMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	// 0: indexing
	// 1: searching
	private int clientRequestType;
	private String indexDirectoryPathName;
	private List<String> searchKeyWords;

	// constructor for indexing request
	public ClientToTinyGoogleMsg(Address senderAddress, Address receiverAddress,
			String indexDirectoryPathName) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.indexDirectoryPathName = indexDirectoryPathName;
		this.clientRequestType = 0;
	}

	// constructor for querying request
	public ClientToTinyGoogleMsg(Address senderAddress, Address receiverAddress,
			List<String> searchKeyWords) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.searchKeyWords = searchKeyWords;
		this.clientRequestType = 1;
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

	public String getIndexDirectoryPathName() {
		return indexDirectoryPathName;
	}

	public void setIndexDirectoryPathName(String indexDirectoryPathName) {
		this.indexDirectoryPathName = indexDirectoryPathName;
	}

	public List<String> getSearchKeyWords() {
		return searchKeyWords;
	}

	public void setSearchKeyWords(List<String> queryKeyWords) {
		this.searchKeyWords = queryKeyWords;
	}

	public int getClientRequestType() {
		return clientRequestType;
	}

}
