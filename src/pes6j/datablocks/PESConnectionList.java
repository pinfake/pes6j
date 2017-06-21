package pes6j.datablocks;

import java.io.IOException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;

public class PESConnectionList {
	Vector<PESConnection> cons;

	public PESConnectionList() {
		cons = new Vector<PESConnection>();
	}
	
	public Enumeration<PESConnection> elements() {
		return(cons.elements());
	}

	public synchronized void addConnection(PESConnection con) {
		cons.add(con);
	}

	public synchronized void removeConnection(PESConnection con) {
		cons.remove(con);
	}

	public synchronized void sendToAllEncoded(Message mes) throws IOException {
		for (int i = 0; i < cons.size(); i++) {
			((PESConnection) cons.get(i)).streamWriteEncoded(mes);
		}
	}
	
	public synchronized void sendToAllEncodedNIO(Message mes) throws IOException {
		for (int i = 0; i < cons.size(); i++) {
			((PESConnection) cons.get(i)).streamWriteEncodedNIO(mes);
		}
	}
	
	public void sendToEncodedNIO( PESConnection[] conList, Message mes ) throws IOException {
		for( int i = 0; i < conList.length; i++ ) {
			conList[i].streamWriteEncodedNIO(mes);
		}
	}
	
	public void sendToChannelEncodedNIO( ChannelInfo chan, Message mes ) throws IOException {
		PlayerList players = chan.getPlayerList();
		for( int i = 0; i < players.getNumPlayers(); i++ ) {
			PESConnection c = players.getPlayer( i ).getConnection();
			try {			
				c.streamWriteEncodedNIO( mes );
			} catch( IOException ex ) {
			}
		}
	}
	
	/*
	 * No se lo enviamos a los que están ya en una sala
	 */
	public void sendToChannelNotInGameEncodedNIO( ChannelInfo chan, Message mes ) throws IOException {
		PlayerList players = chan.getPlayerList();
		RoomList rooms = chan.getRoomList();
		for( int i = 0; i < players.getNumPlayers(); i++ ) {
			PlayerInfo p = players.getPlayer(i);
			RoomInfo r = rooms.searchPlayer( p.getId() );
			if( r == null ||r.getType() <= 1 || r.getPlayerSelFromPid( p.getId() ) == (0x00FF) ) { 
				PESConnection c = p.getConnection();
				try {			
					c.streamWriteEncodedNIO( mes );
				} catch( IOException ex ) {
				}
			}
		}
	}
	
	
	/*
	 * No se lo enviamos a los que están ya en una sala
	 */
	public void sendToChannelNotInRoomEncodedNIO( ChannelInfo chan, Message mes ) throws IOException {
		PlayerList players = chan.getPlayerList();
		RoomList rooms = chan.getRoomList();
		for( int i = 0; i < players.getNumPlayers(); i++ ) {
			PlayerInfo p = players.getPlayer(i);
			RoomInfo r = rooms.searchPlayer( p.getId() );
			if( r == null ) { 
				PESConnection c = p.getConnection();
				try {			
					c.streamWriteEncodedNIO( mes );
				} catch( IOException ex ) {
				}
			}
		}
	}

	public synchronized PESConnection searchConnection(long pid) {
		for (int i = 0; i < cons.size(); i++) {
			PESConnection c = (PESConnection) cons.get(i);
			if (c.getPid() == pid)
				return (c);
		}
		return (null);
	}
	
	public synchronized PESConnection searchConnection( Socket socket ) {
		for (int i = 0; i < cons.size(); i++) {
			PESConnection c = (PESConnection) cons.get(i);
			if (c.getSocket() == socket)
				return (c);
		}
		return (null);
	}

	public void sendEncodedTo(long[] pids, Message mes) throws IOException {
		for (int i = 0; i < pids.length; i++) {
			PESConnection c = searchConnection(pids[i]);
			if (c != null) {
				c.streamWriteEncoded(mes);
			}
		}
	}
	
	public void sendEncodedToNIO(long[] pids, Message mes) throws IOException {
		for (int i = 0; i < pids.length; i++) {
			PESConnection c = searchConnection(pids[i]);
			if (c != null) {
				c.streamWriteEncodedNIO(mes);
			}
		}
	}
	
	public void sendEncodedToGroup( long gid, Message mes ) throws IOException {
		for( int i = 0; i < size(); i++ ) {
			PESConnection c = cons.get(i);
			if( c.getPlayerInfo() != null && 
				c.getPlayerInfo().getGid() != 0 && 
				c.getPlayerInfo().getGid() == gid) {
				c.streamWriteEncodedNIO(mes);
			}
		}
	}
	
	public int size() {
		return( cons.size() );
	}
}
