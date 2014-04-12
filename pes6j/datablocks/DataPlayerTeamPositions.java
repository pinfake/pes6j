package pes6j.datablocks;

public class DataPlayerTeamPositions {
	byte[] data;

	public DataPlayerTeamPositions(byte[] data) {
		this.data = data;
	}

	public int getPlayerTeam(long pid) {
		for (int i = 0; i < 4; i++) {
			int idx = i * 8;
			long t_pid = Util.word2Long(data[idx], data[idx + 1],
					data[idx + 2], data[idx + 3]);
			if (t_pid != pid)
				continue;
			return (data[idx + 4]);
		}
		return (0x000000FF);
	}

	public int getPlayerTeamPos(long pid) {
		for (int i = 0; i < 4; i++) {
			int idx = i * 8;
			long t_pid = Util.word2Long(data[idx], data[idx + 1],
					data[idx + 2], data[idx + 3]);
			if (t_pid != pid)
				continue;
			return (data[idx + 7]);
		}
		return (0x00000000);
	}
}
