package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameStart1 extends MessageBlock {

	/*
	 * Primero van los ids de los contendientes, luego los id de grupo de cada
	 * uno
	 */
	byte[] DUNNO = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x1C,
			(byte) 0x07 };

	public MessageBlockGameStart1(RoomInfo room, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[37];
		Arrays.fill(data, (byte) 0x00);
		data[0] = (byte) 0x02; // Fue igual en un partido de 1 contra 1 que 1
								// contra 2
		int idx = 1;
		for (int i = 0; i < room.getNumPlayersOnSel(); i++) {
			long pid = room.getPlayerIdOnSel(i);
			byte b_pid[] = Util.long2Word(pid);
			System.arraycopy(b_pid, 0, data, idx, b_pid.length);
			idx += b_pid.length;
		}

		setData(data);
	}
}
