package pes6j.datablocks;

public class MessageBlockGameIpInfo extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x80,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x38,
			(byte) 0x37, (byte) 0x2E, (byte) 0x32, (byte) 0x32, (byte) 0x30,
			(byte) 0x2E, (byte) 0x31, (byte) 0x33, (byte) 0x30, (byte) 0x2E,
			(byte) 0x33, (byte) 0x32, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x16, (byte) 0x6B, (byte) 0x31, (byte) 0x39, (byte) 0x32,
			(byte) 0x2E, (byte) 0x31, (byte) 0x36, (byte) 0x38, (byte) 0x2E,
			(byte) 0x31, (byte) 0x2E, (byte) 0x31, (byte) 0x30, (byte) 0x00,
			(byte) 0xB2, (byte) 0xAB, (byte) 0x03, (byte) 0x16, (byte) 0x6B,
			(byte) 0x00, (byte) 0x0C, (byte) 0xE1, (byte) 0xBA, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF };

	public MessageBlockGameIpInfo(RoomInfo r, IpInfo ip[], int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[MSG_DATA.length * ip.length];

		// pos 32 es la ip 1 con un 0 al final
		// pos 50 ip2 con un 0 al final
		// pos 124 es el id

		// Arrays.fill( data, (byte)0x00 );
		for (int i = 0; i < ip.length; i++) {
			int idx = i * MSG_DATA.length;
			System.arraycopy(MSG_DATA, 0, data, idx, MSG_DATA.length);
			String ip1 = ip[i].getIp1();
			String ip2 = ip[i].getIp2();
			long pId = ip[i].getPlayerId();
			if (ip1.length() > 15)
				ip1 = ip1.substring(0, 15);
			if (ip2.length() > 15)
				ip2 = ip2.substring(0, 15);
			byte[] b_ip1 = ip1.getBytes();
			byte[] b_port1 = Util.int2Word(ip[i].getPort1());
			byte[] b_ip2 = ip2.getBytes();
			byte[] b_port2 = Util.int2Word(ip[i].getPort2());
			byte[] b_pid = Util.long2Word(pId);

//			System.out.println("pid vale: " + pId + " y el ultimo byte vale "
//					+ b_pid[3]);

			System.arraycopy(b_ip1, 0, data, idx + 32, b_ip1.length);
			data[idx + 32 + b_ip1.length] = (byte) 0x00;

			// Novedades!
			System.arraycopy(b_port1, 0, data, idx + 32 + 16, b_port1.length);

			System.arraycopy(b_ip2, 0, data, idx + 50, b_ip2.length);
			data[idx + 50 + b_ip2.length] = (byte) 0x00;

			// Novedades!
			System.arraycopy(b_port2, 0, data, idx + 50 + 16, b_port2.length);

			System.arraycopy(b_pid, 0, data, idx + 68, b_pid.length);
			data[idx + 68 + 4] = Util.int2Word(i)[1];
			data[idx + 68 + 5] = Util.int2Word(i)[1];
			data[idx + 68 + 6] = Util.int2Word(r.getPlayerSel(i))[1];
		}
		setData(data);
	}
}
