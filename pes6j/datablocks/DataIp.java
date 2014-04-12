package pes6j.datablocks;

public class DataIp {
	byte[] data;

	public DataIp(byte[] data) {
		this.data = data;
	}

	public int getChannelId() {
		return( (int)data[0] );
	}
	
	public String getIp1() {
		return (new String(data, 1, Util.strlen(data, 1)));
	}

	public int getPort1() {
		return (Util.word2Int(data[17], data[18]));
	}

	public String getIp2() {
		return (new String(data, 19, Util.strlen(data, 19)));
	}

	public int getPort2() {
		return (Util.word2Int(data[35], data[36]));
	}
}
