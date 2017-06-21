package pes6j.datablocks;

public class IpInfo {
	long pId;
	String ip1;
	String ip2;
	int port1;
	int port2;
	int channel;

	public IpInfo(long pId, int channel, String ip1, int port1, String ip2, int port2) {
		this.ip1 = ip1;
		this.ip2 = ip2;
		this.channel = channel;
		this.port1 = port1;
		this.port2 = port2;
		this.pId = pId;
	}

	public int getChannel() {
		return( channel );
	}
	
	public String getIp1() {
		return (ip1);
	}

	public String getIp2() {
		return (ip2);
	}

	public int getPort1() {
		return (port1);
	}

	public int getPort2() {
		return (port2);
	}

	public long getPlayerId() {
		return (pId);
	}
}
