package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockRnkUrlLst extends MessageBlock {
	static final int URL_STR_SIZE = 128;

	public MessageBlockRnkUrlLst(RankUrlInfo[] urls, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = new byte[(URL_STR_SIZE + 2) * urls.length];
		for (int i = 0; i < urls.length; i++) {
			RankUrlInfo ri = urls[i];

			String rankUrl = ri.getName();
			int rankType = ri.getType();

			if (rankUrl.length() > URL_STR_SIZE)
				rankUrl = rankUrl.substring(0, 32);

			byte[] b_type = Util.int2Word(rankType);
			byte[] b_name = new byte[URL_STR_SIZE];
			Arrays.fill(b_name, (byte) 0x00);
			System
					.arraycopy(rankUrl.getBytes(), 0, b_name, 0, rankUrl
							.length());

			int idx = (URL_STR_SIZE + 2) * i;
			System.arraycopy(b_type, 0, data, idx, b_type.length);
			idx += b_type.length;
			System.arraycopy(b_name, 0, data, idx, b_name.length);
		}

		setData(data);
	}
}
