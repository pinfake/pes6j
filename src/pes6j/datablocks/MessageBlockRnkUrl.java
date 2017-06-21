package pes6j.datablocks;

public class MessageBlockRnkUrl extends MessageBlock {

	static final byte[] DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x55 };

	public MessageBlockRnkUrl(int qId) {
		super();
		header.setQuery(qId);
		setData(DATA);
	}
}
