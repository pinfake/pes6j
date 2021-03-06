package pes6j.datablocks;

import java.io.FileInputStream;
import java.util.Arrays;

public class MessageBlockGameGroupMembers extends MessageBlockMulti {

	static final int MAX_PLAYERS_PER_BLOCK = 6;
	
//	byte[] pinfo = new byte[] {
//			// --- ?? --- ---- CATEGORIA --
//			/*
//			 * Byte de invitacion, con 0x01 sale un "-" con 0x03 sale un "O"
//			 */
//			// -inv- -div-
//			(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
//			(byte) 0x30 
//	};

	public MessageBlockGameGroupMembers(PlayerInfo[] players,
			 int qId) {
		super();
//		header.setQuery(qId);
//		header.setSeq(seq);

		/*
		 * Correccion, nada de 314, son 23
		 */

//		byte[] data = new byte[(4 + 48 + 112)
//				* player.getGroupInfo().getPlayers().length];
//		byte[] tail = new byte[112];
//		try {
//			FileInputStream fi = new FileInputStream("5062tail.dec");
//			fi.read(tail);
//			fi.close();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}

		//Arrays.fill(data, (byte) 0x00);

		int numBlocks = (players.length / MAX_PLAYERS_PER_BLOCK) + 1;
		
		for( int j = 0; j < numBlocks; j++ ) {
			int start = j*MAX_PLAYERS_PER_BLOCK;
			int end = j*MAX_PLAYERS_PER_BLOCK + MAX_PLAYERS_PER_BLOCK;
			if( j == (numBlocks - 1) ) end = players.length;
			byte[] data = new byte[(4 + 48 + 112) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				PlayerInfo p = players[i];
				String plName = p.getName();
				long plId = p.getId();
				int chan_id = p.getLobbyId();
				byte[] b_plid = Util.long2Word(plId);
	
				System.arraycopy(b_plid, 0, data, idx, b_plid.length);
				System.arraycopy(plName.getBytes(), 0, data, idx + 4, plName
						.getBytes().length);
				/*
				 * Esto vale 04 si soy yo, FF en caso contrario
				 * 
				 */
				//System.arraycopy(pinfo, 0, data, idx + 4 + 48, pinfo.length);
				
				/*
				 * Si pongo un 0, no carga la lista bien
				 * Si pongo un 1, no puedo invitar
				 * Si pongo un 2, puedo invitar
				 * Si pongo un 3, puedo invitar y salgo el primero de la lista.
				 * Si pongo un 4, no puedo invitar y no cargo toda la lista bien.
				 * Si pongo un 5, no puedo invitar y no cargo toda la lista bien.
				 * Si pingo un 6, no puedo invitar y no cargo toda la lista bien.
				 */
//				if( g.getLeaderId() == p.getId() )
//					data[idx + 4+ 48] = 3;
//				else
					data[idx + 4+ 48] = Util.int2Word(p.getInvitation())[1];
				
				data[idx+4+48+1] = Util.int2Word(p.getDivision())[1];
				
				System.arraycopy(Util.int2Word(p.getCategory()), 0, data, idx + 4 + 48 + 2, 2); 
				System.arraycopy(Util.int2Word(p.getServerId()), 0, data, idx + 4 + 48 + 4, 2);
				if ( chan_id != 255 ) {
					/*
					 * No se, bocachoti ten�a un 4
					 * Claro, esto dice en que salón está el tio
					 */
//					ChannelInfo chan = channels.getChannel( chan_id );
					data[idx + 4 + 48 + 6] = (byte)chan_id;
					System.arraycopy(p.getLobbyName().getBytes(), 0, data,
							idx + 4 + 48 + 7, p.getLobbyName().getBytes().length);
					data[idx + 4 + 48 + 7 + 32] = (byte) 0x03; // OJO ERA UN 3!
					data[idx + 4 + 48 + 7 + 32 + 1] = Util.int2Word(p.getLobbyType())[1];
					System.arraycopy(Util.int2Word(p.getGroupMatches()), 0, data, idx+3+48+7+32+2, 2);
					
//					data[idx + 4 + 48 + 7 + 32 + 3] = ; // Numero de
//																	// partidos de
//																	// grupo jugados
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
