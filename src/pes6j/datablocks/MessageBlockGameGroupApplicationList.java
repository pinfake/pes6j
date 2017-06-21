package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameGroupApplicationList extends MessageBlockMulti {

	static final int MAX_PLAYERS_PER_BLOCK = 8;


	public MessageBlockGameGroupApplicationList(PlayerInfo[] players,
			 int qId) {
		super();		

		int numBlocks = (players.length / MAX_PLAYERS_PER_BLOCK) + 1;
		
		for( int j = 0; j < numBlocks; j++ ) {
			int start = j*MAX_PLAYERS_PER_BLOCK;
			int end = j*MAX_PLAYERS_PER_BLOCK + MAX_PLAYERS_PER_BLOCK;
			if( j == (numBlocks - 1) ) end = players.length;
			byte[] data = new byte[(4 + 4 + 48 + 4) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				String plName = players[i].getName();
				long plId = players[i].getId();
				byte[] b_plid = Util.long2Word(plId);
	
//				System.arraycopy(Util.long2Word(i), 0, data, idx, 4);
//				idx += 4;
				System.arraycopy(b_plid, 0, data, idx, b_plid.length);
				idx += b_plid.length;
				System.arraycopy(b_plid, 0, data, idx, b_plid.length);
				idx += b_plid.length;
				System.arraycopy(plName.getBytes(), 0, data, idx,
						plName.getBytes().length);
				idx += 48 + 4;
			}
			MessageBlock mb = new MessageBlock();
			mb.header.query = qId;
			mb.setData(data);
			addBlock(mb);
		}
	}
}
