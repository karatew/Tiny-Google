package helper_server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.HelperToNamingMsg;
import util.Address;

public class HelperServer {
	private static Socket nameServerSocket;
	private static Address nameServerAddress;

	private static Address tinyGoogleServerAddress;
	private static Socket tinyGoogleServerSocket;

	private static Address helperServerAddress;

	private static ServerSocket listener;

	private static ExecutorService executorService;

	private static Timer registrationTimer = new Timer();
	private final static int REFRESH_TIME = 30; // in unit of second

	public static void main(String[] args) {
		try {
			listener = new ServerSocket(0);
			executorService = Executors.newCachedThreadPool();
			helperServerAddress = getCurrentAddress();
			if (helperServerAddress != null) {
				System.out.println("\nSuccessfully created Tiny_Goole server{"
						+ helperServerAddress.toString() + "}\n");
			} else {
				System.err.println("Failed in getting an address for Tiny_Google server!");
				return;
			}

			getNameServerAddress();
			if (nameServerAddress == null) {
				System.err.println("Failed in finding a correct Name_Server!");
				return;
			}

			/*
			 * (2) Register HelperServer to NameServer using a "heart-beat" acknowledgement
			 * mechanism.
			 */
			registrationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						registerHelperToNameServer();
					} catch (Exception e) {
						System.err.println("Tiny_Google server timer scheduling failure!");
						e.printStackTrace();
					}
				}
			}, 0, REFRESH_TIME * 1000);

			/* (3) Listen to incoming request that are sent by TinyGoogle Server */
			while (true) {
				Socket socket = listener.accept();
				Boolean result = executorService.submit(new HelperServerThread()).get();
				if (!result) {
					System.err.println("Helper Server{" + helperServerAddress.toString()
							+ "} failed in executing a thread!");
				} else {

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * (1) get the public address of the this Helper
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
	 * @throws IOException 
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
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws NumberFormatException 
	 */
	private static void registerHelperToNameServer() throws NumberFormatException,
			UnknownHostException, IOException {
		System.out.println("Registering to Name_Server{" + nameServerAddress.toString() + "}.");
		nameServerSocket = new Socket(nameServerAddress.ip,
				Integer.parseInt(nameServerAddress.port));
		ObjectOutputStream nameServerObjectOutputStream = new ObjectOutputStream(
				nameServerSocket.getOutputStream());
		HelperToNamingMsg h2nMsg = new HelperToNamingMsg(helperServerAddress, nameServerAddress);
		nameServerObjectOutputStream.writeObject(h2nMsg);
		nameServerObjectOutputStream.flush();
		nameServerObjectOutputStream.close();
		nameServerSocket.close();
		System.out.println("Registration to Name_Server{" + nameServerAddress.toString()
				+ "} is completed.");
	}

}
