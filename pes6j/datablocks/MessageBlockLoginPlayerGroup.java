package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockLoginPlayerGroup extends MessageBlock {
	/*
	 * HEADER - PLAYERID - 37 bytes HEADER_2 - NOMBRE GRUPO (51 bytes)
	 */
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	static final byte[] PLY_HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };
	static final byte[] REPEAT = { (byte) 0x00, (byte) 0x60, (byte) 0xDC,
			(byte) 0x2E, (byte) 0x34, (byte) 0xD7, (byte) 0x69, (byte) 0x64,
			(byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// static final byte[]

	// 4 (zeros) + 48 (nombre_player) + 4 (id-grupo) + 48 (nombre grupo) + 126
	// (traperadas)
	// Es login_r7

	public MessageBlockLoginPlayerGroup(PlayerInfo player, GroupInfo group,
			int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 48 + 4 + 48 + 294];
		Arrays.fill(data, (byte) 0x00);

		String plName = player.getName();
		String grName = group.getName();
		long grId = group.getId();
		long plId = player.getId();
		byte[] b_plid = Util.long2Word(plId);
		byte[] b_grid = Util.long2Word(grId);

		// System.arraycopy(b_plid, 0, data, 0, b_plid.length);
		System.arraycopy(plName.getBytes(), 0, data, 4,
				plName.getBytes().length);
		System.arraycopy(b_grid, 0, data, 4 + 48, b_grid.length);
		System.arraycopy(grName.getBytes(), 0, data, 4 + 48 + 4, grName
				.getBytes().length);

		setData(data);
	}
}
