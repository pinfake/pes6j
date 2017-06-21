package pes6j.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import pes6j.datablocks.Util;

public class KonamiEncoder {
	public static void main(String[] arg) {
		// start server on port 1500
		if (arg.length < 2) {
			System.out
					.println("1st parameter input decoded filename, 2nd parameter output encoded filename, 3rd param maskIdx");
		}
		int maskIdx = 0;
		if (arg.length == 3)
			maskIdx = Integer.parseInt(arg[2]);
		try {
			byte enc[] = new byte[(int) (new File(arg[0])).length()];
			FileInputStream fi = new FileInputStream(arg[0]);
			fi.read(enc);
			fi.close();
			FileOutputStream fo = new FileOutputStream(arg[1]);
			fo.write(Util.konamiMaskIt(enc, maskIdx));
			fo.close();
			System.out.println(Util.toHex(Util.konamiMaskIt(enc, maskIdx)));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
