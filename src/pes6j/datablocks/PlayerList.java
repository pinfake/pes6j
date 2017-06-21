package pes6j.datablocks;

import java.util.ArrayList;
import java.util.Vector;

public class PlayerList {
	Vector<PlayerInfo> players;

	public PlayerList() {
		players = new Vector<PlayerInfo>();
	}

	public void addPlayer(PlayerInfo pl) {
		players.add(pl);
	}

	public void removePlayer(long pid) {
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if (p.getId() == pid) {
				players.remove(i);
				break;
			}
		}
	}

	public PlayerInfo getPlayer( int idx ) {
		return( players.get( idx ));
	}
	
	public PlayerInfo getPlayerInfo(long pid) {
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if (p.getId() == pid) {
				return (p);
			}
		}
		return (null);
	}

	public PlayerInfo[] getPlayerArray() {
		PlayerInfo pl[] = new PlayerInfo[players.size()];
		for (int i = 0; i < players.size(); i++) {
			pl[i] = (PlayerInfo) players.get(i);
		}
		return (pl);
	}
	
	public Long[] getPidsInGroup( long gid ) {
		ArrayList<Long> al = new ArrayList<Long>();
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if( p.getGid() == gid ) al.add( p.getId());
		}
		return( (Long[])al.toArray() );
	}
	
	public PlayerInfo getPlayerInfo(String name) {
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if (p.getName().equals(name)) {
				return (p);
			}
		}
		return (null);
	}
	
	public int getNumPlayers() {
		return (players.size());
	}

}
