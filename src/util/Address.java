package util;

import java.io.Serializable;

public class Address implements Serializable {
	public String ip;
	public String port;

	public Address(String ip, String port) {
		this.ip = ip;
		this.port = port;
	}

	public String toString() {
		if (ip != null || port != null) {
			return new String(ip + ":" + port);
		} else {
			return null;
		}
	}

	@Override
	public int hashCode() {
		return ip.hashCode() * 3 + port.hashCode() * 8;
	}

	@Override
	public boolean equals(Object a) {
		if (a instanceof Address) {
			return ((Address) a).ip.equals(((Address) a).ip)
					&& ((Address) a).port.equals(((Address) a).port);
		} else {
			return false;
		}
	}

}
