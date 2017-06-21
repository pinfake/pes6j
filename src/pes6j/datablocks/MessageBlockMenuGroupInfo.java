package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockMenuGroupInfo extends MessageBlock {

	static final byte[] HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	/*
	 * Ojo modifico esto, donde hay un 1 voy a poner un 3FFF es un nivel total
	 * del grupo, no se si servirá de algo.
	 */
	static final byte[] HEADER_2 = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x80,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00, (byte) 0x00, (byte) 0x3F, (byte) 0xFF, (byte) 0x00,		
													// Que coño será este uno?
//			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00,

											 // El ultimo byte indica slots por
											 // Habitación para mi SI!
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04 };

	public MessageBlockMenuGroupInfo(PlayerInfo p, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[4 + 4 + 37 + 51];
		Arrays.fill(data, (byte) 0x00);

		String grName = p.getGroupName();

		if (grName.length() > 48) {
			grName = grName.substring(0, 48);
		}

		byte[] b_grname = grName.getBytes();
		byte[] b_playerid = Util.long2Word(p.getId());
		int idx = 0;
		System.arraycopy(HEADER, 0, data, idx, HEADER.length);
		idx += HEADER.length;
		System.arraycopy(b_playerid, 0, data, idx, b_playerid.length);
		idx += b_playerid.length;
		System.arraycopy(HEADER_2, 0, data, idx, HEADER_2.length);
		idx += HEADER_2.length;
		/*
		 * Salas de 8, pero no van igual!
		 */
	//		if( p.isAdmin() ) {
//			data[idx-1] = 0x08;
//		}
		System.arraycopy(b_grname, 0, data, idx, b_grname.length);

		/*
		 * Probamos a poner los dos ultimos bytes de los cuales no tengo información
		 * a FF FF
		 */

		data[data.length-2] = (byte)0xFF;
		data[data.length-1] = (byte)0xFF;
		
		setData(data);
	}
}
