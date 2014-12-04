package message;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import util.Address;
import util.IndexingWorkload;

/**
 * This message contains the results that Helper_Servers need to 
 * send back to Tiny_Google Server.
 */
public class HelperToTinyGoogleMsg implements Serializable {

	private Address senderAddress;
	private Address receiverAddress;
	private ConcurrentHashMap<String, Integer> countByWordTable = new ConcurrentHashMap<>();
	private IndexingWorkload indexingWorkload;

	public HelperToTinyGoogleMsg(Address senderAddress, Address receiverAddress,
			ConcurrentHashMap<String, Integer> countByWordTable, IndexingWorkload indexingWorkload) {
		this.senderAddress = senderAddress;
		this.receiverAddress = receiverAddress;
		this.countByWordTable = countByWordTable;
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

	public ConcurrentHashMap<String, Integer> getCountByWordTable() {
		return countByWordTable;
	}

	public void setCountByWordTable(ConcurrentHashMap<String, Integer> countByWordTable) {
		this.countByWordTable = countByWordTable;
	}

	public IndexingWorkload getIndexingWorkload() {
		return indexingWorkload;
	}

	public void setIndexingWorkload(IndexingWorkload indexingWorkload) {
		this.indexingWorkload = indexingWorkload;
	}

}
