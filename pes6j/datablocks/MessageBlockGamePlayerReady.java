package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGamePlayerReady extends MessageBlock {

	/*
	 * Primero van los ids de los contendientes, luego los id de grupo de cada
	 * uno
	 */
	byte[] DUNNO = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x1C,
			(byte) 0x07 };

	public MessageBlockGamePlayerReady(long pid, int ready, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[5];
		Arrays.fill(data, (byte) 0x00);
		byte[] b_pid = Util.long2Word(pid);

		System.arraycopy(b_pid, 0, data, 0, b_pid.length);
		data[4] = Util.int2Word( ready )[1];
		setData(data);
	}
}
