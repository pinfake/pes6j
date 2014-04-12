package pes6j.datablocks;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageBlockLoginDateTimeInfo extends MessageBlock {
	static final byte[] MSG_DATA = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0xAA,
			(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xE2, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32, (byte) 0x30,
			(byte) 0x30, (byte) 0x38, (byte) 0x2D, (byte) 0x30, (byte) 0x36,
			(byte) 0x2D, (byte) 0x32, (byte) 0x33, (byte) 0x20, (byte) 0x31,
			(byte) 0x35, (byte) 0x3A, (byte) 0x34, (byte) 0x35, (byte) 0x3A,
			(byte) 0x32, (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0xE2,
			(byte) 0x93, (byte) 0x00, (byte) 0x04, (byte) 0x71, (byte) 0xFB,
			(byte) 0x00, (byte) 0x04, (byte) 0x2D, (byte) 0x83, (byte) 0x00,
			(byte) 0x03, (byte) 0xC8, (byte) 0x7E, (byte) 0x00, (byte) 0x00,
			(byte) 0x87, (byte) 0x2B, (byte) 0x01, (byte) 0xF6, (byte) 0x02,
			(byte) 0x57, (byte) 0x01, (byte) 0xF2, (byte) 0x01, (byte) 0xD9,
			(byte) 0x01, (byte) 0x82 };

	public MessageBlockLoginDateTimeInfo(long msecs, int qId) {
		super();
		header.setQuery(qId);

		byte[] data = MSG_DATA;

		Date d = new Date(msecs);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = df.format(d);
		byte[] b_datestr = dateStr.getBytes();

		System.arraycopy(b_datestr, 0, data, 36, b_datestr.length);
		// 36

		setData(data);
	}
}
