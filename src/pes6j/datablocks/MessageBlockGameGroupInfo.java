package pes6j.datablocks;

import java.io.FileInputStream;
import java.util.Arrays;

public class MessageBlockGameGroupInfo extends MessageBlock {

	/*
	 * Hay que determinar que es cada cosa...
	 */
	byte[] groupData = new byte[] {
			// ----------- Posicion ------------ ----------------- Puntos
			// ------------
			(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x5E, (byte) 0x01,
			(byte) 0x02, (byte) 0xFF, (byte) 0xE6,
			// --- Partidos --- --- Victorias --- --- Derrotas --- --- Empates
			// ---
			(byte) 0x01, (byte) 0x7A, (byte) 0x00, (byte) 0x82, (byte) 0x00,
			(byte) 0xC7, (byte) 0x00, (byte) 0x31 };

	public MessageBlockGameGroupInfo(GroupInfo group, int qId) {
		
		/*
		 * NECESITO SABER DONDE SE ESPECIFICA SI EL GRUPO ESTA O NO CONTRATANDO Y SU COMENTARIO SI
		 * LO ESTA!
		 */
		
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 4 + 48 + 16 + 4 + 48 + 519];
//		byte[] tail = new byte[519];
//		try {
//			FileInputStream fi = new FileInputStream("5025tail.dec");
//			fi.read(tail);
//			fi.close();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}

		Arrays.fill(data, (byte) 0x00);

		String plName = group.getLeaderName();
		String grName = group.getName();
		// String comment = leader.getComment();
		long grId = group.getId();
		long plId = group.getLeaderId();
		byte[] b_plid = Util.long2Word(plId);
		byte[] b_grid = Util.long2Word(grId);
		System.arraycopy(b_grid, 0, data, 4, b_grid.length);
		System.arraycopy(grName.getBytes(), 0, data, 4 + 4,
				grName.getBytes().length);
		byte[] b_position = Util.long2Word(group.getPosition());
		byte[] b_points = Util.long2Word(group.getPoints());
		byte[] b_matches = Util.int2Word(group.getMatches());
		byte[] b_victories = Util.int2Word(group.getVictories());
		byte[] b_defeats = Util.int2Word(group.getDefeats());
		byte[] b_draws = Util.int2Word(group.getDraws());
		byte[] b_level = Util.long2Word(group.getLevel());
		System.arraycopy(b_position, 0, data, 4 + 4 + 48, b_position.length);
		System.arraycopy(b_points, 0, data, 4 + 4 + 48 + 4, b_points.length);
		System.arraycopy(b_matches, 0, data, 4 + 4 + 48 + 4 + 4,
				b_matches.length);
		System.arraycopy(b_victories, 0, data, 4 + 4 + 48 + 4 + 4 + 2,
				b_victories.length);
		System.arraycopy(b_defeats, 0, data, 4 + 4 + 48 + 4 + 4 + 2 + 2,
				b_defeats.length);
		System.arraycopy(b_draws, 0, data, 4 + 4 + 48 + 4 + 4 + 2 + 2 + 2,
				b_draws.length);
		// System.arraycopy(groupData, 0, data, 4+4+48, groupData.length);
		System.arraycopy(b_plid, 0, data, 4 + 4 + 48 + 16, b_plid.length);
		System.arraycopy(plName.getBytes(), 0, data, 4 + 4 + 64 + 4, plName
				.getBytes().length);
		//System.arraycopy(tail, 0, data, 4 + 4 + 48 + 16 + 4 + 48, tail.length);
		
		data[4 + 4 + 48 + 16 + 4 + 48 + 2 + 4] = Util.int2Word(group.getRecruiting())[1];
		System.arraycopy(group.getRecruitComment().getBytes(), 0, data, 4 + 4 + 48 + 16 + 4 + 48 + 2 + 4 + 1, group.getRecruitComment().getBytes().length);	
		
//		Arrays.fill(data, 4 + 4 + 48 + 16 + 4 + 48 + 2 + 4, 
//				4 + 4 + 48 + 16 + 4 + 48 + 2 + 4 + 1, (byte)0x01);
		
		if (!group.getComment().equals(""))
			System.arraycopy(group.getComment().getBytes(), 0, data, 4 + 4 + 48
					+ 16 + 4 + 48 + 263, group.getComment().getBytes().length);

		//Arrays.fill(data, 4 + 4 + 48 + 16 + 4 + 48 + 263 + 48, data.length, (byte)0x01);
		
		data[4 + 4 + 48 + 16 + 4 + 48] = Util.int2Word(group.getNumPlayers())[1];
		data[4 + 4 + 48 + 16 + 4 + 48 + 1] = Util.int2Word(group.getSlots())[1];
		/*
		 * Estos dos de abajo si son 3F y 37 dicen que el grupo es nivel 11
		 */

		System.arraycopy(b_level, 0, data, 4 + 4 + 48 + 16 + 4 + 48 + 2,
				b_level.length);
		
		// data[4+4+48+16+4+48+4] = (byte)0x00;
		// data[4+4+48+16+4+48+5] = (byte)0x00;

		// System.arraycopy(comment.getBytes(), 0, data, 4+4+48+4+48+30,
		// comment.getBytes().length);

		// data[4+4+48+4+48] = (byte)0x01;
		setData(data);
	}
}
