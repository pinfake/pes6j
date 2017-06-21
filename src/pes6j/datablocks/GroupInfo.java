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
	long leaderId;
	int recruiting;
	int numPlayers;
	


	String recruitComment;
	String leaderName;
	
	PlayerInfo[] players;

	public GroupInfo(long id, String name) {
		this.id = id;
		this.name = name;
		this.players = new PlayerInfo[0];
		this.comment = "";
	}

	public void setNumPlayers(int numPlayers) {
		this.numPlayers = numPlayers;
	}
	
	public int getRecruiting() {
		return recruiting;
	}

	public void setRecruiting(int recruiting) {
		this.recruiting = recruiting;
	}

	public String getRecruitComment() {
		return recruitComment;
	}

	public void setRecruitComment(String recruitComment) {
		this.recruitComment = recruitComment;
	}

//	public int getNumPlayers() {
//		return (players.length );
//	}
	
	public int getNumPlayers() {
		return( this.numPlayers );
	}
	
	public long[] getPlayerIds() {
		long[] pids = new long[players.length];
		for( int i = 0; i < players.length; i++ ) {
			pids[i] = players[i].getId();
		}
		return( pids );
	}

	public void setLeaderId(long pid) {
		this.leaderId = pid;
	}
	
	public void setLeaderName( String name ) {
		this.leaderName = name;
	}

	public long getLeaderId( ) {
		return( this.leaderId );
	}
	
	public String getLeaderName() {
		return( this.leaderName );
	}

	public void setSlots(int slots) {
		this.slots = slots;
	}

	public int getSlots() {
		return (slots);
	}

	public long getPoints() {
		return (points);
	}

	public void setPoints(long points) {
		this.points = points;
	}

	public long getPosition() {
		return (position);
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public long getId() {
		return (id);
	}

	public String getName() {
		return (name);
	}

	public void setPlayers(PlayerInfo[] players) {
		this.players = players;
	}

	public void addPlayer(PlayerInfo player) {
		PlayerInfo newPInfo[] = new PlayerInfo[players.length + 1];
		System.arraycopy(players, 0, newPInfo, 0, players.length);
		newPInfo[players.length] = player;
		players = newPInfo;
	}
	
	public void removePlayer(long pid) {
		int idx = 0;
		PlayerInfo newPlayers[] = new PlayerInfo[players.length - 1];
		for( int i = 0; i < players.length; i++ ) {
			if( players[i].getId() != pid ) {
				newPlayers[idx++] = players[i];
			}
		}
		players = newPlayers;
	}

	public PlayerInfo[] getPlayers() {
		return (players);
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return (this.comment);
	}

	public int getMatches() {
		return matches;
	}

	public void setMatches(int matches) {
		this.matches = matches;
	}

	public long getLevel() {
		return level;
	}

	public void setLevel(long level) {
		this.level = level;
	}

	public int getDraws() {
		return draws;
	}

	public void setDraws(int draws) {
		this.draws = draws;
	}

	public int getDefeats() {
		return defeats;
	}

	public void setDefeats(int defeats) {
		this.defeats = defeats;
	}

	public int getVictories() {
		return victories;
	}

	public void setVictories(int victories) {
		this.victories = victories;
	}
}
