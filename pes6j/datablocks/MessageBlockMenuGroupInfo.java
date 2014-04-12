package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockMenuGroupInfo extends MessageBlock {

	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	static final byte[] HEADER_2 = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x80,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04 };

	public MessageBlockMenuGroupInfo(long playerId, GroupInfo group, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 4 + 37 + 51];
		Arrays.fill(data, (byte) 0x00);

		String grName = group.getName();

		if (grName.length() > 51) {
			grName = grName.substring(0, 51);
		}

		byte[] b_grname = grName.getBytes();
		byte[] b_playerid = Util.long2Word(playerId);
		int idx = 0;
		System.arraycopy(HEADER, 0, data, idx, HEADER.length);
		idx += HEADER.length;
		System.arraycopy(b_playerid, 0, data, idx, b_playerid.length);
		idx += b_playerid.length;
		System.arraycopy(HEADER_2, 0, data, idx, HEADER_2.length);
		idx += HEADER_2.length;
		System.arraycopy(b_grname, 0, data, idx, b_grname.length);

		setData(data);
	}
}
