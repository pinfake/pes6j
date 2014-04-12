package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameRoomList extends MessageBlockMulti {

	static final int MAX_ROOMS_PER_BLOCK = 6;
	// completamente copiado
	static final byte[] ITEM_TAIL = {
			// -------------------------------------- ALL COVERED
			// -------------------------------------------------------------------
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF,
			//     --- TEAM 2 ----           g1            g2           g3           g4           g5           ---- TEAM 1 ---              g1
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			//    g2           g3            g4            g5                      PWD              -------- FOUR BYTES ------------
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public MessageBlockGameRoomList(RoomInfo rooms[], int qId) {
		super();
//		header.setQuery(qId);
//		header.setSeq(seq);

		//byte[] data = new byte[(4 + 2 + 65 + 60) * rooms.length];
		//Arrays.fill(data, (byte) 0x00);

		int numBlocks = (rooms.length / MAX_ROOMS_PER_BLOCK) + 1;
		
		for( int k = 0; k < numBlocks; k++ ) {
			int start = k*MAX_ROOMS_PER_BLOCK;
			int end = k*MAX_ROOMS_PER_BLOCK + MAX_ROOMS_PER_BLOCK;
			if( k == (numBlocks - 1) ) end = rooms.length;
			
			byte[] data = new byte[(4 + 2 + 65 + 60) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				String rName = rooms[i].getName();
				long rId = rooms[i].getId();
				int rType = rooms[i].getType();
	
				if (rName.length() > 64) {
					rName = rName.substring(0, 64);
				}
	
				byte[] b_rid = Util.long2Word(rId);
				byte[] b_rtype = Util.int2Word(rType);
				byte[] b_half = Util.int2Word(rooms[i].getHalf());
				// if( rooms[i].getGamePhase() == 4 ) b_rtype =
				// Util.int2Word(0x00000703);
	
				byte[] b_rname = rName.getBytes();
	
				System.arraycopy(b_rid, 0, data, idx, b_rid.length);
				idx += b_rid.length;
	
				data[idx++] = b_rtype[1];
				data[idx++] = b_half[1];
	
				// System.arraycopy(b_rtype, 0, data, idx, b_rtype.length);
				// idx += b_rtype.length;
	
				System.arraycopy(rName.getBytes(), 0, data, idx,
						rName.getBytes().length);
				idx += 64;
	
				data[idx++] = Util.int2Word(rooms[i].getTime())[1];
	
				System.arraycopy(ITEM_TAIL, 0, data, idx, ITEM_TAIL.length);
	
				for (int j = 0; j < rooms[i].getNumPlayers(); j++) {
					long pId = rooms[i].getPlayerId(j);
					byte[] b_pid = Util.long2Word(pId);
					System.arraycopy(b_pid, 0, data, idx + (10 * j), b_pid.length);
	
					// Esto determina quien es el due�o del salon comentado por
					// efectos extra�os
//					if (j == 0)
//						data[idx + (10 * j) + 4] = (byte) 0x01;
					data[idx + (10 * j) + 6] = Util.int2Word(rooms[i]
							.getPlayerTeam(pId))[1];
					data[idx + (10 * j) + 7] = Util
							.int2Word(rooms[i].getSpect(pId))[1];
					data[idx + (10 * j) + 8] = Util.int2Word(j)[1];
					data[idx + (10 * j) + 9] = Util.int2Word(rooms[i]
							.getPlayerSel(j))[1];
				}
				idx += 40;
				byte[] b_team2 = Util.int2Word(rooms[i].getTeam2());
				System.arraycopy(b_team2, 0, data, idx, b_team2.length);
	
				idx += 2;
				for (int j = 0; j < 5; j++) {
					data[idx++] = Util.int2Word(rooms[i].getTeam2Goals(j))[1];
				}
	
				byte[] b_team1 = Util.int2Word(rooms[i].getTeam1());
				System.arraycopy(b_team1, 0, data, idx, b_team1.length);
				idx += 2;
				for (int j = 0; j < 5; j++) {
					data[idx++] = Util.int2Word(rooms[i].getTeam1Goals(j))[1];
				}
	
				idx += 6;
	
				byte[] b_last = Util.long2Word(rooms[i].getLastBytes());
				System.arraycopy(b_last, 0, data, idx - 4, b_last.length);
	
				if (rooms[i].hasPassword())
					data[idx - 5] = (byte) 0x01;
				else
					data[idx - 5] = (byte) 0x00;
			}
			
			/*
			 * We don't want to add an empty 4302 block, so we check if there
			 * are rooms in this block beforehand.
			 */
			if( data.length > 0 ) {
				MessageBlock mb = new MessageBlock();
				mb.header.query = qId;
				mb.setData(data);
				addBlock(mb);
			}
		}
		//setData(qId, seq, data);
	}
}
