package pes6j.datablocks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.Date;

public class Util {
	
	static final byte[] xorMatrix = { (byte) 0xA6, (byte) 0x77, (byte) 0x95, (byte) 0x7c };

	public static int strlen(byte[] data, int offset) {
		int len = 0;
		for (int i = offset; i < data.length; i++) {
			if (data[i] == 0x00)
				break;
			len++;
		}
		return (len);
	}

	public static long getTimeOfToday( ) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( new Date() );
		cal.set( Calendar.HOUR_OF_DAY, 0 );
		cal.set( Calendar.MINUTE, 0 );
		cal.set( Calendar.SECOND, 0 );
		return( cal.getTimeInMillis() );
	}
	
	public static int word2Int(byte h, byte l) {
		int firstByte = (0x000000FF & (h));
		int secondByte = (0x000000FF & (l));
		return ((firstByte << 8 | secondByte));
	}

	public static byte[] int2Word(int val) {
		byte[] ret = new byte[2];

		int firstByte = (0x0000FF00 & val) >> 8;
		int secondByte = (0x000000FF & val);
		ret[0] = (byte) firstByte;
		ret[1] = (byte) secondByte;
		return (ret);
	}

	public static byte[] long2Word(long val) {
		byte[] ret = new byte[4];
		int firstByte = (int) (val & 0xFF000000L) >> 24;
		int secondByte = (int) (val & 0x00FF0000L) >> 16;
		int thirdByte = (int) (val & 0x0000FF00L) >> 8;
		int fourthByte = (int) (val & 0x000000FFL);

		ret[0] = (byte) firstByte;
		ret[1] = (byte) secondByte;
		ret[2] = (byte) thirdByte;
		ret[3] = (byte) fourthByte;

		return (ret);
	}

	public static long word2Long(byte h1, byte l1, byte h2, byte l2) {
		int firstByte = (0x000000FF & (h1));
		int secondByte = (0x000000FF & (l1));
		int thirdByte = (0x000000FF & (h2));
		int fourthByte = (0x000000FF & (l2));

		return (((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFFL);

	}

	public static String toJavaHex(byte in[], int len) {

		byte ch = 0x00;
		int i = 0;

		if (in == null || in.length <= 0)
			return null;

		String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(len * 12);

		while (i < len) {
			ch = (byte) (in[i] & 0xF0); // Strip off high nibble
			ch = (byte) (ch >>> 4);
			// shift the bits down
			ch = (byte) (ch & 0x0F);
			// must do this is high order bit is on!
			out.append("(byte)0x");
			out.append(pseudo[ch]); // convert the nibble to a String
			// Character

			ch = (byte) (in[i] & 0x0F); // Strip off low nibble
			out.append(pseudo[ch]); // convert the nibble to a String
			// Character
			out.append(", ");
			i++;
		}

		String rslt = new String(out);

		return rslt;

	}

	public static String toHex(byte in[]) {

		byte ch = 0x00;
		int i = 0;

		if (in == null || in.length <= 0)
			return null;

		String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(in.length * 3);

		while (i < in.length) {
			ch = (byte) (in[i] & 0xF0); // Strip off high nibble
			ch = (byte) (ch >>> 4);
			// shift the bits down
			ch = (byte) (ch & 0x0F);
			// must do this is high order bit is on!
			out.append(pseudo[ch]); // convert the nibble to a String
			// Character

			ch = (byte) (in[i] & 0x0F); // Strip off low nibble
			out.append(pseudo[ch]); // convert the nibble to a String
			// Character
			out.append(" ");
			i++;
		}

		String rslt = new String(out);

		return rslt;
	}

	public static byte[] konamiEncode(byte asc[]) {
		return (konamiEncode(asc, 0));
	}

	public static byte[] konamiMaskIt(byte asc[], int maskIdx) {
		byte[] ret = new byte[asc.length];
		byte[] xorMatrix = { (byte) 0xA6, (byte) 0x77, (byte) 0x95, (byte) 0x7c };

		int xorIdx = maskIdx;

		for (int i = 0; i < asc.length; i++) {
			ret[i] = (byte) (asc[i] ^ xorMatrix[xorIdx++]);
			if (xorIdx > 3)
				xorIdx = 0;
		}

		return (ret);
	}

	public static byte[] konamiEncode(byte asc[], int maskIdx) {
		byte[] ret = new byte[asc.length];
		byte[] header = new byte[24];

		/*
		 * Hay una cabecera de 24 bytes iniciales, aparentemente los bytes en
		 * posiciones 2y3 forman un short que dice cuanto va a medir la cadena
		 * siguiente En la cadena siguiente los 6 bytes iniciales parecen datos
		 * de algún tipo antes del texto
		 */

		/*
		 * Leemos la cabecera primero
		 */
		int xorIdx;
		int i = 0;
		while (i < asc.length) {
			xorIdx = maskIdx;
			int pos = i;
			int j = 0;
			for (; i < pos + 24; i++) {
				ret[i] = (byte) (asc[i] ^ xorMatrix[xorIdx++]);
				header[j++] = asc[i];
				if (xorIdx > 3)
					xorIdx = 0;
			}


			int firstByte = (0x000000FF & (header[2]));
			int secondByte = (0x000000FF & (header[3]));

			long tamano = (firstByte << 8 | secondByte);

			pos = i;

			for (; i < (pos + tamano); i++) {
				if (i == asc.length) {
//					System.out.println("pues la longitud real era: "
//							+ asc.length + " y yo me quiero ir a: "
//							+ (pos + tamano));
					break;
				}
				ret[i] = (byte) (asc[i] ^ xorMatrix[xorIdx++]);
				if (xorIdx > 3)
					xorIdx = 0;
			}
		}

		return (ret);
	}

//	public static byte[] konamiDecode(byte kon[], int len) {
//		byte[] ret = new byte[len];
//		int xorIdx = 0;
//		
//		for (int i = 0; i < len; i++) {
//			ret[i] = (byte) (kon[i] ^ xorMatrix[xorIdx++]);
//			if (xorIdx > 3)
//				xorIdx = 0;
//		}
//		return( ret );
//	}
	
	public static byte[] konamiDecode(byte kon[], int len) {
		byte[] ret = new byte[len];
		byte[] header = new byte[24];

		/*
		 * Hay una cabecera de 24 bytes iniciales, aparentemente los bytes en
		 * posiciones 2y3 forman un short que dice cuanto va a medir la cadena
		 * siguiente En la cadena siguiente los 6 bytes iniciales parecen datos
		 * de alg�n tipo antes del texto
		 */

		/*
		 * Leemos la cabecera primero
		 */
		int xorIdx;
		int i = 0;
		while (i < len) {
			xorIdx = 0;
			int pos = i;
			int j = 0;
			
			/*
			 * Broken message, we can't fully decode it
			 */
			if( len < pos + 24 ) return( null );
			
			for (; i < pos + 24; i++) {
				ret[i] = (byte) (kon[i] ^ xorMatrix[xorIdx++]);
				header[j++] = ret[i];
				if (xorIdx > 3)
					xorIdx = 0;
			}

			/*
			 * Broken message, we can't fully decode it
			 */
			if( j < 24 ) return( null );
			
			int tamano = word2Int(header[2], header[3]);

			pos = i;

			for (; i < (pos + tamano); i++) {
				/*
				 * The message is incomplete, we can't fully decode it
				 */
				if (i == len) {
					return( null );
				}
				ret[i] = (byte) (kon[i] ^ xorMatrix[xorIdx++]);
				if (xorIdx > 3)
					xorIdx = 0;
			}
		}

		return (ret);
	}

	public static byte[] konamiDecode(byte kon[]) {
		return (konamiDecode(kon, kon.length));
	}

	public static boolean byteArrayCompare(byte a1[], byte a2[], int len) {
		for (int i = 0; i < len; i++) {
			if (a1[i] != a2[i])
				return false;
		}
		return true;
	}

	public static byte[] streamRead(InputStream in) throws IOException {
		byte buf[] = new byte[4096];
		int len = in.read(buf);

		if (len == -1)
			return null;

		byte ret[] = new byte[len];
		System.arraycopy(buf, 0, ret, 0, len);
		return (ret);
	}
	
	public static byte[] streamRead(SocketChannel in) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		//byte buf[] = new byte[4096];
		int len = in.read(buf);

		if (len == -1)
			return null;

		byte ret[] = new byte[len];
		buf.get(ret);
		//System.arraycopy(buf, 0, ret, 0, len);
		return (ret);
	}

	public static byte[] streamReadDecoded(InputStream in) throws IOException {
		byte[] enc = streamRead(in);
		if (enc == null)
			return null;
		byte[] dec = konamiDecode(enc);
		return (dec);
	}
	
	public static byte[] streamReadDecoded(SocketChannel in) throws IOException {
		byte[] enc = streamRead(in);
		if (enc == null)
			return null;
		byte[] dec = konamiDecode(enc);
		return (dec);
	}

	public static void streamWriteEncoded(OutputStream out, byte[] data)
			throws IOException {
		out.write(konamiEncode(data));
		out.flush();
	}
	
	public static int streamWriteEncoded(SocketChannel out, byte[] data)
			throws IOException {
		//ByteBuffer buf = ByteBuffer.wrap( data );
		//int max_packet_len = 1024;
		
		return( out.write(ByteBuffer.wrap(konamiEncode(data))) );
	}
	
	public static String fetchURLContents( String url ) {
		URL u;
		try {
			u = new URL(url);
			HttpURLConnection uc = (HttpURLConnection)u.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));
			String s;
			uc.setConnectTimeout(1000);
			uc.setReadTimeout(1000);
	        in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));
	        String ret = new String();
			while( (s = in.readLine()) != null ) {
				if( ret.length() > 0 ) ret += "\n";
				ret += s;
			}
			return( ret );
		} catch(ConnectException ex ) {
			//System.out.println("Connect exception");
		} catch(MalformedURLException ex ) {
		} catch(IOException ex) {
		} catch(Exception ex ) {
		}
		return( null );
	}
}
