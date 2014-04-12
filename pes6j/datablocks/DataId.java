package pes6j.datablocks;

public class DataId {
	byte[] data;

	public DataId(byte[] data) {
		this.data = data;
	}

	public long getId() {
		return (Util.word2Long(data[0], data[1], data[2], data[3]));
	}
}
