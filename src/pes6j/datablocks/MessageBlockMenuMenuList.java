package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockMenuMenuList extends MessageBlock {

	public MessageBlockMenuMenuList(ChannelList channels, PlayerInfo player, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[2 + ((1 + 32 + 2) * channels.size())];
		Arrays.fill(data, (byte) 0x00);
		byte b_header[] = Util.int2Word(channels.size());
		System.arraycopy(b_header, 0, data, 0, 2);
		int idx = 2;
		for (int i = 0; i < channels.size(); i++) {
			String chName = channels.getChannel(i).getName();
			byte chType = channels.getChannel(i).getType();
			int numPlayers = channels.getChannel(i).getPlayerList().getNumPlayers();

			/*
			 * Useless more than 100 player is not supported by the client
			 */
//			if( numPlayers >= 100 && player.getAdmin() == 1 )
//				numPlayers = 99;
			
			data[idx] = chType;
			idx++;
			if (chName.length() > 32) {
				chName = chName.substring(0, 32);
			}
			byte[] b_chname = chName.getBytes();
			byte[] b_numplayers = Util.int2Word(numPlayers);

			System.arraycopy(b_chname, 0, data, idx, b_chname.length);
			idx += 32;
			System.arraycopy(b_numplayers, 0, data, idx, b_numplayers.length);
			idx += b_numplayers.length;
		}
		setData(data);
	}
}
