package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameFinishPoints extends MessageBlock {

	// completamente copiado
	static final byte[] ITEM_TAIL = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00

	// (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
	// (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x01
	};

	// static final byte[]

	// 4 (zeros) + 48 (nombre_player) + 4 (id-grupo) + 48 (nombre grupo) + 126
	// (traperadas)

	public MessageBlockGameFinishPoints(RoomInfo rooms[], int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[(4 + 2 + 65 + 60) * rooms.length];
		Arrays.fill(data, (byte) 0x00);

		int idx = 0;

		for (int i = 0; i < rooms.length; i++) {
			String rName = rooms[i].getName();
			long rId = rooms[i].getId();
			int rType = rooms[i].getType();

			if (rName.length() > 65) {
				rName = rName.substring(0, 65);
			}

			byte[] b_rid = Util.long2Word(rId);
			byte[] b_rtype = Util.int2Word(rType);
			byte[] b_rname = rName.getBytes();

			System.arraycopy(b_rid, 0, data, idx, b_rid.length);
			idx += b_rid.length;

			System.arraycopy(b_rtype, 0, data, idx, b_rtype.length);
			idx += b_rtype.length;

			System.arraycopy(rName.getBytes(), 0, data, idx,
					rName.getBytes().length);
			idx += 65;

			System.arraycopy(ITEM_TAIL, 0, data, idx, ITEM_TAIL.length);

			for (int j = 0; j < rooms[i].getNumPlayers(); j++) {
				long pId = rooms[i].getPlayerId(j);
				byte[] b_pid = Util.long2Word(pId);
				System.arraycopy(b_pid, 0, data, idx + (10 * j), b_pid.length);

				if (j == 0)
					data[idx + (10 * j) + 4] = (byte) 0x01;
				data[idx + (10 * j) + 8] = Util.int2Word(j)[1];
				data[idx + (10 * j) + 9] = Util.int2Word(rooms[i]
						.getPlayerSel(j))[1];
			}

			idx += 60;
			if (rooms[i].hasPassword())
				data[idx - 5] = (byte) 0x01;
			else
				data[idx - 5] = (byte) 0x00;
		}
		setData(data);
	}
}
