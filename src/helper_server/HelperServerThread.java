package helper_server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import message.HelperToTinyGoogleMsg;
import message.TinyGoogleToHelperMsg;
import util.IndexingWorkload;
import util.TextToolsBox;

public class HelperServerThread implements Callable<Boolean> {

	private Socket helperSocket;

	public HelperServerThread(Socket helperSocket) {
		this.helperSocket = helperSocket;
	}

	@Override
	public Boolean call() throws Exception {

		InputStream helperInputStream = helperSocket.getInputStream();
		ObjectInputStream helperObjectInputStream = new ObjectInputStream(helperInputStream);
		Object requestMsg = helperObjectInputStream.readObject();
		ObjectOutputStream helperObjectOutputStream = new ObjectOutputStream(
				helperSocket.getOutputStream());

		if (requestMsg == null) {
			System.err.println("\nHelper_Server received a \"null\" message.");
			helperObjectInputStream.close();
			helperInputStream.close();
			helperSocket.close();
			return false;
		}
		if (requestMsg instanceof TinyGoogleToHelperMsg) {
			TinyGoogleToHelperMsg t2hMsg = (TinyGoogleToHelperMsg) requestMsg;
			System.out.println("\nHelper{" + HelperServer.helperServerAddress.toString()
					+ "} received a message from Tiny_Google Server{"
					+ t2hMsg.getSenderAddress().toString() + "}!");
			IndexingWorkload indexingWorkload = t2hMsg.getIndexingWorkload();
			if (indexingWorkload == null) {
				System.err
						.println("Error! Helper received an invalid workload assignment for indexing!");
				return false;
			}
			ConcurrentHashMap<String, Integer> countByWordTable = TextToolsBox
					.getWordCountTable(indexingWorkload);
			indexingWorkload.setCompletedWork(true);

			// FOR DEBUGGING PURPOSES:
			TextToolsBox.printCountTable(countByWordTable);

			// send countByWordTable back to tiny google server
			HelperToTinyGoogleMsg h2tMsg = new HelperToTinyGoogleMsg(
					HelperServer.helperServerAddress, t2hMsg.getSenderAddress(), countByWordTable,
					indexingWorkload);
			// Socket toTinyGoogleSocket = new Socket(t2hMsg.getSenderAddress().ip,
			// Integer.parseInt(t2hMsg.getSenderAddress().port));
			// ObjectOutputStream toTinyGoogleObjectOutputStream = new ObjectOutputStream(
			// toTinyGoogleSocket.getOutputStream());
			helperObjectOutputStream.writeObject(h2tMsg);
			helperObjectOutputStream.flush();
			helperObjectOutputStream.close();
			helperInputStream.close();
			helperSocket.close();
		}
		return true;
	}

}
