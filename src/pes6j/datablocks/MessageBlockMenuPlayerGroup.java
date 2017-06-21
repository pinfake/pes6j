package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockMenuPlayerGroup extends MessageBlock {

	// static final byte[]

	// 4 (zeros) + 48 (nombre_player) + 4 (id-grupo) + 48 (nombre grupo) + 126
	// (traperadas)

	/*
	 * ESTO SON LAS VICTORIAS EMPATES Y TODO ESE TIPO DE COSAS QUE SE ASOCIAN A
	 * UN JUGAGOR Y QUE SALEN EN UNA SALA DE JUEGO
	 */

	static final byte[] TAIL = new byte[] { 
//		              -- DIV --
		(byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
//			-- points??? --					-- CAT ---		  -- MATCHES PLYD --		 -- VICTORIES ---
		(byte) 0x06, (byte) 0xAA, (byte) 0x01, (byte) 0xF6, (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x07, 
//		    -- DEFEATS --               ---- DRAWS ---       
		(byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x00 };

	public MessageBlockMenuPlayerGroup(PlayerInfo player,
			long rid, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 48 + 4 + 48 + 23];
		Arrays.fill(data, (byte) 0x00);

		String plName = player.getName();
		String grName = player.getGroupName();
		long plId = player.getId();
		long grId = player.getGid();
		byte[] b_plid = Util.long2Word(plId);
		byte[] b_grid = Util.long2Word(grId);
		System.arraycopy(b_plid, 0, data, 0, b_plid.length);
		System.arraycopy(plName.getBytes(), 0, data, 4,
				plName.getBytes().length);
		System.arraycopy(b_grid, 0, data, 4 + 48, b_grid.length);
		System.arraycopy(grName.getBytes(), 0, data, 4 + 48 + 4, grName
				.getBytes().length);

		data[104] = (byte) 0x01;
		
		/*
		 * primer byte indica si el tio pertenece a un grupo, segundo la
		 * divisi�n un 01 funciona como pertenecer al grupo un 02 permite
		 * invitar a gente al grupo, no permite cambiar el comentario del grupo
		 * un 03 peta / no carga al principio un 04 Estoy solicitando
		 * incorporacion al grupo un 05 peta al darle a "mi grupo" un 06 no
		 * salgo y peta al darle a "mi grupo"
		 */
//		if (grId > 0)
//			data[4 + 48 + 4 + 48] = (byte) 0x02;
		
		data[4+48+4+48] = Util.int2Word(player.getInvitation())[1];
		
		data[105] = Util.int2Word( player.getDivision())[1];
		
		System.arraycopy( Util.long2Word( player.getPoints() ), 0, data, 110, 4 );
		System.arraycopy( Util.int2Word( player.getCategory() ), 0, data, 108 + 6, 2 );

		System.arraycopy( Util.int2Word( player.getMatchesPlayed() ), 0, data, 108 + 8, 2 );

		System.arraycopy( Util.int2Word( player.getVictories()), 0, data, 108 + 10, 2 );

		System.arraycopy( Util.int2Word( player.getDefeats()), 0, data, 108 + 12, 2 );

		System.arraycopy( Util.int2Word( player.getDraws()), 0, data, 108 + 14, 2 );
		// System.arraycopy(TAIL, 0, data, 4+48+4+48, TAIL.length);
		
		System.arraycopy( Util.int2Word( player.getLang()),1, data, 108 + 16 + 1, 1);

		/*
		 * GUESSING: De lo que me queda en pos + 2 va el room_id
		 */
		
		/*
		 * Aqui hay un hueco, lo del room_id no me lo creo ni yo. Voy a rellenarlo
		 * con FF's ULTIMA PRUEBA SI NO ES ESTO ME PEGO DOS TIROS
		 */
		
		System.arraycopy(Util.long2Word(0xFFFFFFFF), 0, data, 4+48+4+48+2, 4);
		
//		byte[] b_rid = Util.long2Word(rid);
//		System.arraycopy(b_rid, 0, data, 4 + 48 + 4 + 48 + 2, b_rid.length);
		setData(data);
	}
}
