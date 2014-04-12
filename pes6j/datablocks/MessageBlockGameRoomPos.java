package pes6j.datablocks;

public class MessageBlockGameRoomPos extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };

	public MessageBlockGameRoomPos(int pos, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = MSG_DATA;
		data[4] = Util.int2Word(pos)[1];
		setData(MSG_DATA);
	}
}
