package tiny_google_server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import name_server.NameServer;
import message.TinyGoogleToNamingMsg;
import util.Address;

public class TinyGoogleServer {

	private static Socket nameServerSocket;
	private static Address nameServerAddress;

	private static ServerSocket listener;
	private static ExecutorService executorService;
	private static Timer registrationTimer = new Timer();
	private static Address tinyGoogleAddress = null;
	private final static int REFRESH_TIME = 30; // in unit of second

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
				Socket socket = listener.accept();
				// TODO define that TinyGoogler Server needs to do in handling requests from the
				// following components: NameServer, Client, Helper
				Boolean result = executorService.submit(new TinyGoogleServerThread()).get();
				if (result == null) {
					System.err.println("Tiny_Google_Server{" + tinyGoogleAddress.toString()
							+ "} failed in execution!");
				}
				if (!result) {
					System.err.println("Tiny_Google Server failed in executing a thread!");
				} else {

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

}
