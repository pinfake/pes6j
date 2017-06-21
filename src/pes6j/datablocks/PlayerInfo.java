package pes6j.datablocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Stack;

public class PlayerInfo {
	long id;
	
	int category;
	long points;
	int matchPoints;
	int matchesPlayed;
	int victories;
	int defeats;
	int draws;
	int winningStreak;
	int bestStreak;
	int decos;
	int division;
	Stack<Integer> teams;
	long goalsScored;
	long goalsReceived;
	long timePlayed;
	long lastLogin;
	long position;
	int oldCategory;
	long oldPoints;
	int lang;
	int groupMatches;
	int groupLevel;
	int invitation;
	long gid;
	String groupName;
	int serverId;
	int lobbyId;
	String lobbyName;
	int lobbyType;
	
	String name;
	String comment;
	byte[] settings;
	boolean logged;
	int admin;
	IpInfo ip;
	PESConnection con;
	
//	public PlayerInfo clone() {
//		PlayerInfo c = new PlayerInfo( id, name );
//		
//		c.category = category;
//		c.points = points;
//		c.matchPoints = matchPoints;
//		c.matchesPlayed = matchesPlayed;
//		c.victories = victories;
//		c.defeats = defeats;
//		c.draws = draws;
//		c.winningStreak = winningStreak;
//		c.bestStreak = bestStreak;
//		c.decos = decos;
//		c.division = division;
//		c.teams = teams;
//		c.goalsScored = goalsScored;
//		c.goalsReceived = goalsReceived;
//		c.timePlayed = timePlayed;
//		c.lastLogin = lastLogin;
//		c.position = position;
//		c.oldCategory = oldCategory;
//		c.oldPoints = oldPoints;
//		c.lang = lang;
//		c.groupMatches = groupMatches;
//		c.invitation = invitation;
//		c.gid = gid;
//		c.groupName = groupName;
//		c.serverId = serverId;
//		c.lobbyId = lobbyId;
//		c.lobbyName = lobbyName;
//		c.lobbyType = lobbyType;
//		c.comment = comment;
//
//		c.settings = settings;
//		//byte[] settings;
//		c.logged = logged;
//		c.admin = admin;
//		c.ip = ip;
//		c.con = con;
//		//IpInfo ip;
//		//PESConnection con;
//		
//		return( c );
//	}
	
	public PlayerInfo(long id, String name) {
		this.name = name;
		this.category = 500;
		this.oldCategory = 500;
		this.points = 0;
		this.oldPoints = 0;
		this.matchesPlayed = 0;
		this.lang = 0;
		
		victories = 0;
		defeats = 0;
		draws = 0;
		winningStreak = 0;
		bestStreak = 0;
		decos = 0;
		goalsScored = 0;
		goalsReceived = 0;
		timePlayed = 0;
		division = 2;
		invitation = 0;
		gid = 0;
		
		lastLogin = (new Date()).getTime();
		
		teams = new Stack<Integer>();
		
		this.id = id;
		this.logged = false;
		this.comment = "";
		this.admin = 0;
		
		try {
			FileInputStream fi = new FileInputStream("mis_messages.dec");
			byte[] defSettings = new byte[(int) new File("mis_messages.dec")
					.length()];
			fi.read(defSettings);
			fi.close();
			Message mes = new Message(defSettings);
			MessageBlockLoginMessagesList mb = new MessageBlockLoginMessagesList(
					mes);
			this.settings = mb.getData();
		} catch (IOException ex) {
		}
	}
	
	public long getGid() {
		return gid;
	}

	public int getGroupLevel() {
		return groupLevel;
	}

	public void setGroupLevel(int groupLevel) {
		this.groupLevel = groupLevel;
	}

	public int getLobbyType() {
		return lobbyType;
	}

	public void setLobbyType(int lobbyType) {
		this.lobbyType = lobbyType;
	}

	public int getLobbyId() {
		return lobbyId;
	}

	public void setLobbyId(int lobbyId) {
		this.lobbyId = lobbyId;
	}

	public String getLobbyName() {
		return lobbyName;
	}

	public void setLobbyName(String lobbyName) {
		this.lobbyName = lobbyName;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void setGid(long gid) {
		this.gid = gid;
	}
	
	public int getGroupMatches() {
		return groupMatches;
	}

	public void setGroupMatches(int groupMatches) {
		this.groupMatches = groupMatches;
	}

	public int getInvitation() {
		return invitation;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setInvitation(int invitation) {
		this.invitation = invitation;
	}

	public void setLang( int lang ) {
		this.lang = lang;
	}
	
	public int getLang( ) {
		return( lang );
	}
	
	public void setConnection( PESConnection con ) {
		this.con = con;
	}
	
	public PESConnection getConnection( ) {
		return( this.con );
	}
	
	public void setIpInfo( IpInfo ipInfo ) {
		ip = ipInfo;
	}
	
	public IpInfo getIpInfo( ) {
		return( ip );
	}

	public synchronized void setAdmin( int value ) {
		admin = value;
	}
	
	public synchronized int getAdmin( ) {
		return( admin );
	}
	
	public synchronized boolean isAdmin() {
		return( admin == 1 ? true : false );
	}
	
	public synchronized void setLoggedIn() {
		this.logged = true;
	}

	public synchronized void setComment(String comment) {
		this.comment = comment;
	}

	public synchronized String getComment() {
		return (this.comment);
	}

	public synchronized boolean isLoggedIn() {
		return (this.logged);
	}

	public synchronized void setSettings(byte[] data) {
		if (data != null && data.length > 0)
			this.settings = data;
	}
	
	public synchronized void resetSettings( ) {
		this.settings = new byte[0];
	}
	
	public synchronized void insertSettings( byte[] data ) {
		byte[] newSettings = new byte[this.settings.length + data.length];
		System.arraycopy( this.settings, 0, newSettings, 0, this.settings.length);
		System.arraycopy( data, 0, newSettings, this.settings.length, data.length);
		this.settings = newSettings;
	}

	public synchronized byte[] getSettings() {
		return (this.settings);
	}

	public synchronized long getId() {
		return (id);
	}

	public synchronized String getName() {
		return (name);
	}

	public int getMatchesPlayed() {
		return matchesPlayed;
	}

	public void setMatchesPlayed(int matchesPlayed) {
		this.matchesPlayed = matchesPlayed;
	}

	public long getPoints() {
		return points;
	}

	public void setPoints(long points) {
		this.points = points;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		if( category < 0 ) this.category = 0;
		else this.category = category;
	}

	public int getVictories() {
		return victories;
	}

	public void setVictories(int victories) {
		this.victories = victories;
	}

	public int getDefeats() {
		return defeats;
	}

	public void setDefeats(int defeats) {
		this.defeats = defeats;
	}

	public int getDraws() {
		return draws;
	}

	public void setDraws(int draws) {
		this.draws = draws;
	}

	public int getWinningStreak() {
		return winningStreak;
	}

	public void setWinningStreak(int winningStreak) {
		this.winningStreak = winningStreak;
	}

	public int getBestStreak() {
		return bestStreak;
	}

	public void setBestStreak(int bestStreak) {
		this.bestStreak = bestStreak;
	}

	public int getDecos() {
		return decos;
	}

	public void setDecos(int decos) {
		this.decos = decos;
	}

	public long getGoalsScored() {
		return goalsScored;
	}

	public void setGoalsScored(long goalsScored) {
		this.goalsScored = goalsScored;
	}

	public long getGoalsReceived() {
		return goalsReceived;
	}

	public void setGoalsReceived(long goalsReceived) {
		this.goalsReceived = goalsReceived;
	}

	public long getTimePlayed() {
		return timePlayed;
	}

	public void setTimePlayed(long timePlayed) {
		this.timePlayed = timePlayed;
	}

	public int getDivision() {
		return division;
	}

	public void setDivision(int division) {
		this.division = division;
	}

	public int getTeam1() {
		if( teams.size() > 0 ) return( teams.get(0).intValue() );
		else return 0xFFFF;
	}

	public int getTeam2() {
		if( teams.size() > 1 ) return( teams.get(1).intValue() );
		else return 0xFFFF;
	}

	public int getTeam3() {
		if( teams.size() > 2 ) return( teams.get(2).intValue() );
		else return 0xFFFF;
	}

	public int getTeam4() {
		if( teams.size() > 3 ) return( teams.get(3).intValue() );
		else return 0xFFFF;
	}

	public int getTeam5() {
		if( teams.size() > 4 ) return( teams.get(4).intValue() );
		else return 0xFFFF;
	}

	public void pushTeam( int team ) {
		if( teams.size() >= 5 )
			teams.remove(0);
		
		teams.push(new Integer( team ));
	}
	
	public long getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public int getOldCategory() {
		return oldCategory;
	}

	public void setOldCategory(int oldCategory) {
		if( oldCategory < 0 ) this.oldCategory = 0;
		else this.oldCategory = oldCategory;
	}

	public int getMatchPoints() {
		return matchPoints;
	}

	public void setMatchPoints(int matchPoints) {
		this.matchPoints = matchPoints;
	}

	public long getOldPoints() {
		return oldPoints;
	}

	public void setOldPoints(long oldPoints) {
		this.oldPoints = oldPoints;
	}
}
