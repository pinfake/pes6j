package pes6j.datablocks;

public class ServerInfo {
	int type;
	String name;
	String ip;
	int port;
	int numPlayers;

	public ServerInfo(int type, String name, String ip, int port, int numPlayers) {
		this.type = type;
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.numPlayers = numPlayers;
	}

	public int getType() {
		return (type);
	}

	public String getName() {
		return (name);
	}

	public String getIp() {
		return (ip);
	}

	public int getPort() {
		return (port);
	}
	
	public int getNumPlayers() {
		return (numPlayers);
	}
}
