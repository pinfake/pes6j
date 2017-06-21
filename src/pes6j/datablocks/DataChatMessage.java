package pes6j.datablocks;

public class DataChatMessage {
	byte[] data;

	public DataChatMessage(byte[] data) {
		this.data = data;
	}

	/*
	 * 4 bytes anteriores el id del player 4 anteriores FFFFFFFF (probablemente
	 * el id del salon) 2 anteriores 00 01
	 */

	public int getChannel() {
		return( data[1]);
//		int chan_id = Util.word2Int(data[0], data[1]);
//		return (chan_id);
	}
	
	public int getPlace() {
		return( data[0]);
	}

	public String getMessage() {
		return (new String(data, 10, Util.strlen(data, 10)));
	}
}
