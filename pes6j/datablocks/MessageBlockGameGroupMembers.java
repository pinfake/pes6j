package pes6j.datablocks;

import java.io.FileInputStream;
import java.util.Arrays;

public class MessageBlockGameGroupMembers extends MessageBlockMulti {

	static final int MAX_PLAYERS_PER_BLOCK = 6;
	
	byte[] pinfo = new byte[] {
			// --- ?? --- ---- CATEGORIA --
			/*
			 * Byte de invitacion, con 0x01 sale un "-" con 0x03 sale un "O"
			 */
			// -inv- -div-
			(byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
			(byte) 0x30 };

	public MessageBlockGameGroupMembers(PlayerInfo player,
			ChannelList channels, int qId) {
		super();
//		header.setQuery(qId);
//		header.setSeq(seq);

		/*
		 * Correccion, nada de 314, son 23
		 */

//		byte[] data = new byte[(4 + 48 + 112)
//				* player.getGroupInfo().getPlayers().length];
		byte[] tail = new byte[112];
		try {
			FileInputStream fi = new FileInputStream("5062tail.dec");
			fi.read(tail);
			fi.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//Arrays.fill(data, (byte) 0x00);

		int numBlocks = (player.getGroupInfo().getPlayers().length / MAX_PLAYERS_PER_BLOCK) + 1;
		
		for( int j = 0; j < numBlocks; j++ ) {
			int start = j*MAX_PLAYERS_PER_BLOCK;
			int end = j*MAX_PLAYERS_PER_BLOCK + MAX_PLAYERS_PER_BLOCK;
			if( j == (numBlocks - 1) ) end = player.getGroupInfo().getPlayers().length;
			byte[] data = new byte[(4 + 48 + 112) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				PlayerInfo p = player.getGroupInfo().getPlayers()[i];
				String plName = p.getName();
				long plId = p.getId();
				int chan_id = channels.getChannelForPlayer( plId );
				byte[] b_plid = Util.long2Word(plId);
	
				System.arraycopy(b_plid, 0, data, idx, b_plid.length);
				System.arraycopy(plName.getBytes(), 0, data, idx + 4, plName
						.getBytes().length);
				/*
				 * Esto vale 04 si soy yo, FF en caso contrario
				 * 
				 */
				System.arraycopy(pinfo, 0, data, idx + 4 + 48, pinfo.length);
				data[idx+4+48+1] = Util.int2Word(p.getDivision())[1];
				if ( chan_id != -1 ) {
					/*
					 * No se, bocachoti ten�a un 4
					 * Claro, esto dice en que salón está el tio
					 */
					ChannelInfo chan = channels.getChannel( chan_id );
					data[idx + 4 + 48 + 6] = (byte)chan_id;
					System.arraycopy(chan.getName().getBytes(), 0, data,
							idx + 4 + 48 + 7, chan.getName().getBytes().length);
					data[idx + 4 + 48 + 7 + 32] = (byte) 0x03; // OJO ERA UN 3!
					data[idx + 4 + 48 + 7 + 32 + 1] = chan.getType();
					data[idx + 4 + 48 + 7 + 32 + 3] = (byte) 0x00; // Numero de
																	// partidos de
																	// grupo jugados
				} else {
					data[idx + 4 + 48 + 6] = (byte) 0xFF;
				}
	
				/*
				 * Aqui hay algo que para boca vale 4, marba 4, evelito 8 y fedekan
				 * 0 -- son los partidos de grupo jugados!!
				 */
				// data[idx+4+48+7+32+3] = (byte)0x04;
				idx += (4 + 48 + 112);
			}
			MessageBlock mb = new MessageBlock();
			mb.header.query = qId;
			mb.setData(data);
			addBlock(mb);
		}
		//setData(qId, seq, data);
	}
}
