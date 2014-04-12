package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockServers extends MessageBlock {
	static final byte[] SRV_HEADER = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
	// El 0x0A se refiere al número de gente que hay ocupando el servidor, lo
	// siguiente es el identificador de canal
	// Ya que un mismo server parece ser que puede tener mas canales
//	static final byte[] SRV_TAIL = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
//			(byte) 0x00 };
	static final byte[] SRV_TAIL = { (byte) 0x00, (byte) 0x00 };

	public MessageBlockServers(ServerInfo servers[], int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[61 * servers.length];
		for (int i = 0; i < servers.length; i++) {
			ServerInfo si = servers[i];
			String srvName = si.getName();
			String srvIp = si.getIp();
			int srvType = si.getType();

			if (srvName.length() > 32)
				srvName = srvName.substring(0, 32);
			if (srvIp.length() > 15)
				srvIp = srvIp.substring(0, 15);

			byte[] b_header = buildTypeHeader(srvType);
			byte[] b_name = new byte[32];
			byte[] b_numPlayers = Util.int2Word(si.getNumPlayers());
			Arrays.fill(b_name, (byte) 0x00);
			System.arraycopy(srvName.getBytes(), 0, b_name, 0, srvName
					.getBytes().length);
			byte[] b_ip = new byte[15];
			Arrays.fill(b_ip, (byte) 0x00);
			System.arraycopy(srvIp.getBytes(), 0, b_ip, 0,
					srvIp.getBytes().length);

			byte[] b_port = Util.int2Word(si.getPort());
			
			int idx = 61 * i;
			System.arraycopy(b_header, 0, data, idx, b_header.length);
			idx += b_header.length;
			System.arraycopy(b_name, 0, data, idx, b_name.length);
			idx += b_name.length;
			System.arraycopy(b_ip, 0, data, idx, b_ip.length);
			idx += b_ip.length;
			System.arraycopy(b_port, 0, data, idx, b_port.length);
			idx += b_port.length;
			System.arraycopy(b_numPlayers, 0, data, idx, b_numPlayers.length);
			idx += b_numPlayers.length;
			System.arraycopy(SRV_TAIL, 0, data, idx, SRV_TAIL.length);
		}

		setData(data);
	}

	byte[] buildTypeHeader(int type ) {
		byte[] ret = SRV_HEADER;
		byte[] b_type = Util.int2Word(type);
		ret[ret.length - 1] = b_type[1];
		return (ret);
	}
}
