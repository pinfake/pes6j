package pes6j.datablocks;

public class MessageBlockLoginMessages extends MessageBlock {
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };// (byte)0x00, (byte)0x0C, (byte)0xE1, (byte)0xBA };

	public MessageBlockLoginMessages(long playerId, int qId) {
		super();
		header.setQuery(qId);
		byte[] data = new byte[8];
		System.arraycopy(HEADER, 0, data, 0, HEADER.length);
		byte[] b_id = Util.long2Word(playerId);
		System.arraycopy(b_id, 0, data, HEADER.length, b_id.length);
		setData(data);
	}
}
