package pes6j.datablocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class Message {
	Vector<MetaMessageBlock> blocks;
	int byteSize;

	public static Message getInstance(byte[] data) {
		
		if( data == null ) return( null );
		
		Message mes = new Message();
		mes.byteSize = 0;
		mes.blocks = new Vector<MetaMessageBlock>();
		int idx = 0;

		if( data.length < GenericHeader.getByteSize()) return null;
		while (idx < data.length) {
			byte[] header = new byte[GenericHeader.getByteSize()];
			System.arraycopy(data, idx, header, 0, GenericHeader.getByteSize());
			MetaMessageBlock mb = new MessageBlock();
			GenericHeader gh = new GenericHeader(header);
			if( (gh.getSize() + GenericHeader.getByteSize()) > (data.length - idx)) {
				return( null );
			}
			((MessageBlock) mb).setHeader(gh);
			byte[] blockData = new byte[gh.getSize()];

			/*
			 * Problemas de fragmentacion!
			 */
			System.arraycopy(data, idx + GenericHeader.getByteSize(),
					blockData, 0, gh.getSize());
			((MessageBlock) mb).setData(blockData);
			mes.addBlock(mb);
			idx += (GenericHeader.getByteSize() + gh.getSize());
		}
		return( mes );
	}
	
	public Message(byte[] data) {
		this.byteSize = 0;
		this.blocks = new Vector<MetaMessageBlock>();
		int idx = 0;

		while (idx < data.length) {
			byte[] header = new byte[GenericHeader.getByteSize()];
			System.arraycopy(data, idx, header, 0, GenericHeader.getByteSize());
			MetaMessageBlock mb = new MessageBlock();
			GenericHeader gh = new GenericHeader(header);
			((MessageBlock) mb).setHeader(gh);
			byte[] blockData = new byte[gh.getSize()];

			/*
			 * Problemas de fragmentacion!
			 */
			System.arraycopy(data, idx + GenericHeader.getByteSize(),
					blockData, 0, gh.getSize());
			((MessageBlock) mb).setData(blockData);
			addBlock(mb);
			idx += (GenericHeader.getByteSize() + gh.getSize());
		}
	}

	public Message() {
		this.blocks = new Vector<MetaMessageBlock>();
	}

	public void addBlock(MetaMessageBlock mb) {
		blocks.add(mb);
		byteSize += mb.getByteSize();
	}

	public void setBlock(int idx, MetaMessageBlock mb) {
		blocks.set(idx, mb);
	}

	public byte[] getBytes() {
		byte[] ret = new byte[byteSize];
		int idx = 0;
		for (int i = 0; i < blocks.size(); i++) {
			MetaMessageBlock mb = (MetaMessageBlock) blocks.get(i);
			System.arraycopy(mb.getBytes(), 0, ret, idx, mb.getByteSize());
			idx += mb.getByteSize();
		}
		return (ret);
	}

	public MetaMessageBlock getBlock(int idx) {
		return ((MetaMessageBlock) this.blocks.get(idx));
	}

	public int getNumBlocks() {
		return (blocks.size());
	}
}
