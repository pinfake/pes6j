package pes6j.datablocks;

public class DataJoinRoom {
	byte[] data;

	public DataJoinRoom(byte[] data) {
		this.data = data;
	}

	public long getRoomId() {
		return (Util.word2Long(data[0], data[1], data[2], data[3]));
	}

	public String getPassword() {
		return (new String(data, 4, Util.strlen(data, 4)));
	}
}
