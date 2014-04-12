package pes6j.datablocks;

public class MessageBlockGameAnyId extends MessageBlock {

	public MessageBlockGameAnyId(long rId, int qId) {
		super();
		header.setQuery(qId);
		setData(Util.long2Word(rId));
	}
}
