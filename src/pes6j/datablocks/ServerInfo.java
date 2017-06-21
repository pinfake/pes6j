package pes6j.datablocks;

public class ServerInfo {
	int type;
	int id;
	String name;
	String ip;
	int port;
	int wport;
	int numPlayers;

	public ServerInfo(int id, int type, String name, String ip, int port ) {
		this.type = type;
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.id = id;
		this.numPlayers = 0;
		this.wport = 0;
	}

	public int getWPort() {
		return( this.wport );
	}
	
	public void setWPort( int wport ) {
		this.wport = wport;
	}
	
	public int getId() {
		return( id );
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
	
	public void setNumPlayers(int numPlayers) {
		this.numPlayers = numPlayers;
	}
}
