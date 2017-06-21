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
import pes6j.datablocks.ChannelInfo;
import pes6j.datablocks.DataCdKeyAndPass;
import pes6j.datablocks.DataCrtPlayer;
import pes6j.datablocks.GroupInfo;
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.ServerInfo;
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
			pInfo = dbLoadPlayerInfoPrivate( db, t.getLong( 0, "pid"), default_settings );
			pInfo.setAdmin( uInfo.getAccess() == 2 ? 1 : 0 );
			//dbUpdatePlayerServer(db, pInfo.getId());
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (pInfo);
	}
	
	public static void dbUpdatePlayerServerAndLobby( Db db, long pid, int lobby_id ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set server_id = ?, lobby_id = ? where pid = ?";
			ps = db.prepareStatement(query);
			int serverId = Integer.parseInt( Configuration.gameServerProperties.getProperty("id"));
			db.setStatementValues(new Object[] { serverId, lobby_id, pid }, ps );
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static ServerInfo[] dbLoadServers( Db db ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select * from servers";
			ps = db.prepareStatement(query);
			Table t = db.executeQuery(ps);
			ServerInfo[] ret = new ServerInfo[t.size()];
			for( int i = 0; i < t.size(); i++ ) {
				ret[i] = new ServerInfo(t.getInt(i, "id"), 3, t.get(i, "name"), t.get(i, "ip"), t.getInt(i, "port"));
				ret[i].setWPort(t.getInt(i,"wport"));
			}
			return( ret );
		} catch( SQLException ex ) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static ServerInfo dbLoadServer( Db db, int server_pid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select * from servers where id = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[]{ server_pid }, ps);
			Table t = db.executeQuery(ps);
			ServerInfo ret = new ServerInfo(t.getInt(0, "id"), 3, t.get(0, "name"), t.get(0, "ip"), t.getInt(0, "port"));
			ret.setWPort(t.getInt(0, "wport"));
			return( ret );
		} catch( SQLException ex ) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static ChannelInfo[] dbLoadServerLobbies( Db db, int server_id ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select * from lobbies where server_id = ? order by id asc;";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { server_id}, ps);
			Table t = db.executeQuery(ps);
			ChannelInfo[] ret = new ChannelInfo[t.size()];
			for( int i = 0; i < t.size(); i++ ) {
				ret[i] = new ChannelInfo((byte)t.getInt(i, "type"), t.get(i, "name"), t.get(i,"continents"));
				ret[i].setId(t.getInt(i,"id"));
			}
			return( ret );
		} catch( SQLException ex ) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
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
	
//	public static PlayerInfo dbGetPlayerSel(Db db, DataCdKeyAndPass key, byte[] default_settings ) throws SQLException {
//		PreparedStatement ps;
//		PlayerInfo pInfo = null;
//		GroupInfo gInfo = null;
//		
//		try {
//			String query = "select players.* from players,users where players.pid = users.pid_sel and users.cd_key = ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { key.getCdKey() }, ps);
//			Table t = db.executeQuery(ps);
//			db.closeStatement(ps);
//			pInfo = new PlayerInfo(t.getLong(0, "pid"), t.get(0, "name"));
//			
//			byte[] settings = (byte[])t.getObject(0, "settings");
//			if( settings == null || settings.length != 1300 ) {
//				settings = default_settings;
//			}
//			pInfo.setSettings(settings);
//			
//			pInfo.setComment(t.get(0, "comment"));
//			pInfo.setAdmin(t.getInt(0, "admin"));
//
//			query = "select groups.* from groups,groups_players where groups_players.gid = groups.gid and "
//					+ "groups_players.pid = ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { pInfo.getId() }, ps);
//			t = db.executeQuery(ps);
//			db.closeStatement(ps);
//
//			if (t.size() > 0) {
//				PlayerInfo[] players;
//				query = "select players.* from players,groups_players where players.pid = groups_players.pid and groups_players.gid = ?";
//				ps = db.prepareStatement(query);
//				db.setStatementValues(new Object[] { t.getLong(0, "gid") }, ps);
//				Table t2 = db.executeQuery(ps);
//				db.closeStatement(ps);
//				players = new PlayerInfo[t2.size()];
//				for (int i = 0; i < t2.size(); i++) {
//					players[i] = new PlayerInfo(t2.getLong(i, "pid"), t2.get(i,
//							"name"));
//				}
//
//				gInfo = new GroupInfo(t.getLong(0, "gid"), t.get(0, "name"));
//				gInfo.setPlayers(players);
//				gInfo.setComment(t.get(0,"comment"));
//				gInfo.setDefeats(t.getInt(0,"defeats"));
//				gInfo.setDraws(t.getInt(0,"draws"));
//				gInfo.setPosition(t.getLong(0,"position"));
//				gInfo.setPoints(t.getLong(0,"points"));
//				gInfo.setMatches(t.getInt(0, "matches"));
//				gInfo.setVictories(t.getInt(0,"victories"));
//				gInfo.setDraws(t.getInt(0,"draws"));
//				gInfo.setSlots(t.getInt(0,"slots"));
//				gInfo.setLevel(t.getInt(0,"level"));
//			} else
//				gInfo = new GroupInfo(0, "");
//			pInfo.setGroupInfo( gInfo );
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw( ex );
//		} finally {
//		}
//		return (pInfo);
//	}
	
	public static void dbChangePlayerInvitation( Db db, long pid ) throws SQLException {
		PreparedStatement ps;
		String query;
		
		try {
			int newInvitationLevel = 1;
			query = "select invitation from players where pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid } , ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if( t.size() > 0 ) {
				if( t.getInt(0,"invitation") == 1 ) {
					newInvitationLevel = 2;
				} else
					newInvitationLevel = 1;
				query = "update players set invitation = ? where pid = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] { newInvitationLevel, pid }, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
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
					"invitation = ?," +
					"server_id = ?," +
					"lobby_id = ?," +
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
					player.getInvitation(),
					0,
					255,
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
	
	static PlayerInfo dbLoadPlayerInfoPrivate( Db db, long pid, byte[] default_settings ) throws SQLException {
		PreparedStatement ps;
		PlayerInfo pInfo = null;
		
		String query = "select players.*,groups.name as gname,groups.level as glevel from players left join groups on " +
				"players.gid = groups.gid where players.pid = ?";
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
		pInfo.setMatchesPlayed( t.getInt( 0, "group_matches" ));
		pInfo.setInvitation( t.getInt( 0, "invitation"));
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
		pInfo.setGid( t.getLong( 0, "gid"));
		pInfo.pushTeam( t.getInt( 0, "team_1"));
		pInfo.pushTeam( t.getInt( 0, "team_2"));
		pInfo.pushTeam( t.getInt( 0, "team_3"));
		pInfo.pushTeam( t.getInt( 0, "team_4"));
		pInfo.pushTeam( t.getInt( 0, "team_5"));
		pInfo.setPosition( t.getLong( 0, "position"));
		pInfo.setComment(t.get(0, "comment"));
		pInfo.setGid(t.getLong(0, "gid"));
		pInfo.setGroupName(t.get(0,"gname"));
		pInfo.setGroupLevel(t.getInt(0,"glevel"));
		pInfo.setServerId(t.getInt(0, "server_id"));
		pInfo.setLobbyId(t.getInt(0,"lobby_id"));

		return (pInfo);
	}
	
	public static PlayerInfo dbLoadPlayerInfo(Db db, long pid, byte[] default_settings ) throws SQLException {
		PlayerInfo pInfo = null;
		try {
			pInfo = dbLoadPlayerInfoPrivate( db, pid, default_settings );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (pInfo);
	}
	
	public static GroupInfo dbLoadGroupInfoFromPid(Db db, long pid ) throws SQLException {
		GroupInfo gInfo = null;
		try {
			String query = "select gid from groups_players where pid = ? limit 1";
			PreparedStatement ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if( t.size() > 0) {
				gInfo = dbLoadGroup( db, t.getLong(0,"gid") );
				return( gInfo );
			} else {
				return( new GroupInfo( 0, ""));
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
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
				
				ret[0] = dbLoadPlayerInfoPrivate( db, p1, null );
				if( ret[0] == null ) ret[0] = new PlayerInfo( 0, "" );
				ret[1] = dbLoadPlayerInfoPrivate( db, p2, null );
				if( ret[1] == null ) ret[1] = new PlayerInfo( 0, "" );
				ret[2] = dbLoadPlayerInfoPrivate( db, p3, null );
				if( ret[2] == null ) ret[2] = new PlayerInfo( 0, "" );
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
		}
		return (ret);
	}
	
//	public static PlayerInfo[] dbLoadPlayers(Db db, DataCdKeyAndPass key) throws SQLException {
//		PlayerInfo[] ret = new PlayerInfo[3];
//		PreparedStatement ps;
//		try {
//			//db.setAutoCommit(false);
//			String query = "select * from users where cd_key = ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { key.getCdKey() }, ps);
//			Table t = db.executeQuery(ps);
//			db.closeStatement(ps);
//			if (t.size() == 0) {
//				query = "insert into users (cd_key,pid1,pid2,pid3) values(?,?,?,?)";
//				ps = db.prepareStatement(query);
//				db.setStatementValues(new Object[] { key.getCdKey(),
//						new Long(0x00000000), new Long(0x00000000),
//						new Long(0x00000000) }, ps);
//				db.executeUpdate(ps);
//				db.closeStatement(ps);
//				ret[0] = new PlayerInfo(0x00000000, "");
//				ret[1] = new PlayerInfo(0x00000000, "");
//				ret[2] = new PlayerInfo(0x00000000, "");
//			} else {
//				if( t.getInt(0, "banned") == 1 )
//					throw new SQLException("THE USER IS BANNED!!!");
//				long p1 = t.getLong(0, "pid1");
//				long p2 = t.getLong(0, "pid2");
//				long p3 = t.getLong(0, "pid3");
//				query = "select * from players where pid = ?";
//				ps = db.prepareStatement(query);
//				db.setStatementValues(new Object[] { new Long(p1) }, ps);
//				Table aux = db.executeQuery(ps);
//				String name = "";
//				if (aux.size() > 0)
//					name = aux.get(0, "name");
//				ret[0] = new PlayerInfo(p1, name);
//				db.setStatementValues(new Object[] { new Long(p2) }, ps);
//				aux = db.executeQuery(ps);
//				if (aux.size() > 0)
//					name = aux.get(0, "name");
//				ret[1] = new PlayerInfo(p2, name);
//				db.setStatementValues(new Object[] { new Long(p3) }, ps);
//				aux = db.executeQuery(ps);
//				db.closeStatement(ps);
//				if (aux.size() > 0)
//					name = aux.get(0, "name");
//				ret[2] = new PlayerInfo(p3, name);
//
//			}
//			//db.commit();
//		} catch (SQLException ex) {
//			//db.rollback();
//			ex.printStackTrace();
//			throw ex;
//		} finally {
//		}
//		return (ret);
//	}
	
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
	
	private static int dbGetGroupPlayerCount( Db db, long gid ) throws SQLException {
		String query = "select count(pid) as numplayers from players where invitation <> 4 and gid = ?";
		PreparedStatement ps = db.prepareStatement(query);
		db.setStatementValues(new Object[] {gid}, ps);
		Table t = db.executeQuery(ps);
		db.closeStatement(ps);
		return( t.getInt(0, "numplayers"));
	}
	
	public static GroupInfo dbLoadGroup(Db db, long grid) throws SQLException {
		GroupInfo g = null;
		PreparedStatement ps;
		try {

			String query = "select groups.*,players.pid as pid, players.name as leader_name " +
					"from groups,players " +
					"where " +
					"groups.gid = ? and " +
					"players.gid = groups.gid and " +
					"players.invitation = 3";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { new Long(grid)

			}, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if (t.size() < 1)
				throw new SQLException("No such group - id: " + grid + " !!!");
			
			g = new GroupInfo(grid, t.get(0, "name"));
			g.setLeaderId(t.getLong(0, "pid"));
			g.setLeaderName(t.get(0, "leader_name"));
			g.setComment(t.get(0, "comment"));
			g.setDefeats(t.getInt(0, "defeats"));
			g.setVictories(t.getInt(0, "victories"));
			g.setDraws(t.getInt(0, "draws"));
			g.setMatches(t.getInt( 0, "matches"));
			g.setLevel(t.getLong(0, "level"));
			g.setPoints(t.getLong(0, "points"));
			g.setPosition(t.getLong(0, "position"));
			g.setSlots(t.getInt(0, "slots"));
			g.setRecruitComment(t.get(0,"recruit_comment"));
			g.setRecruiting(t.getInt( 0, "recruiting"));

			/*
			 * Player count
			 */
			g.setNumPlayers( dbGetGroupPlayerCount(db, grid));
			
//			PlayerInfo[] players;
//			query = "select players.* from players,groups_players where players.pid = groups_players.pid and groups_players.gid = ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { new Long(grid) }, ps);
//			Table t3 = db.executeQuery(ps);
//			db.closeStatement(ps);
//			players = new PlayerInfo[t3.size()];
//			for (int i = 0; i < t3.size(); i++) {
//				players[i] = new PlayerInfo(t3.getLong(i, "pid"), t3.get(i,
//						"name"));
//			}
//
//			g.setPlayers(players);

		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (g);
	}
	
	public static PlayerInfo[] dbLoadGroupMembers( Db db, long gid ) throws SQLException {
		PlayerInfo[] players;
		PreparedStatement ps;
		try {
			String query = "select players.pid,players.name,players.lobby_id," +
					"players.invitation,players.division,players.category," +
					"players.server_id,lobbies.name as lobby_name,lobbies.type from players " +
					"left join lobbies on players.lobby_id = lobbies.id and " +
					"players.server_id = lobbies.server_id where gid = ? and invitation <> 4";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { new Long(gid) }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			players = new PlayerInfo[t.size()];
			for (int i = 0; i < t.size(); i++) {
				players[i] = new PlayerInfo(t.getLong(i, "pid"), t.get(i,"name"));
				players[i].setLobbyId(t.getInt(i, "lobby_id"));
				players[i].setInvitation(t.getInt(i, "invitation"));
				players[i].setDivision(t.getInt(i,"division"));
				players[i].setCategory(t.getInt(i,"category"));
				players[i].setServerId(t.getInt(i,"server_id"));
				players[i].setLobbyName(t.get(i,"lobby_name"));
				players[i].setLobbyType(t.getInt(i,"type"));
				players[i].setGroupMatches(t.getInt(i,"group_matches"));
			}
			return( players );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static Table dbLoadGroupInviteList( Db db ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select * from groups where recruiting=1 order by position desc";
			ps = db.prepareStatement(query);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			for (int i = 0; i < t.size(); i++) {
				int numPlayers = dbGetGroupPlayerCount(db, t.getInt(i, "gid"));
				t.get(i).setInt("num_players", numPlayers);
			}
			return( t );
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
	
	public static long dbCreateGroup(Db db, PlayerInfo p, String groupName)
	throws SQLException {
		PreparedStatement ps;
		try {
			//db.setAutoCommit(false);
			String query = "insert into groups (name) values (?) returning gid";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { groupName }, ps);
			
			Table t = db.executeQuery(ps);
			long gid = t.getLong(0, "gid");
			db.closeStatement(ps);
			
//			query = "insert into groups_players (gid,pid) values (?,?)";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { gid,
//					new Long(p.getId()) }, ps);
//			db.executeUpdate(ps);
//			db.closeStatement(ps);
			
			query = "update players set invitation = 3, gid = ?, group_name = ? where pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long( gid ), groupName, new Long(p.getId()) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			//db.commit();
			p.setInvitation(3);
			p.setGid(gid);
			p.setGroupName(groupName);
			return( gid );
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbInsertPlayerFriend( Db db, long pid, long friend_pid, int block ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "insert into friend_list (pid,friend_pid,block) values (?,?,?)";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid,friend_pid,block }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbRemovePlayerFriend( Db db, long pid, long friend_pid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "delete from friend_list where pid = ? and friend_pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid,friend_pid }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static PlayerInfo[] dbLoadPlayerFriends( Db db, long pid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select friend_list.friend_pid,players.name,friend_list.block from friend_list,players " +
					"where players.pid = friend_list.friend_pid and friend_list.pid = ? order by players.name asc";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			
			if( t.size() == 0 ) return( null );
			PlayerInfo[] ret = new PlayerInfo[t.size()];
			for( int i = 0; i < t.size(); i++ ) {
				ret[i] = new PlayerInfo( t.getLong(i, "friend_pid"), t.get(i, "name"));
				/*
				 * Usamos invitation para saber el nivel de bloqueo
				 */
				ret[i].setInvitation( t.getInt(i, "block"));
			}
			return( ret );
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static boolean dbGetFriendListFull( Db db, long pid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select count(friend_pid) as num_friends from friend_list where pid = ? and block = 0";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if( t.getInt(0, "num_friends") >= 50 ) return true;
			return false;
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static boolean dbGetBlockListFull( Db db, long pid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select count(friend_pid) as num_friends from friend_list where pid = ? and block = 1";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { pid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if( t.getInt(0, "num_friends") >= 20 ) return true;
			return false;
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static boolean dbGetGroupFull( Db db, long gid ) throws SQLException {
		PreparedStatement ps;
		try {
			int numPlayers = dbGetGroupPlayerCount(db, gid);
			String query = "select slots from groups where gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { gid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			if( t.getInt(0, "slots") == numPlayers ) return true;
			return false;
		} catch (SQLException ex) {
			//db.rollback();
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
//	public static PlayerInfo[] dbGetPlayersOfGroup(Db db, GroupInfo gInfo) throws SQLException {
//		PlayerInfo[] players = null;
//		PreparedStatement ps;
//		try {
//			String query = "select pid,name from players where players.pid = groups_players.pid and group_players.gid = ?";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[] { gInfo.getId() }, ps);
//			Table t = db.executeQuery(ps);
//			db.closeStatement(ps);
//			if (t.size() > 0) {
//				players = new PlayerInfo[t.size()];
//				for (int i = 0; i < t.size(); i++) {
//					players[i] = new PlayerInfo(t.getLong(0, "pid"), t.get(0,
//							"name"));
//				}
//			}
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw( ex );
//		} finally {
//		}
//		return (players);
//	}
	
	public static int dbRemovePlayerFromGroup(Db db, long pid, long gid) throws SQLException {
		PreparedStatement ps;
		try {
			int playersBefore = dbGetGroupPlayerCount(db, gid);
			if( playersBefore == 1) {
				dbRemoveGroup(db, gid);
				return( 0 );
			} else {
				
				String query = "update players set invitation = 0, gid = 0 where pid = ?";
				ps = db.prepareStatement(query);
				db.setStatementValues(new Object[] {
						new Long(pid),
				}, ps);
				db.executeUpdate(ps);
				db.closeStatement(ps);
				return( playersBefore - 1 );
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbChangeGroupLeader(Db db, long gid, long pid) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set invitation = 2 where invitation = 3 and gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(gid)
			}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			
			query = "update players set invitation = 3 where pid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(pid)

			}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbRemoveGroup( Db db, long gid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set invitation = 0, gid = 0 where gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(gid),
			}, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
			
			query = "delete from groups where gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {
					new Long(gid),
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
	
	public static void dbSaveGroupComment(Db db, long gid, String comment ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update groups set comment = ? where groups.gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { comment,
					new Long(gid) }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static void dbSaveGroupRecruitment(Db db, long grid, int recruiting, String comment) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update groups set recruit_comment = ?, recruiting = ? where groups.gid = ?";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { comment,
					recruiting, grid }, ps);
			db.executeUpdate(ps);
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static PlayerInfo[] dbLoadGroupRecruits( Db db, long gid ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "select pid,name from players where " +
					"gid = ? and invitation = 4";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[]{ gid }, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			PlayerInfo[] ret = new PlayerInfo[t.size()];
			for( int i = 0; i < t.size(); i++ ) {
				ret[i] = new PlayerInfo( t.getLong(i, "pid"), t.get(i, "name"));
			}
			return( ret );
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
	public static String dbUpdatePlayerGidAndInvitation( Db db, long pid, long gid, int invitation ) throws SQLException {
		PreparedStatement ps;
		try {
			String query = "update players set gid = ?, invitation = ?, group_name = (select name from groups where gid = ?) where pid = ? returning group_name";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] {gid,invitation,gid,pid}, ps);
			Table t = db.executeQuery(ps);
			db.closeStatement(ps);
			return( t.get(0, "group_name"));
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
	}
	
//	public static void dbInsertGroupRecruit( Db db, long gid, long pid ) throws SQLException {
//		PreparedStatement ps;
//		try {
//			String query = "insert into groups_recruits (gid,pid) values(?,?)";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[]{ gid,pid }, ps);
//			db.executeUpdate(ps);
//			db.closeStatement(ps);
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw( ex );
//		} finally {
//		}
//	}
	
//	public static void dbRemoveGroupRecruit( Db db, long gid, long pid ) throws SQLException {
//		PreparedStatement ps;
//		try {
//			String query = "delete from groups_recruits (gid,pid) values(?,?)";
//			ps = db.prepareStatement(query);
//			db.setStatementValues(new Object[]{ gid,pid }, ps);
//			db.executeUpdate(ps);
//			db.closeStatement(ps);
//		} catch (SQLException ex) {
//			ex.printStackTrace();
//			throw( ex );
//		} finally {
//		}
//	}
	
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
				if( div < 0 ) div = 0;
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
		
		if (queryType == 1) {			
			if( queryText.length() < 4 ) return( null );
			queryText = "%" + queryText + "%";
		}
		try {
			String query = "select name, gid from groups where name ilike ? limit 50";
			ps = db.prepareStatement(query);
			db.setStatementValues(new Object[] { queryText }, ps);
			Table t = db.executeQuery(ps);
			ret = new GroupInfo[t.size()];
			for (int i = 0; i < t.size(); i++)
				ret[i] = new GroupInfo(t.getLong(i, "gid"), t.get(i, "name"));
			db.closeStatement(ps);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw( ex );
		} finally {
		}
		return (ret);
	}
}
