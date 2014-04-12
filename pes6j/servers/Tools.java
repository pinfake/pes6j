package pes6j.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import pes6j.database.Db;
import pes6j.database.Table;
import pes6j.datablocks.DataCdKeyAndPass;
import pes6j.datablocks.DataCrtPlayer;
import pes6j.datablocks.GroupInfo;
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.UserInfo;

public class Tools {
	public static long dbCreatePlayer(Db db, String username, DataCrtPlayer player, byte[] default_settings )
	throws SQLException {
		long pid = 0;
		PreparedStatement ps;
		try {
			//db.setAutoCommit(false);
			
			String query = "insert into players (name,settings) values (?,?) returning pid";
			ps = db.prepareStatement(query);
			
			db.setStatementValues(new Object[] { player.getName(), default_settings }, ps);
			
			Table t = db.executeQuery(ps);
		
			pid = t.getLong(0, "pid" );
			db.closeStatement(ps);
			
			switch (player.getPos()) {
			case 0:
				query = "update users set pid1 = ? where username = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						username }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			case 1:
				query = "update users set pid2 = ? where username = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						username }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			case 2:
				query = "update users set pid3 = ? where username = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						username }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			default:
				break;
			}
			//db.commit();
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {		
		}
		return (pid);
	}
	
	public static PlayerInfo dbGetPlayerSel(Db db, UserInfo uInfo, byte[] default_settings ) throws SQLException {
		PreparedStatement ps;
		PlayerInfo pInfo = null;

		try {
			String query = "select players.pid from players,users where players.pid = users.pid_sel and users.username = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { uInfo.getUsername() }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			/*
			 * Esta query a veces no da resultado, como es posible?
			 * java.lang.ArrayIndexOutOfBoundsException: 0 >= 0
			 * en la siguiente línea.
			 */
			pInfo = dbLoadPlayerAndGroupInfoPrivate( db, t.getLong( 0, "pid"), default_settings );
			pInfo.setAdmin( uInfo.getAccess() == 2 ? 1 : 0 );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (pInfo);
	}
	
	public static long dbCreatePlayer(Db db, DataCdKeyAndPass key, DataCrtPlayer player, byte[] default_settings )
	throws SQLException {
		long pid = 0;
		PreparedStatement ps;
		try {
			//db.setAutoCommit(false);
			
			String query = "insert into players (name,settings) values (?,?)";
			ps = db.prepareStatement(query);
			
			db.setStatementValues(new Object[] { player.getName(), default_settings }, ps);
			db.executeUpdate(ps);
			Table t = db.getKeys(ps);
			pid = t.getLong(0, "generated_key");
			db.closeStatement(ps);
			
			switch (player.getPos()) {
			case 0:
				query = "update users set pid1 = ? where cd_key = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						key.getCdKey() }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			case 1:
				query = "update users set pid2 = ? where cd_key = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						key.getCdKey() }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			case 2:
				query = "update users set pid3 = ? where cd_key = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(pid),
						key.getCdKey() }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				break;
			default:
				break;
			}
			//db.commit();
		} catch (SQLException ex) {
			ex.printStackTrace();
			//db.rollback();
			throw( ex );
		} finally {
		}
		return (pid);
	}
	
	public static PlayerInfo dbGetPlayerSel(Db db, DataCdKeyAndPass key, byte[] default_settings ) throws SQLException {
		PreparedStatement ps;
		PlayerInfo pInfo = null;
		GroupInfo gInfo = null;
		
		try {
			String query = "select players.* from players,users where players.pid = users.pid_sel and users.cd_key = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { key.getCdKey() }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			pInfo = new PlayerInfo(t.getLong(0, "pid"), t.get(0, "name"));
			
			byte[] settings = (byte[])t.getObject(0, "settings");
			if( settings == null || settings.length != 1300 ) {
				settings = default_settings;
			}
			pInfo.setSettings(settings);
			
			pInfo.setComment(t.get(0, "comment"));
			pInfo.setAdmin(t.getInt(0, "admin"));

			query = "select groups.* from groups,groups_players where groups_players.gid = groups.gid and "
					+ "groups_players.pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pInfo.getId() }, ps);
			t = db.executeQuery(ps);
			db.closeStatement(ps);

			if (t.size() > 0) {
				PlayerInfo[] players;
				query = "select players.* from players,groups_players where players.pid = groups_players.pid and groups_players.gid = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { t.getLong(0, "gid") }, ps);
				Table t2 = db.executeQuery(ps);
				db.closeStatement(ps);
				players = new PlayerInfo[t2.size()];
				for (int i = 0; i < t2.size(); i++) {
					players[i] = new PlayerInfo(t2.getLong(i, "pid"), t2.get(i,
							"name"));
				}

				gInfo = new GroupInfo(t.getLong(0, "gid"), t.get(0, "name"));
				gInfo.setPlayers(players);
				gInfo.setComment(t.get(0,"comment"));
				gInfo.setDefeats(t.getInt(0,"defeats"));
				gInfo.setDraws(t.getInt(0,"draws"));
				gInfo.setPosition(t.getLong(0,"position"));
				gInfo.setPoints(t.getLong(0,"points"));
				gInfo.setMatches(t.getInt(0, "matches"));
				gInfo.setVictories(t.getInt(0,"victories"));
				gInfo.setDraws(t.getInt(0,"draws"));
				gInfo.setSlots(t.getInt(0,"slots"));
				gInfo.setLevel(t.getInt(0,"level"));
			} else
				gInfo = new GroupInfo(0, "");
			pInfo.setGroupInfo( gInfo );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (pInfo);
	}
	
	public static void dbSavePlayerInfo( Db db, PlayerInfo player ) throws SQLException {
		PreparedStatement ps;
		
		long newTimePlayed;
		
		newTimePlayed = player.getTimePlayed();
		long now = new Date().getTime();
		newTimePlayed += (now - player.getLastLogin()) / 1000;
		
		try {
			
			String query = "update players set " +
					"comment = ?," +
					"settings = ?," +
					"category = ?," +
					"points = ?," +
					"matches_played = ?," +
					"victories = ?," +
					"defeats = ?," +
					"draws = ?," +
					"winning_streak = ?," +
					"best_streak = ?," +
					"decos = ?," +
					"goals_scored = ?," +
					"goals_received = ?," +
					"time_played = ?," +
					"team_1 = ?," +
					"team_2 = ?," +
					"team_3 = ?," +
					"team_4 = ?," +
					"team_5 = ?," +
					"last_saved = ? where pid = ?";
			
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { 
					player.getComment(),
					player.getSettings(),
					player.getCategory(),
					player.getPoints(),
					player.getMatchesPlayed(),
					player.getVictories(),
					player.getDefeats(),
					player.getDraws(),
					player.getWinningStreak(),
					player.getBestStreak(),
					player.getDecos(),
					player.getGoalsScored(),
					player.getGoalsReceived(),
					newTimePlayed,
					player.getTeam1(),
					player.getTeam2(),
					player.getTeam3(),
					player.getTeam4(),
					player.getTeam5(),
					now,
					player.getId()}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	static PlayerInfo dbLoadPlayerAndGroupInfoPrivate( Db db, long pid, byte[] default_settings ) throws SQLException {
		PreparedStatement ps;
		PlayerInfo pInfo = null;
		GroupInfo gInfo = null;
		
		String query = "select * from players where pid = ?";
		ps = db.prepareStatement(query);
		db.setStatementValues(new Object[] { new Long( pid ) }, ps);
		Table t = db.executeQuery(ps);
		db.closeStatement(ps);
		
		if( t.size() == 0 ) return null;
		
		pInfo = new PlayerInfo(t.getLong(0, "pid"), t.get(0, "name"));
		byte[] settings = (byte[])t.getObject(0, "settings");
		if( settings == null || settings.length != 1300 ) {
			settings = default_settings;
		}
		pInfo.setSettings(settings);
		
		pInfo.setBestStreak( t.getInt( 0, "best_streak"));
		pInfo.setCategory( t.getInt( 0, "category"));
		pInfo.setOldCategory( pInfo.getCategory() );
		pInfo.setDecos( t.getInt( 0, "decos"));
		pInfo.setDefeats( t.getInt( 0, "defeats"));
		pInfo.setDraws( t.getInt( 0, "draws"));
		pInfo.setGoalsReceived( t.getLong( 0, "goals_received"));
		pInfo.setGoalsScored( t.getLong( 0, "goals_scored"));
		pInfo.setMatchesPlayed( t.getInt( 0, "matches_played"));
		pInfo.setPoints( t.getLong( 0, "points"));
		pInfo.setOldPoints( pInfo.getPoints() );
		pInfo.setVictories( t.getInt( 0, "victories"));
		pInfo.setWinningStreak( t.getInt( 0, "winning_streak"));
		pInfo.setTimePlayed( t.getLong( 0, "time_played"));
		pInfo.setDivision( t.getInt( 0, "division"));
		pInfo.pushTeam( t.getInt( 0, "team_1"));
		pInfo.pushTeam( t.getInt( 0, "team_2"));
		pInfo.pushTeam( t.getInt( 0, "team_3"));
		pInfo.pushTeam( t.getInt( 0, "team_4"));
		pInfo.pushTeam( t.getInt( 0, "team_5"));
		pInfo.setPosition( t.getLong( 0, "position"));
		pInfo.setComment(t.get(0, "comment"));		

		query = "select groups.* from groups,groups_players where groups_players.gid = groups.gid and "
				+ "groups_players.pid = ?";
		ps = db.prepareStatement(query);
		db.setStatementValues(new Object[] { pInfo.getId() }, ps);
		t = db.executeQuery(ps);
		db.closeStatement(ps);

		if (t.size() > 0) {
			PlayerInfo[] players;
			query = "select players.* from players,groups_players where players.pid = groups_players.pid and groups_players.gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { t.getLong(0, "gid") }, ps);
			Table t2 = db.executeQuery(ps);
			db.closeStatement(ps);
			players = new PlayerInfo[t2.size()];
			for (int i = 0; i < t2.size(); i++) {
				players[i] = new PlayerInfo(t2.getLong(i, "pid"), t2.get(i,
						"name"));
			}

			gInfo = new GroupInfo(t.getLong(0, "gid"), t.get(0, "name"));
			gInfo.setPlayers(players);
		} else
			gInfo = new GroupInfo(0, "");
		
		pInfo.setGroupInfo( gInfo);
		
		return (pInfo);
	}
	
	public static PlayerInfo dbLoadPlayerAndGroupInfo(Db db, long pid, byte[] default_settings ) throws SQLException {
		PlayerInfo pInfo = null;
		try {
			pInfo = dbLoadPlayerAndGroupInfoPrivate( db, pid, default_settings );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (pInfo);
	}
	
	public static PlayerInfo[] dbLoadPlayers(Db db, String username) throws SQLException {
		PlayerInfo[] ret = new PlayerInfo[3];
		PreparedStatement ps;
		try {
			String query = "select * from users where username = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { username }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if (t.size() == 0) {
				query = "insert into users (username,pid1,pid2,pid3) values(?,?,?,?)";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { username,
						new Long(0x00000000), new Long(0x00000000),
						new Long(0x00000000) }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				ret[0] = new PlayerInfo(0x00000000, "");
				ret[1] = new PlayerInfo(0x00000000, "");
				ret[2] = new PlayerInfo(0x00000000, "");
			} else {
				if( t.getInt(0, "banned") == 1 )
					throw new SQLException("THE USER IS BANNED!!!");
				long p1 = t.getLong(0, "pid1");
				long p2 = t.getLong(0, "pid2");
				long p3 = t.getLong(0, "pid3");
				
				ret[0] = dbLoadPlayerAndGroupInfoPrivate( db, p1, null );
				if( ret[0] == null ) ret[0] = new PlayerInfo( 0, "" );
				ret[1] = dbLoadPlayerAndGroupInfoPrivate( db, p2, null );
				if( ret[1] == null ) ret[1] = new PlayerInfo( 0, "" );
				ret[2] = dbLoadPlayerAndGroupInfoPrivate( db, p3, null );
				if( ret[2] == null ) ret[2] = new PlayerInfo( 0, "" );
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
		}
		return (ret);
	}
	
	public static PlayerInfo[] dbLoadPlayers(Db db, DataCdKeyAndPass key) throws SQLException {
		PlayerInfo[] ret = new PlayerInfo[3];
		PreparedStatement ps;
		try {
			//db.setAutoCommit(false);
			String query = "select * from users where cd_key = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { key.getCdKey() }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if (t.size() == 0) {
				query = "insert into users (cd_key,pid1,pid2,pid3) values(?,?,?,?)";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { key.getCdKey(),
						new Long(0x00000000), new Long(0x00000000),
						new Long(0x00000000) }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				ret[0] = new PlayerInfo(0x00000000, "");
				ret[1] = new PlayerInfo(0x00000000, "");
				ret[2] = new PlayerInfo(0x00000000, "");
			} else {
				if( t.getInt(0, "banned") == 1 )
					throw new SQLException("THE USER IS BANNED!!!");
				long p1 = t.getLong(0, "pid1");
				long p2 = t.getLong(0, "pid2");
				long p3 = t.getLong(0, "pid3");
				query = "select * from players where pid = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { new Long(p1) }, ps);
				Table aux = db.executeQuery(ps);
				String name = "";
				if (aux.size() > 0)
					name = aux.get(0, "name");
				ret[0] = new PlayerInfo(p1, name);
				db.setStatementValues(new Object[] { new Long(p2) }, ps);
				aux = db.executeQuery(ps);
				if (aux.size() > 0)
					name = aux.get(0, "name");
				ret[1] = new PlayerInfo(p2, name);
				db.setStatementValues(new Object[] { new Long(p3) }, ps);
				aux = db.executeQuery(ps);
				db.closeStatement(ps);
				if (aux.size() > 0)
					name = aux.get(0, "name");
				ret[2] = new PlayerInfo(p3, name);

			}
			//db.commit();
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw ex;
		} finally {
		}
		return (ret);
	}
	
	public static void dbSetPlayerSel(Db db, DataCdKeyAndPass key, long pid) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update users set pid_sel = ? where cd_key = ?";
			ps = db.prepareStatement(query);
			db
					.setStatementValues(new Object[] { new Long(pid),
							key.getCdKey() }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbSetPlayerSel(Db db, String username, long pid) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select pid_sel from users where username = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues( new Object[] { username }, ps );
			Table t = db.executeQuery( ps );
			db.closeStatement(ps);
			/*
			 * Solo actualizamos si es estrictamente necesario
			 */
			if( t.size() > 0 && t.getLong( 0, "pid_sel" ) != pid ) {
				query = "update users set pid_sel = ? where username = ?";
				ps = db.prepareStatement(query);
				db
						.setStatementValues(new Object[] { new Long(pid),
								username }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
//	public static UserInfo getUserInfo( String auth_url, String ip_address ) {
//	    URL u;  
//	    InputStream is = null; 
//	    DataInputStream dis;
//	    UserInfo uInfo = new UserInfo( "", 0 );
//		try {
//			u = new URL(auth_url + "?ip_address=" + ip_address);
//	        is = u.openStream();
//	        dis = new DataInputStream(new BufferedInputStream(is));
//	        String s = dis.readLine();
//	        int idx = s.lastIndexOf('|');
//	        String username = s.substring(0, idx);
//	        int access = Integer.parseInt(s.substring(idx+1));
//	        uInfo.setUsername( username );
//	        uInfo.setAccess( access );
//		} catch(MalformedURLException ex ) {
//			ex.printStackTrace();
//		} catch(IOException ex) {
//			ex.printStackTrace();
//		} finally {
//			try {
//	            is.close();
//	         } catch (IOException ioe) {
//	         }
//	    }
//		return( uInfo );
//	}
	
	public static UserInfo getUserInfo( String auth_url, String ip_address ) {
		URL u;
		HttpURLConnection uc = null;
		BufferedReader in = null;
		UserInfo uInfo = new UserInfo( "", 0 );
		try {
			u = new URL(auth_url + "?ip_address=" + ip_address);
			uc = (HttpURLConnection)u.openConnection();
			uc.setConnectTimeout(10000);
			uc.setReadTimeout(10000);
	        in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));
			String s;

			s = in.readLine();

			int idx = s.lastIndexOf('|');
			String username = s.substring(0, idx);
			int access = Integer.parseInt(s.substring(idx+1));
			uInfo.setUsername( username );
			uInfo.setAccess( access );
		} catch(MalformedURLException ex ) {
			ex.printStackTrace();
		} catch(IOException ex) {
			ex.printStackTrace();
		} catch(Exception ex ) {
			ex.printStackTrace();
		} finally { 
			try {
				if( in != null ) in.close();
				if( uc != null ) uc.disconnect();
			} catch (IOException ioe) {
			}
		}
		return( uInfo );
	}
	
	public static GroupInfo dbLoadGroup(Db db, long grid) throws SQLException {
		GroupInfo g = null;
		PreparedStatement ps;
		try {

			String query = "select * from groups where gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { new Long(grid)

			}, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if (t.size() < 1)
				throw new SQLException("No such group - id: " + grid + " !!!");
			
			g = new GroupInfo(grid, t.get(0, "name"));
			g.setComment(t.get(0, "comment"));
			g.setDefeats(t.getInt(0, "defeats"));
			g.setVictories(t.getInt(0, "victories"));
			g.setDraws(t.getInt(0, "draws"));
			g.setLevel(t.getLong(0, "level"));
			g.setPoints(t.getLong(0, "points"));
			g.setPosition(t.getLong(0, "position"));
			g.setSlots(t.getInt(0, "slots"));

			query = "select name from players where pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { new Long(t.getLong(0,
					"owner_pid")) }, ps);
			Table t2 = db.executeQuery(ps);
			PlayerInfo leader = new PlayerInfo(t.getLong(0, "owner_pid"), t2
					.get(0, "name"));
			g.setLeader(leader);
			db.closeStatement(ps);

			PlayerInfo[] players;
			query = "select players.* from players,groups_players where players.pid = groups_players.pid and groups_players.gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { new Long(grid) }, ps);
			Table t3 = db.executeQuery(ps);
			db.closeStatement(ps);
			players = new PlayerInfo[t3.size()];
			for (int i = 0; i < t3.size(); i++) {
				players[i] = new PlayerInfo(t3.getLong(i, "pid"), t3.get(i,
						"name"));
			}

			g.setPlayers(players);

		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (g);
	}
	
	public static void dbAddPlayerToGroup( Db db, PlayerInfo pg ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "insert into groups_players (gid,pid) values (?,?)";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(pg.getGroupInfo().getId()),
					new Long(pg.getId())
			}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbBanPlayerAccount(Db db, PlayerInfo p) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update users set banned=1 where uid = (select users.uid from users,players where players.pid = ? and " +
					"(users.pid1 = players.pid or users.pid2 = players.pid or users.pid3 = players.pid))";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { 
					new Long(p.getId()) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbCreateGroup(Db db, PlayerInfo p, String groupName)
	throws SQLException {
		PreparedStatement ps;
		try {
			//db.setAutoCommit(false);
			String query = "insert into groups (name,owner_pid) values (?,?) returning gid";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { groupName,
					new Long(p.getId()) }, ps);
			
			Table t = db.executeQuery(ps);
			long gid = t.getLong(0, "gid");
			db.closeStatement(ps);
			
			query = "insert into groups_players (gid,pid) values (?,?)";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { gid,
					new Long(p.getId()) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			//db.commit();
			GroupInfo gInfo = new GroupInfo(gid, groupName);
			gInfo.addPlayer(p);
			p.setGroupInfo(gInfo);
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static PlayerInfo[] dbGetPlayersOfGroup(Db db, GroupInfo gInfo) throws SQLException {
		PlayerInfo[] players = null;
		PreparedStatement ps;
		try {
			String query = "select players.* from players,groups_players where players.pid = groups_players.pid and group_players.gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { gInfo.getId() }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if (t.size() > 0) {
				players = new PlayerInfo[t.size()];
				for (int i = 0; i < t.size(); i++) {
					players[i] = new PlayerInfo(t.getLong(0, "pid"), t.get(0,
							"name"));
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (players);
	}
	
	public static void dbRemovePlayerFromGroup(Db db, PlayerInfo pg) throws SQLException {
		PreparedStatement ps;
		try {

			String query = "delete from groups_players where pid = ? and gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(pg.getId()),
					new Long(pg.getGroupInfo().getId())

			}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbSavePlayerComment(Db db, PlayerInfo p) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set comment = ? where players.pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { p.getComment(),
					new Long(p.getId()) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbUpdateDivisionAndPosition(Db db) throws SQLException {
		PreparedStatement ps;
		try {
			//db.setAutoCommit( false );
			
			String query;
			Table t;
			
//			String query = "select max(category) as max_category, min(category) as min_category from players";
//			ps = db.prepareStatement(query);
//			Table t = db.executeQuery(ps);
//			db.closeStatement( ps );
//			float maxCategory = t.getInt( 0, "max_category" );
//			float minCategory = t.getInt( 0, "min_category" );
//			float difference = maxCategory - minCategory;
//			int step = (int)Math.floor(difference / 5);
//			
//			int min = 0;
//			query = "update players set division = ? where category >= ? and category <= ?";
//			ps = db.prepareStatement(query);
//			for( int i = 0; i < 5; i++ ) {
//				int max;
//				max = Math.round(minCategory + ((i+1) * step));
//				if( i == 4 ) max = (int)maxCategory;
//				System.out.println( "div: " + i + " min: " + min + " max: " + max );
//				db.setStatementValues( new Object[] {
//						i,
//						min,
//						max
//				}, ps );
//				db.executeUpdate( ps );
//				min = max;
//			}
//			db.closeStatement(ps);
			
			/*
			 * We have to clear out positions first
			 */
//			query = "update players set position = 1000000";
//			ps = db.prepareStatement(query);
//			db.executeUpdate(ps);
//			db.closeStatement( ps );
			
			query = "select pid from players order by category desc, points desc";
			ps = db.prepareStatement(query);
			t = db.executeQuery(ps);
			db.closeStatement( ps );
			
			query = "update players set position = ?, division = ? where pid = ?";
			ps = db.prepareStatement(query);
			int bs = t.size() / 5;
			for( int i = 0; i < t.size(); i++ ) {
				int div =  -1*((i / bs) - 4);
				if( div > 4 ) div = 4;
				System.out.println( "Updating player " + i + " out of " + t.size());
				db.setStatementValues( new Object[] {
				    i + 1,
				    div, 
				    t.getLong( i, "pid" )
				}, ps );
				db.executeUpdate( ps );
			}
			db.closeStatement( ps );
			//db.commit();
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbSaveUserInfo(Db db, UserInfo uInfo, String ip_address) throws SQLException {
		PreparedStatement ps;
		try {
			String query;
//			query = "select ip, status from users where username = ?";
//			ps = db.prepareStatement(query);
//			Table t = db.executeQuery( ps );
//			db.closeStatement(ps);
			query = "select ip, status from users where username = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues( new Object[] { uInfo.getUsername() }, ps );
			Table t = db.executeQuery( ps );
			db.closeStatement(ps);
			/*
			 * Solo actualizamos si es estrictamente necesario
			 */
//			if( t.size() > 0 && ( !t.get( 0, "ip" ).equals( ip_address ) || t.getInt( 0, "status") != uInfo.getAccess()) ) {
//				query = "update users set ip = ?, status = ? where username = ?";
//				ps = db.prepareStatement(query);
//				db.setStatementValues(new Object[] { ip_address, uInfo.getAccess(), uInfo.getUsername() }, ps);
//				db.executeUpdate(ps);
//				db.closeStatement(ps);
//			}
			query = "update users set ip = ?, status = ?, last_update = ? where username = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { ip_address, uInfo.getAccess(), new Date().getTime(), uInfo.getUsername() }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static UserInfo dbLoadUserInfo(Db db, String ip_address) throws SQLException {
		PreparedStatement ps;
		UserInfo uInfo = new UserInfo("", 0);
		try {
			String query = "select username, status from users where ip = ? order by last_update desc";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { ip_address }, ps);
			Table t = db.executeQuery(ps);
			if( t.size() > 0 ) {
				uInfo.setUsername( t.get(0, "username"));
				uInfo.setAccess( t.getInt( 0, "status"));
			}
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return( uInfo );
	}
	
	public static void dbSavePlayerSettings(Db db, PlayerInfo p) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set settings = ? where players.pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { p.getSettings(),
					new Long(p.getId()) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static GroupInfo[] dbSearchGroups(Db db, int queryType, String queryText)
	throws SQLException {
		PreparedStatement ps;
		GroupInfo[] ret = null;
		
		/*
		 * Comentado, no sea que sea esto lo que genera queries horribles de postgres
		 */
//		if (queryType == 1)
//			queryText = "%" + queryText + "%";
//		try {
//			String query = "select name, gid from groups where name ilike ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { queryText }, ps);
//			Table t = db.executeQuery(ps);
//			ret = new GroupInfo[t.size()];
//			for (int i = 0; i < t.size(); i++)
//				ret[i] = new GroupInfo(t.getLong(i, "gid"), t.get(i, "name"));
//			db.closeStatement(ps);
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw( ex );
//		} finally {
//		}
		return (ret);
	}
}
