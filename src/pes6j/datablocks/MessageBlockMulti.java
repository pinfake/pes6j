package pes6j.datablocks;

import java.util.Vector;

public class MessageBlockMulti implements MetaMessageBlock {
	Vector<MetaMessageBlock> blocks;
	byte[] data;
	long byteSize;

	public MessageBlockMulti() {
		blocks = new Vector<MetaMessageBlock>();
		this.byteSize = 0;
	}

	public MessageBlockMulti(int qId, int seq, byte[] data) {
		this.byteSize = 0;
		this.blocks = new Vector<MetaMessageBlock>();

		setData(qId, seq, data);
	}

	public GenericHeader getHeader() {
		return (getBlock(0).getHeader());
	}

	public void setData(int qId, int seq, byte[] data) {
		int idx = 0;
		while (idx < data.length) {
			byte[] b_block;
			if ((data.length - idx) > 1024) {
				b_block = new byte[1024];
				System.arraycopy(data, 0, b_block, 0, 1024);
				//idx += 1024;
			} else {
				b_block = new byte[(data.length - idx)];
				System.arraycopy(data, idx, b_block, 0, (data.length - idx));
			}
			MessageBlock mb = new MessageBlock();
			mb.getHeader().setQuery(qId);
			mb.getHeader().setSeq(seq++);
			mb.setData(b_block);
			blocks.add(mb);
			idx += b_block.length;
		}
	}

	public byte[] getData() {
		int size = 0;

		for (int i = 0; i < blocks.size(); i++) {
			MessageBlock mb = (MessageBlock) blocks.get(i);
			size += mb.getHeader().getSize();
		}

		byte[] ret = new byte[size];
		int idx = 0;
		for (int i = 0; i < blocks.size(); i++) {
			MessageBlock mb = (MessageBlock) blocks.get(i);
			System.arraycopy(mb.getData(), 0, ret, idx, mb.getData().length);
			idx += mb.getData().length;
		}

		return (ret);
	}

	public int getNumBlocks() {
		return blocks.size();
	}

	public MessageBlock getBlock(int idx) {
		return ((MessageBlock) blocks.get(idx));
	}

	public void setBlock(int idx, MessageBlock mb) {
		blocks.set(idx, mb);
	}

	public void addBlock(MessageBlock mb) {
		blocks.add(mb);
	}

	public byte[] getBytes() {
		int size = 0;

		for (int i = 0; i < blocks.size(); i++) {
			MessageBlock mb = (MessageBlock) blocks.get(i);
			size += mb.getByteSize();
		}

		byte[] ret = new byte[size];
		int idx = 0;
		for (int i = 0; i < blocks.size(); i++) {
			MessageBlock mb = (MessageBlock) blocks.get(i);
			System.arraycopy(mb.getBytes(), 0, ret, idx, mb.getBytes().length);
			idx += mb.getBytes().length;
		}

		return (ret);
	}

	public int getByteSize() {
		int ret = 0;
		for (int i = 0; i < blocks.size(); i++) {
			MessageBlock mb = (MessageBlock) blocks.get(i);
			ret += mb.getByteSize();
		}
		return (ret);
	}
}
