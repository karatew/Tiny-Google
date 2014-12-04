package helper_server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import message.TinyGoogleToHelperMsg;
import tiny_google_server.TinyGoogleServer;
import util.IndexingWorkload;
import util.TextFileTools;

public class HelperServerThread implements Callable<Boolean> {

	@Override
	public Boolean call() throws Exception {
		Socket helperSocket = HelperServer.listener.accept();
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
			TextFileTools textFileTools = new TextFileTools(t2hMsg.getIndexingFile());
			ConcurrentHashMap<String, Integer> countByWordTable = textFileTools
					.getWordCountTable(indexingWorkload);
			// TODO send countByWordTable back to tiny google server
			
			
		}

		return true;
	}

}
