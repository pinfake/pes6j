package pes6j.datablocks;

public class MessageBlockGameEnterRoomFailPass extends MessageBlock {
	byte[] RAWDATA = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFD,
			(byte) 0xDA, (byte) 0x00 };

	public MessageBlockGameEnterRoomFailPass(int qId) {
		super();
		header.setQuery(qId);
		setData(RAWDATA);
	}
}
