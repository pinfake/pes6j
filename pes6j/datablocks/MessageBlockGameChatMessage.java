package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameChatMessage extends MessageBlock {

	/*
	 * Se ha probado a cambiar los cuatro FF y no hay variacion ninguna
	 * en los mensajes, ni de color ni de nada.
	 */
	
	byte[] HEADER = new byte[] { (byte) 0x00, (byte) 0x01, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	public MessageBlockGameChatMessage(PlayerInfo player, byte place, byte channel, 
			String message, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[HEADER.length + 4 + 48 + message.getBytes().length + 1];
		Arrays.fill(data, (byte) 0x00);
		byte[] b_pid = Util.long2Word(player.getId());
		byte[] b_pname = player.getName().getBytes();
		byte[] b_mes = message.getBytes();
		int idx = 0;
		System.arraycopy(HEADER, 0, data, idx, HEADER.length);
		data[0] = place;
		data[1] = channel;
		
		//System.arraycopy(b_channel, 0, data, 0, 2);
		idx += HEADER.length;
		System.arraycopy(b_pid, 0, data, HEADER.length, b_pid.length);
		idx += 4;
		System.arraycopy(b_pname, 0, data, idx, b_pname.length);
		idx += 48;
		System.arraycopy(b_mes, 0, data, idx, b_mes.length);
		data[data.length - 1] = (byte) 0x00;

		setData(data);
	}
}
