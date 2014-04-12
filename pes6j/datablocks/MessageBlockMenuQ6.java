package pes6j.datablocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MessageBlockMenuQ6 extends MessageBlock {

	// static final byte[]

	public MessageBlockMenuQ6(int qId) {
		super();
		header.setQuery(qId);

		byte[] data;
		try {
			data = new byte[(int) (new File("login2_r6_data.dec")).length()];
			FileInputStream fi = new FileInputStream("login2_r6_data.dec");
			fi.read(data);
			fi.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
		setData(data);
	}
}
