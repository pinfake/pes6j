package pes6j.datablocks;

public class RankUrlInfo {
	int type;
	String name;

	public final static int TYPE_0 = 0x0000;
	public final static int TYPE_1 = 0x0100;
	public final static int TYPE_2 = 0x0200;
	public final static int TYPE_3 = 0x0300;
	public final static int TYPE_4 = 0x0400;
	public final static int TYPE_5 = 0x0500;
	public final static int TYPE_6 = 0x0600;

	public RankUrlInfo(int type, String name) {
		this.type = type;
		this.name = name;
	}

	public int getType() {
		return (type);
	}

	public String getName() {
		return (name);
	}
}
