package tiny_google_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import util.Address;
import util.TextToolsBox;
import util.IndexingWorkload;
import util.WordCountPair;
import message.ClientToTinyGoogleMsg;
import message.HelperToTinyGoogleMsg;
import message.NamingToTinyGoogleMsg;
import message.TinyGoogleToClientMsg;
import message.TinyGoogleToHelperMsg;
import message.TinyGoogleToNamingMsg;

public class TinyGoogleServerThread implements Callable<Boolean> {
	// This table is used to record each helper with its assigned workload.
	ConcurrentHashMap<Address, IndexingWorkload> helperIndexingWordloadTable = new ConcurrentHashMap<>();
	// Inverted index table for each file
	protected static ConcurrentHashMap<String, Integer> countByWordTable = new ConcurrentHashMap<>();
	// the result list that needs to be send back to client
	protected static List<WordCountPair> searchResult = new ArrayList<>();

	private Socket tinyGoogleSocket;
	private Address clientAddress;

	public TinyGoogleServerThread(Socket tinyGoogleSocket) {
		this.tinyGoogleSocket = tinyGoogleSocket;
	}

	@Override
	public Boolean call() throws Exception {

		InputStream tinyGoogleInputStream = tinyGoogleSocket.getInputStream();
		ObjectInputStream tinyGoogleObjectInputStream = new ObjectInputStream(tinyGoogleInputStream);
		Object requestMsg = tinyGoogleObjectInputStream.readObject();
		ObjectOutputStream tinyGoogleObjectOutputStream = new ObjectOutputStream(
				tinyGoogleSocket.getOutputStream());

		if (requestMsg == null) {
			System.err.println("\nTiny_Google Server received a \"null\" message.");
			// System.err.println("Failed in resolving Tiny_Google Server's address!");
			tinyGoogleObjectOutputStream.close();
			tinyGoogleObjectInputStream.close();
			tinyGoogleSocket.close();
			return false;
		}
		// NameServer sends available helpers list to TinyGoogleServer
		if (requestMsg instanceof NamingToTinyGoogleMsg) {
			NamingToTinyGoogleMsg n2tMsg = (NamingToTinyGoogleMsg) requestMsg;
			TinyGoogleServer.availableHelperList = n2tMsg.getHelperServerAddressList();
			System.out.println("\nTiny_Google Server{"
					+ TinyGoogleServer.tinyGoogleAddress.toString()
					+ "} has successfully updated its available Helper_Server list.");
		}
		// Client may send two types of requests to TinyGoogleServer
		if (requestMsg instanceof ClientToTinyGoogleMsg) {
			ClientToTinyGoogleMsg c2tMsg = (ClientToTinyGoogleMsg) requestMsg;
			clientAddress = c2tMsg.getSenderAddress();
			int clientRequestMode = c2tMsg.getClientRequestType();
			switch (clientRequestMode) {
			/*
			 * The following case handles a client indexing request
			 */
			case 0:
				// get requesting file from client message
				System.out.println("\nTiny_Google Server{"
						+ TinyGoogleServer.tinyGoogleAddress.toString()
						+ "} received an indexing request from Client{"
						+ c2tMsg.getSenderAddress().toString() + "}.");
				String indexingFile = c2tMsg.getIndexDirectoryPathName();

				// get available helpers from NameServer
				List<Address> helperList = getAvailableHelperListFromNameServer();

				// split the workload and send them to helpers
				System.out.println("\n Tiny_Google Server is assigning workload to helpers...");
				List<Long> breakPointsList = TextToolsBox.getFileSplitBreakPoints(indexingFile,
						helperList.size());

				// assign workload to each helper
				for (int i = 0; i < breakPointsList.size(); i++) {
					TinyGoogleToHelperMsg t2hMsg = null;
					Address helperAddress = helperList.get(i);
					IndexingWorkload workload = null;
					if (i == breakPointsList.size() - 1) {
						long startIndex = breakPointsList.get(i);
						long endIndex = TextToolsBox.getFileLength(indexingFile);
						workload = new IndexingWorkload(indexingFile, startIndex, endIndex);
						t2hMsg = new TinyGoogleToHelperMsg(TinyGoogleServer.tinyGoogleAddress,
								helperAddress, 0, indexingFile, workload);
					} else {
						long startIndex = breakPointsList.get(i);
						long endIndex = breakPointsList.get(i + 1) - 1;
						workload = new IndexingWorkload(indexingFile, startIndex, endIndex);
						t2hMsg = new TinyGoogleToHelperMsg(TinyGoogleServer.tinyGoogleAddress,
								helperAddress, 0, indexingFile, workload);
					}
					Socket socketToHelper = new Socket(helperAddress.ip,
							Integer.parseInt(helperAddress.port));
					helperIndexingWordloadTable.put(helperAddress, workload);
					System.out.println("  Tiny_Google Server completed workload assignment!");

					// send message to helper
					System.out.println("\n  Sending workload to helpers...");
					ObjectOutputStream toHelperObjectOutputStream = new ObjectOutputStream(
							socketToHelper.getOutputStream());
					toHelperObjectOutputStream.writeObject(t2hMsg);
					toHelperObjectOutputStream.flush();
					System.out.println("  Successfully sent workload to helpers!");

					// receive message from helper
					System.out.println("\n  Receiving return result from helpers...");
					ObjectInputStream fromHelperObjectInputStream = new ObjectInputStream(
							socketToHelper.getInputStream());
					HelperToTinyGoogleMsg h2tMsg = (HelperToTinyGoogleMsg) fromHelperObjectInputStream
							.readObject();
					if (h2tMsg == null) {
						System.err.println("\n  Failed in getting a reply message from Helper{"
								+ helperAddress.toString() + "}!");
						fromHelperObjectInputStream.close();
						toHelperObjectOutputStream.close();
						socketToHelper.close();
						return false;
					}
					/*
					 * After receiving a reply message from helper, TinyGoogleServer needs to do the
					 * follows things: 1. update 'helperIndexingWordloadTable' to mark the helper
					 * entry as work-completed; 2. execute REDUCE as in MapReduce by updating
					 * 'countByWordTable' in this thread; 3. update the two main data structures in
					 * TinyGoogleServer main class.
					 */
					helperIndexingWordloadTable.get(h2tMsg.getSenderAddress()).setCompletedWork(
							true);
					for (String word : h2tMsg.getCountByWordTable().keySet()) {
						if (!countByWordTable.containsKey(word)) {
							countByWordTable.put(word, h2tMsg.getCountByWordTable().get(word));
						} else {
							int newVal = countByWordTable.get(word)
									+ h2tMsg.getCountByWordTable().get(word);
							countByWordTable.put(word, newVal);
						}
					}
					for (String word : countByWordTable.keySet()) {
						if (!TinyGoogleServer.totalCountByWordTable.containsKey(word)) {
							TinyGoogleServer.totalCountByWordTable.put(word,
									countByWordTable.get(word));
						} else {
							int newVal = countByWordTable.get(word)
									+ TinyGoogleServer.totalCountByWordTable.get(word);
							countByWordTable.put(word, newVal);
						}
					}
					TextToolsBox.printCountTable(countByWordTable);
					/*
					 * Send the result back to client
					 */

					TinyGoogleToClientMsg t2cMsg = new TinyGoogleToClientMsg(
							TinyGoogleServer.tinyGoogleAddress, clientAddress, true);
					tinyGoogleObjectOutputStream.writeObject(t2cMsg);
					tinyGoogleObjectOutputStream.flush();

					fromHelperObjectInputStream.close();
					toHelperObjectOutputStream.close();
					socketToHelper.close();
				}
				break;
			/*
			 * The following case handles a client searching request
			 */
			case 1:
				List<String> keywordsList = c2tMsg.getSearchKeyWords();
				if (keywordsList == null) {
					System.err
							.println("Error in getting the list of keywords from a client request message.");
					return false;
				}
				List<WordCountPair> wordCountList = new ArrayList<>();
				for (String word : keywordsList) {
					wordCountList.add(new WordCountPair(word,
							TinyGoogleServer.totalCountByWordTable.get(word)));
				}
				Collections.sort(wordCountList, new Comparator<WordCountPair>() {
					@Override
					public int compare(WordCountPair o1, WordCountPair o2) {
						return o1.count == o2.count ? 0 : o1.count < o2.count ? -1 : 1;
					}
				});
				// print it to console and send to client
				for (WordCountPair w : wordCountList) {
					searchResult.add(w);
					System.out.println(w.word + " : " + w.count);
				}
				TinyGoogleToClientMsg t2cMsg = new TinyGoogleToClientMsg(
						TinyGoogleServer.tinyGoogleAddress, c2tMsg.getSenderAddress(), searchResult);
				tinyGoogleObjectOutputStream.writeObject(t2cMsg);
				tinyGoogleObjectOutputStream.flush();

				break;

			default: // fault
				System.err.println("\nTiny_Google Server{"
						+ TinyGoogleServer.tinyGoogleAddress.toString()
						+ "} failed to resolve client request type from Client{"
						+ c2tMsg.getSenderAddress() + "}!");
				return false;
			}
		}

		tinyGoogleObjectOutputStream.close();
		tinyGoogleObjectInputStream.close();
		tinyGoogleSocket.close();
		return true;
	}

	public static List<Address> getAvailableHelperListFromNameServer()
			throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
		System.out.println("Tiny_Google_Server{" + TinyGoogleServer.tinyGoogleAddress.toString()
				+ "} requesting the list of available Helper_Servers to Name_Server{"
				+ TinyGoogleServer.nameServerAddress.toString() + "}...");
		TinyGoogleServer.nameServerSocket = new Socket(TinyGoogleServer.nameServerAddress.ip,
				Integer.parseInt(TinyGoogleServer.nameServerAddress.port));
		ObjectOutputStream nameServerObjectOutputStream = new ObjectOutputStream(
				TinyGoogleServer.nameServerSocket.getOutputStream());
		TinyGoogleToNamingMsg t2nMsg = new TinyGoogleToNamingMsg(
				TinyGoogleServer.tinyGoogleAddress, TinyGoogleServer.nameServerAddress, 1);
		nameServerObjectOutputStream.writeObject(t2nMsg);
		nameServerObjectOutputStream.flush();

		ObjectInputStream nameServerObjectInputStream = new ObjectInputStream(
				TinyGoogleServer.nameServerSocket.getInputStream());
		NamingToTinyGoogleMsg n2tMsg = (NamingToTinyGoogleMsg) nameServerObjectInputStream
				.readObject();
		if (n2tMsg == null) {
			return null;
		}
		nameServerObjectOutputStream.close();
		nameServerObjectInputStream.close();
		TinyGoogleServer.nameServerSocket.close();
		TinyGoogleServer.availableHelperList = n2tMsg.getHelperServerAddressList();
		TinyGoogleServer.printAvailableHelperList();
		return TinyGoogleServer.availableHelperList;
	}

}
