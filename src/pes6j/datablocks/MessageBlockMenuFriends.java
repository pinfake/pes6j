package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockMenuFriends extends MessageBlock {

	public MessageBlockMenuFriends(PlayerInfo players[], int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[(5 + 48) * players.length];
		int idx = 0;
		for (int i = 0; i < players.length; i++) {
			PlayerInfo pi = players[i];
			String plName = pi.getName();
			long id = pi.getId();

			if (plName.length() > 48)
				plName = plName.substring(0, 48);

			byte[] b_header = buildPlyHeader(pi);
			byte[] b_name = new byte[48];
			Arrays.fill(b_name, (byte) 0x00);
			System.arraycopy(plName.getBytes(), 0, b_name, 0,
					plName.getBytes().length);

			System.arraycopy(b_header, 0, data, idx, b_header.length);
			idx += b_header.length;
			System.arraycopy(b_name, 0, data, idx, b_name.length);
			idx += b_name.length;
		}

		setData(data);
	}

	byte[] buildPlyHeader(PlayerInfo player) {
		byte[] ret = new byte[5];
		byte[] b_id = Util.long2Word(player.getId());
		System.arraycopy(b_id, 0, ret, 0, 4);
		ret[4] = (byte)Util.int2Word(player.getInvitation())[1];

		return (ret);
	}
}
