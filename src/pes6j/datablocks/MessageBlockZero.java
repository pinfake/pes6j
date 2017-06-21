package pes6j.datablocks;

public class MessageBlockZero extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };

	public MessageBlockZero(int qId) {
		super();
		header.setQuery(qId);
		setData(MSG_DATA);
	}
}
