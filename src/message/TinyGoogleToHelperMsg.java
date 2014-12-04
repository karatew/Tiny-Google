package message;

import java.io.Serializable;

import util.Address;
import util.IndexingWorkload;

/**
 * This message is used to let TinyGoogle Server assign workload to Helper_Server
 * according to the request mode.
 */
public class TinyGoogleToHelperMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	// 0: to map
	// 1: to reduce
	private int requestType = -1;
	// file that a helper need to map
	private String indexingFile = null;

	private IndexingWorkload indexingWorkload;

	public TinyGoogleToHelperMsg(Address senderAddress, Address receiverAddress, int requestType,
			String indexingFile, IndexingWorkload indexingWorkload) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.requestType = requestType;
		this.indexingFile = indexingFile;
		this.indexingWorkload = indexingWorkload;
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
		this.requestType = requestType;
	}

	public String getIndexingFile() {
		return indexingFile;
	}

	public void setIndexingFile(String indexingFile) {
		this.indexingFile = indexingFile;
	}

	public IndexingWorkload getIndexingWorkload() {
		return indexingWorkload;
	}

	public void setIndexingWorkload(IndexingWorkload indexingWorkload) {
		this.indexingWorkload = indexingWorkload;
	}

}
