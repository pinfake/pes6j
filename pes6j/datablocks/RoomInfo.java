package pes6j.datablocks;

import java.util.Arrays;
import java.util.logging.Logger;

public class RoomInfo {
	long id;
	String name;
	int type;
	long[] pId;
	String password;
	boolean hasPassword;
	int[] pSelOrder;
	int[] readyStatus;
	int team[];
	int teampos[];
	int gamePhase;
	int team1;
	int time;
	int team2;
	int half;
	int team1goals[];
	int team2goals[];
	int spect[];
	long lastBytes = 0x0000000000020100L;;
	byte[] matchSettings;
	int peopleOnMatch;
	ChannelInfo channel;

	public RoomInfo(long id, ChannelInfo channel, int type, boolean hasPassword, String password,
			String name, long playerIds[]) {
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.name = name;
		this.password = password;
		this.hasPassword = hasPassword;
		this.pId = playerIds;
		this.pSelOrder = new int[pId.length];
		this.readyStatus = new int[pId.length];
		this.team = new int[pId.length];
		this.teampos = new int[pId.length];
		Arrays.fill(pSelOrder, (0x00FF));
		Arrays.fill(readyStatus, -1);
		Arrays.fill(team, 0x000000FF);
		Arrays.fill(teampos, 0);
		this.spect = new int[pId.length];
		Arrays.fill(spect, 0);
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

		Arrays.fill(team2goals, 0);
	}
	
	public ChannelInfo getChannel() {
		return( this.channel );
	}
	
	public synchronized void setPlayerTeam(long pid, int team) {
		this.team[getPlayerIdx(pid)] = team;
	}

	public synchronized void setSpect(long pid, int spect) {
		this.spect[getPlayerIdx(pid)] = spect;
	}

	public synchronized int getSpect(long pid) {
		return (this.spect[getPlayerIdx(pid)]);
	}

	public synchronized int getSpectIdx(int idx) {
		return (this.spect[idx]);
	}

	public synchronized void setMatchSettings(byte[] data) {
		this.matchSettings = data;
	}

	public synchronized byte[] getMatchSettings() {
		return (this.matchSettings);
	}

	public synchronized boolean allTeamsReady() {
		if (this.team1 == 0x0000FFFF || this.team2 == 0x0000FFFF)
			return false;
		return (true);
	}

	synchronized int getGoalIdxForHalf(int half) {
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
	
	public synchronized void addTeam1goal(int half) {
		this.team1goals[getGoalIdxForHalf(half)]++;
	}

	public synchronized void addTeam2goal(int half) {
		this.team2goals[getGoalIdxForHalf(half)]++;
	}

	public synchronized int getTeam1Goals(int idx) {
		return (this.team1goals[idx]);
	}

	public synchronized int getTeam2Goals(int idx) {
		return (this.team2goals[idx]);
	}

	public synchronized void resetGoals() {
		Arrays.fill(team2goals, 0);
		Arrays.fill(team1goals, 0);
	}

	public synchronized void setPlayerTeamPos(long pid, int teampos) {
		this.teampos[getPlayerIdx(pid)] = teampos;
	}

	public synchronized int getPlayerTeam(long pid) {
		return (this.team[getPlayerIdx(pid)]);
	}
	
	public long[] getPlayersFromTeam( int team ) {
		int count = 0;
		for( int i = 0; i < pId.length; i++ ) {
			if( this.team[i] == team ) count++;
		}
		long ret[] = new long[count];
		count = 0;
		for( int i = 0; i < pId.length; i++ ) {
			if( this.team[i] == team ) ret[count++] = pId[i];
		}
		return( ret );
	}

	public synchronized int getPlayerTeamPos(long pid) {
		return (this.teampos[getPlayerIdx(pid)]);
	}

	public synchronized void setTeam2(int bytes) {
		this.team2 = bytes;
	}

	public synchronized void setHalf(int half) {
		this.half = half;
	}

	public synchronized int getHalf() {
		return (this.half);
	}

	public synchronized int getTeam2() {
		return (this.team2);
	}

	public synchronized void setTime(int time) {
		this.time = time;
	}

	public synchronized int getTime() {
		return (this.time);
	}

	public synchronized void setTeam1(int time) {
		this.team1 = time;
	}

	public synchronized void setLastBytes(long value) {
		this.lastBytes = value;
	}

	public synchronized long getLastBytes() {
		return (this.lastBytes);
	}

	public synchronized int getTeam1() {
		return (this.team1);
	}

	public synchronized void setType(int type) {
		this.type = type;
	}

	public synchronized void setGamePhase(int phase) {
		this.gamePhase = phase;
	}

	public synchronized int getGamePhase() {
		return (this.gamePhase);
	}

	public synchronized void setPassworded(boolean pwd) {
		this.hasPassword = pwd;
	}

	public synchronized String getPassword() {
		return (password);
	}

	public synchronized void setPassword(String password) {
		this.password = password;
	}

	public synchronized void setName(String name) {
		this.name = name;
	}

	public synchronized long getId() {
		return (id);
	}

	public synchronized int getType() {
		return (type);
	}

	public synchronized String getName() {
		return (name);
	}

	public synchronized int getPlayerIdx(long pid) {
		for (int j = 0; j < getNumPlayers(); j++) {
			if (getPlayerId(j) == pid)
				return (j);
		}
		return (-1);
	}

	public synchronized long getPlayerIdOnSel(int sel) {
		for (int i = 0; i < getNumPlayers(); i++) {
			if (pSelOrder[i] == sel)
				return (pId[i]);
		}
		return (0x00000000);
	}

	public synchronized int getNumPlayersOnSel() {
		int count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (pSelOrder[i] != 0x00FF)
				count++;
		}
		return (count);
	}

	/*
	 * OJO, NO SOLO SWAPEAR LOS IDS, SINO EL RESTO DE LA INFORMACION AS WELL!!!
	 */
	public synchronized void swapPositions(long pid1, long pid2) {
		int idx1 = getPlayerIdx(pid1);
		int idx2 = getPlayerIdx(pid2);
		
		/*
		 * If the we haven't that player in the room
		 * we wont swap anything.
		 */
		if( idx2 == -1 || idx1 == -1 ) return;
		
		int pSelOrder_1 = pSelOrder[idx1];
		int pSelOrder_2 = pSelOrder[idx2];
		
		
		pId[idx1] = pid2;
		pSelOrder[idx1] = pSelOrder_2;
		pSelOrder[idx2] = pSelOrder_1;
		pId[idx2] = pid1;
	}

	public synchronized int getNumPlayers() {
		return (pId.length );
	}

	public synchronized long getPlayerId(int idx) {
		if (idx > pId.length)
			return 0;
		return (pId[idx]);
	}

	public synchronized int getPlayerSel(int idx) {
		if (idx > pSelOrder.length)
			return 0x00FF;
		return (pSelOrder[idx]);
	}
	
	public int getPlayerSelFromPid( long pid ) {
		int idx = getPlayerIdx( pid );
		return(pSelOrder[idx]);
	}

	public synchronized void setPlayerSel(long pid, int selOrder) {
		int idx = getPlayerIdx(pid);
		pSelOrder[idx] = selOrder;
	}

	public synchronized void setReadyStatus(long pid, int status) {
		int idx = getPlayerIdx(pid);
		readyStatus[idx] = status;
	}

	public synchronized int getReadyStatus(long pid) {
		int idx = getPlayerIdx(pid);
		return (readyStatus[idx]);
	}

	public synchronized boolean allReadyOn( int sel ) {
		//System.out.println( "*** Comprobando si están todos listos");
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerSel(i) != 0x00FF) {
				//System.out.println( "Paso y player sel vale: " + getPlayerSel(i));
				long pid = getPlayerId(i);
				if (getReadyStatus(pid) != sel) {
					//System.out.println( "alguno no está listo, devuelvo false");
					return (false);
				}
			}
		}
		//System.out.println( "todos listos, adelante!");
		return (true);
	}

	public synchronized void setPlayerSel(long pid) {
		int ultimo = -1;
		for (int i = 0; i < pSelOrder.length; i++) {
			if (pSelOrder[i] != 0x00FF) {
				if (pSelOrder[i] > ultimo)
					ultimo = pSelOrder[i];
			}
		}
		ultimo++;
		setPlayerSel(pid, ultimo);
	}
	
	public synchronized boolean hasPassword() {
		return (hasPassword);
	}

	public synchronized void removePlayer(long pid) {
		long ret[] = new long[pId.length - 1];
		int sel[] = new int[pSelOrder.length - 1];
		int ready[] = new int[readyStatus.length - 1];
		int n_team[] = new int[pId.length - 1];
		int n_teampos[] = new int[pId.length - 1];
		int n_spect[] = new int[pId.length - 1];
		int idx = 0;
		boolean found = false;
		int selOrderGone = 0;
		for (int i = 0; i < pId.length; i++) {
			if (pId[i] != pid) {
				ret[idx] = pId[i];
				sel[idx] = pSelOrder[i];
				ready[idx] = readyStatus[i];
				n_team[idx] = team[i];
				n_teampos[idx] = teampos[i];
				n_spect[idx] = spect[i];
				idx++;
			} else {
				selOrderGone = pSelOrder[i];
				found = true;
			}
		}

		if (found) {
			pId = ret;
			readyStatus = ready;
			team = n_team;
			teampos = n_teampos;
			spect = n_spect;
			/*
			 * Arreglamos el orden se nos ha ido uno que participaba
			 */
			if (selOrderGone != 0x00FF) {
				for (int i = 0; i < sel.length; i++) {
					if (sel[i] > selOrderGone)
						sel[i] = sel[i] - 1;
				}
			}
			pSelOrder = sel;
		}
	}

	public synchronized void removePlayerSel(long pid) {
		int sel = getPlayerSel(getPlayerIdx(pid));
		setPlayerSel(pid, 0x00FF);
		for (int i = 0; i < pSelOrder.length; i++) {
			if (pSelOrder[i] != 0x00FF) {
				if (pSelOrder[i] > sel)
					pSelOrder[i] = pSelOrder[i] - 1;
			}
		}
	}

	public synchronized void resetAll() {
		this.team1 = 0x0000FFFF;
		//this.lastBytes = 0x00000000;
		this.lastBytes = 0x0000000000020100L;
		this.time = 0;
		this.team2 = 0x0000FFFF;
		this.half = 0;
		// this.gamePhase = 0x0100;
		for (int i = 0; i < getNumPlayers(); i++) {
			pSelOrder[i] = 0x00FF;
			readyStatus[i] = -1;
			team[i] = 0x00FF;
			teampos[i] = 0;
			spect[i] = 0;
		}
		Arrays.fill(team1goals, 0);
		Arrays.fill(team2goals, 0);
		this.matchSettings = null;
	}

	public synchronized void resetAllReady() {
		for (int i = 0; i < getNumPlayers(); i++) {
			readyStatus[i] = -1;
		}
	}
	
	public void resetAllSels() {
		for (int i = 0; i < getNumPlayers(); i++) {
			pSelOrder[i] = 0x00FF;
		}
	}

	public synchronized long[] getPlayerIds() {
		return (pId);
	}
	
	public long[] getPlayerIdsNotInMatch() {
		if( getType() == 1 ) return( pId );
		int count = 0;
		for( int i = 0; i < pId.length; i++ ) {
			if( getPlayerSelFromPid( pId[i]) == (0x00FF)) count++;
		}
		long[] ret = new long[count];
		count = 0;
		for( int i = 0; i < pId.length; i++ ) {
			if( getPlayerSelFromPid( pId[i]) == (0x00FF)) ret[count++] = pId[i];
		}
		return( ret );
	}

	public synchronized long[] getPlayerIdsOnSel() {
		long ret[] = new long[getNumPlayersOnSel()];
		for (int i = 0; i < getNumPlayersOnSel(); i++) {
			ret[i] = getPlayerIdOnSel(i);
		}
		return (ret);
	}
	
	public synchronized long[] getPlayerIdsOnSelExcept( long pid ) {
		long ret[] = new long[getNumPlayersOnSel()-1];
		int j = 0;
		for (int i = 0; i < getNumPlayersOnSel(); i++) {
			long id = getPlayerIdOnSel(i);
			if( pid != id && j < ret.length )
				ret[j++] = getPlayerIdOnSel(i);
		}
		return (ret);
	}

	public synchronized long[] getPlayerIdsWatching() {

		int count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (spect[i] == 1)
				count++;
		}

		long[] ret = new long[count];
		count = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (spect[i] == 1)
				ret[count++] = getPlayerId(i);
		}
		return (ret);
	}

	public synchronized long[] getPlayerIdsExcept(long pid) {
		long ret[] = new long[getNumPlayers() - 1];
		int idx = 0;
		for (int i = 0; i < getNumPlayers(); i++) {
			if (getPlayerId(i) != pid)
				ret[idx++] = getPlayerId(i);
		}
		return (ret);
	}

	public synchronized void addPlayer(long pid) {
		long ret[] = new long[pId.length + 1];
		int sel[] = new int[pSelOrder.length + 1];
		int ready[] = new int[readyStatus.length + 1];
		int n_team[] = new int[pId.length + 1];
		int n_teampos[] = new int[pId.length + 1];
		int n_spect[] = new int[pId.length + 1];
		System.arraycopy(pId, 0, ret, 0, pId.length);
		System.arraycopy(pSelOrder, 0, sel, 0, pSelOrder.length);
		System.arraycopy(readyStatus, 0, ready, 0, readyStatus.length);
		System.arraycopy(team, 0, n_team, 0, team.length);
		System.arraycopy(teampos, 0, n_teampos, 0, teampos.length);
		System.arraycopy(spect, 0, n_spect, 0, spect.length);
		ret[pId.length] = pid;
		sel[pSelOrder.length] = 0x00FF;
		ready[readyStatus.length] = -1;
		n_team[team.length] = 0;
		n_teampos[teampos.length] = 0;
		n_spect[spect.length] = 0;
		pId = ret;
		pSelOrder = sel;
		readyStatus = ready;
		team = n_team;
		teampos = n_teampos;
		spect = n_spect;
	}

	public int getPeopleOnMatch() {
		return peopleOnMatch;
	}

	public void setPeopleOnMatch(int peopleOnMatch) {
		this.peopleOnMatch = peopleOnMatch;
	}
}
