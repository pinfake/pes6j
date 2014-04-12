package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGamePlayerGroupList extends MessageBlockMulti {
	/*
	 * HEADER - PLAYERID - 37 bytes HEADER_2 - NOMBRE GRUPO (51 bytes)
	 */
	static final int MAX_PLAYERS_PER_BLOCK = 8;
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	static final byte[] PLY_HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };

	// completamente inventado
	static final byte[] ITEM_TAIL = { (byte) 0x00, (byte) 0x60, (byte) 0xDC,
			(byte) 0x2E, (byte) 0x34, (byte) 0xD7, (byte) 0x69, (byte) 0x64,
			(byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// static final byte[]

	// 4 (zeros) + 48 (nombre_player) + 4 (id-grupo) + 48 (nombre grupo) + 126
	// (traperadas)

	public MessageBlockGamePlayerGroupList(PlayerList plist,
			RoomList rlist, int qId) {
		super();
//		header.setQuery(qId);
//		header.setSeq(seq);

		PlayerInfo players[] = plist.getPlayerArray();
		
		//byte[] data = new byte[(4 + 48 + 4 + 48 + 23) * players.length];
		

		int numBlocks = (players.length / MAX_PLAYERS_PER_BLOCK) + 1;
		
		for( int j = 0; j < numBlocks; j++ ) {
			int start = j*MAX_PLAYERS_PER_BLOCK;
			int end = j*MAX_PLAYERS_PER_BLOCK + MAX_PLAYERS_PER_BLOCK;
			if( j == (numBlocks - 1) ) end = players.length;
			byte[] data = new byte[(4 + 48 + 4 + 48 + 23) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				String plName = players[i].getName();
				long plId = players[i].getId();
				String grName = players[i].getGroupInfo().getName();
				long grId = players[i].getGroupInfo().getId();
				byte[] b_grid = Util.long2Word(grId);
				byte[] b_plid = Util.long2Word(plId);
	
				System.arraycopy(b_plid, 0, data, idx, b_plid.length);
				idx += b_plid.length;
				System.arraycopy(plName.getBytes(), 0, data, idx,
						plName.getBytes().length);
				idx += 48;
				System.arraycopy(b_grid, 0, data, idx, b_grid.length);
				idx += b_grid.length;
				System.arraycopy(grName.getBytes(), 0, data, idx,
						grName.getBytes().length);
				idx += 48;
				/*
				 * Lo mismo de antes este byte indica si el tio pertenece a un
				 * grupo, el siguiente la divisi�n
				 */
				if (grId != 0)
					data[idx] = (byte) 0x01;
				
				data[idx+1] = Util.int2Word(players[i].getDivision())[1];
				/*
				 * System.arraycopy(ITEM_TAIL, 0, data, idx, ITEM_TAIL.length ); idx +=
				 * ITEM_TAIL.length;
				 */
				// data[idx] = (byte)0x01;
				byte[] b_rid = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
						(byte) 0x00 };
				RoomInfo r = rlist.searchPlayer(plId);
				if (r != null)
					b_rid = Util.long2Word(r.getId());
				System.arraycopy(b_rid, 0, data, idx + 2, b_rid.length);
				System.arraycopy(Util.int2Word(players[i].getCategory()), 0, data, idx + 10, 2 );
				
				//System.arraycopy(Util.int2Word(1), 0, data, idx + 12, 2 );
				System.arraycopy(Util.int2Word(players[i].getVictories()), 0, data, idx + 14, 2 );
				System.arraycopy(Util.int2Word(players[i].getDefeats()), 0, data, idx + 16, 2 );
				System.arraycopy(Util.int2Word(players[i].getDraws()), 0, data, idx + 18, 2 );
				idx += 23;
			}
			MessageBlock mb = new MessageBlock();
			mb.header.query = qId;
			mb.setData(data);
			addBlock(mb);
		}
		//setData( qId, seq, data);
	}
}
