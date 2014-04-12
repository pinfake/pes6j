package pes6j.datablocks;

public class MessageBlockLong extends MessageBlock {

	public MessageBlockLong(long value, int qId) {
		super();
		header.setQuery(qId);
		setData(Util.long2Word(value));
	}
}
