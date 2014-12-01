package name_server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

import util.Address;

public class NameServer {
	protected static Address publicAddress;
	protected static ServerSocket listener;
	protected static ExecutorService executorService;
	protected static Timer serverRegistrationTimer = new Timer();
	protected final static int SERVER_TIME_OUT = 90; // in unit of seconds
	protected static ConcurrentHashMap<Address, Date> tinyGoogleServerAddressMap = new ConcurrentHashMap<>();
	protected static ConcurrentHashMap<Address, Date> helperServerAddressMap = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		try {
			listener = new ServerSocket(0);// create a listening port
			executorService = Executors.newCachedThreadPool(); // create a thread pool
			// create a thread pool with fixed threads
			// executorService = Executors.newFixedThreadPool(100);

			/* (1) get the public address of the NameServer */
			publicAddress = getCurrentAddress();
			System.out.println("\nSuccessfully created Name_Server{" + publicAddress.toString()
					+ "}\n");

			/* (2) update this address to the public directory */
			updatePublicDirectory();

			/* (3) set up the timer to maintain registration tables */
			setServerAvailabilityTimer();

			/* (4) looping, continuously listening to requests */
			while (true) {
				System.out.println("Name_Server is looping...");
				Boolean result = executorService.submit(new NameServerThread()).get();
				if (result) {
					// System.out.println("A request has been successfully handled by Name_Server!");
				} else {
					System.err.println("Name_Server failed in handling a request.");
				}
			}

		} catch (IOException e1) {
			System.out.println("Error in creating a listener for Name_Server.\n");
			e1.printStackTrace();
		} catch (InterruptedException | ExecutionException e) {
			System.out
					.println("Name_Server failed in getting a message or processing the message.\n");
			e.printStackTrace();
		}

	}

	/**
	 * (1) get the public address of the NameServer
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
	 * (2) update this address to the public directory
	 */
	private static void updatePublicDirectory() {
		try {
			/* Please CHANGE the file name and address in case needed!! */
			BufferedWriter writer = new BufferedWriter(new FileWriter("publicDNS.txt"));
			writer.write(publicAddress.ip + ":" + publicAddress.port);
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error: port-mapper cannot write to the public directory!\n");
			e.printStackTrace();
		}
	}

	/**
	 * (3) Start the timer for server registration table maintenance 
	 *      Please be noted that both TinyGoogle Server and Helper 
	 *      Servers will be maintained according to this timer. 
	 */
	private static void setServerAvailabilityTimer() {
		serverRegistrationTimer = new Timer();
		serverRegistrationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Date currentTime = new Date();
				System.out.println("\nName_Server 'heart-beat' timer is running...");
				for (Address addr : tinyGoogleServerAddressMap.keySet()) {
					if (currentTime.getTime() - tinyGoogleServerAddressMap.get(addr).getTime() > SERVER_TIME_OUT * 1000) {
						tinyGoogleServerAddressMap.remove(addr);
						System.out.println("Tiny_Google_Server{" + addr.toString()
								+ "} has been removed due to long-inactivity.");
					}
				}
				// for (Address addr : helperServerAddressMap.keySet()) {
				// if (currentTime.getTime() - helperServerAddressMap.get(addr).getTime() >
				// SERVER_TIME_OUT * 1000)
				// helperServerAddressMap.remove(addr);
				// }
				helperServerAddressMap
						.entrySet()
						.parallelStream()
						.unordered()
						.forEach(
								entry -> {
									if (currentTime.getTime() - entry.getValue().getTime() > SERVER_TIME_OUT * 1000) {
										Address addr = entry.getKey();
										System.out.println("Helper_Server{" + addr.toString()
												+ "} has been removed due to long-inactivity.");
										helperServerAddressMap.remove(addr);
									}
								});
				System.out.println("Active Tiny_Google_Server: "
						+ tinyGoogleServerAddressMap.size() + ";\nActive Helper_Server: "
						+ helperServerAddressMap.size());
			}
		}, 0, SERVER_TIME_OUT * 1000);
	}
}
