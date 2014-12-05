package tiny_google_server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.TinyGoogleToNamingMsg;
import util.Address;

public class TinyGoogleServer {

	protected static Socket nameServerSocket;
	protected static Address nameServerAddress;

	protected static ServerSocket listener;
	protected static ExecutorService executorService;
	protected static Timer registrationTimer = new Timer();
	protected static Address tinyGoogleAddress = null;
	protected final static int REFRESH_TIME = 30; // in unit of second

	// List of available helpers that TinyGoogle Server keeps records of
	protected static List<Address> availableHelperList = null;

	/**
	 * The main class of TinyGoogleServer is responsible in handling all the 
	 * indexing information for the files that has been requested by clients. 
	 * To enhance durability when failures occur, all the necessary data 
	 * structures are stored in both main memory and on local disk. A timer 
	 * is used to update the local disk copies for every 30 to 90 seconds, 
	 * depending on the size of the files to write.
	 * 
	 * 'indexTableByFile' is a table to record the index table for each file. It 
	 * stores the result of word-count for each file.
	 * 
	 * 'totalCountByWordTable' is used to store the entire word counts for 
	 * all indexed files. Every time when a client issues a search request, the 
	 * results is given according to the results in this table.
	 * 
	 * Both of these two tables are store in-memory and on-disk.
	 */
	// This table records the index table for each file
	// Map<file_path, Map<word, count>>
	protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> indexTableByFile = new ConcurrentHashMap<>();
	// Inverted Index Table
	// Map<word, count>
	protected static ConcurrentHashMap<String, Integer> totalCountByWordTable = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		try {
			/* (1) Set up a server listener for Tiny_Google server */
			listener = new ServerSocket(0);
			executorService = Executors.newCachedThreadPool(); // create a thread pool
			tinyGoogleAddress = getCurrentAddress();
			if (tinyGoogleAddress != null) {
				System.out.println("\nSuccessfully created Tiny_Google server{"
						+ tinyGoogleAddress.toString() + "}\n");
			} else {
				System.err.println("Failed in getting an address for Tiny_Google server!");
				return;
			}

			getNameServerAddress();
			if (nameServerAddress == null) {
				System.out.println("Failed in finding a correct Name_Server!");
				return;
			}

			// (2) Register Tiny_Google server to Name_Server using a "heart-beat" acknowledgement
			// mechanism.
			registrationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						registerTinyGoogleToNameServer();
					} catch (Exception e) {
						System.err.println("Tiny_Google server timer scheduling failure!");
						e.printStackTrace();
					}
				}
			}, 0, REFRESH_TIME * 1000);

			/* (3) Listen to incoming request that are sent by Name_Server, Clients and/or helpers */
			while (true) {
				Socket tinyGoogleSocket = listener.accept();
				Boolean result = executorService.submit(
						new TinyGoogleServerThread(tinyGoogleSocket)).get();
				if (result == null) {
					System.err.println("Tiny_Google_Server{" + tinyGoogleAddress.toString()
							+ "} failed in execution!");
				}
				if (!result) {
					System.err.println("Tiny_Google Server failed in executing a thread!");
				} else {
					System.out.println("Correct result of MapReduce by a helper is returned.");
				}
			}

		} catch (Exception e) {
			System.err.println("Tiny_Google server runtime error occurred in main method!");
			e.printStackTrace();
		}
	}

	/**
	 * (1) get the public address of the TinyGoogle
	 */
	private static Address getCurrentAddress() {
		String publicIP = "";
		Enumeration<NetworkInterface> e;
		try {
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
			if (!publicIP.equals("") && !publicPortNum.equals("-1")) {
				return new Address(publicIP, publicPortNum);
			} else {
				return null;
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	/**
	 * Read the public directory to find out the address of the Name_Server
	 */
	private static void getNameServerAddress() throws IOException {
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
	 * (2) Register Tiny_Google server to Name_Server 
	 */
	private static void registerTinyGoogleToNameServer() throws Exception {
		System.out.println("Tiny_Google_Server{" + tinyGoogleAddress.toString()
				+ "} registering to Name_Server{" + nameServerAddress.toString() + "}...");
		nameServerSocket = new Socket(nameServerAddress.ip,
				Integer.parseInt(nameServerAddress.port));
		ObjectOutputStream nameServerObjectOutputStream = new ObjectOutputStream(
				nameServerSocket.getOutputStream());
		TinyGoogleToNamingMsg t2nMsg = new TinyGoogleToNamingMsg(tinyGoogleAddress,
				nameServerAddress, 0);
		nameServerObjectOutputStream.writeObject(t2nMsg);
		nameServerObjectOutputStream.flush();
		nameServerObjectOutputStream.close();
		nameServerSocket.close();
		System.out.println("Registration to Name_Server{" + nameServerAddress.toString()
				+ "} is completed!");
	}

	// public static List<Address> getAvailableHelperListFromNameServer()
	// throws NumberFormatException, UnknownHostException, IOException, ClassNotFoundException {
	// System.out.println("Tiny_Google_Server{" + tinyGoogleAddress.toString()
	// + "} requesting the list of available Helper_Servers to Name_Server{"
	// + nameServerAddress.toString() + "}...");
	// nameServerSocket = new Socket(nameServerAddress.ip,
	// Integer.parseInt(nameServerAddress.port));
	// ObjectOutputStream nameServerObjectOutputStream = new ObjectOutputStream(
	// nameServerSocket.getOutputStream());
	// TinyGoogleToNamingMsg t2nMsg = new TinyGoogleToNamingMsg(tinyGoogleAddress,
	// nameServerAddress, 0);
	// nameServerObjectOutputStream.writeObject(t2nMsg);
	// nameServerObjectOutputStream.flush();
	//
	// ObjectInputStream nameServerObjectInputStream = new ObjectInputStream(
	// nameServerSocket.getInputStream());
	// NamingToTinyGoogleMsg n2tMsg = (NamingToTinyGoogleMsg) nameServerObjectInputStream
	// .readObject();
	// if (n2tMsg == null) {
	// return null;
	// }
	// nameServerObjectOutputStream.close();
	// nameServerObjectInputStream.close();
	// nameServerSocket.close();
	// availableHelperList = n2tMsg.getHelperServerAddressList();
	// return availableHelperList;
	// }

	public static void printAvailableHelperList() {
		if (availableHelperList == null) {
			System.err.println("No content in available helper list!!");
			return;
		}
		System.out.println("\n  Available Helpers:");
		int count = 0;
		for (Address addr : availableHelperList) {
			System.out.println("    (" + count + ") " + addr.toString());
			count++;
		}
	}
}
