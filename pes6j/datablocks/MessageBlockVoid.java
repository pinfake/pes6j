package pes6j.datablocks;

public class MessageBlockVoid extends MessageBlock {
	public MessageBlockVoid(int qId) {
		super();
		header.setQuery(qId);
	}
}
