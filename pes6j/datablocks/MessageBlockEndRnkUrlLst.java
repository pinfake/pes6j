package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockEndRnkUrlLst extends MessageBlock {
	public MessageBlockEndRnkUrlLst() {
		super();
		header.setQuery(QUERIES.END_RNKURLLST_SERVER_QUERY);
		byte[] data = new byte[4];
		Arrays.fill(data, 0, 4, (byte) 0x00);
		setData(data);
	}
}
