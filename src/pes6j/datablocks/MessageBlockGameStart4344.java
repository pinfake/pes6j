package pes6j.datablocks;

public class MessageBlockGameStart4344 extends MessageBlock {

	public MessageBlockGameStart4344(int phase, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[] { (byte) phase };

		setData(data);
	}
}
