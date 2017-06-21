package pes6j.datablocks;

public class MessageBlockGameIpInfoShort extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x38, (byte) 0x37, (byte) 0x2E, (byte) 0x32,
			(byte) 0x32, (byte) 0x30, (byte) 0x2E, (byte) 0x31, (byte) 0x33,
			(byte) 0x30, (byte) 0x2E, (byte) 0x33, (byte) 0x32, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x16, (byte) 0x6B, (byte) 0x31,
			(byte) 0x39, (byte) 0x32, (byte) 0x2E, (byte) 0x31, (byte) 0x36,
			(byte) 0x38, (byte) 0x2E, (byte) 0x31, (byte) 0x2E, (byte) 0x31,
			(byte) 0x30, (byte) 0x00, (byte) 0xB2, (byte) 0xAB, (byte) 0x03,
			(byte) 0x16, (byte) 0x6B, (byte) 0x00, (byte) 0x0C, (byte) 0xE1,
			(byte) 0xBA, };

	public MessageBlockGameIpInfoShort(IpInfo ip, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = MSG_DATA;

		// pos 4 es la ip 1 con un 0 al final
		// pos 22 ip2 con un 0 al final
		// pos 40 es el id

		String ip1 = ip.getIp1();
		String ip2 = ip.getIp2();
		if (ip1.length() > 15)
			ip1 = ip1.substring(0, 15);
		if (ip2.length() > 15)
			ip2 = ip2.substring(0, 15);
		byte[] b_ip1 = ip1.getBytes();
		byte[] b_port1 = Util.int2Word(ip.getPort1());
		byte[] b_ip2 = ip2.getBytes();
		byte[] b_port2 = Util.int2Word(ip.getPort2());
		byte[] b_pid = Util.long2Word(ip.getPlayerId());

		System.arraycopy(b_ip1, 0, data, 4, b_ip1.length);
		data[4 + b_ip1.length] = (byte) 0x00;
		System.arraycopy(b_port1, 0, data, 4 + 16, b_port1.length);
		System.arraycopy(b_ip2, 0, data, 22, b_ip2.length);
		data[22 + b_ip2.length] = (byte) 0x00;
		System.arraycopy(b_port2, 0, data, 22 + 16, b_port2.length);
		System.arraycopy(b_pid, 0, data, 40, b_pid.length);

		setData(data);
	}
}
