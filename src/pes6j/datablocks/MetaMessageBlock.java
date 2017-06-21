package pes6j.datablocks;

public interface MetaMessageBlock {
	public byte[] getData();

	public byte[] getBytes();

	// public void setData( byte[] data );
	public int getByteSize();

	public GenericHeader getHeader();
}
