package pes6j.datablocks;

public class DataRoom {
	byte[] data;

	public DataRoom(byte[] data) {
		this.data = data;
	}

	public String getName() {
		return (new String(data, 0, Util.strlen(data, 0)));
	}

	public boolean hasPassword() {
		byte b_haspass = data[64];
		if (b_haspass == (byte) 0x01)
			return (true);
		return (false);
	}

	public String getPassword() {
		return (new String(data, 65, Util.strlen(data, 65)));
	}
}
