package pes6j.datablocks;

import java.util.ArrayList;
import java.util.Arrays;

public class RoomInfo {
	
	public static final long TIME_TO_SEND_PIN_CODES = 1 * 60 * 1000;
	
	long id;
	String name;
	int type;
	String password;
	boolean hasPassword;
	int gamePhase;
	int team1;
	int time;
	int team2;
	int half;
	int team1goals[];
	int team2goals[];
	int team1goalsBackup[];
	int team2goalsBackup[];
	long lastBytes = 0x00020100L;
	byte[] matchSettings;
	int peopleOnMatch;
	ChannelInfo channel;
	long timestampLagged = 0;
	ArrayList<RoomPlayerData> current;
	ArrayList<RoomPlayerData> playerData;
	ArrayList<RoomPlayerData> playerDataCache;
	
	private RoomPlayerData searchByPid( long pid ) {
		for( int i = 0; i < current.size(); i++ ) {
			RoomPlayerData rpd = current.get(i);
			if( rpd.getPlayer().getId() == pid ) return( rpd );
		}
		return( null );
	}
	
	public void backupGoals() {
		team1goalsBackup = team1goals.clone();
		team2goalsBackup = team2goals.clone();
	}
	
//	public boolean isAGroupMatch() {
//		long team0gid = 0;
//		long team1gid = 0;
//		if( gamePhase == 7 ) {
//			for( int i = 0; i < current.size(); i++ ) {
//				RoomPlayerData rpd = current.get(i);
//				if( rpd.getSelOrder() != 0x00FF && rpd.getPlayer().getGid() != 0 ) {
//					if( rpd.getTeam() == 0 ) {
//						
//					}
//				}
//			}
//		}
//	}
	
	public void restoreGoals() {
		team1goals = team1goalsBackup.clone();
		team2goals = team2goalsBackup.clone();
	}
	
	public long getTimestampLagged() {
		return( timestampLagged );
	}
	
	public void setTimestamplagged( long ts ) {
		timestampLagged = ts;
	}
	
	public void restorePlayerSelsFromCache( ) {
		if( playerDataCache == null ) return;
		for( int i = 0; i < getNumPlayers(); i++ ) {
			setUseCacheData();
			RoomPlayerData rpd = searchByPid(playerData.get(i).getPlayer().getId());
			if( rpd != null ) {
				int sel = rpd.getSelOrder();
				playerData.get(i).setSelOrder(sel);
			}
			setUseRealData();
			
		}
		//setUseCacheData();
		//System.out.println( "EN CACHE TENIA " + getNumPlayersOnSel() + " PLAYERS ON SEL");
		//setUseRealData();
		//System.out.println( "EN REAL AHORA TENGO " + getNumPlayersOnSel() + " PLAYERS ON SEL");
	}
	
	public void saveCache() {
		playerDataCache = new ArrayList<RoomPlayerData>();
		for( int i = 0; i < playerData.size(); i++ ) {
			playerDataCache.add( (RoomPlayerData)playerData.get(i).clone() );
		}
	}
	
	public boolean cacheClean() {
		return( playerDataCache == null );
	}
	
	public void resetCache() {
		playerDataCache = null;
	}
	
	public void setUseCacheData() {
		current = playerDataCache;
	}
	
	public void setUseRealData() {
		current = playerData;
	}
	
	public PlayerInfo getPlayerFromPid( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getPlayer() );
		return( null );
	}
	
	public PlayerInfo getPlayerFromIdx( int idx ) {
		if(idx < current.size()) return( current.get(idx).getPlayer());
		return( null );
	}
	
	public void resetTeams() {
		this.team1 = 0x0000FFFF;
		this.team2 = 0x0000FFFF;
//		for( int i = 0; i < current.size(); i++ ) {
//			RoomPlayerData rpd = current.get(i);
//			rpd.setTeam(0x000000FF);
//		}
	}
	
	public RoomInfo(long id, ChannelInfo channel, int type, boolean hasPassword, String password,
			String name, PlayerInfo p ) {
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.name = name;
		this.password = password;
		this.hasPassword = hasPassword;
		
		playerData = new ArrayList<RoomPlayerData>();
		playerData.add( new RoomPlayerData(p));
		
		this.matchSettings = null;
		this.gamePhase = 0;
		this.team1 = 0x0000FFFF;
		this.lastBytes = 0L;
		this.lastBytes = 0x0000000000020100L;
		this.time = 0;
		this.team2 = 0x0000FFFF;
		this.half = 0;
		this.team1goals = new int[5];
		Arrays.fill(team1goals, 0);
		this.team2goals = new int[5];
		this.peopleOnMatch = 0;
		this.playerDataCache = null;
		Arrays.fill(team2goals, 0);
		timestampLagged = 0;
		current = playerData;
	}
	
	public String generatePinCode( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.generatePinCode());
		return( null );
	}
	
	public void clearPinCodes( ) {
		for( int i = 0; i < getNumPlayers(); i++ ) {
			RoomPlayerData rpd = current.get(i);
			if( rpd.getSelOrder() != 0x00FF )
				rpd.setPinCode(null);
		}
	}
	
	public String getPinCode( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getPinCode());
		return( null );
	}
	
	public boolean teamSentMessage( int team ) {
		if( current == null ) return false;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getTeam() == team && !current.get(i).getCodeSent() ) return false;
		}
		return( true );
	}
	
	public boolean getCodeSent( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getCodeSent());
		return( false );
	}
	
	public void setCodeSent( long pid, boolean sent ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setCodeSent(sent);
	}
	
	public void setTeamCodeSent( int team, boolean sent ) {
		if( current == null ) return;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getTeam() == team ) current.get(i).setCodeSent(sent);
		}
	}
	
	public ChannelInfo getChannel() {
		return( this.channel );
	}
	
	public void setPlayerTeam(long pid, int team) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setTeam(team);
	}

	public void setSpect(long pid, int spect) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setSpect(spect);
	}

	public int getSpect(long pid) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getSpect());
		return (0);
	}

	public  int getSpectIdx(int idx) {
		if( current == null ) return 0;
		if( idx >= current.size() ) return( 0 );
		RoomPlayerData rpd = current.get(idx);
		return (rpd.getSpect());
	}

	public void setMatchSettings(byte[] data) {
		this.matchSettings = data;
	}

	public byte[] getMatchSettings() {
		return (this.matchSettings);
	}

	public boolean allTeamsReady() {
		if (this.team1 == 0x0000FFFF || this.team2 == 0x0000FFFF)
			return false;
		return (true);
	}

	 int getGoalIdxForHalf(int half) {
		switch (half) {
		case 1:
			return (0);
		case 3:
			return (1);
		case 5:
			return (2);
		case 7:
			return (3);
		case 9:
			return (4);
		default:
			return (0);
		}
	}
	
	public int getTeam1Goals( ) {
		int ret = 0;
		/*
		 * We don't care about goals scored on penalties
		 */
		for( int i = 0; i < 4; i++ ) {
			ret += team1goals[i];
		}
		
		return( ret );
	}
	
	/*
	 * ESTA ALREVES OJO SI GANA EL EQUIPO 0 esto devuelve un 1
	 */
	public int getWinner( ) {
		int team1g = getTeam1Goals();
		int team2g = getTeam2Goals();

		if( team1goals[4] > team2goals[4] ) team1g++;
		else if( team2goals[4] > team1goals[4] ) team2g++;
		
		if( team1g > team2g ) return 1;
		else if( team2g > team1g ) return 0;
		else return( 2 );
	}
	
	public int getTeam1TotalGoals( ) {
		int ret = 0;
		/*
		 * We don't care about goals scored on penalties
		 */
		for( int i = 0; i < 5; i++ ) {
			ret += team1goals[i];
		}
		return( ret );
	}
	
	public int getTeam2Goals( ) {
		int ret = 0;
		/*
		 * We don't care about goals scored on penalties
		 */
		for( int i = 0; i < 4; i++ ) {
			ret += team2goals[i];
		}
		
		return( ret );
	}
	
	public void setTeam2Goals( int goals ) {
		/*
		 * We set the goals as if they were scored on the first half
		 * reset the rest of goals to 0
		 */
		team2goals[0] = goals;
		for( int i = 1; i < 5; i++ ) {
			team2goals[i] = 0;
		}
	}
	
	public void setTeam1Goals( int goals ) {
		/*
		 * We set the goals as if they were scored on the first half
		 * reset the rest of goals to 0
		 */
		team1goals[0] = goals;
		for( int i = 1; i < 5; i++ ) {
			team1goals[i] = 0;
		}
	}
	
	public int getTeam2TotalGoals( ) {
		int ret = 0;
		/*
		 * We don't care about goals scored on penalties
		 */
		for( int i = 0; i < 4; i++ ) {
			ret += team2goals[i];
		}
		return( ret );
	}
	
	public void addTeam1goal(int half) {
		this.team1goals[getGoalIdxForHalf(half)]++;
	}

	public void addTeam2goal(int half) {
		this.team2goals[getGoalIdxForHalf(half)]++;
	}

	public int getTeam1Goals(int idx) {
		return (this.team1goals[idx]);
	}

	public int getTeam2Goals(int idx) {
		return (this.team2goals[idx]);
	}

	public void resetGoals() {
		Arrays.fill(team2goals, 0);
		Arrays.fill(team1goals, 0);
	}

	public void setPlayerTeamPos(long pid, int teampos) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setTeamPos(teampos);
	}

	public int getPlayerTeam(long pid) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getTeam() );
		return( 0x000000FF );
	}
	
	public long[] getPlayersFromTeam( int team ) {
		if( current == null ) return( new long[0] );
		int count = 0;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getTeam() == team ) count++;
		}
		long ret[] = new long[count];
		count = 0;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getTeam() == team ) ret[count++] = current.get(i).getPlayer().getId();
		}
		return( ret );
	}
	
	public boolean allPlayersOnSelOnline( ) {
		long pids[] = getPlayerIdsOnSel();
		for( int i = 0; i < pids.length; i++ ) {
			if( channel.getPlayerList().getPlayerInfo(pids[i]) == null ) return( false );
		}
		return( true );
	}

	public int getPlayerTeamPos(long pid) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getTeamPos() );
		return(0);
	}

	public void setTeam2(int bytes) {
		this.team2 = bytes;
	}

	public void setHalf(int half) {
		this.half = half;
	}

	public int getHalf() {
		return (this.half);
	}

	public int getTeam2() {
		return (this.team2);
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getTime() {
		return (this.time);
	}

	public void setTeam1(int time) {
		this.team1 = time;
	}

	public void setLastBytes(long value) {
		this.lastBytes = value;
	}

	public long getLastBytes() {
		return (this.lastBytes);
	}

	public int getTeam1() {
		return (this.team1);
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setGamePhase(int phase) {
		this.gamePhase = phase;
	}
	
	public void setPlayersPhase( int phase ) {
		for( int i = 0; i < current.size(); i++ ) {
			current.get(i).setPlayerPhase( phase );
		}
	}
	

	
	public void setPlayerPhase( long pid, int phase ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setPlayerPhase(phase);
	}

	public int getPlayerPhase( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return(rpd.getPlayerPhase());
		return( 0 );
	}
	
	public int getGamePhase() {
		return (this.gamePhase);
	}

	public void setPassworded(boolean pwd) {
		this.hasPassword = pwd;
	}

	public String getPassword() {
		return (password);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return (id);
	}

	public int getType() {
		return (type);
	}

	public String getName() {
		return (name);
	}

	public int getPlayerIdx(long pid) {
		for (int j = 0; j < getNumPlayers(); j++) {
			if (getPlayerId(j) == pid)
				return (j);
		}
		return (-1);
	}

	public  long getPlayerIdOnSel(int sel) {
		for (int i = 0; i < getNumPlayers(); i++) {
			if (current.get(i).getSelOrder() == sel)
				return (current.get(i).getPlayer().getId());
		}
		return (0x00000000);
	}

	public  int getNumPlayersOnSel() {
		int count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (current.get(i).getSelOrder() != 0x00FF)
				count++;
		}
		return (count);
	}

	/*
	 * OJO, NO SOLO SWAPEAR LOS IDS, SINO EL RESTO DE LA INFORMACION AS WELL!!!
	 */
	public  void swapPositions(long pid1, long pid2) {
		if( current == null ) return;
		int idx1 = getPlayerIdx(pid1);
		int idx2 = getPlayerIdx(pid2);
		
		/*
		 * If the we haven't that player in the room
		 * we wont swap anything.
		 */
		if( idx2 == -1 || idx1 == -1 ) return;
		
		/*
		 * Ojo con esto! no se si estoy sobreescribiendo!
		 */
		RoomPlayerData rpd1 = current.get(idx1);
		RoomPlayerData rpd2 = current.get(idx2);
		current.set( idx1, rpd2 );
		current.set( idx2, rpd1 );
	}

	public  int getNumPlayers() {
		if( current == null ) return 0;
		return (current.size() );
	}

	public long getPlayerId(int idx) {
		if( current == null ) return 0;
		if( idx >= current.size() ) return( 0 );
		RoomPlayerData rpd = current.get(idx);
		return( rpd.getPlayer().getId() );
	}

	public  int getPlayerSel(int idx) {
		if( current == null ) return 0x00FF;
		if( idx >= current.size() ) return( 0x00FF );
		return (current.get(idx).getSelOrder());
	}
	
	public int getPlayerSelFromPid( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getSelOrder());
		return(0x00FF);
	}

	public boolean getWatchedScoresFromPid( long pid ) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getWatchedScores());
		return(false);
	}
	
	public void resetWatchedScores() {
		for( int i = 0; i < getNumPlayers(); i++ ) {
			current.get(i).setWatchedScores(false);
		}
	}
	
	public  void setPlayerSel(long pid, int selOrder) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setSelOrder(selOrder);
	}

	public  void setReadyStatus(long pid, int status) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) rpd.setReadyStatusPhase(status);
	}
	
	public void setReadyStatusOfTeam( int team, int status ) {
		for( int i = 0; i < getNumPlayers(); i++ ) {
			RoomPlayerData rpd = current.get(i);
			if( rpd != null && rpd.getSelOrder() != 0x00FF && rpd.getTeam() == team ) 
				rpd.setReadyStatusPhase( status );
		}
	}
	
	public void setPlayerPhaseOfTeam( int team, int phase ) {
		for( int i = 0; i < getNumPlayers(); i++ ) {
			RoomPlayerData rpd = current.get(i);
			if( rpd != null && rpd.getSelOrder() != 0x00FF && rpd.getTeam() == team ) 
				rpd.setPlayerPhase(phase);
		}
	}

	public  int getReadyStatus(long pid, int phase) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) return( rpd.getReadyStatusPhase( phase ));
		return(-1);
	}

	public boolean allReadyOnCacheOfSels( int phase, int sel ) {
		if( cacheClean() ) return( false );
		boolean atLeastOne = false;
		//System.out.println( "*** Comprobando si están todos listos");
		for (int i = 0; i < playerDataCache.size(); i++) {
			if (playerDataCache.get(i).getSelOrder() != 0x00FF) {
				//System.out.println( "Paso y player sel vale: " + getPlayerSel(i));
				RoomPlayerData rpd = searchByPid(playerDataCache.get(i).getPlayer().getId());
				if( rpd == null ) continue;
				if (rpd.getReadyStatusPhase(phase) != sel) {
					//System.out.println( "alguno no está listo, devuelvo false");
					return (false);
				} else
					atLeastOne = true;
			}
		}
		if( atLeastOne ) return(true);
		return (false);
	}
	
	public  boolean allReadyOn( int phase, int sel ) {
		boolean atLeastOne = false;
		//System.out.println( "*** Comprobando si están todos listos");
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerSel(i) != 0x00FF) {
				//System.out.println( "Paso y player sel vale: " + getPlayerSel(i));
				long pid = getPlayerId(i);
				if (getReadyStatus(pid, phase) != sel) {
					//System.out.println( "alguno no está listo, devuelvo false");
					return (false);
				} else
					atLeastOne = true;
			}
		}
		//System.out.println( "todos listos, adelante!");
		if( atLeastOne ) return(true);
		return (false);
	}
	
	public  boolean allTeamLeadersReady( int phase, int sel ) {
		boolean atLeastOne = false;
		//System.out.println( "*** Comprobando si están todos listos");
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerSel(i) != 0x00FF && current.get(i).getTeamPos() == 1) {
				//System.out.println( "Paso y player sel vale: " + getPlayerSel(i));
				long pid = getPlayerId(i);
				if (getReadyStatus(pid, phase) != sel) {
					//System.out.println( "alguno no está listo, devuelvo false");
					return (false);
				} else
					atLeastOne = true;
			}
		}
		//System.out.println( "todos listos, adelante!");
		if( atLeastOne ) return(true);
		return (false);
	}
	
	public boolean allPlayersFell() {
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerSel(i) != 0x00FF) {
				if( !current.get(i).getFell() ) return( false );
			}
		}
		return( true );
	}
	
	public void setPlayerFell(long pid, boolean value) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) {
			rpd.setFell(value);
		}
	}
	
	public boolean getPlayerFell(long pid) {
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd != null ) {
			return( rpd.getFell() );
		}
		return( false );
	}

	public  void setPlayerSel(long pid) {
		if( current == null ) return;
		int ultimo = -1;
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i).getSelOrder() != 0x00FF) {
				if (current.get(i).getSelOrder() > ultimo)
					ultimo = current.get(i).getSelOrder();
			}
		}
		ultimo++;
		setPlayerSel(pid, ultimo);
	}
	
	public  boolean hasPassword() {
		return (hasPassword);
	}

	public  void removePlayer(long pid) {
		if( current == null ) return;
		RoomPlayerData rpd = searchByPid(pid);
		if( rpd == null ) return;
		int selOrderGone = rpd.getSelOrder();
		int idx = getPlayerIdx(pid);
		current.remove(idx);
		/*
		 * Arreglamos el orden se nos ha ido uno que participaba
		 */
		if (selOrderGone != 0x00FF) {
			for (int i = 0; i < current.size(); i++) {
				if (current.get(i).getSelOrder() != 0x00FF && current.get(i).getSelOrder() > selOrderGone)
					current.get(i).setSelOrder(current.get(i).getSelOrder() - 1);
			}
		}
	}

	public  void removePlayerSel(long pid) {
		int sel = getPlayerSel(getPlayerIdx(pid));
		setPlayerSel(pid, 0x00FF);
		for (int i = 0; i < current.size(); i++) {
			if (current.get(i).getSelOrder() != 0x00FF) {
				if (current.get(i).getSelOrder() > sel)
					current.get(i).setSelOrder(current.get(i).getSelOrder() - 1);
			}
		}
	}

	public  void resetAll() {
		this.team1 = 0x0000FFFF;
		//this.lastBytes = 0x00000000;
		this.lastBytes = 0x0000000000020100L;
		this.time = 0;
		this.team2 = 0x0000FFFF;
		this.half = 0;
		// this.gamePhase = 0x0100;
		if( current != null )
			for (int i = 0; i < getNumPlayers(); i++) {
				current.get(i).reset();
			}
		Arrays.fill(team1goals, 0);
		Arrays.fill(team2goals, 0);
		this.matchSettings = null;
		this.playerDataCache = null;
		timestampLagged = 0;
		current = this.playerData;
	}
	
	public void resetAllButCache() {
		//this.team1 = 0x0000FFFF;
		//this.lastBytes = 0x00000000;
		this.lastBytes = 0x0000000000020100L;
		this.time = 0;
		//this.team2 = 0x0000FFFF;
		this.half = 0;
		// this.gamePhase = 0x0100;
		if( current != null )
			for (int i = 0; i < getNumPlayers(); i++) {
				current.get(i).reset();
			}
		//Arrays.fill(team1goals, 0);
		//Arrays.fill(team2goals, 0);
		this.matchSettings = null;
	}

	public  void resetAllReady( int phase ) {
		for (int i = 0; i < getNumPlayers(); i++) {
			//for( int j = phase; j >= 0; j-- )
			current.get(i).setReadyStatusPhase(phase, -1);
		}
	}
	
	public  void resetAllReady( ) {
		for (int i = 0; i < getNumPlayers(); i++) {
			current.get(i).resetAllReadyStatus();
		}
	}
	
	public void resetAllSels() {
		for (int i = 0; i < getNumPlayers(); i++) {
			current.get(i).setSelOrder(0x00FF);
		}
	}

	public  long[] getPlayerIds() {
		if( current == null ) return( new long[0]);
		long[] pids = new long[current.size()];
		for( int i = 0; i < current.size(); i++ ) {
			pids[i] = current.get(i).getPlayer().getId();
		}
		return (pids);
	}
	
	public long[] getPlayerIdsNotInMatch() {
		/*
		 * ESTO PUEDE ESTAR CAUSANDO LAS PANTALLAS NEGRAS QUE QUEDAN!!!!! PERO AL MISMO TIEMPO
		 * HAY MENSAJES QUE DEBEN RECEBIR ESTANDO EN ESTE ESTADO!.
		 */
		//if( getType() == 1 ) return( getPlayerIds() );
		if( current == null ) return( new long[0]);
		if( getGamePhase() < 2 ) return( getPlayerIds() );
		int count = 0;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getSelOrder() == (0x00FF)) count++;
		}
		long[] ret = new long[count];
		count = 0;
		for( int i = 0; i < current.size(); i++ ) {
			if( current.get(i).getSelOrder() == (0x00FF)) ret[count++] = current.get(i).getPlayer().getId();
		}
		return( ret );
	}

	public  long[] getPlayerIdsOnSel() {
		long ret[] = new long[getNumPlayersOnSel()];
		for (int i = 0; i < getNumPlayersOnSel(); i++) {
			ret[i] = getPlayerIdOnSel(i);
		}
		return (ret);
	}
	
	public  long[] getPlayerIdsOnSelExcept( long pid ) {
		long ret[] = new long[getNumPlayersOnSel()-1];
		int j = 0;
		for (int i = 0; i < getNumPlayersOnSel(); i++) {
			long id = getPlayerIdOnSel(i);
			if( pid != id && j < ret.length )
				ret[j++] = getPlayerIdOnSel(i);
		}
		return (ret);
	}

	public  long[] getPlayerIdsWatching() {

		int count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (current.get(i).getSpect() == 1)
				count++;
		}

		long[] ret = new long[count];
		count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (current.get(i).getSpect() == 1)
				ret[count++] = getPlayerId(i);
		}
		return (ret);
	}

	public  long[] getPlayerIdsExcept(long pid) {
		long ret[] = new long[getNumPlayers() - 1];
		int idx = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerId(i) != pid)
				ret[idx++] = getPlayerId(i);
		}
		return (ret);
	}

	public  void addPlayer(PlayerInfo p) {
		if( current == null ) return;
		RoomPlayerData rpd = new RoomPlayerData(p);
		current.add( rpd );
	}

	public int getPeopleOnMatch() {
		return peopleOnMatch;
	}

	public void setPeopleOnMatch(int peopleOnMatch) {
		this.peopleOnMatch = peopleOnMatch;
	}
}
