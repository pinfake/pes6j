package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockLoginPlayers extends MessageBlock {
	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	static final byte[] PLY_HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };
	static final byte[] REPEAT = { (byte) 0x00, (byte) 0x60, (byte) 0xDC,
			(byte) 0x2E, (byte) 0x34, (byte) 0xD7, (byte) 0x69, (byte) 0x64,
			(byte) 0x0B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// static final byte[]

	public MessageBlockLoginPlayers(PlayerInfo players[], int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 66 * players.length];
		System.arraycopy(HEADER, 0, data, 0, HEADER.length);
		int idx = HEADER.length;
		for (int i = 0; i < players.length; i++) {
			PlayerInfo pi = players[i];
			String plName = pi.getName();
			long plPoints = pi.getPoints();
			int plCat = pi.getCategory();
			int plMatches = pi.getMatchesPlayed();
			long id = pi.getId();

			if (plName.length() > 32)
				plName = plName.substring(0, 32);

			byte[] b_header = buildPlyHeader(i, id);
			byte[] b_name = new byte[32];
			Arrays.fill(b_name, (byte) 0x00);
			System.arraycopy(plName.getBytes(), 0, b_name, 0,
					plName.getBytes().length);

			byte[] b_repeat = REPEAT;

			byte[] b_rest = new byte[13];
			Arrays.fill(b_rest, (byte) 0x00);
			
//			for( int j = 0; j < b_rest.length; j++ ) {
//				b_rest[j] = (byte)j;
//			}
			
			/*
			 * Time Played
			 */
			
			System.arraycopy( Util.long2Word( pi.getTimePlayed()), 0, b_rest, 0, 4);

			// EL BYTE 4 ES MUY POSIBLE QUE SEA LA DIVISION!!!!
			
			b_rest[4] = Util.int2Word( pi.getDivision() )[1];
			
			/*
			 * Puntos
			 */
			System.arraycopy( Util.long2Word( pi.getPoints()), 0, b_rest, 5, 4 );
			
			/*
			 * Categoria
			 */
			System.arraycopy( Util.int2Word( pi.getCategory()), 0, b_rest, 9, 2 );
			
			/*
			 * Partidos
			 */
			System.arraycopy( Util.int2Word( pi.getMatchesPlayed()), 0, b_rest, 11, 2 );
			
			System.arraycopy(b_header, 0, data, idx, b_header.length);
			idx += b_header.length;
			System.arraycopy(b_name, 0, data, idx, b_name.length);
			idx += b_name.length;
			System.arraycopy(b_repeat, 0, data, idx, b_repeat.length);
			idx += b_repeat.length;
			System.arraycopy(b_rest, 0, data, idx, b_rest.length);
			idx += b_rest.length;
		}

		setData(data);
	}

	byte[] buildPlyHeader(int order, long id) {
		byte[] ret = PLY_HEADER;
		byte[] b_order = Util.int2Word(order);
		byte[] b_id = Util.long2Word(id);
		ret[0] = b_order[1];
		System.arraycopy(b_id, 0, ret, 1, 4);

		return (ret);
	}
}
