package pes6j.datablocks;

public class MessageBlock implements MetaMessageBlock {
	GenericHeader header;
	byte[] data;

	public MessageBlock() {
		this.header = new GenericHeader();
		this.data = new byte[0];
	}

	public MessageBlock(GenericHeader header, byte[] data) {
		this.header = header;
		this.data = data;
	}

	public GenericHeader getHeader() {
		return (header);
	}

	public void setHeader(GenericHeader header) {
		this.header = header;
	}

	public byte[] getData() {
		return (data);
	}

	public byte[] getBytes() {
		byte[] ret = new byte[GenericHeader.getByteSize() + data.length];
		System.arraycopy(header.getBytes(), 0, ret, 0, GenericHeader
				.getByteSize());
		System
				.arraycopy(data, 0, ret, GenericHeader.getByteSize(),
						data.length);
		return (ret);
	}

	public int getByteSize() {
		return (GenericHeader.getByteSize() + data.length);
	}

	public void setData(byte[] data) {
		this.data = data;
		header.setSize(data.length);
	}
}
