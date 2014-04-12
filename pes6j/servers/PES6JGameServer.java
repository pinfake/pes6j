package pes6j.servers;

//The server code Server.java:
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import pes6j.WEBMethods.PES6JWebHandler;
import pes6j.database.Db;
import pes6j.datablocks.ChannelInfo;
import pes6j.datablocks.ChannelList;
import pes6j.datablocks.DataCdKeyAndPass;
import pes6j.datablocks.DataChatMessage;
import pes6j.datablocks.DataId;
import pes6j.datablocks.DataIp;
import pes6j.datablocks.DataJoinRoom;
import pes6j.datablocks.DataPlayerTeamPositions;
import pes6j.datablocks.DataRoom;
import pes6j.datablocks.GroupInfo;
import pes6j.datablocks.GroupList;
import pes6j.datablocks.IpInfo;
import pes6j.datablocks.Message;
import pes6j.datablocks.MessageBlock;
import pes6j.datablocks.MessageBlockByte;
import pes6j.datablocks.MessageBlockGameAnyId;
import pes6j.datablocks.MessageBlockGameChatMessage;
import pes6j.datablocks.MessageBlockGameEndInfo;
import pes6j.datablocks.MessageBlockGameEnterRoomFailFull;
import pes6j.datablocks.MessageBlockGameEnterRoomFailPass;
import pes6j.datablocks.MessageBlockGameGroupInfo;
import pes6j.datablocks.MessageBlockGameGroupList;
import pes6j.datablocks.MessageBlockGameGroupMembers;
import pes6j.datablocks.MessageBlockGameIpInfo;
import pes6j.datablocks.MessageBlockGameIpInfoShort;
import pes6j.datablocks.MessageBlockGamePlayerGroup;
import pes6j.datablocks.MessageBlockGamePlayerGroupList;
import pes6j.datablocks.MessageBlockGamePlayerId;
import pes6j.datablocks.MessageBlockGamePlayerReady;
import pes6j.datablocks.MessageBlockGameRoomInfoShort;
import pes6j.datablocks.MessageBlockGameRoomList;
import pes6j.datablocks.MessageBlockGameRoomOptionSel;
import pes6j.datablocks.MessageBlockGameRoomPos;
import pes6j.datablocks.MessageBlockGameStart1;
import pes6j.datablocks.MessageBlockGameStart4344;
import pes6j.datablocks.MessageBlockLong;
import pes6j.datablocks.MessageBlockMenuGroupInfo;
import pes6j.datablocks.MessageBlockMenuMenuList;
import pes6j.datablocks.MessageBlockMenuPlayerGroup;
import pes6j.datablocks.MessageBlockMenuQ1;
import pes6j.datablocks.MessageBlockVoid;
import pes6j.datablocks.MessageBlockZero;
import pes6j.datablocks.MetaMessageBlock;
import pes6j.datablocks.PESConnection;
import pes6j.datablocks.PESConnectionList;
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.PlayerList;
import pes6j.datablocks.QUERIES;
import pes6j.datablocks.RoomInfo;
import pes6j.datablocks.RoomList;
import pes6j.datablocks.UserInfo;
import pes6j.datablocks.Util;

import com.maxmind.geoip.LookupService;
import com.sun.net.httpserver.HttpServer;

/**
 *
 */

public class PES6JGameServer {
	// the socket used by the server

	private ServerSocketChannel ssChannel;
	ChannelList channels = new ChannelList();
	boolean shuttingDown = false;
	long shutdownTime = 0l;
	static final long SHUTDOWN_MESSAGE_REPEAT_TIME = 5 * 60 * 1000;
	static final long GHOST_ROOM_REMOVAL_CHECK_TIME = 5 * 60 * 1000;
	
	long lastShutdownMessageTime = 0l;
	long lastRoomRemovalCheckTime = 0l;
	
	long roomIdx = 1;
	PESConnectionList connections = new PESConnectionList();
	GroupList groups = new GroupList();
	byte[] default_settings = null;
	private Logger logger;
    Selector selector;
    LookupService cl;
    boolean debug = false;
	// the socket where to listen/talk
    int port;
    int wport;
	private Db db;

	// server constructor

	PES6JGameServer() {
		
		/* create socket server and wait for connection requests */
		try
		
		{
			logger = new Logger( "log/pes6j_GAME_SERVER.log" );
			port = Integer.parseInt(Configuration.gameServerProperties.getProperty("port"));
			wport = Integer.parseInt(Configuration.gameServerProperties.getProperty("wport"));
			debug = Configuration.gameServerProperties.getProperty("debug").trim().toLowerCase().equals("on");
			initialize();
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(port));
			
			selector = Selector.open();
			SelectionKey acceptKey = ssChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			
			logger.log("PES6JGameServer waiting for client on port "
					+ port);
			
			File f = new File( "def_settings.raw");
			try {
				FileInputStream fi = new FileInputStream( f );
				default_settings = new byte[(int)f.length()];
				fi.read(default_settings);
				fi.close();
			} catch( IOException ex1) {
				logger.log( "Default settings file not found!" );
				ex1.printStackTrace();
				return;
			}
			
			while (true) {
				if(selector.select(5000) > 0) {
					// Someone is ready for I/O, get the ready keys
					Set readyKeys = selector.selectedKeys();
					Iterator i = readyKeys.iterator();
					// Walk through the ready keys collection and process date requests.
					while (i.hasNext()) {
						SelectionKey sk = (SelectionKey)i.next();
						i.remove();
						if (!sk.isValid()) {
							continue;
						}
						if (sk.isAcceptable()) {
							try {
								this.accept(sk);
							} catch( IOException e ) {
								sk.cancel();
							}
						} else if (sk.isReadable()) {
							try {
								this.read(sk);
							} catch( IOException e ) {
								sk.cancel();
							}
						} else if (sk.isWritable()) {
							try {
								this.write(sk);
							} catch( IOException e ) {
								sk.cancel();
							}
						}

					}
				}
				
				/*
				 * Comprobamos si tenemos a algun jugador zombie
				 */
				long now = (new Date()).getTime();
				Enumeration<PESConnection> cons = connections.elements();
				while( cons.hasMoreElements() ) {
					PESConnection con1 = cons.nextElement();
					SelectionKey sk1 = con1.getSocketChannel().keyFor( selector );
					/*
					 * Al server del juego le damos timeouts de 80000
					 */
					if( now - con1.getLastKATime() > 120000L ) {
						
						con1.log( "TIMED OUT, NO KEEP ALIVE REPLY FOR ONE MINUTE" );					
						if( sk1 != null ) {
							sk1.cancel();
						}
						disconnectPlayer( con1 );
					} else {
						try {
							if( con1.hasRemaining() ) con1.write();
						} catch( IOException ex ) {
							con1.log( ex );
							disconnectPlayer( con1 );
						}
						if( sk1 != null && sk1.isValid() ) {
							if(!con1.hasRemaining()) sk1.interestOps(SelectionKey.OP_READ);
							else sk1.interestOps(SelectionKey.OP_WRITE);
						}
					}
				}
				
				/*
				 * Comprobamos si debemos hacer shutdown
				 */
				if( shutdownTime > 0 && now >= shutdownTime ) {				
					/*
					 * We have to save every user matches and profiles
					 */
					//System.out.println( "Aver, ahora es: " + now + " y debo hacer shutdown en: " + shutdownTime );
					shuttingDown = true;
					cons = connections.elements();
					while( cons.hasMoreElements() ) {
						PESConnection c = (PESConnection)cons.nextElement();
						disconnectPlayer( c );
					}
					System.exit( 1 );
				}
				
				/*
				 * Comprobamos si debemos enviar mensaje de warning de shutdown
				 */
				if( shutdownTime > 0 && lastShutdownMessageTime + SHUTDOWN_MESSAGE_REPEAT_TIME <= now ) {
					long t = shutdownTime - now;
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
					String message = "WARNING!!! System shutdown at " + sdf.format(new Date(shutdownTime)) + "!.Current server time: " + sdf.format(now);
					PlayerInfo adminBot = new PlayerInfo( Long.MAX_VALUE, "AdminBot");
					Message mes = new Message();
					mes.addBlock(new MessageBlockGameChatMessage(adminBot,  
							(byte)0x00, (byte)0x02, message,
							0x00004402));
					connections.sendToAllEncodedNIO( mes );
					mes = new Message();
					mes.addBlock(new MessageBlockGameChatMessage(adminBot,  
							(byte)0x00, (byte)0x01, message,
							0x00004402));
					connections.sendToAllEncodedNIO( mes );
					lastShutdownMessageTime = now;
				}
				
				/*
				 * Comprobamos si debemos limpiar salas vacías
				 */
				if( lastRoomRemovalCheckTime + GHOST_ROOM_REMOVAL_CHECK_TIME <= now ) {
					for( int i = 0; i < channels.size(); i++ ) {
						RoomList rl = channels.getChannel(i).getRoomList();
						RoomInfo[] chanRooms = rl.getRoomInfoArray();
						if( chanRooms != null ) {
							for( int j = 0; j < chanRooms.length; j++ ) {
								if( chanRooms[j].getNumPlayers() < 1 ) {
									rl.removeRoom(chanRooms[j].getId());
								}
							}
						}
					}
					
					lastRoomRemovalCheckTime = now;
				}
			}
			
		}
		
		catch (Exception e) {
			
			System.err.println("Server down, exception: " + e);
			e.printStackTrace();
			
		}
		
	}
	
	private void accept(SelectionKey key) throws IOException {
		    // For an accept to be pending the channel must be a server socket channel.
		    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		    // Accept the connection and make it non-blocking
		    SocketChannel socketChannel = serverSocketChannel.accept();
		    Socket socket = socketChannel.socket();
		    socketChannel.configureBlocking(false);

		    // Register the new SocketChannel with our Selector, indicating
		    // we'd like to be notified when there's data waiting to be read
		    socketChannel.register(this.selector, SelectionKey.OP_READ );
		    
			try {
				PESConnection con = new PESConnection( socket, debug );
				connections.addConnection(con);
			}

			catch (IOException e) {
				logger.log("Exception creating new Input/output Streams: "
								+ e);
				return;
			}
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		PESConnection con = connections.searchConnection(socketChannel.socket());
		if( con == null ) {
			System.out.println( "NO EXISTE la conexion para la clave al intentar escribir!!!");
			key.cancel();
			socketChannel.close();
			return;
		}
		try {
			con.write();
			if(!con.hasRemaining())key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			con.log( e );
			disconnectPlayer( con );
		}
	}
	
	  private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out our read buffer so it's ready for new data
		PESConnection con = connections.searchConnection(socketChannel.socket());
		if( con == null ) {
			System.out.println( "NO EXISTE!!!");
			key.cancel();
			socketChannel.close();
			return;
		}
		
		try {
			Message mes;
			MetaMessageBlock mb;
			
			mes = con.networkReadMessage();
			

			if( mes != null ) {
				con.setLastKATime((new Date().getTime()));
				for (int i = 0; i < mes.getNumBlocks(); i++) {
					mb = mes.getBlock(i);
					processMessageBlock(con, mb);
				}
			}
		} catch (IOException e) {
			try {
				key.cancel();
				socketChannel.close();
				con.log("Exception reading/writing  Streams: " + e);
				disconnectPlayer( con );
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			con.log( ex );
			disconnectPlayer( con );
			ex.printStackTrace();
		}
	} 

	public static void main(String[] arg) {
		// start server on port 1500
		try {
			if( arg.length > 0 ) Configuration.initializeGameServerProperties( arg[0] );
			else Configuration.initializeGameServerProperties();
		} catch( IOException ex ) {
			ex.printStackTrace();
			return;
		}
		new PES6JGameServer();
	}

	void initialize() throws IOException, SQLException {		
		int i = 0;
		String lobby;
		while( (lobby = Configuration.gameServerProperties.getProperty("lobby-" + i)) != null ) {
			String lobbyContinents = Configuration.gameServerProperties.getProperty("lobby-" + i + "-continents");
			channels.addChannel( new ChannelInfo(
					ChannelInfo.TYPE_PC_CHANNEL, lobby, lobbyContinents) );
			i++;
		}
		
		HttpServer server = HttpServer.create(new InetSocketAddress(wport), 10);
		server.createContext("/query", new PES6JWebHandler(channels));
		server.setExecutor(null); // creates a default executor
		server.start();

		initDB();
		cl = new LookupService("config/GeoIP.dat",LookupService.GEOIP_MEMORY_CACHE);
	}
	
		void initDB() throws IOException, SQLException {
			db = new Db(Configuration.gameServerProperties);
			db.connect();
		}

		void processMessageBlock(PESConnection out, MetaMessageBlock mb) throws IOException {
			
			DataCdKeyAndPass cdkey = out.getCdkey();
			PlayerInfo player = out.getPlayerInfo();

			int channelId = 0;
			PlayerList players = null;
			RoomList rooms = null;
			ChannelInfo channel = null;
			UserInfo uInfo = out.getUserInfo();
			
			String countryCode = cl.getCountry(out.getSocket().getInetAddress().getHostAddress()).getCode();
			String continent = Configuration.country2Continent.getProperty( countryCode );
			
			/*
			 * Necesitamos las conexiones del canal de este jugador
			 * suponiendo que el jugador ya tenga IP
			 */
			
			if( player != null && player.getIpInfo() != null ) {
				channelId = player.getIpInfo().getChannel();

				channel = channels.getChannel( channelId );
				players = channel.getPlayerList();
				rooms = channel.getRoomList();
				
			}
			
			byte[] enc = Util.konamiEncode(mb.getBytes());
			byte[] enccab = new byte[4];
			System.arraycopy(enc, 0, enccab, 0, 4);
			out.log("HEX ENC CAB: " + Util.toHex(enccab));

			int qId = ((MetaMessageBlock) mb).getHeader().getQuery();

			Message mes;
			if (qId == QUERIES.MENU_INIT_CLIENT_QUERY) {
				out
						.log("MATCHES MENU INIT CLIENT QUERY ANSWERING LOGIN INIT SERVER:");
				mes = new Message();
				mes.addBlock(new MessageBlockMenuQ1(++qId));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00003003 ) {
				out
						.log("MATCHES MENU LOGIN CLIENT QUERY ANSWERING BLOCK ZERO:");
				/*
				 * Recuperamos su nombre de usuario
				 */
				try {
					uInfo = Tools.dbLoadUserInfo( db, out.getSocket().getInetAddress().getHostAddress() );
				} catch( SQLException ex ) {
					out.log( "EXCEPTION TRYING TO FETCH USER INFO: " );
					out.log( ex );
					throw new IOException( "UNABLE TO FETCH USER INFO!!!");
				}
				if( uInfo.getAccess() < 1 ) {
					throw new IOException( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " +
							uInfo.getAccess() );
				}
				cdkey = new DataCdKeyAndPass(mb.getData());
				out.setCdkey(cdkey);
				out.setUserInfo( uInfo );
				mes = new Message();
				mes.addBlock(new MessageBlockZero(++qId));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004100) {
				out
						.log("[GAMESERVER] MATCHES MENU GROUP CLIENT QUERY ANSWERING MENU GROUP INFO SERVER:");
				try {
					player = Tools.dbGetPlayerSel(db, uInfo, default_settings);
					player.setConnection( out );
					out.setPlayerInfo( player );
					out.setPid( player.getId() );
					
					mes = new Message();
					mes.addBlock(new MessageBlockMenuGroupInfo(player.getId(),
							player.getGroupInfo(), 0x00004101));
					out.streamWriteEncodedNIO(mes);
				} catch (SQLException ex) {
					ex.printStackTrace();
				}

			} else if (qId == 0x00004200 ) {
				out
						.log("MATCHES MENU MENUS CLIENT QUERY ANSWERING MENU MENU LIST SERVER:");
				mes = new Message();
				mes.addBlock(new MessageBlockMenuMenuList(
						channels, player, 0x00004201));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004202) {
				DataIp ip = new DataIp(mb.getData());
				/*
				 * OJO ESTA COMPROBACIÓN SOLO SIRVE PARA QUE NO ENTREN LOS
				 * DOS EN EL MISMO LOBBY!
				 */

				player.setLoggedIn();
				player.setIpInfo( new IpInfo(player.getId(), ip.getChannelId(), ip
						.getIp1(), ip.getPort1(), ip.getIp2(), ip.getPort2()) );
				
				channelId = player.getIpInfo().getChannel();

				channel = channels.getChannel( channelId );
				players = channel.getPlayerList();
				rooms = channel.getRoomList();
				
				int numPlayers = channel.getPlayerList().getNumPlayers();
				
				if ( players != null && players.getPlayerInfo(player.getId()) != null) {
					throw new IOException("The player was already logged in");
				}
				
				if( continent != null && out.getUserInfo().getAccess() < 2 && !channel.inContinentList( continent )) {
					mes = new Message();
					/*
					 * Would give a message like "coudn't enter the lobby", not like
					 * a "lobby full" message, but it would do.
					 */
					mes.addBlock(new MessageBlockGameEnterRoomFailFull(
							0x00004203));
					out.streamWriteEncodedNIO(mes);
				} else 				
				if( numPlayers >= 100 ) {
					mes = new Message();
					/*
					 * Would give a message like "coudn't enter the lobby", not like
					 * a "lobby full" message, but it would do.
					 */
					mes.addBlock(new MessageBlockGameEnterRoomFailFull(
							0x00004203));
					out.streamWriteEncodedNIO(mes);
				} else {			
					players.addPlayer( player );
					out.log("Adding new player!!!!");
	
					out.setPid(player.getId());
	
					out
							.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING MENU PLAYER AND GROUP SERVER:");
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00004203));
					out.streamWriteEncodedNIO(mes);
	
					mes = new Message();
					mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0, 0x00004220));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x00003080) {
				out
						.log("MATCHES MENU MYPLAYERID CLIENT QUERY ANSWERING WITH FRIEND LIST:");

				DataId did = new DataId(mb.getData());
				long pid = did.getId();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00003082));
				out.streamWriteEncodedNIO(mes);

				/*
				 * Lo comento porque de momento no tenemos amigos
				 */
				// mes = new Message();
				// mes.addBlock(new MessageBlockMenuFriends( new PlayerInfo[] {
				// new PlayerInfo( 0x00000077L, "Amigo") }, 0x00003084, 0
				// ));
				// out.streamWriteEncodedNIO(mes);
				mes = new Message();
				mes.addBlock(new MessageBlockVoid(0x003086));
				out.streamWriteEncodedNIO(mes);

				// mes = new Message();
				//				
				// // StefenG
				// mes.addBlock(new MessageBlockMenuPlayerGroup( new
				// PlayerInfo(0x00000001L, "Uno"),
				// new GroupInfo( 0x00000001L, "Grupix"),
				// QUERIES.MENU_PLYGRP_SERVER_QUERY, 0));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				//				
				// mes = new Message();
				// mes.addBlock(new MessageBlockZero(qId + 2, 0));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				//				
				// mes = new Message();
				// mes.addBlock(new MessageBlockMenuPlayersOfGroup( new
				// PlayerInfo[] {
				// new PlayerInfo( 0x00000002L, "Dos") }, qId + 4, 0
				// ));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				//				
				// mes = new Message();
				// mes.addBlock(new MessageBlockVoid(qId + 6, 0));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				//				
				// // ST-Lyon
				// mes = new Message();
				// mes.addBlock(new MessageBlockMenuPlayerGroup( new
				// PlayerInfo(0x00000001L, "Uno"),
				// new GroupInfo( 0x00000001L, "Grupix"),
				// QUERIES.MENU_PLYGRP_SERVER_QUERY, 0));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				//				
				// mes = new Message();
				// mes.addBlock(new MessageBlockMenuPlayerId( 0x00000001L,
				// QUERIES.MENU_PLYGRP_SERVER_QUERY + 1, 0));
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
			} else if (qId == 0x00004210) {
				out
						.log("MATCHES GAME GETPLYLST CLIENT QUERY ANSWERING PLAYERLIST:");
				sendPlayerList( channel );

			} else if (qId == 0x00004300) {
				out
						.log("MATCHES GAME GETROOMLST CLIENT QUERY ANSWERING ROOMLIST:");
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004301));
				out.streamWriteEncodedNIO( mes );
				//connections.sendToChannelNotInRoomEncodedNIO( channel, mes );
				
				/*
				 * COMPROBADO; HAY QUE ENVIARSELO A TODO EL MUNDO
				 */

				/*
				 * Aqui viene la lista de salas creadas
				 */
				mes = new Message();

				mes.addBlock(new MessageBlockGameRoomList(rooms
						.getRoomsInChannelArray( channel ), 0x00004302));
				//connections.sendToChannelNotInRoomEncodedNIO( channel, mes );
				out.streamWriteEncodedNIO( mes );
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004303));
				//connections.sendToChannelNotInRoomEncodedNIO( channel, mes );
				out.streamWriteEncodedNIO( mes );
			} else if (qId == 0x00004102) {
				/*
				 * Recuperamos el Id
				 */

				byte[] data = mb.getData();
				long id = new DataId(data).getId();
				out.log("MATCHES GAME GETPLYANDGROUP CLIENT QUERY ID: " + id
						+ " ANSWERING PLAYERANDGROUP INFO:");
				mes = new Message();

				/*
				 * Es posible que no exista, tengo trazas que indican que eso
				 * está ocurriendo
				 */
				PlayerInfo tmp = players.getPlayerInfo(id);
				if( tmp == null ) {
					/*
					 * Si no existe cargamos su informacion de BD
					 */
					try {
						tmp = Tools.dbLoadPlayerAndGroupInfo( db, id, default_settings );
					} catch( SQLException ex ) {
						ex.printStackTrace();
						out.log( "THE PLAYER WASNT ONLINE AND WE FAILED TO LOAD HIS DATABASE INFO");
						out.log( ex.toString() );
						return;
					}
				}
				if( tmp != null ) {
					mes.addBlock(new MessageBlockGamePlayerGroup(tmp, tmp.getGroupInfo(), 0x00004103));
					out.streamWriteEncodedNIO(mes);
				} else {
					out.log( "THE PLAYER ID DOESNT EVEN EXIST ON THE DATABASE!, RETURNING");
					return;
				}
			} else
			// No se lo que es esto, pero se me pregunta al final y con
			// responderle un void el cliente
			// parece estar de acuerdo
			if (qId == 0x0000308C) {
				out.log( "READ 0x308C Leave the lobby?");
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000308D));
				out.streamWriteEncodedNIO(mes);
			} else

			// El jugador est� cambiando su comentario, el comentario es el
			// contenido del paquete y un 0x00 al final
			if (qId == 0x00004110) {
				out
						.log("MATCHES GAME CHANGECOMMENT CLIENT QUERY ANSWERING ZEROS:");

				/*
				 * Recuperamos el comentario
				 */

				String comment = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				player.setComment(comment);
//				try {
//					Tools.dbSavePlayerComment(db, player);
//				} catch (SQLException ex) {
//					out.log("UNABLE TO SAVE THE COMMENT!!!!");
//					ex.printStackTrace();
//				}
				mes = new Message();
				mes.addBlock(new MessageBlockZero(++qId));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000434D) {
				out
						.log("MATCHES GAME ROOM CHANGE NAME AND PASS QUERY ANSWERING WITH NEW ROOM INFO:");
				DataRoom room = new DataRoom(mb.getData());
				RoomInfo r = rooms.searchPlayer(player.getId());
				r.setPassword(room.getPassword());
				r.setName(room.getName());
				r.setPassworded(room.hasPassword());
				//rooms.setRoom(r.getId(), r);
				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000434E));
				out.streamWriteEncodedNIO(mes);

			} else if (qId == 0x00004380) {
				out
						.log("MATCHES GAME FORCE DISCARD PARTICIPATION QUERY ANSWERING WITH NEW ROOM INFO:");
				DataId did = new DataId(mb.getData());

				rooms.removePlayerSelInRoom(did.getId());
				RoomInfo r = rooms.searchPlayer(did.getId());

				if( r == null ) {
					out.log( "THE ROOM DOESNT EXIST!!! RETURNING");
					return;
				}
				
				mes = new Message();
				mes
						.addBlock(new MessageBlockGameRoomInfoShort(r,
								0x00004365));
				// Es posible que vaya para todo el mundo mundial
				// connections.sendToEncodedNIO( channelCons, mes);
				connections.sendEncodedToNIO(r.getPlayerIds(), mes);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004381));
				out.streamWriteEncodedNIO(mes);

			} else if (qId == 0x00004349) {
				out
						.log("MATCHES GAME CHANGE ROOM OWNER QUERY ANSWERING WITH NEW ROOM INFO:");
				DataId did = new DataId(mb.getData());
				RoomInfo r = rooms.searchPlayer(player.getId());
				r.swapPositions(player.getId(), did.getId());
				//rooms.setRoom(r.getId(), r);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000434A));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				mes.addBlock(new MessageBlockLong(did.getId(), 0x0000434C));
				connections.sendEncodedToNIO(r.getPlayerIds(), mes);

				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );
			} else if (qId == QUERIES.GAME_CRTROOM_CLIENT_QUERY) {
				/*
				 * Lo primero es el nombre, creo que de 64 bytes y luego 17 mas
				 * que no se lo que son
				 */
				out
						.log("MATCHES GAME CRTROOM CLIENT QUERY ANSWERING WITH NEW ROOM INFO:");

				DataRoom room = new DataRoom(mb.getData());
				RoomInfo r = new RoomInfo(roomIdx++, channel, 1, room.hasPassword(),
						room.getPassword(), room.getName(), new long[] { player
								.getId() });
				rooms.addRoom(r);
				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				mes.addBlock(new MessageBlockMenuPlayerGroup(player, r.getId(),
						QUERIES.GAME_PLYGRP_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);

				/*
				 * DEBO REPONER ESTO O NO FUNCIONARA!!!!
				 * 
				 */

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004311));
				mes.addBlock(new MessageBlockZero(0x00004346));

				mes.addBlock(new MessageBlockGameIpInfo(r,
						new IpInfo[] { player.getIpInfo() },
						0x00004347));
				mes.addBlock(new MessageBlockZero(0x00004348));
				out.streamWriteEncodedNIO(mes);
				
				//sendRoomList( channel );

			} else if (qId == 0x00004345) {
				// He visto un caso en el que me pasan el id de la habitacion
				// Significa: listame la informaci�n de ip de todos los que
				// est�n en la sala en la
				// que yo estoy.
				// Hmmmm, tambien ocurre un rato despues de entrar en una
				// sala...

				/*
				 * OJO EL CLIENTE EN SU QUERY DEBERIA PASARME AQUI EL ID DE LA
				 * SALA CREADA Y NO LA MIERDA DE ZEROS QUE ME ESTA PASANDO,
				 * LUEGO HAY ALGUN ERROR EN LO ANTERIOR
				 */
				out
						.log("MATCHES WEIRD AFTER CRTROOM CLIENT QUERY ANSWERING MY IP INFO:");

				DataId did = new DataId(mb.getData());
				RoomInfo r = rooms.getRoom(did.getId());
				
				sendRoomList( channel );
				
				if( r != null ) {		
					// RoomInfo r = rooms.searchPlayer(player.getId());
					IpInfo ips[] = new IpInfo[r.getNumPlayers()];
					for (int i = 0; i < r.getNumPlayers(); i++) {
						ips[i] = players.getPlayerInfo(r.getPlayerId(i)).getIpInfo();
					}
	
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00004346));
					connections.sendEncodedToNIO(r.getPlayerIdsNotInMatch(), mes);
					//out.streamWriteEncodedNIO( mes );
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameIpInfo(r, ips,
							QUERIES.GAME_IPINFO_SERVER_QUERY));
	
					connections.sendEncodedToNIO(r.getPlayerIdsNotInMatch(), mes);
					//out.streamWriteEncodedNIO( mes );
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00004348));
					connections.sendEncodedToNIO(r.getPlayerIdsNotInMatch(), mes);
					//out.streamWriteEncodedNIO( mes );
					out.log("HE TERMINADO EL ROLLO WEIRD!!!!");
					
					/*
					 * Actualizacion de las salas para todo el mundo
					 */
					//System.out.println( "estoy en weird, y envio a todo el mundo la info de salas");		
					
				} else {
					out.log( "THE ROOM NO LONGER EXISTS!!, DOING NOTHING!" );
				}
			
			} else if (qId == 0x00004B00) { // Me da la informaci�n de direccion
											// IP de la id que consulto (4 bytes
											// de payload)
				out.log("MATCHES 0x00004B00 ANSWER IP INFO:");
				DataId did = new DataId(mb.getData());
				// Dos veces en el c�digo, mismo query id
				mes = new Message();
				/*
				 * Excepcion, no tengo la info de ip de este señor, porque?
				 */
				PlayerInfo p = players.getPlayerInfo(did.getId());
				if( p == null ) {
					out.log( "No se encuentra el player con ID: " + did.getId());
					return;
				}
				IpInfo tmp = players.getPlayerInfo(did.getId()).getIpInfo();
				
				if( tmp != null ) {
					mes.addBlock(new MessageBlockGameIpInfoShort(tmp, 0x00004B01));
					out.streamWriteEncodedNIO(mes);
	
					/*
					 * Si si, dos veces menuda broma pero es asi tienen secuencias
					 * diferentes
					 */
	
					/*
					 * Los logs dicen que es dos veces, deberíamos probar a comentarlo de cualquier forma
					 * solo por probar. 27 abril 2009, sin subirse al server
					 */
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameIpInfoShort(tmp, 0x00004B01));
					out.streamWriteEncodedNIO(mes);
				} else {
					out.log( "THERE ISNT ANY IPINFO FOR THE SELECTED PLAYER, RETURNING");
					return;
				}
			} else if (qId == 0x0000432A) { // Salir del salon de juego!
				out.log("MATCHES 0x0000432A (Exit room) ANSWERING ????:");

				RoomInfo r = rooms.searchPlayer(player.getId());
				
				if( r == null ) {
					out.log( "THE ROOM DOESNT EXIST!, RETURNING");
					return;
				}
				
				rooms.removePlayerFromRoom(r.getId(), player.getId());

				if (rooms.getRoom(r.getId()).getNumPlayers() == 0) {
					out.log("REMOVING ROOM");
					rooms.removeRoom(r.getId());

					mes = new Message();
					mes.addBlock(new MessageBlockGameAnyId(r.getId(),
							0x00004305));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );

					mes = new Message();
					mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
							QUERIES.GAME_PLYGRP_SERVER_QUERY));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );

					// SEGURO??????
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x0000432B));

					connections.sendToChannelNotInGameEncodedNIO( channel, mes );

				} else {

					/*
					 * 
					 * PARA MI
					 * 
					 * 
					 */

					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomList(
							new RoomInfo[] { r },
							QUERIES.GAME_ROOMINFO_SERVER_QUERY));
					out.streamWriteEncodedNIO(mes);

					mes = new Message();
					mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
							QUERIES.GAME_PLYGRP_SERVER_QUERY));
					out.streamWriteEncodedNIO(mes);

					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x0000432B));
					out.streamWriteEncodedNIO(mes);

					/*
					 * 
					 * 
					 * PARA LOS DEMAS!
					 * 
					 * 
					 * 
					 */

					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomInfoShort(r,
							0x00004365));

					connections.sendToChannelNotInGameEncodedNIO( channel, mes );

					mes = new Message();
					mes.addBlock(new MessageBlockLong(r.getPlayerId(0),
							0x0000434C));

					connections.sendEncodedToNIO(r.getPlayerIds(), mes);

					mes = new Message();
					mes.addBlock(new MessageBlockGamePlayerId(player.getId(),
							0x00004331));
					mes.addBlock(new MessageBlockGameRoomList(
							new RoomInfo[] { r },
							QUERIES.GAME_ROOMINFO_SERVER_QUERY));
					mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
							QUERIES.MENU_PLYGRP_SERVER_QUERY));

					connections.sendToChannelNotInGameEncodedNIO( channel, mes );

				}

			} else if (qId == 0x00004320) { // Entrar en un salon de juego!
				out.log("MATCHES 0x00004320 (Enter room) ANSWERING ????:");

				DataJoinRoom droom = new DataJoinRoom(mb.getData());

				RoomInfo r = rooms.getRoom(droom.getRoomId());

				if( r == null ) {
					out.log( "THE ROOM NO LONGER EXISTS, RETURNING");
					return;
				}
				
				if (r.getNumPlayers() == 4) {
					mes = new Message();
					mes.addBlock(new MessageBlockGameEnterRoomFailFull(
							0x00004321));
					out.streamWriteEncodedNIO(mes);
				} else

				if (r.hasPassword()
						&& !r.getPassword().equals(droom.getPassword())) {
					mes = new Message();
					mes.addBlock(new MessageBlockGameEnterRoomFailPass(
							0x00004321));
					out.streamWriteEncodedNIO(mes);

				} else {

					r.addPlayer( player.getId() );

					/*
					 * NO ES NECESARIO ENVIAR NADA DE ESTO, COMPROBADO A 31 - 05 - 2009
					 */
//					mes = new Message();
//
//					mes
//							.addBlock(new MessageBlockGameRoomList(
//									new RoomInfo[] { r }, 0x00004306));
					//out.streamWriteEncodedNIO(mes);
					/*
					 * Prueba, voy a enviar la información de la sala a tods
					 * la sala
					 */
					//connections.sendEncodedToNIO( r.getPlayerIds(), mes );
					
					for (int i = 0; i < r.getNumPlayers(); i++) {
						PlayerInfo p = players.getPlayerInfo(r.getPlayerId(i));
						if( p != null ) {
							if( p.getGroupInfo() != null ) {
								mes = new Message();
								mes.addBlock(new MessageBlockMenuPlayerGroup(p
										, r.getId(),
										0x00004222));
								out.streamWriteEncodedNIO(mes);
								//connections.sendEncodedToNIO( r.getPlayerIds(), mes );
							} else {
								out.log( "El jugador tiene NULL como grupo!");
							}
						} else {
							out.log( "No existe el jugador " + i + " en la sala --> p = null");
						}

					}

					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomPos(r
							.getPlayerIdx(player.getId()), 0x00004321));
					mes.addBlock(new MessageBlockZero(0x00004346));

					IpInfo[] ips = new IpInfo[r.getNumPlayers()];
					for (int i = 0; i < r.getNumPlayers(); i++) {
						PlayerInfo p = players.getPlayerInfo(r.getPlayerId(i));
						if( p != null && p.getIpInfo() != null )
							ips[i] = p.getIpInfo();
						else {
							out.log( "Bugged room, some player doesn't exist here, we will force a disconnect here!" );
							throw new IOException( "Trying to join a bugged room, disconnecting");
						}
					}

					mes.addBlock(new MessageBlockGameIpInfo(r, ips, 0x00004347));
					mes.addBlock(new MessageBlockZero(0x00004348));
					out.streamWriteEncodedNIO(mes);
					/*
					 * Prueba, voy a enviar la información de ip a toda
					 * la sala
					 */
					//connections.sendEncodedToNIO( r.getPlayerIds(), mes );
					
					//byte[] data;
					/*
					 * A ver si sale el tiempo bien de una puta vez,
					 * DADO QUE ESTO ESTÁ COMPROBADO QUE NO FUNCIONA LO OBVIAMOS
					 */
//					if ((data = r.getMatchSettings()) != null) {
//						mes = new Message();
//						MessageBlock m = new MessageBlockVoid(0x0000436E);
//						m.setData(data);
//						mes.addBlock(m);
//						out.streamWriteEncodedNIO(mes);
//					}
//					sendRoomList( channel );
				}

			} else
			// Envio mensaje
			if (qId == 0x00004400) {
				
				DataChatMessage msg = new DataChatMessage(mb.getData());
				out.log("RECIBO UN MENSAJE, SE LO ENVIO A LOS DESTINATARIOS Canal: " + msg.getChannel() + " Place: " +
						msg.getPlace() );
				
				String str = msg.getMessage();
				str = str.trim();
				
				if( str.equals("") ) {
					return;
				}
				
				if( str.charAt(0) == '#' && player.isAdmin()) {
					/*
					 * Comandos de administracion
					 */
					String[] cmdArg = str.split(" ", 2);
					cmdArg[0] = cmdArg[0].toLowerCase();
					if( cmdArg.length > 1  && cmdArg[0].equals( "#kick" )) {
						String target = cmdArg[1];
						PlayerInfo pg = players.getPlayerInfo(target);
						if( pg != null && !pg.isAdmin()) {
							PESConnection con1 = connections.searchConnection(pg.getId());
							if( con1 != null ) disconnectPlayer(con1);
						}
//					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#ban" )) {
//						String target = cmdArg[1];
//						PlayerInfo pg = players.getPlayerInfo(target);
//						if( pg != null && !pg.isAdmin()) {
//							try {
//								dbBanPlayerAccount(pg);
//							} catch( SQLException ex ) {
//								out.log( "Couldnt BAN the account, SQL Error");
//								ex.printStackTrace();
//							}
//							PESConnection con1 = connections.searchConnection(pg.getId());
//							if( con1 != null ) disconnectPlayer(con1);
//						}
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#supersay" )) {
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)0x02, cmdArg[1],
								0x00004402));
						connections.sendToAllEncodedNIO( mes );
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)0x01, cmdArg[1],
								0x00004402));
						connections.sendToAllEncodedNIO( mes );
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#getusername" )) {
						PlayerInfo pl = channel.getPlayerList().getPlayerInfo( cmdArg[1] );
						String message = null;
						if( pl == null ) {
							message = "Can't find that player in the lobby.";
						} else {
							PESConnection c = connections.searchConnection( pl.getId() );
							if( c == null ) {
								message = "There was an error retrieving the player's connection.";
							} else {
								message = cmdArg[1]+" is " + c.getUserInfo().getUsername();
							}
						}
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								0x00004402));
						out.streamWriteEncodedNIO(mes);
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#getipaddress" )) {
						PlayerInfo pl = channel.getPlayerList().getPlayerInfo( cmdArg[1] );
						String message = null;
						if( pl == null ) {
							message = "Can't find that player in the lobby.";
						} else {
							PESConnection c = connections.searchConnection( pl.getId() );
							if( c == null ) {
								message = "There was an error retrieving the player's connection.";
							} else {
								message = cmdArg[1]+" ip: " + c.getSocket().getInetAddress().getHostAddress();
							}
						}
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								0x00004402));
						out.streamWriteEncodedNIO(mes);
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#shutdown" )) {
						
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
						Date d = null;
						String message = "Shutdown scheduled for " + cmdArg[1];
						try {
							d = sdf.parse( cmdArg[1] );
						} catch( ParseException pe ) {
							message = "There was an error parsing the time argument.";
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(player,  
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									0x00004402));
							out.streamWriteEncodedNIO(mes);
							return;
						}
						
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								0x00004402));
						out.streamWriteEncodedNIO(mes);
//						System.out.println( "yo tenía: \"" + cmdArg[1] + "\"");
//						System.out.println( "aver: " + sdf.format( d ));
//						System.out.println( "-----hoy----: " + Util.getTimeOfToday());
//						System.out.println( "-----d------: " + d.getTime());
//						System.out.println( "--hoy+d-----: " + (Util.getTimeOfToday() + d.getTime()));
//						System.out.println( "---now------: " + new Date().getTime());
						shutdownTime = (Util.getTimeOfToday() + d.getTime());

					}
				} else {
				
					/*
					 * Gran mentira, el canal no son dos bytes, el primer byte indica donde estan
					 * 0 = lobby, 1 = sala
					 * 
					 * canal 3 y 2 = privado
					 * canal 4 = grupo
					 * canal 8, place 1 (sala) = Sala en general
					 * canal 7, place 1 (sala) = estadio
					 * canal 5, place 1 (sala) = partido
					 * canal 9, place 1 (sala) = equipo
					 */
					mes = new Message();
					mes.addBlock(new MessageBlockGameChatMessage(player,  
							(byte)msg.getPlace(), (byte)msg.getChannel(), msg.getMessage(),
							0x00004402));
					if (msg.getChannel() == 0x00000001)
						connections.sendToChannelNotInGameEncodedNIO( channel, mes );
					else if (msg.getChannel() == 4 ) {
						/*
						 * Mensaje para el grupito!!! de momentorg se lo envio a
						 * todos
						 */
						connections.sendEncodedToNIO(player.getGroupInfo().getPlayerIds(), mes );
						
						//connections.sendToEncodedNIO( channelCons, mes);
					} else if( msg.getChannel() == 3 || msg.getChannel() == 2) {
						if( str.charAt(0) == '/') {
							String[] cmdArg = str.split(" ", 2);
							if( cmdArg.length > 1 && !cmdArg[1].trim().equals("")) {
								String plName = new String( cmdArg[0].toCharArray(), 1, cmdArg[0].length() - 1 );
								PlayerInfo target = players.getPlayerInfo( plName );
								if( target != null ) {
									out.streamWriteEncodedNIO(mes);
									connections.searchConnection(target.getId()).streamWriteEncodedNIO(mes);
								}
							}
						}
					} else if (msg.getChannel() == 7 && msg.getPlace() == 1) {
						/*
						 * Nuevo, probablemente chat al equipo
						 */
						RoomInfo r = rooms.searchPlayer(player.getId());
						if( r == null ) return;
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					} else if (msg.getChannel() == 8 && msg.getPlace() == 1) {
						RoomInfo r = rooms.searchPlayer(player.getId());
						if( r == null ) return;
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					} else if (msg.getChannel() == 5 && msg.getPlace() == 1) {
						RoomInfo r = rooms.searchPlayer(player.getId());
						if( r == null ) return;
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					} else if( msg.getChannel() == 9 && msg.getPlace() == 1) {
						// Charla al equipo
						RoomInfo r = rooms.searchPlayer(player.getId());
						if( r!= null ) {
							int myteam = r.getPlayerTeam( player.getId());
							long[] pids = r.getPlayersFromTeam( myteam );
							connections.sendEncodedToNIO(pids, mes);
						}
					} else {
						RoomInfo r = rooms.searchPlayer(player.getId());
						if (r != null)
							connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					}
				}
			} else
			// Quiero participar o anular, recibo el orden de participacion
			// segun el cliente
			if (qId == 0x00004363) {
				out.log( "RECEIVE 4563 PARTICIPATE IN A MATCH (OR CANCEL PARTICIPATION)");
				// byte[] orden = mes.getBlock(0).getData();
				byte[] orden = mb.getData();
				int sel = 0x00FF;
				int opcion = 0;

				RoomInfo r;
				r = rooms.searchPlayer(player.getId());
				if( r == null ) {
					out.log( "CANNOT FIND THE ROOM IN WHICH THE PLAYER IS, RETURNING" );
					return;
				}
				
				if (orden[0] == (byte) 0x01) {
					opcion = 1;
					rooms.setPlayerSelInRoom(player.getId());
					sel = r.getPlayerSel(r.getPlayerIdx(player.getId()));
				} else {
					opcion = 0;
					sel = 0x00FF;
					rooms.removePlayerSelInRoom(player.getId());
				}
				// mes = new Message();
				// mes.addBlock(new MessageBlockGameRoomInfoShort( r,
				// 0x00004365, 0));
				// //connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				// connections.sendToEncodedNIO( channelCons, mes);

				/*
				 * No se como se da el caso, pero el es posible que el tipo
				 * no este en ninguna habitación según el servidor
				 */
				
				mes = new Message();
				mes
						.addBlock(new MessageBlockGameRoomInfoShort(r,
								0x00004365));
				// connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );

				out.log("RESPONDO: ");
				out.log(Util.toHex(mes.getBytes()));

				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomOptionSel(opcion, sel,
						0x00004364));
				out.streamWriteEncodedNIO(mes);

				out.log("RESPONDO: ");
				out.log(Util.toHex(mes.getBytes()));

				// mes = new Message();
				// mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] {
				// r
				// }
				// , QUERIES.GAME_ROOMINFO_SERVER_QUERY, 0));
				// out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004360) {
				out.log("READ START MATCH, ANSWER ??:");
				RoomInfo r = rooms.searchPlayer(player.getId());
				r.setGamePhase(3);
				// HYPER EXPERIMENTAL!!!!
				//r.setGamePhase(2);
				
				r.setType(2);
				
				//rooms.setRoom(r.getId(), r);
				mes = new Message();
				mes.addBlock(new MessageBlockGameStart1(r, 0x00004362));
				// out.streamWriteEncodedNIO(mes);
				/*
				 * Cambio de idea, mejor enviarselo a todos... creo que esto
				 * actualiza el que le salga el baloncillo a la gente antes de
				 * que yo entre en la sala
				 */
				// connections.sendToEncodedNIO( channelCons, mes);
				connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				mes.addBlock(new MessageBlockZero(0x00004361));
				out.streamWriteEncodedNIO(mes);

				sendRoomList(channel);
				/*
				 * No es necesario parece ser
				 */
				// mes = new Message();
				// mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r
				// }, 0x00004606, 0));
				// connections.sendToEncodedNIO( channelCons, mes);
			} else if (qId == 0x00004350) {
				out
						.log("RECEIVING PLAYER MOVE TEAM CLIENT QUERY, SENDING THE SAME TO PLAYERS IN ROOM:");
				RoomInfo r = rooms.searchPlayer(player.getId());
				mes = new Message();
				mes.addBlock(mb);
				if (r != null) {
					connections.sendEncodedToNIO(r.getPlayerIdsExcept(player.getId()), mes );
					//connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				}
			} else if (qId == 0x00004351) {
				out
						.log("RECEIVING 4351 WATCH A MATCH, BEHAVING THE SAME AS WITH 4350:");
				RoomInfo r = rooms.searchPlayer(player.getId());
				mes = new Message();
				mes.addBlock(mb);
				/*
				 * Cambio de idea, se lo voy a enviar a todo kisk NOOO, ESTO
				 * GENERA UN CRITICAL!!!!
				 */
				// if( r != null ) {
				// connections.sendEncodedToNIO(r.getPlayerIds(), mes );
				// }
				if (r != null) {
					connections.sendEncodedToNIO(r.getPlayerIdsExcept(player
							.getId()), mes);
				}

				// if( r != null ) {
				// connections.sendEncodedToNIO(r.getPlayerIdsWatching(), mes );
				// }
			} else if (qId == 0x00004366) {
				/*
				 * Notas en el partido que vi a marba evel y ring, esto tiene un
				 * solo byte de payload y vale 01
				 */
				out
						.log("RECEIVING 4366 WATCH A MATCH, ANSWERING WITH a 4367 payload 00 00 00 00 whatever query said:");
				byte sel = mb.getData()[0];

				mes = new Message();
				MessageBlock mb1 = new MessageBlockVoid(0x00004367);
				mb1.setData(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00,
						(byte) 0x00, sel });
				mes.addBlock(mb1);
				out.streamWriteEncodedNIO(mes);

				RoomInfo r = rooms.searchPlayer(player.getId());
				if (r != null) {
					r.setSpect(player.getId(), sel);
					//rooms.setRoom(r.getId(), r);
				}
				/*
				 * Se lo envio a todos porque marba nunca envio el 4366
				 * CORRECCION ES FALSO, MARBA SIGUE CON BUG IGUALMENTE
				 */
				// RoomInfo r = rooms.searchPlayer(player.getId());
				// if( r != null )
				// connections.sendEncodedToNIO(r.getPlayerIds(), mes );
			} else if (qId == 0x00004373) {
				out.log("RECEIVING 4373 THE CLIENT CHOOSED A TEAM!");

				int team = Util.word2Int(mb.getData()[0], mb.getData()[1]);
				RoomInfo r = rooms.searchPlayer(player.getId());

				if( r == null ) {
					out.log( "THE ROOM DID NOT EXIST, RETURNING");
					return;
				}
				
				if (r.getPlayerTeam(player.getId()) == 0)
					r.setTeam2(team);
				if (r.getPlayerTeam(player.getId()) == 1)
					r.setTeam1(team);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004370));
				out.streamWriteEncodedNIO(mes);

				r.setReadyStatus(player.getId(), 1);
				//rooms.setRoom(r.getId(), r);

				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						0x00004306));
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );

				if (r != null && r.getGamePhase() >= 4 && r.allTeamsReady()) {
					r.setGamePhase(5);
					r.resetAllReady();
				}
			} else if (qId == 0x0000436F) {
				out
						.log("RECEIVING PLAYER READY CLIENT QUERY, ANSWERING DUNNO: ");

				int sel;

				/*
				 * La madre que me pari�, me han eviado un 04 en lugar de 1 o 0
				 * esto ocurrio al darle a jugar otro partido.
				 * 
				 * Quizá el enviar el mismo número que me mandaron arregle el bug
				 * del señor que entra mientras elegimos bando.
				 * 
				 * MIRAR ESTO!
				 */
				sel = (int)mb.getData()[0];

				RoomInfo r = rooms.searchPlayer(player.getId());
				
				if( r == null ) {
					out.log( "CANNOT FIND THE ROOM ON WHICH THIS PLAYER IS, PLAYER ID: " + player.getId() + ", RETURNING");
					return;
				}
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004370));
				out.streamWriteEncodedNIO(mes);

				// if( r.getGamePhase() < 4 ) {
				r.setReadyStatus(player.getId(), sel);

				//rooms.setRoom(r.getId(), r);
				// }
				mes = new Message();
				mes.addBlock(new MessageBlockGamePlayerReady(player.getId(),
						sel, 0x00004371));

				/*
				 * No hace falta enviarselo a todos los de la sala solo a todos
				 * menos yo mismo. Hmmm tal vez solo a los que están participando.
				 */
				if (r != null)
//					connections.sendEncodedToNIO(r.getPlayerIdsOnSelExcept(player
//							.getId()), mes);
					connections.sendEncodedToNIO(r.getPlayerIdsExcept(player
							.getId()), mes);

				/*
				 * Pasar a selección de equipos
				 */
				if( r != null && r.getGamePhase() == 7 && r.allReadyOn(3) ) {
					out
					.log("GO TO TEAM SELECTION!");
					r.setGamePhase(5);
					r.resetAllReady();
					r.resetGoals();
					r.setType(4);
					sendRoomUpdate(channel,r);
				}
				
				/*
				 * Jugar otro partido
				 */
				if( r != null && r.getGamePhase() == 7 && r.allReadyOn(4) ) {
					out
					.log("PLAY ANOTHER MATCH!");
					r.resetGoals();
					r.resetAllReady();
					player.setOldCategory( player.getCategory() );
					player.setOldPoints( player.getPoints() );
					/*
					 * Con 6 funciona
					 */
					r.setType(5);
					r.setGamePhase(7);
					sendRoomUpdate(channel,r);
				}
				
				if (r != null && r.allReadyOn(1) && r.getGamePhase() == 3) {
					out
							.log("ALL PLAYERS ARE READY, SENDING 4344 BLOCK TO ALL!");
					//r.setGamePhase(3);
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameStart4344(
							r.getGamePhase(), 0x00004344));

					connections.sendEncodedToNIO(r.getPlayerIds(), mes);

					r.resetAllReady();
					r.resetGoals();
					
					//rooms.setRoom(r.getId(), r);
				} else if (r != null && r.getGamePhase() > 3
						&& r.getGamePhase() < 7 && r.allReadyOn(1)) {

					out.log("PASO CON FASE: " + r.getGamePhase());
					mes = new Message();
					mes.addBlock(new MessageBlockGameStart4344(
							r.getGamePhase(), 0x00004344));
					// out.streamWriteEncodedNIO(mes);
					connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					// connections.sendEncodedToNIO(r.getPlayerIdsOnSel(), mes);
					// connections.sendEncodedToNIO(r.getPlayerIds(), mes);

					/*
					 * Hay que actualizar la info general de la sala, el tipo
					 * ahora es 7 el byte de detras del siete indica en que
					 * parte est�n jugando
					 */

					r.resetAllReady();

					if( r.getGamePhase() == 6 ) {
						long[] pids = r.getPlayerIdsOnSel();
						for( int i = 0; i < pids.length; i++ ) {
							PlayerInfo p = players.getPlayerInfo( pids[i] );
							if( p != null ) {
								p.setOldCategory( p.getCategory() );
								p.setOldPoints( p.getPoints() );
							}
						}
						r.resetGoals();
						r.setPeopleOnMatch( r.getNumPlayersOnSel() );
					}
					
					if (r.getGamePhase() > 4)
						r.setGamePhase(r.getGamePhase() + 1);

					//rooms.setRoom(r.getId(), r);
				} else if (r != null && r.getGamePhase() == 7) {
					// Cambio, si un tio se va del partido se va todo kisk
//					if (r.allReadyOn(0)) {
					if (sel == 0) {

						out.log( "CLEANING ROOM STATUS" );
						r.setType(1);
						r.setGamePhase(1);
						r.resetAll();

						sendRoomList( channel );
						/*
						 * No se si es necesario
						 */
						// mes = new Message();
						// mes.addBlock(new MessageBlockGameRoomList(new
						// RoomInfo[] { r }, 0x00004306, 0));
						// connections.sendToEncodedNIO( channelCons, mes);
						//rooms.setRoom(r.getId(), r);
					} else if (r.allReadyOn(1)) {
						/*
						 * No solo a mi, sino a todo el que este participando
						 * y esté listo.
						 */
						
						long[] pids = r.getPlayerIdsOnSel();
						for( int i = 0; i < pids.length; i++ ) {
							PlayerInfo p = players.getPlayerInfo( pids[i] );
							if( p != null ) {
								p.setOldCategory( p.getCategory() );
								p.setOldPoints( p.getPoints() );
							}
						}
						r.resetGoals();
						r.setPeopleOnMatch( r.getNumPlayersOnSel() );
						//out.log("PASO CON FASE: " + r.getGamePhase());
//						logger.log("PASO CON FASE: " + r.getGamePhase());
						mes = new Message();
						mes.addBlock(new MessageBlockGameStart4344(r
								.getGamePhase(), 0x00004344));
						// out.streamWriteEncodedNIO(mes);
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!");
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!");
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!");
						r.setType(7);
						r.setHalf(1);
						sendRoomList( channel );
//						mes = new Message();
//						mes.addBlock(new MessageBlockGameRoomList(
//								new RoomInfo[] { r }, 0x00004306));
//						connections.sendToChannelNotInRoomEncodedNIO( channel, mes );
						//rooms.setRoom(r.getId(), r);
					}
				}

			} else if (qId == 0x00004369) {
				out.log("RECEIVING GAME START PLAYER FINAL POSITIONS: ");

				
				/*
				 * Debo recuperar donde se colocaron los jugadores en la
				 * seleccion de mu�equitos.
				 * 
				 * Por lo que se ve y por este orden: - id del jugador (4 bytes) -
				 * 4 mas: primer byte: id del equipo en el que est� empezando en
				 * 0 ultimo byte: indica si es lider del equipo 1 = lider, 0 =
				 * no lider osea para un 2vs2: 00 00 00 01 y 01 00 00 01 y 00 00
				 * 00 00 y 01 00 00 00
				 */

				RoomInfo r = rooms.searchPlayer(player.getId());
				DataPlayerTeamPositions dtpos = new DataPlayerTeamPositions(mb
						.getData());

				for (int i = 0; i < r.getNumPlayersOnSel(); i++) {
					long pid = r.getPlayerIdOnSel(i);
					r.setPlayerTeam(pid, dtpos.getPlayerTeam(pid));
					r.setPlayerTeamPos(pid, dtpos.getPlayerTeamPos(pid));
				}

				byte[] newdata = new byte[mb.getData().length + 1];
				newdata[0] = (0x00);
				System.arraycopy(mb.getData(), 0, newdata, 1,
						mb.getData().length);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000436A));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				MessageBlock m = new MessageBlockVoid(0x0000436B);
				m.setData(newdata);
				mes.addBlock(m);

				connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				r.setGamePhase(4);
				// Cambio el type! ahora es 0x0300, nuevo en ver partido
				r.setType(3);
				//rooms.setRoom(r.getId(), r);
				
				/*
				 * Se lo env�o a todo kisk, esto es nuevo para "ver partido" o
				 * sin verlo es para los que esten fuera
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						0x00004306));
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );

			} else if (qId == 0x0000436C) {
				// Puede tener relaci�n con los 4 ultimos bytes del mensaje de
				// habitaci�n
				out
						.log("RECEIVING GAME SETTINGS INFO ANSWERING JUST THE SAME: ");
				byte[] data = mb.getData();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000436D));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				MessageBlock m = new MessageBlockVoid(0x0000436E);
				m.setData(data);
				mes.addBlock(m);
				RoomInfo r = rooms.searchPlayer(player.getId());
				connections.sendEncodedToNIO(r.getPlayerIds(), mes);

				r.setMatchSettings(data);
				//rooms.setRoom(r.getId(), r);

				/*
				 * Si estabamos en fase 4 debo cambiar el roomtype a 0x0400
				 */
				if (r.getGamePhase() == 4) {
					r.setType(4);
					// r.setLastBytes(0x00020100);
					r.setLastBytes(0x0000000000020100L);
					// r.setLastBytes(0x00010100);
					//rooms.setRoom(r.getId(), r);

					/*
					 * Prueba psicod�lica
					 */
					// r.setTime(5);
					// rooms.setRoom(r.getId(), r);
					/*
					 * NO SE SI ESTO ES NECESARIO
					 */
					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomList(
							new RoomInfo[] { r }, 0x00004306));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );
				}

			} else if (qId == 0x00003087) {
				out.log("BEFORE SAVING USER SETTINGS: ");

//				byte[] data = mb.getData();
//				long pid = Util.word2Long(data[4], data[5], data[6], data[7]);
				player.resetSettings();
			} else if (qId == 0x00003088) {
				out.log("ME ENVIAN LA LISTA DE MENSAJETES: ");
				/*
				 * Quiza me envien los settings en varios bloques, no solo uno
				 */
				try {
					player.insertSettings(mb.getData());
				} catch( Exception ex ) {
					ex.printStackTrace();
				}
					//player.setSettings(mb.getData());

			} else if (qId == 0x00003089) {
				out
						.log("HAN TERMINADO DE ENVIARME LOS MENSAJES RESPONDO 308B con zeros");
//				try {
//					Tools.dbSavePlayerSettings(db, player);
//				} catch (SQLException ex) {
//					ex.printStackTrace();
//				}
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000308B));
				out.streamWriteEncodedNIO(mes);
			} else

			if (qId == 0x00004383) {
				// SE ACABA EL PARTIDOOORLL
				
				RoomInfo r = rooms.searchPlayer(player.getId());
				
				if( r != null ) {	
					/*
					 * Ahora vamos con los puntos en si, se los actualizamos
					 * a todo dios, y si hay que hacerlo mas de una vez se hace.
					 */
					
					long ids[] = r.getPlayerIdsOnSel( );
					for( int i = 0; i < ids.length; i++ ) {
						PlayerInfo p = channel.getPlayerList().getPlayerInfo( ids[i] );
						if( p!= null ) updateProfileAndPoints( p, r );
					}
					
					// Hacemos los calculos de puntos y recuperamos cuantos nos han dado
					// Ej 36 al ganador y 12 al perdedor
					
					/*
					 * Esto también creo que le llega al espectador, comprobar que no
					 * sea un puto espectador
					 */
					int idx = r.getPlayerIdx(player.getId());
					if( idx != -1 && r.getSpectIdx(idx) == 0 ) {				
						if( r.getPeopleOnMatch() == 2 && r.getNumPlayersOnSel() > 1 ) {
							ids = r.getPlayerIdsOnSelExcept( player.getId() );
							PlayerInfo p2 = channel.getPlayerList().getPlayerInfo( ids[0] );
							if( p2 != null ) {
								updateCategory( player, p2, r );
							}
						}
					}
					
	//				player.setOldCategory( player.getCategory() );
	//				player.setOldPoints( player.g
					
//					out.log( "CLEANING ROOM STATUS" );
					
					// Estas 3 líneas eran correctas ANTES, estoy probando
//					r.setType(1);
//					r.setGamePhase(1);
//					r.resetAllReady();
					/*
					 * Lo pongo a un valor ficticio para que hasta que no pulsen los
					 * dos alguna opción el partido no cambie de estado.
					 */
					r.setReadyStatus(player.getId(), 0xFF);
					/*
					 * No puedo hacer esto porque no se pueden computar las categorías despues
					 */
					//r.resetAllSels();
					//
					mes = new Message();
					mes.addBlock(new MessageBlockGameEndInfo(r, 0x00004384));
					out.streamWriteEncodedNIO(mes);
				}
				//rooms.setRoom(r.getId(), r);

				// r.setType( 1 );
				// r.setGamePhase(0x01);
				// r.resetAll();
				// /*
				// * No estar seguro de estor
				// */
				// rooms.setRoom(r.getId(), r);

				/*
				 * Supongo que solo ser�a si le han dado los dos, este mensaje
				 * se genera en los dos que estan jugando (terminaron)
				 */
				// mes = new Message();
				// mes.addBlock(new MessageBlockGameStart4344( r.getGamePhase(),
				// 0x00004344, 0));
				// out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00000005) {
				out.log("READ KEEP ALIVE, ANSWER YES YOU ARE ALIVE:");
				mes = new Message();
				mes.addBlock(new MessageBlockVoid(0x00000005));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000437B) {
				out
						.log("READ 437B, SOMEONE LAGGED OUT OF THE MATCH (MAYBE NOT LEAVING THE SERVER)");
				RoomInfo r = rooms.searchPlayer(player.getId());
				if (r != null) {
					out.log( "SOMEONE LAGGED OUT AND ITS NOT ME: " + player.getName() );
					/*
					 * Aqui le doy un 3-0 al equipo que no lageo antes de resetear la info de la sala
					 * REMEMBER!!!!
					 */
					
					
//					r.setGamePhase(0x01);
//					r.resetAll();
					//rooms.setRoom(r.getId(), r);
					
					/*
					 * Comprobamos si era un 1vs1, en tal caso
					 * actualizamos la categoría con un 3-0 para el equipo contrario
					 */
					
					if( r.getGamePhase() == 7 ) {
						/*
						 * Informamos por privado de los KA times del resto
						 * 
						 */
						
						long ids[] = r.getPlayerIdsExcept(player.getId());
						long now = new Date().getTime();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
						String time = sdf.format(now);
						for( int i = 0; i < ids.length; i++ ) {
							PlayerInfo p = players.getPlayerInfo(ids[i]);
							if( p == null ) continue;
							mes = new Message();
							long secondsElapsed = ((now - p.getConnection().getLastKATime())/1000);
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),  
									(byte)0x00, (byte)0x02, p.getName() + "["+time+"] last alive time: " + 
									secondsElapsed + " secs." + (secondsElapsed > 60 ? " Probably legal lag" : " Probably still online"),
									0x00004402));
							out.streamWriteEncodedNIO(mes);
							//connections.sendEncodedToNIO(r.getPlayerIdsExcept(player.getId()), mes);
						}
						updateProfileAndPoints( player, r );
					} else {
						r.setGamePhase(0x01);
						r.resetAll();
					}
					r.setType(1);
					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomInfoShort(r,
							0x00004365));
					// connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );
					//								
					// mes = new Message();
					// mes.addBlock(new MessageBlockGamePlayerId(
					// player.getId(), 0x00004331, 0));
					// mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[]
					// {
					// r
					// }
					// , QUERIES.GAME_ROOMINFO_SERVER_QUERY, 0));
					// mes.addBlock(new MessageBlockMenuPlayerGroup( player,
					// new GroupInfo( 0x00000000L, ""), 0,
					// QUERIES.MENU_PLYGRP_SERVER_QUERY, 0));
					// //connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					// connections.sendToEncodedNIO( channelCons, mes);
				} else {
					out.log( "THE ROOM DID NOT EXIST, RETURNING");
					return;
				}

			} else if (qId == 0x00004377) {
				out.log("NEW QUERY, 4377 ON WHICH HALF ARE WE?????");
				byte half = mb.getData()[0];

//				logger.log("I'm on half: " + half);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004378));
				out.streamWriteEncodedNIO(mes);

				RoomInfo r = rooms.searchPlayer(player.getId());
				r.setHalf(Util.word2Int((byte) 0x00, half));
				
				// OJO!!! comentado, cuidado con este horror!
//				if (half == (byte) 0x0A) {
//					r.setType(10);
//				}
				//rooms.setRoom(r.getId(), r);
				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));

				/*
				 * Realmente a los que est�n jugando no hay que enviarles esto
				 */
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );
			} else if (qId == 0x00004385) {
				out.log("RECEIVE 4385 TIME ELAPSES AS HE SAYS!!!");
				byte time = mb.getData()[0];
				RoomInfo r = rooms.searchPlayer(player.getId());
				r.setTime(Util.word2Int((byte) 0x00, time));
				//rooms.setRoom(r.getId(), r);
				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));

				/*
				 * Realmente a los que est�n jugando no hay que enviarles esto
				 */
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );
			} else if (qId == 0x00004375) {
				out.log("RECEIVE 4375 GOAL CLIENT QUERY!!!!!!!");
				RoomInfo r = rooms.searchPlayer(player.getId());
				byte team = mb.getData()[0];
				if (team == (byte) 0x00) {
					r.addTeam2goal(r.getHalf());
				}
				if (team == (byte) 0x01) {
					r.addTeam1goal(r.getHalf());
				}

				//rooms.setRoom(r.getId(), r);

				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
						QUERIES.GAME_ROOMINFO_SERVER_QUERY));
				/*
				 * Realmente a los que est�n jugando no hay que enviarles esto
				 */
				connections.sendToChannelNotInGameEncodedNIO( channel, mes );
			} else if( qId == 0x00005047 ) {
				/*
				 * Han aceptado unirse a mi grupo!!!
				 */
				out.log( "READ 5047 CANCELED INVITATION TO GROUP?");
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005048));
				out.streamWriteEncodedNIO(mes);
			} else if( qId == 0x00005043 ) {
				/*
				 * Respuesta a invitación de grupo
				 */
				
				byte ret = mb.getData()[0];
				out.log( "READ 5043, THE USER CHOOSES TO ACCEPT OR DECLINE GROUP INVITATION: " + ret + " requester: " + out.getRequester() );
				
				long reqId = out.getRequester();
				PlayerInfo pg = players.getPlayerInfo(reqId);
				if( pg != null && pg.getGroupInfo() != null ) { 
				
					if( ret == (byte)0x01 ) {
						/*
						 * Ha aceptado a unirse, le metemos en el grupo
						 */
						pg.getGroupInfo().addPlayer(player);
						player.setGroupInfo(pg.getGroupInfo());
						
						try {
							Tools.dbAddPlayerToGroup(db, player);
						} catch( SQLException ex ) {
							out.log( "Unable to add player to the group");
							out.log( ex.getMessage() );
						}
						
						mes = new Message();
						MessageBlock mb1 = new MessageBlockZero( 0x00005044 );
						mb1.setData(new byte[] { (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 });
						mes.addBlock(mb1);
						out.streamWriteEncodedNIO(mes);
	
						mes = new Message();
						mes.addBlock(new MessageBlockByte( (byte)0x00, 0x00005046));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);

						sendPlayerList( channel );
					}
					if( ret == (byte)0x00 ) {
						/*
						 * Tanto el 5046 con algo que no sea un cero como el 5045 producen el
						 * mismo efecto en el cliente: "Como el rival no pudo responder.... etc"
						 */
						mes = new Message();
						mes.addBlock(new MessageBlockByte( (byte)0x02, 0x00005046));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
					if( ret == (byte)0x02 ) {
						mes = new Message();
						mes.addBlock(new MessageBlockByte(ret, 0x00005045));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
				} else {
					out.log( "The player which offered group membership to me, no longer exists (or has no group at all)");
				}
				

				
			} else if (qId == 0x00005040) {
				/*
				 * Invitar a alguien al grupo!
				 */
				out.log( "Received 5040 Invite to group");
				long plid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				mes = new Message();
				mes.addBlock(new MessageBlockLong(player.getId(), 0x00005042));
				connections.searchConnection(plid).setRequester(player.getId());
				connections.searchConnection(plid).streamWriteEncodedNIO(mes);
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00005041));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x000050F0) {
				/*
				 * Crear un grupo, no tengo logs voy a responder que 50F1 con
				 * zeros OJO es excesivamente largo el mensaje para enviar solo
				 * en nombre, sin embargo en la prueba realizada todo lo demas
				 * que viene son cerotes
				 */
				String grName = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				out.log("RECEIVE 50F0 CREATE GROUP CLIENT QUERY!!! NAME: "
						+ grName);

				try {
					Tools.dbCreateGroup(db, player, grName);
//					logger.log("GROUP CREATED AND NO EXCEPTION!");
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x000050F1));
					out.streamWriteEncodedNIO(mes);
					/*
					 * Es impepinable actualizar la lista de jugadores
					 */
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );
				} catch (SQLException ex) {
					ex.printStackTrace();
					/*
					 * Enviar esto no parece funcionar para generar un error, no
					 * enviar nada a ojos del cliente tambien es como si lo
					 * hubiera creado perfectamente
					 */
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFEBL, 0x000050F1));
					out.streamWriteEncodedNIO(mes);
				}

			} else if (qId == 0x00005024) {

				/*
				 * Pregunta sobre el grupo, de momento desconozco a que se
				 * refiere, envia el identificador del grupo
				 */
				long grid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);

				out.log("RECEIVE 5024 INFO GROUP CLIENT QUERY!!! GROUP ID: "
						+ grid);

				/*
				 * Probablemente aqui vengan todos los miembros del grupo, es un
				 * mensaje bastante largo
				 */
				try {
					GroupInfo g = Tools.dbLoadGroup(db, grid);
					mes = new Message();
					mes
							.addBlock(new MessageBlockGameGroupInfo(g,
									0x00005025));
					out.streamWriteEncodedNIO(mes);
//					FileOutputStream fout = new FileOutputStream("5025_mio.dec");
//					fout.write(mes.getBytes());
//					fout.close();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}

			} else if (qId == 0x00005084) {
				/*
				 * Dejar un mensaje en el tablon de un grupo
				 */
				long grid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				String msg = new String(mb.getData(), 4, Util.strlen(mb
						.getData(), 4));

				out.log("RECEIVE 5084 SEND MESSAGE TO GROUP!!!");
				FileOutputStream fout = new FileOutputStream("5084_mio.dec");
				fout.write(mb.getBytes());
				fout.close();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005085));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000508E) {
				/*
				 * Lista de mensajes de prohibicion
				 */

				out.log("RECEIVE 508E GROUP BAN MESSAGE LIST!!!");

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000508F));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005091));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00005060) {

				out.log("RECEIVE 5060 EMPTY ANSWERING ZEROES: ");

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005061));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				mes.addBlock(new MessageBlockGameGroupMembers(player,
						channels,
						0x00005062));
				out.streamWriteEncodedNIO(mes);
//				FileOutputStream fo = new FileOutputStream("5062_mio.dec");
//				fo.write(mes.getBytes());
//				fo.close();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005063));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004580) {

				out.log("RECEIVE 4580 PLAYER PUSHED THE FRIENDS BUTTON: ");

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004581));
				out.streamWriteEncodedNIO(mes);

				/*
				 * Lo de abajo es perfectamente correcto, pero como no podemos
				 * a�adir amigos de momento enviamos una lista vacia
				 */
				// mes = new Message();
				// mes.addBlock(new MessageBlockMenuFriends( new PlayerInfo[] {
				// new PlayerInfo( 0x00000077L, "Amigo") }, 0x00004582, 0
				// ));
				// out.streamWriteEncodedNIO(mes);
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004583));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000450B) {
				/*
				 * Cancela la invitación de amigo
				 */
				out.log( "READ 450B CANCELED FRIEND INVITE?");
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000450C));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004504) {

				long plid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				out.log("RECEIVE 4504 ADD FRIEND! FRIEND ID: " + plid);
				
//				for( int q = 0x00004505; q <= 0x00004999; q++ ) {
//					mes = new Message();
//					mes.addBlock(new MessageBlockLong(player.getId(), q));
//					//connections.searchConnection(plid).setRequester(player.getId());
//					connections.searchConnection(plid).streamWriteEncodedNIO(mes);
//				}
				
				/*
				 * Creo que esto de abajo no sirve para una mierda
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00004505));
				out.streamWriteEncodedNIO(mes);
				
				/*
				 * ME LO ESTOY INVENTANDO Y NO FUNCIONA
				 */

//				for( int q = 0x00004500; q < 0x00004800; q++ ) {
//					 logger.log( "paso con " +
//					 Util.toHex(Util.long2Word(q)) );
//					 mes = new Message();
//					 MessageBlock mb1 = new MessageBlockVoid( q, 0 );
//					 mb1.setData(new byte[] { (byte)0x00, mb.getData()[0], mb.getData()[1], mb.getData()[2], mb.getData()[3] } );
//					 mes.addBlock( mb1 );
//					 //mes.addBlock(new MessageBlockLong( player.getId(), q, 0 ));
//					 connections.searchConnection(plid).streamWriteEncodedNIO(mes);
//				}
					
				// mes = new Message();
				// //mes.addBlock(new MessageBlockZero( 0x00004505, 0 ));
				// mes.addBlock(new MessageBlockLong( plid, q, 0 ));
				// connections.searchConnection(plid).streamWriteEncodedNIO(mes);
				// }
				// mes = new Message();
				// //mes.addBlock(new MessageBlockZero( 0x00004505, 0 ));
				// mes.addBlock(new MessageBlockLong( plid, 0x00004505, 0 ));
				// connections.searchConnection(plid).streamWriteEncodedNIO(mes);

				// out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00000003) {
				/*
				 * Adios muy buenas que me vooooy!!! mide 1 y me viene un 0x01
				 */
				/*
				 * Mal hecho pero para arreglar lo del pedete de momento sirve
				 * forzamos la desconexion
				 */
				out.log("RECEIVE 00 03 disconnect!!! ");
				out.logLastSentMessage("***Dump of last sent message***");
				throw new IOException("Forced shutdown of client!");
			} else if (qId == 0x000050B0) {
				/*
				 * Retirada del grupo, viene con un motivo
				 */
				String reason = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				out.log("RECEIVE 50 B0 Group disband!!!: " + reason);

				/*
				 * Me invento la respuesta quiza sea esto
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x000050B1));
				out.streamWriteEncodedNIO(mes);
				try {
					Tools.dbRemovePlayerFromGroup(db, player);
					player.getGroupInfo().removePlayer(player.getId());

					player.setGroupInfo(new GroupInfo(0, ""));
					/*
					 * Hay que actualizar la lista de jugadores por que este tio
					 * ya no es de ningun grupo
					 */
					sendPlayerList( channel );
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			} else if (qId == 0x00005020) {
				/*
				 * Busqueda de grupo, viene con un nombre de grupo y un tipo de
				 * busqueda por defecto coincidencia parcial
				 * 
				 * Primer byte indica el tipo de busqueda. 0x01 = Coincidencia
				 * parcial 0x02 = exacta
				 */
				byte queryType = mb.getData()[0];
				String queryText = new String(mb.getData(), 1, Util.strlen(mb
						.getData(), 1));
				out.log("RECEIVE 50 20 Group search!!!: " + queryText);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005021));
				out.streamWriteEncodedNIO(mes);
				try {
					GroupInfo[] groups = Tools.dbSearchGroups(db, queryType, queryText);

					if (groups != null && groups.length > 0) {
						mes = new Message();
						mes.addBlock(new MessageBlockGameGroupList(groups,
								0x00005022));
						out.streamWriteEncodedNIO(mes);
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005023));
				out.streamWriteEncodedNIO(mes);
			} else {
				out.log("UNKNOWN QUERY!!!!! ************************");
			}
		}
		
		public void sendRoomList( ChannelInfo channel ) throws IOException {
			Message mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004301));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
			
			/*
			 * COMPROBADO; HAY QUE ENVIARSELO A TODO EL MUNDO
			 */

			/*
			 * Aqui viene la lista de salas creadas
			 */
			mes = new Message();

			mes.addBlock(new MessageBlockGameRoomList(channel.getRoomList()
					.getRoomsInChannelArray( channel ), 0x00004302));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );

			mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004303));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
		}
		
		public void sendRoomUpdate( ChannelInfo channel, RoomInfo r ) throws IOException {
			Message mes = new Message();

			mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r }, 0x00004302));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
		}

		public synchronized void sendPlayerList( ChannelInfo channel ) throws IOException {
			Message mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004211));

			/*
			 * QUERY COMPROBADA Y CORRECTA HAY QUE ENVIARLE ESTOS MENSAJES A
			 * TODO EL MUNDO
			 */
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );

			/*
			 * Aqui viene la lista de tipos conectados
			 */
			mes = new Message();
//			mes.addBlock(new MessageBlockGamePlayerGroupList(players, rooms,
//					0x00004212, 0));

			/*
			 * Ya ya pero solo los players del canal correspondiente!!!, ARREGLARLO!
			 */
			mes.addBlock(new MessageBlockGamePlayerGroupList(channel.getPlayerList(), channel.getRoomList(),
					0x00004212));
			
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );

			mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004213));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
		}

		/*
		 * Para actualizar la categoría, esto solo es válido para 1vs1
		 */
		public void updateCategory( PlayerInfo player1, PlayerInfo player2, RoomInfo r ) {
			
			/*
			 * Even though we receive both players, we are only updating player1
			 */
			
			float winBasePoints = 20;
			float winMinPoints = 2;
			float extraPercent = 0.30f;
			float losingMultiplier = 0.9f;
			float maxDifPoints = 200;
				
			
			float p1Cat = player1.getOldCategory();
			float p2Cat = player2.getOldCategory();
			float difPoints;
			
			//System.out.println( "cat1 old: " + p1Cat + ", cat2 old: " + p2Cat );
			
			difPoints = p1Cat - p2Cat;
			float p1CatMod = 0;
			float p2CatMod = 0;
			
			int winner = r.getWinner();
			int p1team = r.getPlayerTeam( player1.getId() );
			
			if( winner == p1team ) {
				//System.out.println( "Gana: " + player1.getName());
//				p1CatMod = winBasePoints;
//				p2CatMod = (int)(-winBasePoints * losingMultiplier);
				
				if( difPoints < 0 ) {
						p1CatMod = winBasePoints;
						p1CatMod += (int)(-1 * difPoints * extraPercent);
						p2CatMod = (int)(-winBasePoints * losingMultiplier);
						p2CatMod += (int)(difPoints * extraPercent * losingMultiplier);
				} else if( difPoints > 0 || difPoints == 0 ){
						
					/*
					 * Winner had more points than the loser
					 */
					if( difPoints < maxDifPoints ) {
						p1CatMod = winBasePoints;
						p1CatMod -= (difPoints / maxDifPoints * winBasePoints);
						p2CatMod = (int)(-winBasePoints * losingMultiplier);
						p2CatMod += (int)(difPoints / maxDifPoints * winBasePoints * losingMultiplier);
					} else {
						p1CatMod = winMinPoints;
						p2CatMod = (int)(-1 * winMinPoints * losingMultiplier);
					}
				}
			} else if( winner == 2 ) {
//				System.out.println( "Empate! " );
				float drawPoints = winBasePoints / 2;
				if( difPoints < 0 ) {
					p1CatMod = (int)(-1 * difPoints * extraPercent);
					p2CatMod = (int)(difPoints * extraPercent * losingMultiplier);
				} else if( difPoints > 0 || difPoints == 0){
					p1CatMod = (int)(-1 * difPoints * extraPercent * losingMultiplier);
					p2CatMod = (int)(difPoints * extraPercent);
				}
			} else {
//				System.out.println( "Gana: " + player2.getName());
				if( difPoints > 0 ) {
					p2CatMod = winBasePoints;
					p2CatMod += (int)(difPoints * extraPercent); 
					p1CatMod = (int)(-winBasePoints * losingMultiplier);
					p1CatMod += (int)(-1 * difPoints * extraPercent * losingMultiplier );
				} else if( difPoints < 0 || difPoints == 0 ) {
					if( (difPoints*-1) < maxDifPoints ) {
							p2CatMod = winBasePoints;
//							System.out.println( "p2catmod valiendo: " + p2CatMod );
							p2CatMod += (difPoints / maxDifPoints * winBasePoints);
//							System.out.println( "dif: " + difPoints + " maxDif: " + maxDifPoints + " winBase: " + winBasePoints );
//							System.out.println( "resul: " + (difPoints / maxDifPoints * winBasePoints));
//							System.out.println( "p2catmod valiendo ahora: " + p2CatMod );
							p1CatMod = (int)(-winBasePoints * losingMultiplier);
							p1CatMod -= (int)(difPoints / maxDifPoints * winBasePoints * losingMultiplier);
					} else {
							p2CatMod = winMinPoints;
							p1CatMod = (int)(-1 * winMinPoints * losingMultiplier);
					}
				}
			}
			player1.setCategory( player1.getOldCategory() + Math.round(p1CatMod) );
			player2.setCategory( player2.getOldCategory() + Math.round(p2CatMod) );
		}
		
		public void updateProfileAndPoints( PlayerInfo player, RoomInfo r ) {
			/*
			 * The same client can call this function twice when someone lagged out
			 * that would count as 2 matches, thats not acceptable.
			 */
			if( player.getOldPoints() != player.getPoints() ) return;
			player.setMatchesPlayed( player.getMatchesPlayed() + 1 );
			
			//System.out.println( "ahora tenemos: " + player.getMatchesPlayed() );
			
			int myteam = r.getPlayerTeam( player.getId());
			int team1goals = r.getTeam1Goals();
			int team2goals = r.getTeam2Goals();
			
			// Lets say 3 is a draw
			int winnerTeam = r.getWinner();
			
			if( winnerTeam == myteam ) {
				player.setVictories( player.getVictories() + 1 );
				player.setWinningStreak( player.getWinningStreak() + 1 );
				if( player.getWinningStreak() > player.getBestStreak() )
					player.setBestStreak( player.getWinningStreak() );
			} else if ( winnerTeam == 2 ) {
				player.setDraws( player.getDraws() + 1 );
				player.setWinningStreak( 0 );
			} else {
				player.setDefeats( player.getDefeats() + 1 );
				player.setWinningStreak( 0 );
			}
			int scoredGoals;
			int receivedGoals;
			
			if( myteam == 0 ) {
				scoredGoals = team2goals;
				receivedGoals = team1goals;
				player.pushTeam( r.getTeam2() );
			} else {
				scoredGoals = team1goals;
				receivedGoals = team2goals;
				player.pushTeam( r.getTeam1() );
			}
			
			player.setGoalsScored( player.getGoalsScored() + scoredGoals );
			player.setGoalsReceived( player.getGoalsReceived() + receivedGoals );
			
			int pointsEarned = 5;
			pointsEarned += 7 * scoredGoals;
			player.setMatchPoints( pointsEarned );
			player.setPoints( player.getOldPoints() + pointsEarned );
		}
		
		public void disconnectPlayer( PESConnection con ) {
			
			/*
			 * Necesitamos las conexiones del canal de este jugador
			 * suponiendo que el jugador ya tenga IP
			 */
			

			try {

				
				connections.removeConnection(con);
				PlayerInfo player = con.getPlayerInfo();
				PlayerList players = null;
				RoomList rooms = null;
				ChannelInfo channel = null;
				if( player != null && player.getIpInfo() != null ) {
					channel = channels.getChannel(player.getIpInfo().getChannel());
					players = channel.getPlayerList();
					rooms = channel.getRoomList();
				}
				
				if (player != null && player.isLoggedIn()) {
					/*
					 * Intentamos grabar la info del jugador
					 */
					
					con
							.log("I WAS LOGGED IN, REMOVING MYSELF FROM EVERYWHERE!");
					players.removePlayer(player.getId());

					RoomInfo r = rooms.searchPlayer(player.getId());
					if (r != null) {
						Message mes;

						/*
						 * If the player was in a started match (on phase 7)
						 * this is a deco, and we have to update his profile
						 */
						if( !shuttingDown ) {
							if( r.getGamePhase() >= 6 /*&& r.getType() == 7*/) {
								/*
								 * Only if he was playing!
								 */
								if( r.getPlayerSelFromPid( player.getId() ) != 0x00FF ) {
									player.setDecos( player.getDecos() + 1 );
									int myteam = r.getPlayerTeam( player.getId() );
									if( myteam == 0 ) {
										r.setTeam1Goals( 3 );
										r.setTeam2Goals( 0 );
									} else {
										r.setTeam1Goals( 0 );
										r.setTeam2Goals( 3 );
									}
									updateProfileAndPoints( player, r );
									/*
									 * Mandamos un mensaje a la sala informando de la desconexión
									 * del jugador y del resultado final
									 */

									mes = new Message();
									mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),  
											(byte)0x00, (byte)0x02, player.getName() + " got disconnected, the opponent team won by 3-0.",
											0x00004402));
									connections.sendEncodedToNIO(r.getPlayerIdsExcept(player.getId()), mes);
									con.log( "INFORMANDO A LA SALA DE MI DESCONEXION");

									/*
									 * Comprobamos si era un 1vs1, en tal caso
									 * actualizamos la categoría con un 3-0 para el equipo contrario
									 */
									int idx = r.getPlayerIdx(player.getId());
									if( idx != -1 && r.getSpectIdx(idx) == 0 ) {	
										if( r.getPeopleOnMatch() == 2 && r.getNumPlayersOnSel() > 1 ) {
											long ids[] = r.getPlayerIdsOnSelExcept( player.getId() );
											PlayerInfo p2 = channel.getPlayerList().getPlayerInfo( ids[0] );
											if( p2 != null ) {
												updateCategory( player, p2, r );
											}
										}
									}
								}
							}
						}
						
						/*
						 * Voy a ver en que estado estaba el tio que se cay�
						 */
						
						/*
						 * Ojo, he puesto <= 7 donde había < 7
						 */
						if (r.getGamePhase() > 1 && r.getGamePhase() < 7
								&& r.getPlayerSel(r
										.getPlayerIdx(player.getId())) != 0x00FF) {
							r.setType(1);
							r.setGamePhase(0x01);
							r.resetAll();
						}
						//rooms.setRoom(r.getId(), r);

						r.removePlayer(player.getId());
						//rooms.setRoom(r.getId(), r);
						if (r.getNumPlayers() == 0) {
							con.log("Removing room");
							rooms.removeRoom(r.getId());

							mes = new Message();
							mes.addBlock(new MessageBlockGameAnyId(r.getId(),
									0x00004305));
							connections.sendToChannelNotInGameEncodedNIO( channel, mes );

							mes = new Message();
							mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
									QUERIES.GAME_PLYGRP_SERVER_QUERY));
							connections.sendToChannelNotInGameEncodedNIO( channel, mes );

							// SEGURO??????
							mes = new Message();
							mes.addBlock(new MessageBlockZero(0x0000432B));

							connections.sendToChannelNotInGameEncodedNIO( channel, mes );
							mes = new Message();
							mes.addBlock(new MessageBlockGameAnyId(r.getId(),
									0x00004305));
							connections.sendToChannelNotInGameEncodedNIO( channel, mes);

						} else {
							/*
							 * 
							 * 
							 * PARA LOS DEMAS!
							 * 
							 * 
							 * 
							 */

							// Estas 3 lineas comentadas el 11 FEB (no aparecen
							// en un exit room para los demas)
							// mes = new Message();
							// mes.addBlock(new MessageBlock10Zero( 0x0000434B,
							// 0));
							// connections.sendToEncodedNIO( channelCons, mes);

							mes = new Message();
							mes.addBlock(new MessageBlockGameRoomInfoShort(r,
									0x00004365));
							// connections.sendEncodedToNIO(r.getPlayerIds(), mes);
							connections.sendToChannelNotInGameEncodedNIO( channel, mes);

							// Estas otras a�adidas el 11 FEB para asignar el
							// nuevo owner de la sala correctamente
							// TEST
							mes = new Message();
							mes.addBlock(new MessageBlockLong(r.getPlayerId(0),
									0x0000434C));

							connections.sendEncodedToNIO(r.getPlayerIds(), mes);

							mes = new Message();
							mes.addBlock(new MessageBlockGamePlayerId(player
									.getId(), 0x00004331));
							mes.addBlock(new MessageBlockGameRoomList(
									new RoomInfo[] { r },
									QUERIES.GAME_ROOMINFO_SERVER_QUERY));
							mes.addBlock(new MessageBlockMenuPlayerGroup(
									player, 0,
									QUERIES.MENU_PLYGRP_SERVER_QUERY));
							// connections.sendEncodedToNIO(r.getPlayerIds(), mes);
							connections.sendToChannelNotInGameEncodedNIO( channel, mes);
						}
					}
					/*
					 * OJO Mirar bug del 16 de abril de 2009 hora: 00:09:30
					 */
					try {
						Tools.dbSavePlayerInfo( db, player );
						con.log( "USER PROFILE SAVED!" );
					} catch( SQLException ex ) {
						con.log( "UNABLE TO SAVE USER PROFILE!!!!" + ex.getMessage());
					}
					Message mes = new Message();
					mes.addBlock(new MessageBlockGameAnyId(player.getId(),
							0x00004221));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes);

				}
				con.close();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					con.close();
				} catch(Exception ex ) {
					ex.printStackTrace();
				}
			} finally {
			}
		}
}
