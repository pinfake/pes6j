package pes6j.datablocks;

public class MessageBlockGameRoomInfoShort extends MessageBlock {

	// completamente copiado
	static final byte[] ITEM_TAIL = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0xFF };

	// static final byte[]

	public MessageBlockGameRoomInfoShort(RoomInfo room, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[24];
		System.arraycopy(ITEM_TAIL, 0, data, 0, ITEM_TAIL.length);

		for (int j = 0; j < room.getNumPlayers(); j++) {
			long pId = room.getPlayerId(j);
			byte[] b_pid = Util.long2Word(pId);
			byte[] b_pos = Util.int2Word(j);
			System.arraycopy(b_pid, 0, data, j * 6, b_pid.length);
			// pos
			System.arraycopy(Util.int2Word(j), 1, data, (6 * j) + 4, 1);
			System.arraycopy(Util.int2Word(room.getPlayerSel(j)), 1, data,
					(6 * j) + 5, 1);
		}

		setData(data);
	}
}
