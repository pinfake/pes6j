package pes6j.datablocks;

import java.util.Vector;

public class RoomList {
	Vector<RoomInfo> v;

	public RoomList() {
		v = new Vector<RoomInfo>();
	}

	public synchronized void addRoom(RoomInfo room) {
		v.add(room);
	}

	public synchronized void removeRoom(long rid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getId() == rid) {
				v.remove(i);
				break;
			}
		}
	}

	public synchronized void removePlayerFromRoom(long rid, long pid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getId() == rid) {
				p.removePlayer(pid);
				//v.set(i, p);
			}
		}
	}

	public synchronized void addPlayerToRoom(long rid, long pid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getId() == rid) {
				p.addPlayer(pid);
				//v.set(i, p);
			}
		}
	}

	public synchronized void setPlayerSelInRoom(long pid, int sel) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getPlayerIdx(pid) != -1) {
				p.setPlayerSel(pid, sel);
				//v.set(i, p);
			}
		}
	}

	public synchronized void setPlayerSelInRoom(long pid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getPlayerIdx(pid) != -1) {
				p.setPlayerSel(pid);
				//v.set(i, p);
			}
		}
	}

	public synchronized void removePlayerSelInRoom(long pid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getPlayerIdx(pid) != -1) {
				p.removePlayerSel(pid);
				//v.set(i, p);
			}
		}
	}

	public synchronized RoomInfo getRoom(long rid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getId() == rid) {
				return (p);
			}
		}
		return (null);
	}

	public synchronized void setRoom(long rid, RoomInfo r) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			if (p.getId() == rid) {
				v.set(i, r);
			}
		}
	}

	public synchronized RoomInfo[] getRoomInfoArray() {
		RoomInfo pl[] = new RoomInfo[v.size()];
		for (int i = 0; i < v.size(); i++) {
			pl[i] = (RoomInfo) v.get(i);
		}
		return (pl);
	}
	
	public synchronized RoomInfo[] getRoomsInChannelArray( ChannelInfo channel ) {
		Vector<RoomInfo> ret = new Vector<RoomInfo>();
		
		for (int i = 0; i < v.size(); i++) {
			if( v.get(i).getChannel() == channel )
				ret.add( v.get(i));
		}
		return ((RoomInfo[])ret.toArray(new RoomInfo[0]));
	}

	public synchronized RoomInfo searchPlayer(long pid) {
		for (int i = 0; i < v.size(); i++) {
			RoomInfo p = (RoomInfo) v.get(i);
			for (int j = 0; j < p.getNumPlayers(); j++) {
				if (p.getPlayerId(j) == pid)
					return (p);
			}
		}
		return (null);
	}
}
