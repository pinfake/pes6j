package pes6j.datablocks;

import java.util.Arrays;

public class GenericHeader {
	int query; // 0x2009 para r1
	// 0x200A para r12 y luego 0x200B
	// 0x2003 para r423
	// 0x2007 para r2
	// 0x2008 para p1
	// 0x2005 para p4
	// 0x2002 para r4
	// 0x2006 para p2
	// 0x0003 para p3 END!
	// 0x2200 para p5
	// 0x2201 para r5
	// 0x2202 para r52
	// 0x2006 para p6 = que p2
	// 0x2007 para r6 = que r2
	// 0x2008 para p7 = que p1!
	// 0x2009 para r7 = que r1!
	// 0x200A para r72
	// 0x0003 para p8 END!

	int size; // Tama�o de la respuesta o pregunta
	long seq; // En r1 0x0001
	// En r123 y r423 0x0002 y luego 0x0003 y luego 0x0004
	// En r2 0x0005
	// En p1 Y en p4 0x0001
	// En r4 0x0001
	// En p2 0x0002
	// En p3 0x0003
	// En p5 0x0002
	// En r5 0x0007 y luego 0x0008 y luego 0x0009
	// En p6 0x0003
	// En r6 0x000A
	// En p7 0x0004
	// En r7 0x000B
	// En r72 0x000C y luego 0x000D y luego 0x000E
	// En p8 0x0005

	byte stuff[];

	public GenericHeader() {
		query = 0;
		size = 0;
		stuff = new byte[16];
		Arrays.fill(stuff, (byte) 0x00);
	}

	public GenericHeader(byte[] header) {
		query = Util.word2Int(header[0], header[1]);
		size = Util.word2Int(header[2], header[3]);
		seq = Util.word2Long(header[4], header[5], header[6], header[7]);
		stuff = new byte[16];
		System.arraycopy(header, 8, stuff, 0, 16);
	}

	public int getQuery() {
		return (query);
	}

	public void setQuery(int query) {
		this.query = query;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public void setStuff(byte[] stuff) {
		this.stuff = stuff;
	}

	public long getSeq() {
		return (seq);
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return (this.size);
	}

	public byte[] getStuff() {
		return (stuff);
	}

	public byte[] getBytes() {
		byte[] header = new byte[24];
		byte[] b_query = Util.int2Word(query);
		byte[] b_size = Util.int2Word(size);
		byte[] b_seq = Util.long2Word(seq);
		System.arraycopy(b_query, 0, header, 0, 2);
		System.arraycopy(b_size, 0, header, 2, 2);
		System.arraycopy(b_seq, 0, header, 4, 4);
		System.arraycopy(stuff, 0, header, 8, 16);
		return (header);
	}

	public static int getByteSize() {
		return (24);
	}
}
