package pes6j.datablocks;

import java.util.Arrays;

public class MessageBlockGameGroupList extends MessageBlockMulti {

	static final int MAX_GROUPS_PER_BLOCK = 6;
	
	public MessageBlockGameGroupList(GroupInfo[] groups, int qId) {
		super();
//		header.setQuery(qId);
//		header.setSeq(seq);

//		byte[] data = new byte[(4 + 48) * groups.length];
//		Arrays.fill(data, (byte) 0x00);

//		int idx = 0;

		int numBlocks = (groups.length / MAX_GROUPS_PER_BLOCK) + 1;
		
		for( int j = 0; j < numBlocks; j++ ) {
			int start = j*MAX_GROUPS_PER_BLOCK;
			int end = j*MAX_GROUPS_PER_BLOCK + MAX_GROUPS_PER_BLOCK;
			if( j == (numBlocks - 1) ) end = groups.length;
			byte[] data = new byte[(4 + 48) * (end - start)];
			Arrays.fill(data, (byte) 0x00);
			int idx = 0;
			for (int i = start; i < end; i++) {
				String grName = groups[i].getName();
				long grId = groups[i].getId();
				byte[] b_grid = Util.long2Word(grId);
	
				System.arraycopy(b_grid, 0, data, idx, b_grid.length);
				idx += b_grid.length;
				System.arraycopy(grName.getBytes(), 0, data, idx,
						grName.getBytes().length);
				idx += 48;
			}
			MessageBlock mb = new MessageBlock();
			mb.header.query = qId;
			mb.setData(data);
			addBlock(mb);
		}
		//setData(qId, seq, data);
	}
}
