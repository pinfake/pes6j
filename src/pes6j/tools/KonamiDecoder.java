package pes6j.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import pes6j.datablocks.Util;

public class KonamiDecoder {
	public static void main(String[] arg) {
		// start server on port 1500
		if (arg.length < 2) {
			System.out
					.println("1st parameter input encode filename, 2nd parameter output decoded filename");
		}
		try {
			byte enc[] = new byte[(int) (new File(arg[0])).length()];
			FileInputStream fi = new FileInputStream(arg[0]);
			fi.read(enc);
			fi.close();
			FileOutputStream fo = new FileOutputStream(arg[1]);
			fo.write(Util.konamiDecode(enc));
			fo.close();
			System.out.println(Util.toHex(Util.konamiDecode(enc)));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
