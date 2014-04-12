package pes6j.datablocks;

public class MessageBlockMenuPlayerId extends MessageBlock {

	public MessageBlockMenuPlayerId(long plId, int qId) {
		super();
		header.setQuery(qId);
		setData(Util.long2Word(plId));
	}
}
