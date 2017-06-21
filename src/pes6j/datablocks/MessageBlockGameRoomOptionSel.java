package pes6j.datablocks;

public class MessageBlockGameRoomOptionSel extends MessageBlock {
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };

	public MessageBlockGameRoomOptionSel(int option, int sel, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[HEADER.length + 2];

		System.arraycopy(HEADER, 0, data, 0, HEADER.length);

		data[HEADER.length] = Util.int2Word(option)[1];
		data[HEADER.length + 1] = Util.int2Word(sel)[1];

		setData(data);
	}
}
