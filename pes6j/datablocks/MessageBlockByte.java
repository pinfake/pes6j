package pes6j.datablocks;

public class MessageBlockByte extends MessageBlock {

	public MessageBlockByte(byte value, int qId) {
		super();
		header.setQuery(qId);
		setData(new byte[] { value });
	}
}
