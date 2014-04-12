package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockInfo extends MessageBlock {

	static final byte[] MSG_HEADER = { (byte) 0x00, (byte) 0x00, (byte) 0x03,
			(byte) 0x10, (byte) 0x01, (byte) 0x00 };

	public MessageBlockInfo(String title, String text, int qId) {
		super();
		header.setQuery(qId);

		if (title.length() > 63)
			title = title.substring(0, 63);

		byte[] b_title = new byte[64];
		Arrays.fill(b_title, (byte) 0x00);
		System.arraycopy(title.getBytes(), 0, b_title, 0,
				title.getBytes().length);
		byte[] b_date = "2009-01-15 11:40:00".getBytes();
		byte[] b_text = new byte[text.getBytes().length + 1];
		System.arraycopy(text.getBytes(), 0, b_text, 0, b_text.length - 1);
		b_text[b_text.length - 1] = (byte) 0x00;

		byte[] data = new byte[MSG_HEADER.length + b_date.length
				+ b_title.length + b_text.length];

		System.arraycopy(MSG_HEADER, 0, data, 0, MSG_HEADER.length);
		System.arraycopy(b_date, 0, data, MSG_HEADER.length, b_date.length);
		System.arraycopy(b_title, 0, data, MSG_HEADER.length + b_date.length,
				b_title.length);
		System.arraycopy(b_text, 0, data, MSG_HEADER.length + b_date.length
				+ b_title.length, b_text.length);

		setData(data);
	}
}
