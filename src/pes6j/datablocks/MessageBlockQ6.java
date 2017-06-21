package pes6j.datablocks;

public class MessageBlockQ6 extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x49, (byte) 0x70, (byte) 0xDF,
			(byte) 0x1A };

	public MessageBlockQ6(long time, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[MSG_DATA.length];

		data = Util.long2Word((time / 1000));
		// Arrays.fill( data, (byte)0x00 );

		setData(data);

		// setData( MSG_DATA );
	}
}
