package pes6j.datablocks;

import java.util.Vector;

public class PlayerList {
	Vector<PlayerInfo> players;

	public PlayerList() {
		players = new Vector<PlayerInfo>();
	}

	public synchronized void addPlayer(PlayerInfo pl) {
		players.add(pl);
	}

	public synchronized void removePlayer(long pid) {
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
	
	public synchronized PlayerInfo getPlayerInfo(long pid) {
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if (p.getId() == pid) {
				return (p);
			}
		}
		return (null);
	}

	public synchronized PlayerInfo[] getPlayerArray() {
		PlayerInfo pl[] = new PlayerInfo[players.size()];
		for (int i = 0; i < players.size(); i++) {
			pl[i] = (PlayerInfo) players.get(i);
		}
		return (pl);
	}
	
	public synchronized PlayerInfo getPlayerInfo(String name) {
		for (int i = 0; i < players.size(); i++) {
			PlayerInfo p = (PlayerInfo) players.get(i);
			if (p.getName().equals(name)) {
				return (p);
			}
		}
		return (null);
	}
	
	public synchronized int getNumPlayers() {
		return (players.size());
	}

}
