package pes6j.datablocks;

public class MessageBlockGameEnterRoomFailFull extends MessageBlock {
	byte[] RAWDATA = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFE,
			(byte) 0x09, (byte) 0x01 };

	public MessageBlockGameEnterRoomFailFull(int qId) {
		super();
		header.setQuery(qId);

		setData(RAWDATA);
	}
}
