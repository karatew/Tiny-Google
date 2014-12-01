package name_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import util.Address;
import message.ClientToNamingMsg;
import message.HelperToNamingMsg;
import message.NamingToClientMsg;
import message.NamingToTinyGoogleMsg;
import message.TinyGoogleToNamingMsg;

public class NameServerThread implements Callable<Boolean> {

	@Override
	public Boolean call() throws Exception {
		try {
			Socket socket = NameServer.listener.accept(); // create a socket for listener
			InputStream inputStream = socket.getInputStream();
			ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
			Object requestMsg = objectInputStream.readObject();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

			if (requestMsg == null) {
				System.err.println("Name_Server received a \"null\" message.");
				System.err.println("Failed in resolving Tiny_Google Server's address!");
				objectOutputStream.close();
				objectInputStream.close();
				socket.close();
				return false;
			}
			// if incoming request is sent by TinyGoogle Server
			if (requestMsg instanceof TinyGoogleToNamingMsg) {
				Address tinyGoogleServerAddress = ((TinyGoogleToNamingMsg) requestMsg)
						.getSenderAddress();
				if (tinyGoogleServerAddress == null) {
					System.err.println("Failed in resolving Tiny_Google Server's address!");
					objectOutputStream.close();
					objectInputStream.close();
					socket.close();
					return false;
				}
				System.out.println("Name_Server received a message from Tiny_Google Server{"
						+ tinyGoogleServerAddress.toString() + "}.");
				int requestType = ((TinyGoogleToNamingMsg) requestMsg).getRequestType();

				switch (requestType) {
				case 0: // TinyGoogle registers to NameServer
					// NameServer.tinyGoogleServerAddressMap.clear();
					NameServer.tinyGoogleServerAddressMap.put(tinyGoogleServerAddress, new Date());
					System.out.println("Tiny_Google_Server{" + tinyGoogleServerAddress.toString()
							+ "} registration is completed!");
					System.out.println("Active Tiny_Google_Server: "
							+ NameServer.tinyGoogleServerAddressMap.size()
							+ ";\nActive Helper_Server: "
							+ NameServer.helperServerAddressMap.size());
					break;
				case 1: // TinyGoogle looks up available Helpers
					List<Address> availableHelperServerList = getAvailableHelperServer();
					NamingToTinyGoogleMsg returnMsg = new NamingToTinyGoogleMsg(
							NameServer.publicAddress, tinyGoogleServerAddress,
							availableHelperServerList);
					objectOutputStream.writeObject(returnMsg);
					objectOutputStream.flush();
					System.out.println("Active Helper_Servers look-up is completed!");
					break;
				}
			}
			// incoming message is sent by a client
			if (requestMsg instanceof ClientToNamingMsg) {
				Address clientAddress = ((ClientToNamingMsg) requestMsg).getSenderAddress();
				if (clientAddress == null) {
					System.err.println("Failed in resolving the client's address!");
					objectOutputStream.close();
					objectInputStream.close();
					socket.close();
					return false;
				}
				System.out.println("Name_Server received a message from a client{"
						+ clientAddress.toString() + "}.");
				if (NameServer.tinyGoogleServerAddressMap == null
						|| NameServer.tinyGoogleServerAddressMap.size() == 0) {
					System.err.println("No available Tiny_Google Server in Name_Server!");
					objectOutputStream.close();
					objectInputStream.close();
					socket.close();
					return false;
				}
				Address tinyGoogleServerAddress = null;
				for (Address addr : NameServer.tinyGoogleServerAddressMap.keySet()) {
					tinyGoogleServerAddress = addr;
				}
				if (tinyGoogleServerAddress == null) {
					System.err.println("Failed in finding an available Tiny_Google Server!");
					objectOutputStream.close();
					objectInputStream.close();
					socket.close();
					return false;
				}
				NamingToClientMsg returnMsg = new NamingToClientMsg(NameServer.publicAddress,
						clientAddress, tinyGoogleServerAddress);
				objectOutputStream.writeObject(returnMsg);
				objectOutputStream.flush();
			}
			// incoming message is sent by a helper
			if (requestMsg instanceof HelperToNamingMsg) {
				Address helperAddress = ((HelperToNamingMsg) requestMsg).getSenderAddress();
				if (helperAddress == null) {
					System.err.println("Failed in resolving the Helper_Server's address!");
					objectOutputStream.close();
					objectInputStream.close();
					socket.close();
					return false;
				}
				System.out.println("Name_Server received a message from a Helper_Server{"
						+ helperAddress.toString() + "}.");
				NameServer.helperServerAddressMap.put(helperAddress, new Date());
				System.out.println("Helper_Server{" + helperAddress.toString()
						+ "} registration is completed!");
				System.out.println("Active Tiny_Google_Server: "
						+ NameServer.tinyGoogleServerAddressMap.size()
						+ ";\nActive Helper_Server: " + NameServer.helperServerAddressMap.size());
			}
			objectOutputStream.close();
			objectInputStream.close();
			socket.close();
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private List<Address> getAvailableHelperServer() {
		List<Address> availableHelperList = new ArrayList<>();
		if (NameServer.helperServerAddressMap == null
				|| NameServer.helperServerAddressMap.size() == 0) {
			return availableHelperList;
		}
		// NameServer.helperServerAddressMap.entrySet().parallelStream().unordered()
		// .forEach(entry -> {
		// availableHelperList.add(entry.getKey());
		// });
		for (Address addr : NameServer.helperServerAddressMap.keySet()) {
			availableHelperList.add(addr);
		}
		return availableHelperList;
	}
}
