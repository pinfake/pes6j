package pes6j.datablocks;

public class DataCdKeyAndPass {
	byte[] data;

	public DataCdKeyAndPass(byte[] data) {
		this.data = data;
	}

	public byte[] getCdKey() {
		byte[] ret = new byte[32];
		System.arraycopy(data, 0, ret, 0, 32);
		return (ret);
	}
}
