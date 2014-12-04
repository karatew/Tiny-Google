package message;

import java.io.Serializable;
import java.util.List;

import util.Address;
import util.WordCountPair;

public class TinyGoogleToClientMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	// 0: indexing
	// 1: searching
	private int returnResultType = -1;
	private boolean isIndexed;
	private List<WordCountPair> searchResult;

	public TinyGoogleToClientMsg(Address senderAddress, Address receiverAddress, boolean isIndexed) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.isIndexed = isIndexed;
		this.returnResultType = 0;
	}

	public TinyGoogleToClientMsg(Address senderAddress, Address receiverAddress,
			List<WordCountPair> searchResult) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.searchResult = searchResult;
		this.returnResultType = 1;
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

	public int getReturnResultType() {
		return returnResultType;
	}

	public boolean isIndexed() {
		return isIndexed;
	}

	public void setIndexed(boolean isIndexed) {
		this.isIndexed = isIndexed;
	}

	public List<WordCountPair> getSearchResult() {
		return searchResult;
	}

	public void setSearchResult(List<WordCountPair> searchResult) {
		this.searchResult = searchResult;
	}

	public void setReturnResultType(int returnResultType) {
		this.returnResultType = returnResultType;
	}

}
