package pes6j.datablocks;

public class MessageBlockLoginQ4 extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };

	public MessageBlockLoginQ4(int qId) {
		super();
		header.setQuery(qId);
		setData(MSG_DATA);
	}
}
