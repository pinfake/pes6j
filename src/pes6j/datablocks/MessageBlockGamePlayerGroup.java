package pes6j.datablocks;

import java.io.FileInputStream;
import java.util.Arrays;

public class MessageBlockGamePlayerGroup extends MessageBlock {
	/*
	 * HEADER - PLAYERID - 37 bytes HEADER_2 - NOMBRE GRUPO (51 bytes)
	 */
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	static final byte[] PLY_HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };
	static final byte[] REPEAT = { (byte) 0x00, (byte) 0x60, (byte) 0xDC,
			(byte) 0x2E, (byte) 0x34, (byte) 0xD7, (byte) 0x69, (byte) 0x64,
			(byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// static final byte[]

	// 4 (zeros) + 48 (nombre_player) + 4 (id-grupo) + 48 (nombre grupo) + 314
	// (traperadas)
	// Es crear_r09

	public MessageBlockGamePlayerGroup(PlayerInfo player,
			int qId) {
		super();
		header.setQuery(qId);

		/*
		 * Correccion, nada de 314, son 23
		 */

		byte[] data = new byte[4 + 4 + 48 + 4 + 48 + 314];
//		byte[] tail = new byte[314];
		Arrays.fill(data, (byte) 0x00);

		String plName = player.getName();
		String grName = player.getGroupName();
		String comment = player.getComment();
		long grId = player.getGid();
		long plId = player.getId();
		byte[] b_plid = Util.long2Word(plId);
		byte[] b_grid = Util.long2Word(grId);

		System.arraycopy(b_plid, 0, data, 4, b_plid.length);
		System.arraycopy(plName.getBytes(), 0, data, 4 + 4,
				plName.getBytes().length);
		System.arraycopy(b_grid, 0, data, 4 + 4 + 48, b_grid.length);
		System.arraycopy(grName.getBytes(), 0, data, 4 + 4 + 48 + 4, grName
				.getBytes().length);
		// System.arraycopy(tail, 0, data, 4+4+48+4+48, 2 );
		/*
		 * primer byte indica si el tio pertenece a un grupo, segundo la
		 * divisi�n un 01 funciona como pertenecer al grupo un 02 permite
		 * invitar a gente al grupo, no permite cambiar el comentario del grupo
		 * un 03 peta / no carga al principio un 04 Estoy solicitando
		 * incorporacion al grupo un 05 peta al darle a "mi grupo" un 06 no
		 * salgo y peta al darle a "mi grupo"
		 */
		if (grId > 0) {
			/*
			 * Si soy el lider, aqui es un 0x03;
			 */
//			if( plId == group.getLeaderId() )
//				data[4 + 4 + 48 + 4 + 48] = (byte) 0x03;
//			else
				data[4 + 4 + 48 + 4 + 48] = Util.int2Word(player.getInvitation())[1];
		}
		// System.arraycopy(new byte[] { (byte)0x01, (byte)0x00 }, 0, data,
		// 4+4+48+4+48, 2 );
		
		/*
		 * Voy a cambiar los 30 bytes que me faltan hasta el comment
		 */
		
		/*
		 * Known score locations
		 * 
		 * Starting point = 4 + 4 + 48 + 4 + 48 = 108
		 */
		
		data[109] = Util.int2Word( player.getDivision())[1];
		
		
		System.arraycopy( Util.long2Word( player.getPoints() ), 0, data, 108 + 2, 4 );
		
		// Categoría
		System.arraycopy( Util.int2Word( player.getCategory() ), 0, data, 108 + 6, 2 );

		System.arraycopy( Util.int2Word( player.getMatchesPlayed() ), 0, data, 108 + 8, 2 );

		System.arraycopy( Util.int2Word( player.getVictories()), 0, data, 108 + 10, 2 );

		System.arraycopy( Util.int2Word( player.getDefeats()), 0, data, 108 + 12, 2 );

		System.arraycopy( Util.int2Word( player.getDraws()), 0, data, 108 + 14, 2 );

		System.arraycopy( Util.int2Word( player.getWinningStreak()), 0, data, 108 + 16, 2 );

		System.arraycopy( Util.int2Word( player.getBestStreak()), 0, data, 108 + 18, 2 );

		System.arraycopy( Util.int2Word( player.getDecos() ), 0, data, 108 + 20, 2 );
		
		System.arraycopy( Util.long2Word( player.getGoalsScored() ), 0, data, 108 + 22, 4 );
		
		System.arraycopy( Util.long2Word( player.getGoalsReceived() ), 0, data, 108 + 26, 4 );
		
		
		System.arraycopy(comment.getBytes(), 0, data, 4 + 4 + 48 + 4 + 48 + 30,
				comment.getBytes().length);
		
		/*
		 * Position field
		 */
		System.arraycopy( Util.long2Word( player.getPosition()), 0, data, 394, 4);
		
		/*
		 * Uno se los dos son medallas plata de competición.
		 */
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 398, 2);
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 400, 2);
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 402, 2);
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 404, 2);
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 406, 2);
//		System.arraycopy( Util.int2Word(0x3FFF),0, data, 408, 2);
		
		/*
		 * Lets see if this is the language:
		 */
		
		// ESTE NO ES
		//System.arraycopy( Util.int2Word(player.getLang()), 0, data, 418, 2);
		// ESTE NO ES
		//System.arraycopy( Util.int2Word(player.getLang()), 0, data, 408, 2);
		
		System.arraycopy( Util.int2Word(player.getLang()), 0, data, 410, 2);
		/*
		System.arraycopy( Util.int2Word(player.getLang()), 0, data, 412, 2);
		System.arraycopy( Util.int2Word(player.getLang()), 0, data, 414, 2);
		System.arraycopy( Util.int2Word(player.getLang()), 0, data, 416, 2);
		*/
		
		System.arraycopy( Util.int2Word( player.getTeam1()), 0, data, 420, 2 );
		System.arraycopy( Util.int2Word( player.getTeam2()), 0, data, 418, 2 );
		System.arraycopy( Util.int2Word( player.getTeam3()), 0, data, 416, 2 );
		System.arraycopy( Util.int2Word( player.getTeam4()), 0, data, 414, 2 );
		System.arraycopy( Util.int2Word( player.getTeam5()), 0, data, 412, 2 );
		
		setData(data);
	}
}
