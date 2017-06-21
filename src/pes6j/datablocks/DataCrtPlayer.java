package pes6j.datablocks;

public class DataCrtPlayer {
	byte[] data;

	public DataCrtPlayer(byte[] data) {
		this.data = data;
	}

	public int getPos() {
		return (data[0]);
	}

	public String getName() {

		String name = null;
		try {
			name = new String(data, 1, Util.strlen(data, 1), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (name);
	}
}
