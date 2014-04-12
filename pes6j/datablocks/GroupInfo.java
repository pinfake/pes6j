package pes6j.datablocks;

public class GroupInfo {
	long id;
	String name;
	String comment;
	long position;
	long points;
	int victories;
	int defeats;
	int draws;
	int matches;
	int slots;
	long level;
	PlayerInfo leader;
	PlayerInfo[] players;

	public GroupInfo(long id, String name) {
		this.id = id;
		this.name = name;
		this.players = new PlayerInfo[0];
		this.comment = "";
	}

	public synchronized int getNumPlayers() {
		return (players.length );
	}
	
	public synchronized long[] getPlayerIds() {
		long[] pids = new long[players.length];
		for( int i = 0; i < players.length; i++ ) {
			pids[i] = players[i].getId();
		}
		return( pids );
	}

	public synchronized void setLeader(PlayerInfo p) {
		this.leader = p;
	}

	public synchronized PlayerInfo getLeader() {
		return (this.leader);
	}

	public synchronized void setSlots(int slots) {
		this.slots = slots;
	}

	public synchronized int getSlots() {
		return (slots);
	}

	public synchronized long getPoints() {
		return (points);
	}

	public synchronized void setPoints(long points) {
		this.points = points;
	}

	public synchronized long getPosition() {
		return (position);
	}

	public synchronized void setPosition(long position) {
		this.position = position;
	}

	public synchronized long getId() {
		return (id);
	}

	public synchronized String getName() {
		return (name);
	}

	public synchronized void setPlayers(PlayerInfo[] players) {
		this.players = players;
	}

	public synchronized void addPlayer(PlayerInfo player) {
		PlayerInfo newPInfo[] = new PlayerInfo[players.length + 1];
		System.arraycopy(players, 0, newPInfo, 0, players.length);
		newPInfo[players.length] = player;
		players = newPInfo;
	}
	
	public synchronized void removePlayer(long pid) {
		int idx = 0;
		PlayerInfo newPlayers[] = new PlayerInfo[players.length - 1];
		for( int i = 0; i < players.length; i++ ) {
			if( players[i].getId() != pid ) {
				newPlayers[idx++] = players[i];
			}
		}
		players = newPlayers;
	}

	public synchronized PlayerInfo[] getPlayers() {
		return (players);
	}

	public synchronized void setComment(String comment) {
		this.comment = comment;
	}

	public synchronized String getComment() {
		return (this.comment);
	}

	public synchronized int getMatches() {
		return matches;
	}

	public synchronized void setMatches(int matches) {
		this.matches = matches;
	}

	public synchronized long getLevel() {
		return level;
	}

	public synchronized void setLevel(long level) {
		this.level = level;
	}

	public synchronized int getDraws() {
		return draws;
	}

	public synchronized void setDraws(int draws) {
		this.draws = draws;
	}

	public synchronized int getDefeats() {
		return defeats;
	}

	public synchronized void setDefeats(int defeats) {
		this.defeats = defeats;
	}

	public synchronized int getVictories() {
		return victories;
	}

	public synchronized void setVictories(int victories) {
		this.victories = victories;
	}
}
