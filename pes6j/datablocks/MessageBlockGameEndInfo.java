package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameEndInfo extends MessageBlock {

	RoomInfo room;
	/*
	 * Primero van los ids de los contendientes, luego los id de grupo de cada
	 * uno
	 */
	byte[] HEADER = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00 };
	
	// En una captura he visto un header que es ff ff fe 00

	// 20 zeros
//	byte[] TAIL = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00, (byte) 0x00 };

	byte[] TAIL = new byte[] { (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01 };
	
	public MessageBlockGameEndInfo(RoomInfo room, int qId) {
		super();
		this.room = room;
		header.setQuery(qId);

		byte[] data = new byte[HEADER.length + 22 * 4 + TAIL.length];
		Arrays.fill(data, (byte) 0x00);

		ChannelInfo channel = room.getChannel();
		
		for (int i = 0; i < room.getNumPlayersOnSel(); i++) {
			long pid = room.getPlayerIdOnSel(i);
			PlayerInfo p = channel.getPlayerList().getPlayerInfo(pid);
			byte b_pid[] = Util.long2Word(pid);
			System.arraycopy(b_pid, 0, data, (i * 22) + 4, b_pid.length);
//			int k = 0;
//			for( int j = (i * 22) + 8; j < (i * 22) + 8 + 18; j++ ) {
//				data[j] = (byte)k++; 
//			}
			
			System.arraycopy( Util.int2Word(p.getMatchPoints()), 0, data, (i*22) + 8, 2);
			System.arraycopy( Util.long2Word(p.getPoints()), 0, data, (i*22) + 10, 4 );
			
			System.arraycopy( Util.int2Word
					(p.getOldCategory()), 0, data, (i*22) + 8 + 16, 2 );
			System.arraycopy( Util.int2Word(p.getCategory()), 0, data, (i*22) + 8 + 14, 2 );
		}
//		int k = 0;
//		for( int i = HEADER.length + 22 * 4; i < data.length; i++ ) {
//			data[i] = (byte)k++;
//		}
		
		/*
		 * Los 20 bytes que quedan al final son los puntos del grupo, se le dan los puntos
		 * del partido al grupo para cada cual, primero va el id de un grupo luego los dos
		 * siguiente bytes son los puntos que se les ha dado en este partido y los 4 siguientes
		 * son los puntos totales del grupo.
		 * 
		 * De momento lo dejamos a cero porque no nos queremos meter en el follo de los
		 * grupos aún.
		 */
		
		setData(data);
	}
}
