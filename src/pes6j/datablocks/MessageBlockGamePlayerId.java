package pes6j.datablocks;

public class MessageBlockGamePlayerId extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };

	public MessageBlockGamePlayerId(long pid, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = MSG_DATA;
		byte[] b_pid = Util.long2Word(pid);
		System.arraycopy(b_pid, 0, data, 4, b_pid.length);

		setData(data);
	}
}
