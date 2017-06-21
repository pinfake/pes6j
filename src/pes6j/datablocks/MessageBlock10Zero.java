package pes6j.datablocks;

public class MessageBlock10Zero extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public MessageBlock10Zero(int qId) {
		super();
		header.setQuery(qId);
		setData(MSG_DATA);
	}
}
