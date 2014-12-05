package client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import message.ClientToNamingMsg;
import message.ClientToTinyGoogleMsg;
import message.NamingToClientMsg;
import message.TinyGoogleToClientMsg;
import util.Address;
import util.WordCountPair;

/**
 * A client sends messages to NameServer and TinyGoogle Server separately.
 * It first sends a request to NameServer by resolving the publicDNS file. Then
 * it obtains the NameServer address and connects to it. NameServer then 
 * returns the address of TinyGoogle Server to the client so that he/she can 
 * send messages to TinyGoogle Server for two types of requests: indexing 
 * and searching.
 */
public class Client {

	private static Address nameServerAddress;
	private static Socket nameServerSocket;

	private static Address tinyGoogleAddress;
	private static Socket tinyGoogleSocket;

	private static Address clientAddress;

	private static String indexDirectoryPathName = null;
	private static List<String> searchKeyWords = new ArrayList<>();

	/**
	 * The input format for a user at client end is defined as following:
	 * For indexing:
	 * >0 afs/zichuan/cs2510/proj2/source/
	 * args[0] is the client request mode;
	 * args[1] is the indexDirectoryPathName;
	 * 
	 * For searching:
	 * >1 forest love jenny
	 * args[0] is the client request mode;
	 * args[1 to last] are the key words for searching;
	 * 
	 */
	public static void main(String[] args) {
		try {
			clientAddress = getCurrentAddress();
			/* (1) Find out NameServer from publicDNS */
			getPublicDNS();
			if (nameServerAddress == null) {
				System.err.println("Failed in finding Name_Server!");
				return;
			}
			/* (2) Send a message to NameServer to obtain TinyGoogle's address */
			getTinyGoogleServerAddress();
			if (tinyGoogleAddress == null) {
				System.err
						.println("Failed in resolving Tiny_Google_Server's address from Name_Server!");
				return;
			} else {
				System.out.println("Client{" + clientAddress.toString()
						+ "} successfully found Tiny_Google_Server{" + tinyGoogleAddress.toString()
						+ "}.");
			}

			/* (3) Connect to TinyGoogle Server */
			ClientToTinyGoogleMsg c2tMsg = null;
			if (args.length == 0) {
				System.err.println("Invalid input!");
				return;
			}
			if (Integer.parseInt(args[0]) == 0) { // client needs indexing
				indexDirectoryPathName = args[1];
				if (indexDirectoryPathName == null) {
					System.err.println("Invalid input Directory Path Name for indexing!");
					return;
				}
				c2tMsg = new ClientToTinyGoogleMsg(clientAddress, tinyGoogleAddress,
						indexDirectoryPathName);
			} else if (Integer.parseInt(args[0]) == 1) { // client needs searching
				for (int i = 1; i < args.length; i++) {
					searchKeyWords.add(args[i]);
				}
				if (searchKeyWords == null || searchKeyWords.size() == 0) {
					System.err
							.println("Invalid input for searching. Please type in the key words.");
					return;
				}
				c2tMsg = new ClientToTinyGoogleMsg(clientAddress, tinyGoogleAddress, searchKeyWords);
			} else {
				System.err.println("Invalid input mode for indexing or searching!");
				return;
			}

			System.out.println("Sending request to Tiny_Google Server...");
			tinyGoogleSocket = new Socket(tinyGoogleAddress.ip,
					Integer.parseInt(tinyGoogleAddress.port));
			ObjectOutputStream tinyGoogleObjectOutputStream = new ObjectOutputStream(
					tinyGoogleSocket.getOutputStream());
			tinyGoogleObjectOutputStream.writeObject(c2tMsg);
			tinyGoogleObjectOutputStream.flush();
			System.out.println("Client request is sent to Tiny_Google Server{"
					+ tinyGoogleAddress.toString() + "} successfully.");

			/* (4) Receive return message from Tiny_Google Server */
			ObjectInputStream tinyGoogleObjectInputStream = new ObjectInputStream(
					tinyGoogleSocket.getInputStream());
			TinyGoogleToClientMsg t2cMsg = (TinyGoogleToClientMsg) tinyGoogleObjectInputStream
					.readObject();
			int returnType = t2cMsg.getReturnResultType();
			switch (returnType) {
			case 0:
				System.out.println("Great! Indexing request is completed by Tiny_Google Server!");
				break;
			case 1:
				List<WordCountPair> result = t2cMsg.getSearchResult();
				Collections.sort(result, new Comparator<WordCountPair>() {
					@Override
					public int compare(WordCountPair o1, WordCountPair o2) {
						return o1.count == o2.count ? 0 : o1.count < o2.count ? -1 : 1;
					}
				});
				System.out.println("Searching key word occurance...");
				for (int i = 0; i < searchKeyWords.size(); i++) {
					System.out.println("      " + result.get(i).word + " : " + result.get(i).count);
				}
				break;
			default:
				System.err
						.println("\n  Error in identifying the return type of messages from Tiny_Google Server!");

			}
			// display the results
			displayReturnResult(t2cMsg);
			tinyGoogleObjectOutputStream.close();
			tinyGoogleObjectInputStream.close();
			tinyGoogleSocket.close();

		} catch (FileNotFoundException e) {
			System.err.println("Failed finding the file publicDNS.txt");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  (0) get current address for this client
	 */
	private static Address getCurrentAddress() throws IOException {
		String publicIP = "";
		Enumeration<NetworkInterface> e;
		ServerSocket listener = new ServerSocket(0);
		e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			Enumeration<InetAddress> addresses = e.nextElement().getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress inetAddress = (InetAddress) addresses.nextElement();
				/* check if inetAddress is a public address */
				if (!inetAddress.isAnyLocalAddress() && !inetAddress.isLinkLocalAddress()
						&& !inetAddress.isLoopbackAddress() && !inetAddress.isMCGlobal()
						&& !inetAddress.isMCLinkLocal() && !inetAddress.isMCNodeLocal()
						&& !inetAddress.isMCOrgLocal() && !inetAddress.isMCSiteLocal()
						&& !inetAddress.isMulticastAddress()) {
					publicIP = inetAddress.getHostAddress();
				}
			}
		}
		String publicPortNum = String.valueOf(listener.getLocalPort());
		listener.close();
		if (!publicIP.equals("") && !publicPortNum.equals("-1")) {
			return new Address(publicIP, publicPortNum);
		} else {
			return null;
		}
	}

	/**
	 *  (1) Find out NameServer from publicDNS 
	 */
	private static void getPublicDNS() throws FileNotFoundException, IOException {
		BufferedReader br = null;
		String sCurrentLine;
		br = new BufferedReader(new FileReader("publicDNS.txt"));
		while ((sCurrentLine = br.readLine()) != null) {
			String[] strArray = sCurrentLine.split(":");
			if (strArray[0] != null && strArray[1] != null) {
				// create a socket
				nameServerAddress = new Address(strArray[0], strArray[1]);
			}
		}
		if (br != null) {
			br.close();
		}
	}

	/**
	 *  (2) Send a message to NameServer to obtain TinyGoogle's address
	 */
	private static void getTinyGoogleServerAddress() throws NumberFormatException,
			UnknownHostException, IOException, ClassNotFoundException {
		nameServerSocket = new Socket(nameServerAddress.ip,
				Integer.parseInt(nameServerAddress.port));
		ObjectOutputStream nameServerObjectOutputStream = new ObjectOutputStream(
				nameServerSocket.getOutputStream());
		ClientToNamingMsg c2nMsg = new ClientToNamingMsg(clientAddress, nameServerAddress);
		nameServerObjectOutputStream.writeObject(c2nMsg);
		nameServerObjectOutputStream.flush();

		ObjectInputStream nameServerObjectInputStream = new ObjectInputStream(
				nameServerSocket.getInputStream());
		NamingToClientMsg n2cMsg = (NamingToClientMsg) nameServerObjectInputStream.readObject();
		tinyGoogleAddress = n2cMsg.getTinyGoogleServerAddress();
		nameServerObjectOutputStream.close();
		nameServerSocket.close();
	}

	/** 
	 * (4) Receive return message from Tiny_Google Server
	 */
	private static void displayReturnResult(TinyGoogleToClientMsg t2cMsg) {
		if (t2cMsg == null) {
			System.err
					.println("Failed in resolving content in Tiny_Google Server's return message.");
			return;
		}
		if (t2cMsg.getReturnResultType() == 0) {
			if (t2cMsg.isIndexed()) {
				System.out.println("Client indexing request is handled successfully.");
			} else {
				System.err
						.println("Tiny_Google Server failed in handling indexing request from client{"
								+ clientAddress.toString() + "}!");
			}
		} else if (t2cMsg.getReturnResultType() == 1) {
			if (t2cMsg.getSearchResult() == null) {
				System.err.println("Failed in getting search result from Tiny_Google Server.");
			} else {
				t2cMsg.getSearchResult().forEach(System.out::println);
			}
		} else {
			System.err.println("Invalid 'return result type'!'");
		}
	}

}
