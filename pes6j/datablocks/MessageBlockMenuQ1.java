package pes6j.datablocks;

public class MessageBlockMenuQ1 extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x38, (byte) 0x2B, (byte) 0x46,
			(byte) 0x47, (byte) 0x02, (byte) 0x4B, (byte) 0x2F, (byte) 0x68,
			(byte) 0x56, (byte) 0x28, (byte) 0x3F, (byte) 0x53, (byte) 0x10,
			(byte) 0x87, (byte) 0x32, (byte) 0xA0 };

	public MessageBlockMenuQ1(int qId) {
		super();
		header.setQuery(qId);
		setData(MSG_DATA);
	}
}
