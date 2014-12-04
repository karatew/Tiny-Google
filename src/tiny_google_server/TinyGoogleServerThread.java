package tiny_google_server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import util.Address;
import util.TextFileTools;
import util.IndexingWorkload;
import util.WordCountPair;
import message.ClientToTinyGoogleMsg;
import message.HelperToTinyGoogleMsg;
import message.NamingToTinyGoogleMsg;
import message.TinyGoogleToClientMsg;
import message.TinyGoogleToHelperMsg;

public class TinyGoogleServerThread implements Callable<Boolean> {
	// This table is used to record each helper with its assigned workload.
	ConcurrentHashMap<Address, IndexingWorkload> helperIndexingWordloadTable = new ConcurrentHashMap<>();
	// Inverted index table for each file
	protected static ConcurrentHashMap<String, Integer> countByWordTable = new ConcurrentHashMap<>();

	@Override
	public Boolean call() throws Exception {
		Socket tinyGoogleSocket = TinyGoogleServer.listener.accept();
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
				List<Address> helperList = TinyGoogleServer.getAvailableHelperListFromNameServer();

				// split the workload and send them to helpers
				TextFileTools textFileTools = new TextFileTools(indexingFile, helperList.size());
				List<Long> breakPointsList = textFileTools.getFileSplitBreakPoints();

				// assign workload to each helper
				for (int i = 0; i < breakPointsList.size(); i++) {
					TinyGoogleToHelperMsg t2hMsg = null;
					Address helperAddress = helperList.get(i);
					if (i == breakPointsList.size() - 1) {
						long startIndex = breakPointsList.get(i);
						long endIndex = textFileTools.file.length();
						IndexingWorkload workload = new IndexingWorkload(textFileTools.file,
								startIndex, endIndex);
						t2hMsg = new TinyGoogleToHelperMsg(TinyGoogleServer.tinyGoogleAddress,
								helperAddress, 0, indexingFile, workload);
					} else {
						long startIndex = breakPointsList.get(i);
						long endIndex = breakPointsList.get(i + 1) - 1;
						IndexingWorkload workload = new IndexingWorkload(textFileTools.file,
								startIndex, endIndex);
						t2hMsg = new TinyGoogleToHelperMsg(TinyGoogleServer.tinyGoogleAddress,
								helperAddress, 0, indexingFile, workload);
					}
					Socket socketToHelper = new Socket(helperAddress.ip,
							Integer.parseInt(helperAddress.port));

					ObjectOutputStream toHelperObjectOutputStream = new ObjectOutputStream(
							socketToHelper.getOutputStream());
					ObjectInputStream fromHelperObjectInputStream = new ObjectInputStream(
							socketToHelper.getInputStream());
					// send message to helper
					toHelperObjectOutputStream.writeObject(t2hMsg);
					toHelperObjectOutputStream.flush();
					// receive message from helper
					HelperToTinyGoogleMsg h2tMsg = (HelperToTinyGoogleMsg) fromHelperObjectInputStream
							.readObject();
					if (h2tMsg == null) {
						System.err.println("Failed in getting a reply message from Helper{"
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
					fromHelperObjectInputStream.close();
					toHelperObjectOutputStream.close();
					socketToHelper.close();
				}
				textFileTools.closeFile();
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
				List<WordCountPair> searchResult = new ArrayList<>();
				// print it to console and send to client
				for (WordCountPair w : wordCountList) {
					searchResult.add(w);
					System.out.println(w.word + " : " + w.count);
				}
				TinyGoogleToClientMsg t2cMsg = new TinyGoogleToClientMsg(
						TinyGoogleServer.tinyGoogleAddress, c2tMsg.getSenderAddress(), searchResult);
				Socket toClientSocket = new Socket(c2tMsg.getSenderAddress().ip,
						Integer.parseInt(c2tMsg.getReceiverAddress().port));
				ObjectOutputStream toClientObjectOutputStream = new ObjectOutputStream(
						toClientSocket.getOutputStream());
				toClientObjectOutputStream.writeObject(t2cMsg);
				toClientObjectOutputStream.flush();
				toClientObjectOutputStream.close();
				toClientSocket.close();
				break;
			default: // fault
				System.err.println("\nTiny_Google Server{"
						+ TinyGoogleServer.tinyGoogleAddress.toString()
						+ "} failed to resolve client request type from Client{"
						+ c2tMsg.getSenderAddress() + "}!");
				return false;
			}
		}
		// Helper sends two types of computed results to TinyGoogleServer
		if (requestMsg instanceof HelperToTinyGoogleMsg) {

		}

		tinyGoogleObjectOutputStream.close();
		tinyGoogleObjectInputStream.close();
		tinyGoogleSocket.close();
		return true;
	}
}
