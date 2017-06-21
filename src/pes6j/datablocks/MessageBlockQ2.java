package pes6j.datablocks;

public class MessageBlockQ2 extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x49, (byte) 0x70, (byte) 0xDF,
			(byte) 0x03 };

	public MessageBlockQ2(int qId) {
		super();
		header.setQuery(qId);

		// byte[] data = new byte[MSG_DATA.length];
		// Arrays.fill( data, (byte)0x00 );
		//
		// setData( data );

		setData(MSG_DATA);
	}
}
