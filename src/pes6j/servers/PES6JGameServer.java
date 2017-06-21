package pes6j.servers;

//The server code Server.java:
import java.io.File;
import java.io.FileInputStream;
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
import pes6j.datablocks.IpInfo;
import pes6j.datablocks.Message;
import pes6j.datablocks.MessageBlock;
import pes6j.datablocks.MessageBlockByte;
import pes6j.datablocks.MessageBlockFriendsList;
import pes6j.datablocks.MessageBlockGameAnyId;
import pes6j.datablocks.MessageBlockGameChatMessage;
import pes6j.datablocks.MessageBlockGameEndInfo;
import pes6j.datablocks.MessageBlockGameEnterRoomFailFull;
import pes6j.datablocks.MessageBlockGameEnterRoomFailPass;
import pes6j.datablocks.MessageBlockGameGroupApplicationList;
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
import pes6j.datablocks.MessageBlockMenuFriends;
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
import pes6j.datablocks.ServerInfo;
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
	byte[] default_settings = null;
	private Logger logger;
    Selector selector;
    LookupService cl;
    boolean debug = false;
	// the socket where to listen/talk
    int port;
    int wport;
	private Db db;
	ServerInfo server;
	
	// server constructor

	PES6JGameServer() {
		
		/* create socket server and wait for connection requests */
		try
		
		{
			logger = new Logger( "log/pes6j_GAME_SERVER.log" ); //$NON-NLS-1$

			debug = Configuration.gameServerProperties.getProperty("debug").trim().toLowerCase().equals("on"); //$NON-NLS-1$ //$NON-NLS-2$
			initialize();
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(server.getPort()));
			
			selector = Selector.open();
			SelectionKey acceptKey = ssChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			
			logger.log("PES6JGameServer waiting for client on port " //$NON-NLS-1$
					+ server.getPort());
			
			File f = new File( "def_settings.raw"); //$NON-NLS-1$
			try {
				FileInputStream fi = new FileInputStream( f );
				default_settings = new byte[(int)f.length()];
				fi.read(default_settings);
				fi.close();
			} catch( IOException ex1) {
				logger.log( "Default settings file not found!" ); //$NON-NLS-1$
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
						
						con1.log( "TIMED OUT, NO KEEP ALIVE REPLY FOR ONE MINUTE" );					 //$NON-NLS-1$
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
				 * Comprobamos si hay juegos lageados pendientes de comprobar sus pincodes
				 *
				 */
				processLaggedGames();
				
				
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
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm"); //$NON-NLS-1$
					String message = Messages.getString(0, "PES6JGameServer.0") + sdf.format(new Date(shutdownTime)) + Messages.getString(0, "PES6JGameServer.11") + sdf.format(now); //$NON-NLS-1$ //$NON-NLS-2$
					PlayerInfo adminBot = new PlayerInfo( Long.MAX_VALUE, "AdminBot"); //$NON-NLS-1$
					Message mes = new Message();
					mes.addBlock(new MessageBlockGameChatMessage(adminBot,  
							(byte)0x00, (byte)0x02, message,
							true, 0x00004402));
					connections.sendToAllEncodedNIO( mes );
					mes = new Message();
					mes.addBlock(new MessageBlockGameChatMessage(adminBot,  
							(byte)0x00, (byte)0x01, message,
							true, 0x00004402));
					connections.sendToAllEncodedNIO( mes );
					lastShutdownMessageTime = now;
				}
				
//				/*
//				 * Comprobamos si debemos limpiar salas vacías
//				 */
//				if( lastRoomRemovalCheckTime + GHOST_ROOM_REMOVAL_CHECK_TIME <= now ) {
//					for( int i = 0; i < channels.size(); i++ ) {
//						RoomList rl = channels.getChannel(i).getRoomList();
//						RoomInfo[] chanRooms = rl.getRoomInfoArray();
//						if( chanRooms != null ) {
//							for( int j = 0; j < chanRooms.length; j++ ) {
//								if( chanRooms[j].getNumPlayers() < 1 ) {
//									rl.removeRoom(chanRooms[j].getId());
//								}
//							}
//						}
//					}
//					
//					lastRoomRemovalCheckTime = now;
//				}
			}
			
		}
		
		catch (Exception e) {
			
			System.err.println("Server down, exception: " + e); //$NON-NLS-1$
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
				logger.log("Exception creating new Input/output Streams: " //$NON-NLS-1$
								+ e);
				return;
			}
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		PESConnection con = connections.searchConnection(socketChannel.socket());
		if( con == null ) {
			System.out.println( "NO EXISTE la conexion para la clave al intentar escribir!!!"); //$NON-NLS-1$
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
			System.out.println( "NO EXISTE!!!"); //$NON-NLS-1$
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
				con.log("Exception reading/writing  Streams: " + e); //$NON-NLS-1$
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
		String lobby;
		initDB();
		
		server = Tools.dbLoadServer(db, Integer.parseInt(Configuration.gameServerProperties.getProperty("id")));
		
		ChannelInfo lobbies[] = Tools.dbLoadServerLobbies(db, 
				Integer.parseInt(Configuration.gameServerProperties.getProperty("id")));
		
		for( int i = 0; i < lobbies.length; i++ ){
			channels.addChannel(lobbies[i]);
		}
		
//		while( (lobby = Configuration.gameServerProperties.getProperty("lobby-" + i)) != null ) { //$NON-NLS-1$
//			String lobbyContinents = Configuration.gameServerProperties.getProperty("lobby-" + i + "-continents"); //$NON-NLS-1$ //$NON-NLS-2$
//			String lobbyType = Configuration.gameServerProperties.getProperty("lobby-"+ i + "-type"); //$NON-NLS-1$ //$NON-NLS-2$
//			byte type = ChannelInfo.TYPE_PC_CHANNEL;
//			try {
//				type = Byte.parseByte(lobbyType);
//			} catch( Exception ex ) {
//				logger.log( "Exception parsing lobbyType! default PC_CHANNEL type will be used."); //$NON-NLS-1$
//			}
//			channels.addChannel( new ChannelInfo(
//					type, lobby, lobbyContinents) );
//			i++;
//		}
		
		HttpServer srv = HttpServer.create(new InetSocketAddress(server.getWPort()), 10);
		srv.createContext("/query", new PES6JWebHandler(channels)); //$NON-NLS-1$
		srv.setExecutor(null); // creates a default executor
		srv.start();

		
		cl = new LookupService("config/GeoIP.dat",LookupService.GEOIP_MEMORY_CACHE); //$NON-NLS-1$
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
			out.log("HEX ENC CAB: " + Util.toHex(enccab)); //$NON-NLS-1$

			int qId = ((MetaMessageBlock) mb).getHeader().getQuery();

			Message mes;
			if (qId == QUERIES.MENU_INIT_CLIENT_QUERY) {
				out
						.log("MATCHES MENU INIT CLIENT QUERY ANSWERING LOGIN INIT SERVER:"); //$NON-NLS-1$
				mes = new Message();
				mes.addBlock(new MessageBlockMenuQ1(++qId));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00003003 ) {
				out
						.log("MATCHES MENU LOGIN CLIENT QUERY ANSWERING BLOCK ZERO:"); //$NON-NLS-1$
				/*
				 * Recuperamos su nombre de usuario
				 */
				try {
					uInfo = Tools.dbLoadUserInfo( db, out.getSocket().getInetAddress().getHostAddress() );
				} catch( SQLException ex ) {
					out.log( "EXCEPTION TRYING TO FETCH USER INFO: " ); //$NON-NLS-1$
					out.log( ex );
					throw new IOException( "UNABLE TO FETCH USER INFO!!!"); //$NON-NLS-1$
				}
				if( uInfo.getAccess() < 1 ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xffffff10, 0x00003004));
					out.streamWriteEncodedNIO(mes);
//					throw new IOException( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " + //$NON-NLS-1$ //$NON-NLS-2$
//							uInfo.getAccess() );
				} else {
					cdkey = new DataCdKeyAndPass(mb.getData());
					out.setCdkey(cdkey);
					out.setUserInfo( uInfo );
					mes = new Message();
					mes.addBlock(new MessageBlockZero(++qId));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x00004100) {
				/*
				 * Viene con 3 bytes:
				 * 
				 * 02 04 00
				 * plataforma, idioma y ?
				 */
				out
						.log("[GAMESERVER] MATCHES MENU GROUP CLIENT QUERY ANSWERING MENU GROUP INFO SERVER:"); //$NON-NLS-1$
				try {
					player = Tools.dbGetPlayerSel(db, uInfo, default_settings);
					player.setConnection( out );
					out.setPlayerInfo( player );
					out.setPid( player.getId() );
					
					mes = new Message();
					mes.addBlock(new MessageBlockMenuGroupInfo(player, 0x00004101));
					out.streamWriteEncodedNIO(mes);
				} catch (SQLException ex) {
					ex.printStackTrace();
				}

			} else if (qId == 0x00004200 ) {
				out
						.log("MATCHES MENU MENUS CLIENT QUERY ANSWERING MENU MENU LIST SERVER:"); //$NON-NLS-1$
				byte lang = mb.getData()[0];
				player.setLang( lang );
				mes = new Message();
				mes.addBlock(new MessageBlockMenuMenuList(
						channels, player, 0x00004201));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004202) {
				DataIp ip = new DataIp(mb.getData());
				/*
				 * Me faltan dos bytes al final que en el caso de captura
				 * caesar2 son 0x00 0xb3
				 */
				/*
				 * OJO ESTA COMPROBACIÓN SOLO SIRVE PARA QUE NO ENTREN LOS
				 * DOS EN EL MISMO LOBBY!
				 */
				
				/*
				 * Primero miro a ver si el tio ya está en un lobby, y en ese caso
				 * me lo cepillo de la misma
				 */
				
				if( channel != null ) {
					players.removePlayer(player.getId());
					mes = new Message();
					
					/*
					 * No se si debo, esto es decir que se ha ido!
					 */
					mes.addBlock(new MessageBlockGameAnyId(player.getId(),
							0x00004221));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes);
				}

				player.setLoggedIn();
				player.setIpInfo( new IpInfo(player.getId(), ip.getChannelId(), ip
						.getIp1(), ip.getPort1(), ip.getIp2(), ip.getPort2()) );
				
				channelId = player.getIpInfo().getChannel();

				channel = channels.getChannel( channelId );
				players = channel.getPlayerList();
				rooms = channel.getRoomList();
				

				
				
				int numPlayers = channel.getPlayerList().getNumPlayers();
				
				if ( players != null && players.getPlayerInfo(player.getId()) != null) {
					throw new IOException("The player was already logged in"); //$NON-NLS-1$
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
					try {
						Tools.dbUpdatePlayerServerAndLobby(db, player.getId(), channelId);
						players.addPlayer( player );
						player.setLobbyId(channelId);
						out.log("Adding new player!!!!"); //$NON-NLS-1$
		
						out.setPid(player.getId());
		
						out
								.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING MENU PLAYER AND GROUP SERVER:"); //$NON-NLS-1$
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00004203));
						out.streamWriteEncodedNIO(mes);
		
						mes = new Message();
						mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0, 0x00004220));
						out.streamWriteEncodedNIO(mes);
					} catch( SQLException ex ) {
						mes = new Message();
						/*
						 * Would give a message like "coudn't enter the lobby", not like
						 * a "lobby full" message, but it would do.
						 */
						mes.addBlock(new MessageBlockGameEnterRoomFailFull(
								0x00004203));
						out.streamWriteEncodedNIO(mes);
					}
				}
			} else if (qId == 0x00006020) {
				/*
				 * Quick Match viene con 3 bytes de payload
				 * 1e 73 61
				 * 0: 0A -> a 10 minutos,
				 * 0: 05 -> a 5 minutos (supongo)
				 * 1: distinto de 0 o 1 -> Rival de similar categoría
				 * 1: 00 -> Sin condiciones.
				 * 2: distinto de 0 o 1 -> Charlar durante el juego: SI
				 * 2: 00 -> Charlar durante el juego: NO
				 */
				out.log("READ 6020 QuickMatch!:");
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00006021));
				out.streamWriteEncodedNIO(mes);
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00006022));
				out.streamWriteEncodedNIO(mes);
				
				/*
				 * Busco salones que coincidan con sus criterios,
				 * si no encuentro ninguno le creo uno
				 */
				
				/*
				 * Cuando entre otro tipo le envio un mbzero 6022
				 */
			} else if (qId == 0x00006023) {
				/*
				 * Que es esto?, me llega un buen rato despues de haber esperado a encontrar un
				 * quick...
				 */
				out.log("READ 6023 Couldn't find anyone?... ");
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00006024));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00006030) {
				/*
				 * Creo que es dime quien o que pasa con el.
				 * Has encontrado a alguien?
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockLong( 516, 0x00006031));
				out.streamWriteEncodedNIO(mes);
				
			} else if (qId == 0x00003080) {
				out
						.log("MATCHES MENU MYPLAYERID CLIENT QUERY ANSWERING WITH BLOCK LIST:"); //$NON-NLS-1$

				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				
				sendFriendList(out, pid);

//				mes = new Message();
//				mes.addBlock(new MessageBlockZero(0x00003082));
//				out.streamWriteEncodedNIO(mes);
//
//				/*
//				 * Lo comento porque de momento no tenemos amigos
//				 */
//				// mes = new Message();
//				// mes.addBlock(new MessageBlockMenuFriends( new PlayerInfo[] {
//				// new PlayerInfo( 0x00000077L, "Amigo") }, 0x00003084, 0
//				// ));
//				// out.streamWriteEncodedNIO(mes);
//				mes = new Message();
//				mes.addBlock(new MessageBlockVoid(0x003086));
//				out.streamWriteEncodedNIO(mes);

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
						.log("MATCHES GAME GETPLYLST CLIENT QUERY ANSWERING PLAYERLIST:"); //$NON-NLS-1$
				sendPlayerList( channel );

			} else if (qId == 0x00004300) {
				out
						.log("MATCHES GAME GETROOMLST CLIENT QUERY ANSWERING ROOMLIST:"); //$NON-NLS-1$
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
				out.log("MATCHES GAME GETPLYANDGROUP CLIENT QUERY ID: " + id //$NON-NLS-1$
						+ " ANSWERING PLAYERANDGROUP INFO:"); //$NON-NLS-1$
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
						tmp = Tools.dbLoadPlayerInfo( db, id, default_settings );
					} catch( SQLException ex ) {
						ex.printStackTrace();
						out.log( "THE PLAYER WASNT ONLINE AND WE FAILED TO LOAD HIS DATABASE INFO"); //$NON-NLS-1$
						out.log( ex.toString() );
						return;
					}
				}
				if( tmp != null ) {
					mes.addBlock(new MessageBlockGamePlayerGroup(tmp, 0x00004103));
					out.streamWriteEncodedNIO(mes);
				} else {
					out.log( "THE PLAYER ID DOESNT EVEN EXIST ON THE DATABASE!, RETURNING"); //$NON-NLS-1$
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x00004104));
					out.streamWriteEncodedNIO(mes);
					return;
				}
			} else
			// No se lo que es esto, pero se me pregunta al final y con
			// responderle un void el cliente
			// parece estar de acuerdo
			if( qId == 0x0000503A ) {
				out.log( "READ 0x503A this comes when the player is the group leader" );
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000503B));
//				mes.addBlock(new MessageBlockGamePlayerGroup(player, player.getGroupInfo(), 0x0000503C));
				try {
					PlayerInfo[] recruits = Tools.dbLoadGroupRecruits( db, player.getGid() );
					mes.addBlock(new MessageBlockGameGroupApplicationList( recruits, 0x0000503C));
				} catch( SQLException ex ) {
					
				}
				mes.addBlock(new MessageBlockZero(0x0000503D));
				out.streamWriteEncodedNIO(mes);
			} else 
			if( qId == 0x000050E0 ) {
				out.log( "READ 50E0 group leader changes group recruiting status" );
				/*
				 * primer byte a 1, estamos contratando.
				 * primer byte a 0, dejamos de contratar.
				 */
				/*
				 * Siguientes bytes: comentario
				 */
				byte sel = mb.getData()[0];
				String comment = new String(mb.getData(), 1, Util.strlen(mb
						.getData(), 1));
				
				out.log( "Se trata de una operacion: " + sel + " y el comentario si lo hay es: " + comment );
				
				try {
					Tools.dbSaveGroupRecruitment(db, player.getGid(), sel, comment );
					mes = new Message();
					mes.addBlock( new MessageBlockZero(0x000050E1));
					out.streamWriteEncodedNIO(mes);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x000050E1));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x00005032 ) {
				out.log( "READ 5032 retire the recruitment request" );
				try {
					Tools.dbUpdatePlayerGidAndInvitation( db, player.getId(), 0, 0 );
					player.setGid(0);
					player.setGroupName("");
					player.setInvitation(0);
					mes = new Message();
					mes.addBlock( new MessageBlockZero(0x00005033));
					out.streamWriteEncodedNIO(mes);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x00005033));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x00005030 ) {
				out.log( "READ 0x5030 the player chooses to join a group");
				DataId did = new DataId(mb.getData());
				long gid = did.getId();
				out.log( "He chooses to join the group: " + gid );
				/*
				 * Ojo en invitation level tengo que ponerle un 4?, creo que esto significaba
				 * que estaba pendiente de que le aceptaran
				 * 
				 */
				try {
					//Tools.dbInsertGroupRecruit(db, gid, player.getId());
					//GroupInfo g = Tools.dbLoadGroup(db, gid);
					String groupName = Tools.dbUpdatePlayerGidAndInvitation( db, player.getId(), gid, 4 );
					player.setGid(gid);
					player.setGroupName(groupName);
					//player.setGroupInfo(g);
					player.setInvitation(4);
						
					mes = new Message();
					mes.addBlock( new MessageBlockZero(0x00005031));
					out.streamWriteEncodedNIO(mes);
					sendPlayerList(channel);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x00005031));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x00005036 ) {
				out.log( "READ 0x5036 this comes when the group leader chooses to decline a group applicant" );
				/*
				 * No debería venir aqui al menos el id del tio al que rechazo??
				 */
				
				// De 0 a 3 debería tener el id del tipo al que quiero añadir!
				long pid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb.getData()[2], mb.getData()[3]);
				byte sel = mb.getData()[4];
				
				out.log( "Se trata del player con pid: " + pid + " y lo que elijo hacer con el es " + sel );
				
				if( sel == 1 ) {
					try {
						Tools.dbUpdatePlayerGidAndInvitation(db, pid, player.getGid(), 1);
						
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005037));
						out.streamWriteEncodedNIO(mes);
						
						/*
						 * Ojo si el player está online....
						 */
						PESConnection c = connections.searchConnection(pid);
						if( c != null && c.getPlayerInfo() != null ) {
							c.getPlayerInfo().setGid( player.getGid() );
							c.getPlayerInfo().setGroupName( player.getGroupName() );
							c.getPlayerInfo().setInvitation( 1 );
							ChannelInfo hisChannel = channels.getChannel( c.getPlayerInfo().getIpInfo().getChannel() );
							sendPlayerList(hisChannel);
						}
						PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());

						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005061));
						out.streamWriteEncodedNIO(mes);
				
						
						mes = new Message();
						mes.addBlock(new MessageBlockGameGroupMembers(pl,
								0x00005062));
						out.streamWriteEncodedNIO(mes);
						
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005063));
						out.streamWriteEncodedNIO(mes);
					} catch( SQLException ex ) {
						mes = new Message();
						mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x00005037));
						out.streamWriteEncodedNIO(mes);
					}
				} else {
					try {
						Tools.dbUpdatePlayerGidAndInvitation(db, pid, 0, 0);
						
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005037));
						out.streamWriteEncodedNIO(mes);
						
						/*
						 * Ojo si el player está online....
						 */
						PESConnection c = connections.searchConnection(pid);
						if( c != null && c.getPlayerInfo() != null ) {
							c.getPlayerInfo().setGid( 0 );
							c.getPlayerInfo().setGroupName( "" );
							c.getPlayerInfo().setInvitation( 0 );
							ChannelInfo hisChannel = channels.getChannel( c.getPlayerInfo().getIpInfo().getChannel() );
							sendPlayerList(hisChannel);
						}
						PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());

						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005061));
						out.streamWriteEncodedNIO(mes);
				
						
						mes = new Message();
						mes.addBlock(new MessageBlockGameGroupMembers(pl,
								0x00005062));
						out.streamWriteEncodedNIO(mes);
						
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005063));
						out.streamWriteEncodedNIO(mes);
					} catch( SQLException ex ) {
						mes = new Message();
						mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x00005037));
						out.streamWriteEncodedNIO(mes);
					}
				}
				

			} else
			if( qId == 0x00005070 ) {
				out.log( "READ 0x5070 Esto es una lista de partidos del grupo tal vez" );
				long gid = new DataId(mb.getData()).getId();
				out.log( "Me piden información de los ultimos partidos del grupo: " + gid );
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005071));
				mes.addBlock(new MessageBlockZero(0x00005073));
				out.streamWriteEncodedNIO(mes);
			} else
			if( qId == 0x00005050 ) {
				out.log( "READ 0x5050 Expulsar a alguien del grupo" );
				/*
				 * Viene el id del pájaro en cuestión y despues el motivo
				 */
				long pid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb.getData()[2], mb.getData()[3]);
				String comment = new String(mb.getData(), 4, Util.strlen(mb
						.getData(), 4));
				out.log( "Vamos a expulsar a player con pid: " + pid + " con motivo: " + comment );
				try {
					Tools.dbRemovePlayerFromGroup(db, pid, player.getGid());
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00005051));
					out.streamWriteEncodedNIO(mes);
					/*
					 * Ojo si el player está online....
					 */
					PESConnection c = connections.searchConnection(pid);
					if( c != null && c.getPlayerInfo() != null ) {
						c.getPlayerInfo().setGid( 0 );
						c.getPlayerInfo().setGroupName( "" );
						ChannelInfo hisChannel = channels.getChannel( c.getPlayerInfo().getIpInfo().getChannel() );
						sendPlayerList(hisChannel);
					}
					PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());

					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00005061));
					out.streamWriteEncodedNIO(mes);
			
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameGroupMembers(pl,
							0x00005062));
					out.streamWriteEncodedNIO(mes);
					
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00005063));
					out.streamWriteEncodedNIO(mes);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x000050C1));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x000050C0 ) {
				out.log( "READ 0x50C0 Disolver el grupo" );
				try {
					Tools.dbRemoveGroup(db, player.getGid());
					player.setGid(0);
					player.setGroupName("");

					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x000050C1));
					out.streamWriteEncodedNIO(mes);
					sendPlayerList(channel);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x000050C1));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x000050A0 ) {
				out.log( "READ 0x50A0 Change leadership of the group" );
				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				/*
				 * Tengo que enviarle algo al otro animal para que se de cuenta
				 * de lo que está pasando.
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockLong(player.getId(), 0x000050A2));
				connections.searchConnection(pid).setRequester(player.getId());
				out.setRecipient(player.getId());
				connections.searchConnection(pid).streamWriteEncodedNIO(mes);
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x000050A1));
				out.streamWriteEncodedNIO(mes);
			} else
			if( qId == 0x000050A3 ) {
				out.log( "READ 0x50A3 Player accepts or declines to be the new group leader");
				/*
				 * Respuesta a invitación de grupo
				 */
				
				byte ret = mb.getData()[0];
				out.log( "Choosed: " + ret + " requester: " + out.getRequester() ); //$NON-NLS-1$ //$NON-NLS-2$
				
				long reqId = out.getRequester();
				PlayerInfo pg = players.getPlayerInfo(reqId);
				if( pg != null && pg.getGid() != 0 ) { 
				
					if( ret == (byte)0x01 ) {
						/*
						 * Ha aceptado ser el nuevo lider, actualizamos la bd
						 */
						
						try {
							Tools.dbChangeGroupLeader(db, player.getGid(), player.getId());
							player.setInvitation(3);
							pg.setInvitation(2);
							PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());

							mes = new Message();
							mes.addBlock(new MessageBlockZero(0x00005061));
							pg.getConnection().streamWriteEncodedNIO(mes);
					
							
							mes = new Message();
							mes.addBlock(new MessageBlockGameGroupMembers(pl,
									0x00005062));
							pg.getConnection().streamWriteEncodedNIO(mes);
							
							mes = new Message();
							mes.addBlock(new MessageBlockZero(0x00005063));
							pg.getConnection().streamWriteEncodedNIO(mes);
							
						} catch( SQLException ex ) {
							out.log( "Unable to change group leader"); //$NON-NLS-1$
							out.log( ex.getMessage() );
						}
						
						mes = new Message();
						MessageBlock mb1 = new MessageBlockZero( 0x000050A4 );
						mb1.setData(new byte[] { (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 });
						mes.addBlock(mb1);
						out.streamWriteEncodedNIO(mes);
	
						mes = new Message();
						mes.addBlock(new MessageBlockByte( (byte)0x00, 0x000050A6));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
					if( ret == (byte)0x00 ) {
						/*
						 * Tanto el 5046 con algo que no sea un cero como el 5045 producen el
						 * mismo efecto en el cliente: "Como el rival no pudo responder.... etc"
						 */
						mes = new Message();
						mes.addBlock(new MessageBlockByte( (byte)0x02, 0x000050A6));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
					if( ret == (byte)0x02 ) {
						mes = new Message();
						mes.addBlock(new MessageBlockByte(ret, 0x000050A5));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
				} else {
					out.log( "The player which offered leadership to me, no longer exists (or has no group at all)"); //$NON-NLS-1$
				}			
			} else
			if( qId == 0x000050F2 ) {
				out.log( "READ 0x50F2 the group leader changes the comment for the group" );
				String comment = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				try {
					Tools.dbSaveGroupComment( db, player.getGid(), comment );
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x000050F3));
					out.streamWriteEncodedNIO(mes);
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xFFFFFFFF, 0x000050F3));
					out.streamWriteEncodedNIO(mes);
				}
			} else
			if( qId == 0x000050D0 ) {
				
				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				
				out.log( "READ 0x50D0 this comes to change a player's permission to invite others into the group," +
						" player to change permission: " + pid );
				
				
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x000050D1));		
				out.streamWriteEncodedNIO(mes);
				
				try {
					Tools.dbChangePlayerInvitation( db, pid );
					PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());
					
					PlayerInfo p = players.getPlayerInfo(pid);

					
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00005061));
					out.streamWriteEncodedNIO(mes);
			
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameGroupMembers(pl,
							0x00005062));
					out.streamWriteEncodedNIO(mes);
					
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x00005063));
					out.streamWriteEncodedNIO(mes);
					
					if( p != null ) {
						p.setInvitation( p.getInvitation() == 1 ? 2 : 1 );
						//sendPlayerList(channel);
					}			
					
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				
//				FileOutputStream fo = new FileOutputStream("5062_mio.dec");
//				fo.write(mes.getBytes());
//				fo.close();
				
			} else
			if (qId == 0x0000308C) {
				out.log( "READ 0x308C Leave the lobby?"); //$NON-NLS-1$
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000308D));
				out.streamWriteEncodedNIO(mes);
			} else

			// El jugador est� cambiando su comentario, el comentario es el
			// contenido del paquete y un 0x00 al final
			if (qId == 0x00004110) {
				out
						.log("MATCHES GAME CHANGECOMMENT CLIENT QUERY ANSWERING ZEROS:"); //$NON-NLS-1$

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
						.log("MATCHES GAME ROOM CHANGE NAME AND PASS QUERY ANSWERING WITH NEW ROOM INFO:"); //$NON-NLS-1$
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
						.log("MATCHES GAME FORCE DISCARD PARTICIPATION QUERY ANSWERING WITH NEW ROOM INFO:"); //$NON-NLS-1$
				DataId did = new DataId(mb.getData());

				rooms.removePlayerSelInRoom(did.getId());
				RoomInfo r = rooms.searchPlayer(did.getId());

				if( r == null ) {
					out.log( "THE ROOM DOESNT EXIST!!! RETURNING"); //$NON-NLS-1$
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
						.log("MATCHES GAME CHANGE ROOM OWNER QUERY ANSWERING WITH NEW ROOM INFO:"); //$NON-NLS-1$
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
						.log("MATCHES GAME CRTROOM CLIENT QUERY ANSWERING WITH NEW ROOM INFO:"); //$NON-NLS-1$

				DataRoom room = new DataRoom(mb.getData());
				
				createRoom(player, channel, room.getName(), room.hasPassword(), room.getPassword());

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
						.log("MATCHES WEIRD AFTER JOIN ROOM CLIENT QUERY ANSWERING MY IP INFO:"); //$NON-NLS-1$

				DataId did = new DataId(mb.getData());
				RoomInfo r = rooms.getRoom(did.getId());
				
				/*
				 * OJO, CUIDADO CON ESTO!
				 */
				//sendRoomList( channel );
				sendRoomUpdate(channel, r);
				
				if( r != null ) {		
					// RoomInfo r = rooms.searchPlayer(player.getId());
					IpInfo ips[] = new IpInfo[r.getNumPlayers()];
					for (int i = 0; i < r.getNumPlayers(); i++) {
						ips[i] = r.getPlayerFromIdx(i).getIpInfo();
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
					out.log("HE TERMINADO EL ROLLO WEIRD!!!!"); //$NON-NLS-1$
					
					/*
					 * Probando, voy a enviar los match settings si es necesario
					 */
					if( r.getGamePhase() > 1 && r.getMatchSettings() != null  ) {
						mes = new Message();
						MessageBlock m = new MessageBlockVoid(0x0000436E);
						m.setData(r.getMatchSettings());
						mes.addBlock(m);
						out.streamWriteEncodedNIO(mes);
					}
					
					/*
					 * Actualizacion de las salas para todo el mundo
					 */
					//System.out.println( "estoy en weird, y envio a todo el mundo la info de salas");		
					
				} else {
					out.log( "THE ROOM NO LONGER EXISTS!!, DOING NOTHING!" ); //$NON-NLS-1$
				}
			
			} else if (qId == 0x00004B00) { // Me da la informaci�n de direccion
											// IP de la id que consulto (4 bytes
											// de payload)
				out.log("MATCHES 0x00004B00 ANSWER IP INFO:"); //$NON-NLS-1$
				DataId did = new DataId(mb.getData());
				// Dos veces en el c�digo, mismo query id
				/*
				 * Excepcion, no tengo la info de ip de este señor, porque?
				 */
				PlayerInfo p = players.getPlayerInfo(did.getId());
				if( p == null ) {
					out.log( "No se encuentra el player con ID: " + did.getId()); //$NON-NLS-1$
					return;
				}
				IpInfo tmp = players.getPlayerInfo(did.getId()).getIpInfo();
				
				if( tmp != null ) {
//					mes.addBlock(new MessageBlockGameIpInfoShort(tmp, 0x00004B01));
//					out.streamWriteEncodedNIO(mes);
	
					/*
					 * Si si, dos veces menuda broma pero es asi tienen secuencias
					 * diferentes
					 */
	
					/*
					 * Los logs dicen que es dos veces, deberíamos probar a comentarlo de cualquier forma
					 * solo por probar. 27 abril 2009, sin subirse al server
					 */
					/*
					 * Es uno para mi con su información y otro para el con mi información
					 */
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameIpInfoShort(tmp, 0x00004B01));
					out.streamWriteEncodedNIO(mes);
					
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameIpInfoShort(player.getIpInfo(), 0x00004B01));
					players.getPlayerInfo(did.getId()).getConnection().streamWriteEncodedNIO(mes);
				} else {
					out.log( "THERE ISNT ANY IPINFO FOR THE SELECTED PLAYER, RETURNING"); //$NON-NLS-1$
					return;
				}
			} else if (qId == 0x0000432A) { // Salir del salon de juego!
				out.log("MATCHES 0x0000432A (Exit room) ANSWERING ????:"); //$NON-NLS-1$

				RoomInfo r = rooms.searchPlayer(player.getId());
				
				if( r == null ) {
					out.log( "THE ROOM DOESNT EXIST!, RETURNING"); //$NON-NLS-1$
					return;
				}
				
				rooms.removePlayerFromRoom(r.getId(), player.getId());

				if (rooms.getRoom(r.getId()).getNumPlayers() == 0) {
					out.log("REMOVING ROOM"); //$NON-NLS-1$
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
				out.log("MATCHES 0x00004320 (Enter room) ANSWERING ????:"); //$NON-NLS-1$

				DataJoinRoom droom = new DataJoinRoom(mb.getData());

				enterRoom(player, rooms, players, droom.getRoomId(), droom.getPassword());

			} else
			// Envio mensaje
			if (qId == 0x00004400) {
				
				DataChatMessage msg = new DataChatMessage(mb.getData());
				out.log("RECIBO UN MENSAJE, SE LO ENVIO A LOS DESTINATARIOS Canal: " + msg.getChannel() + " Place: " + //$NON-NLS-1$ //$NON-NLS-2$
						msg.getPlace() );
				
				String str = msg.getMessage();
				str = str.trim();
				
				if( str.equals("") ) { //$NON-NLS-1$
					return;
				}
				
				if( str.charAt(0) == '#' && player.isAdmin()) {
					/*
					 * Comandos de administracion
					 */
					String[] cmdArg = str.split(" ", 2); //$NON-NLS-1$
					cmdArg[0] = cmdArg[0].toLowerCase();
					if( cmdArg.length > 1  && cmdArg[0].equals( "#kick" )) { //$NON-NLS-1$
						String target = cmdArg[1];
						PlayerInfo pg = players.getPlayerInfo(target);
						if( pg != null && !pg.isAdmin()) {
							PESConnection con1 = connections.searchConnection(pg.getId());
							if( con1 != null ) disconnectPlayer(con1);
						}
					
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#error" )) {
						String codeStr = cmdArg[1];
						try {
							int code = Integer.parseInt(codeStr);
							out.setDebugErrorCode(code);
						} catch( NumberFormatException e ) {
							
						}
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#supersay" )) { //$NON-NLS-1$
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)0x02, cmdArg[1],
								false, 0x00004402));
						connections.sendToAllEncodedNIO( mes );
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)0x01, cmdArg[1],
								false, 0x00004402));
						connections.sendToAllEncodedNIO( mes );
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#getusername" )) { //$NON-NLS-1$
						PlayerInfo pl = channel.getPlayerList().getPlayerInfo( cmdArg[1] );
						String message = null;
						if( pl == null ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.75"); //$NON-NLS-1$
						} else {
							PESConnection c = connections.searchConnection( pl.getId() );
							if( c == null ) {
								message = Messages.getString(player.getLang(), "PES6JGameServer.76"); //$NON-NLS-1$
							} else {
								message = cmdArg[1]+Messages.getString(player.getLang(), "PES6JGameServer.77") + c.getUserInfo().getUsername(); //$NON-NLS-1$
							}
						}
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								true, 0x00004402));
						out.streamWriteEncodedNIO(mes);
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#getipaddress" )) { //$NON-NLS-1$
						PlayerInfo pl = channel.getPlayerList().getPlayerInfo( cmdArg[1] );
						String message = null;
						if( pl == null ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.79"); //$NON-NLS-1$
						} else {
							PESConnection c = connections.searchConnection( pl.getId() );
							if( c == null ) {
								message = Messages.getString(player.getLang(), "PES6JGameServer.80"); //$NON-NLS-1$
							} else {
								message = cmdArg[1]+Messages.getString(player.getLang(), "PES6JGameServer.81") + c.getSocket().getInetAddress().getHostAddress(); //$NON-NLS-1$
							}
						}
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								true, 0x00004402));
						out.streamWriteEncodedNIO(mes);
					} else if( cmdArg.length > 1 && cmdArg[0].equals( "#shutdown" )) { //$NON-NLS-1$
						
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm"); //$NON-NLS-1$
						Date d = null;
						String message = "Shutdown scheduled for " + cmdArg[1]; //$NON-NLS-1$
						try {
							d = sdf.parse( cmdArg[1] );
						} catch( ParseException pe ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.85"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(player,  
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
							return;
						}
						
						mes = new Message();
						mes.addBlock(new MessageBlockGameChatMessage(player,  
								(byte)msg.getPlace(), (byte)msg.getChannel(), message,
								true, 0x00004402));
						out.streamWriteEncodedNIO(mes);
//						System.out.println( "yo tenía: \"" + cmdArg[1] + "\"");
//						System.out.println( "aver: " + sdf.format( d ));
//						System.out.println( "-----hoy----: " + Util.getTimeOfToday());
//						System.out.println( "-----d------: " + d.getTime());
//						System.out.println( "--hoy+d-----: " + (Util.getTimeOfToday() + d.getTime()));
//						System.out.println( "---now------: " + new Date().getTime());
						shutdownTime = (Util.getTimeOfToday() + d.getTime());

					}
				} else 
				if( str.charAt(0) == '#' ) {
					String[] cmdArg = str.split(" ", 2); //$NON-NLS-1$
					cmdArg[0] = cmdArg[0].toLowerCase();
					if( cmdArg.length > 1 && cmdArg[0].equals( "#error" )) {
						String codeStr = cmdArg[1];
						try {
							int code = Integer.parseInt(codeStr);
							out.setDebugErrorCode(code);
						} catch( NumberFormatException e ) {
							
						}
					}
				} else
				if( str.charAt(0) == '!') {
					String[] cmdArg = str.split(" ", 2); //$NON-NLS-1$
					cmdArg[0] = cmdArg[0].toLowerCase();
					if( cmdArg.length > 1  && cmdArg[0].equals( "!verify" )) { //$NON-NLS-1$
						String pinCode = cmdArg[1].trim();
						String message;
						RoomInfo r = rooms.searchPlayer(player.getId());
						
						/*
						 * Si no hay cache, no pueden escribir verificaciones.
						 */
						if( r.cacheClean() ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.90"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
							return;
						}
						r.setUseCacheData();
						if( r == null ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.88"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
						} else
						if( r.getTimestampLagged() == 0 ) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.90"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
						} else
						if( r.getCodeSent(player.getId())) {
							message = Messages.getString(player.getLang(), "PES6JGameServer.92"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
						} else 
						if( !pinCode.equals(r.getPinCode(player.getId()))) {
							message = Messages.getString( player.getLang(), "PES6JGameServer.94"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
						} else {
							r.setCodeSent(player.getId(), true);
							message = Messages.getString(player.getLang(), "PES6JGameServer.96"); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
									(byte)msg.getPlace(), (byte)msg.getChannel(), message,
									true, 0x00004402));
							out.streamWriteEncodedNIO(mes);
						}
						r.setUseRealData();
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
							false, 0x00004402));
					if (msg.getChannel() == 0x00000001)
						connections.sendToChannelNotInGameEncodedNIO( channel, mes );
					else if (msg.getChannel() == 4 ) {
						/*
						 * Mensaje para el grupito!!! de momentorg se lo envio a
						 * todos
						 */
						//connections.sendEncodedToNIO(player.getGroupInfo().getPlayerIds(), mes );
						connections.sendEncodedToGroup(player.getGid(), mes);
						//connections.sendEncodedToNIO(players.getPidsInGroup(player.getGroupInfo().getId()), mes );
						
						//connections.sendToEncodedNIO( channelCons, mes);
					} else if( msg.getChannel() == 3 || msg.getChannel() == 2) {
						if( str.charAt(0) == '/') {
							String[] cmdArg = str.split(" ", 2); //$NON-NLS-1$
							if( cmdArg.length > 1 && !cmdArg[1].trim().equals("")) { //$NON-NLS-1$
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
				out.log( "RECEIVE 4563 PARTICIPATE IN A MATCH (OR CANCEL PARTICIPATION)"); //$NON-NLS-1$
				// byte[] orden = mes.getBlock(0).getData();
				byte[] orden = mb.getData();
				int sel = 0x00FF;
				int opcion = 0;

				RoomInfo r;
				r = rooms.searchPlayer(player.getId());
				if( r == null ) {
					out.log( "CANNOT FIND THE ROOM IN WHICH THE PLAYER IS, RETURNING" ); //$NON-NLS-1$
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

				out.log("RESPONDO: "); //$NON-NLS-1$
				out.log(Util.toHex(mes.getBytes()));

				mes = new Message();
				mes.addBlock(new MessageBlockGameRoomOptionSel(opcion, sel,
						0x00004364));
				out.streamWriteEncodedNIO(mes);

				out.log("RESPONDO: "); //$NON-NLS-1$
				out.log(Util.toHex(mes.getBytes()));

				// mes = new Message();
				// mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] {
				// r
				// }
				// , QUERIES.GAME_ROOMINFO_SERVER_QUERY, 0));
				// out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004360) {
				out.log("READ START MATCH, ANSWER ??:"); //$NON-NLS-1$
				RoomInfo r = rooms.searchPlayer(player.getId());
				r.setGamePhase(2);
				
				// Antes empezabamos con 3 porque no nos interesaban los primeros estados
				// r.setPlayersPhase(3);
				r.resetAllReady();
				r.setPlayersPhase(2);
				
				
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

				sendRoomUpdate(channel, r);
				//sendRoomList(channel);
				/*
				 * No es necesario parece ser
				 */
				// mes = new Message();
				// mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r
				// }, 0x00004606, 0));
				// connections.sendToEncodedNIO( channelCons, mes);
			} else if (qId == 0x00004350) {
				out
						.log("RECEIVING PLAYER MOVE TEAM CLIENT QUERY, SENDING THE SAME TO PLAYERS IN ROOM:"); //$NON-NLS-1$
				RoomInfo r = rooms.searchPlayer(player.getId());
				mes = new Message();
				mes.addBlock(mb);
				if (r != null) {
					connections.sendEncodedToNIO(r.getPlayerIdsExcept(player.getId()), mes );
					//connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				}
			} else if (qId == 0x00004351) {
				out
						.log("RECEIVING 4351 WATCH A MATCH, BEHAVING THE SAME AS WITH 4350:"); //$NON-NLS-1$
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
						.log("RECEIVING 4366 WATCH A MATCH, ANSWERING WITH a 4367 payload 00 00 00 00 whatever query said:"); //$NON-NLS-1$
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
				out.log("RECEIVING 4373 THE CLIENT CHOOSED A TEAM!"); //$NON-NLS-1$

				/*
				 * Ojo hay dos bytes mas, FF FF que coño son? la equipación?
				 */
				
				int team = Util.word2Int(mb.getData()[0], mb.getData()[1]);
				RoomInfo r = rooms.searchPlayer(player.getId());

				if( r == null ) {
					out.log( "THE ROOM DID NOT EXIST, RETURNING"); //$NON-NLS-1$
					return;
				}
				
				if (r.getPlayerSelFromPid(player.getId()) != 0x00FF && r.getPlayerTeam(player.getId()) == 0) {	
					r.setTeam2(team);
					
				}
				if (r.getPlayerSelFromPid(player.getId()) != 0x00FF && r.getPlayerTeam(player.getId()) == 1) {
					r.setTeam1(team);
				}

				/*
				 * Que cojones??? no es 4374???
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004374));
				out.streamWriteEncodedNIO(mes);

				//if (r.getPlayerSelFromPid(player.getId()) != 0x00FF ) {
					/*
					 * No solo a mi, a todos los de mi equipo
					 */
					//r.setReadyStatus(player.getId(), 1);
					//r.setReadyStatusOfTeam( r.getPlayerTeam(player.getId()), 1 );
					//r.setPlayerPhaseOfTeam( r.getPlayerTeam(player.getId()), 5);
					//r.setPlayerPhase(player.getId(), 5);

					//r.setPlayerPhase(player.getId(), r.getPlayerPhase(player.getId()) + 1);	
				//}
				//rooms.setRoom(r.getId(), r);

				
				// DEBERIA METERLO SOLO CUANDO AMBOS HAN ELEGIDO EQUIPO


				if (r != null && r.getPlayerSelFromPid(player.getId()) != 0x00FF && r.getGamePhase() >= 4 && r.allTeamsReady()) {
					//System.out.println( "BIEN!, HAN ELEGIDO LOS EQUIPOS!!!"); //$NON-NLS-1$
					mes = new Message();
					mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r },
							0x00004306));
					connections.sendToChannelNotInGameEncodedNIO( channel, mes );
					r.setGamePhase(5);
					r.setPlayersPhase(5);
					//r.resetAllReady(r.getGamePhase());
				}
			} else if (qId == 0x0000436F) {
				out
						.log("RECEIVING PLAYER READY CLIENT QUERY, ANSWERING DUNNO: "); //$NON-NLS-1$

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
					out.log( "CANNOT FIND THE ROOM ON WHICH THIS PLAYER IS, PLAYER ID: " + player.getId() + ", RETURNING"); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004370));
				out.streamWriteEncodedNIO(mes);

				
				// if( r.getGamePhase() < 4 ) {
				/*
				 * Es posible que me haya enviado mas de uno, en cuyo
				 * caso su fase es
				 */
				r.setReadyStatus(player.getId(), sel);
				System.out.println( player.getName() + ": Asigno ready a fase: " + r.getPlayerPhase(player.getId()) + 
						r.getReadyStatus(player.getId(), r.getPlayerPhase(player.getId())));
				System.out.println( player.getName() + ": FASE ANTES: " + r.getPlayerPhase(player.getId()));
				

				System.out.println( player.getName() + ": FASE PARA DESPUES: " + r.getPlayerPhase(player.getId()));

				
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
					
//					if( (r != null && r.getGamePhase() > 3
					if( (r != null && r.getGamePhase() > 3
							&& r.getGamePhase() < 7 ) ) {				
						r.setPlayerPhase(player.getId(), r.getPlayerPhase(player.getId()) + 1);				
					}
				
					connections.sendEncodedToNIO(r.getPlayerIdsExcept(player
							.getId()), mes);
					
					long pidsx[] = r.getPlayerIdsOnSel();
					
					if( r.getPlayerFell(player.getId())) return;
					if( pidsx.length == 0 ) return;
					
//					System.out.println( "TENGO " + r.getNumPlayersOnSel() + " PLAYERS ON SEL"); //$NON-NLS-1$ //$NON-NLS-2$
//					System.out.println( "FASE " + r.getGamePhase() ); //$NON-NLS-1$		
//					
//					for( int i = 0; i < pidsx.length; i++ ) {
//						String name = "NO PLAYER!"; //$NON-NLS-1$
//						if( r.getPlayerFromPid(pidsx[i]) != null ) name = r.getPlayerFromPid(pidsx[i]).getName();
//						System.out.println( i+": " + name + " ready: " +  //$NON-NLS-1$ //$NON-NLS-2$
//								r.getReadyStatus(pidsx[i], r.getGamePhase()));
//					}
				/*
				 * Pasar a selección de equipos
				 */
				if( r != null && r.getGamePhase() == 7 && r.allReadyOn(7, 3)) {
					
					//r.restorePlayerSelsFromCache();
					out
					.log("GO TO TEAM SELECTION!"); //$NON-NLS-1$
					//System.out.println( "TO TEAM SELECTION!"); //$NON-NLS-1$
					r.setGamePhase(4);
					r.setPlayersPhase(4);
					r.resetAllReady();
					r.resetGoals();
					r.resetTeams();
					// SIN SUBIR LO DEJE EN 3
					r.setType(4);
//					r.resetCache();
//					player.setOldCategory( player.getCategory() );
//					player.setOldPoints( player.getPoints() );
					sendRoomUpdate(channel,r);
				}
				
				/*
				 * Si eligen
				 */
//				if( r != null && r.getGamePhase() == 7 && (sel == 3 || sel == 4)) {
//					player.setOldCategory( player.getCategory() );
//					player.setOldPoints( player.getPoints() );
//				}
				
				/*
				 * Jugar otro partido
				 */
				if( r != null && r.getGamePhase() == 7 && r.allReadyOn(7, 4) ) {
					out
					.log("PLAY ANOTHER MATCH!"); //$NON-NLS-1$
					r.restorePlayerSelsFromCache();
					//System.out.println( "PLAY ANOTHER MATCH!"); //$NON-NLS-1$
					r.resetGoals();
					r.resetAllReady();
//					r.resetCache();
//					player.setOldCategory( player.getCategory() );
//					player.setOldPoints( player.getPoints() );
					/*
					 * Con 6 funciona
					 */
					r.setType(5);
					r.setGamePhase(7);
					r.setPlayersPhase(7);
					sendRoomUpdate(channel,r);
				}
				
				if (r != null && r.allReadyOn(r.getGamePhase(), 1) && r.getGamePhase() <= 3) {
//				if (r != null && r.allReadyOn(r.getGamePhase(), 1) && r.getGamePhase() == 3) {

					out
							.log("ALL PLAYERS ARE READY, SENDING 4344 BLOCK TO ALL!"); //$NON-NLS-1$
					//r.setGamePhase(3);
					/*
					 * Subimos la fase
					 */
					r.setGamePhase(r.getGamePhase()+1);
					
					mes = new Message();
					mes.addBlock(new MessageBlockGameStart4344(
							r.getGamePhase(), 0x00004344));

					connections.sendEncodedToNIO(r.getPlayerIds(), mes);
					r.resetGoals();
					r.setPlayersPhase(r.getGamePhase());
					//rooms.setRoom(r.getId(), r);
				} else if ( r.getGamePhase() > 3
						&& r.getGamePhase() < 7 ) {


					

					// connections.sendEncodedToNIO(r.getPlayerIdsOnSel(), mes);
					// connections.sendEncodedToNIO(r.getPlayerIds(), mes);

					/*
					 * Hay que actualizar la info general de la sala, el tipo
					 * ahora es 7 el byte de detras del siete indica en que
					 * parte est�n jugando
					 */

					//r.resetAllReady();
					
					if( (r.getGamePhase() == 5 || r.getGamePhase() == 6) && r.allTeamLeadersReady(r.getGamePhase(), 1)) {
						/*
						 * Han elegido equipos 
						 */
						out.log("PASO CON FASE: " + r.getGamePhase()); //$NON-NLS-1$
						mes = new Message();
						mes.addBlock(new MessageBlockGameStart4344(
								r.getGamePhase(), 0x00004344));
						// out.streamWriteEncodedNIO(mes);
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
						r.resetWatchedScores();
						long[] pids = r.getPlayerIdsOnSel();
						for( int i = 0; i < pids.length; i++ ) {
							PlayerInfo p = players.getPlayerInfo( pids[i] );
							if( p != null ) {
								//System.out.println( "RESETEO A " + p.getName()); //$NON-NLS-1$
								p.setOldCategory( p.getCategory() );
								p.setOldPoints( p.getPoints() );
							}
						}
						r.resetGoals();
						r.setPeopleOnMatch( r.getNumPlayersOnSel() );
						r.setGamePhase(r.getGamePhase() + 1);
						r.setPlayersPhase(r.getGamePhase());
						/*
						 * Nuevo, debo resetear el cache
						 * de otra forma, si se caen en formaciones antes de empezar partido
						 * no habrán pincodes!
						 */
//						System.out.println( "BIEN PASO EN FASE 5 O 6 Y RESETEO EL CACHE, ADEMAS ME VOY A FOLLAR LOS CODES" );
						r.resetCache();
						r.clearPinCodes();
					} else {
					
						if( r.allReadyOn(r.getGamePhase(), 1) ) {
							out.log("PASO CON FASE: " + r.getGamePhase()); //$NON-NLS-1$
							mes = new Message();
							mes.addBlock(new MessageBlockGameStart4344(
									r.getGamePhase(), 0x00004344));
							// out.streamWriteEncodedNIO(mes);
							connections.sendEncodedToNIO(r.getPlayerIds(), mes);
						}

//					if( r.getGamePhase() == 6 && r.allReadyOn(r.getGamePhase(), 1)) {
//						r.resetWatchedScores();
//						long[] pids = r.getPlayerIdsOnSel();
//						for( int i = 0; i < pids.length; i++ ) {
//							PlayerInfo p = players.getPlayerInfo( pids[i] );
//							if( p != null ) {
//								//System.out.println( "RESETEO A " + p.getName()); //$NON-NLS-1$
//								p.setOldCategory( p.getCategory() );
//								p.setOldPoints( p.getPoints() );
//							}
//						}
//						r.resetGoals();
//						r.setPeopleOnMatch( r.getNumPlayersOnSel() );
//					}
						
						if (r.getGamePhase() > 4 && r.allReadyOn(r.getGamePhase(), 1) )
							r.setGamePhase(r.getGamePhase() + 1);
						
					}
					//rooms.setRoom(r.getId(), r);
				} else if (r != null && r.getGamePhase() == 7) {
					// Cambio, si un tio se va del partido se va todo kisk
// Esta condición bloquearía al otro jugador
//					if (r.allReadyOn(0)) {
// Y esta otra miente porque resetea todo cuando aún no debería haberse reseteado.
					
					if (sel == 0 ) {

						out.log( "TERMINAR PARTIDO!" ); //$NON-NLS-1$
						r.setPlayerSel(player.getId(), 0x00FF);						
						sendRoomUpdate(channel, r);
						//sendRoomList( channel );
						/*
						 * No se si es necesario
						 */
						// mes = new Message();
						// mes.addBlock(new MessageBlockGameRoomList(new
						// RoomInfo[] { r }, 0x00004306, 0));
						// connections.sendToEncodedNIO( channelCons, mes);
						//rooms.setRoom(r.getId(), r);
					}
					
					if( r.allReadyOnCacheOfSels(7, 0) ) {
						r.setType(1);
						r.setGamePhase(1);
						r.resetAllButCache();
						//System.out.println( "TODOS LISTOS EN 0 RESETEO, FASE Y TIPO 1"); //$NON-NLS-1$
						// ANTES!
						//sendRoomList( channel );
						sendRoomUpdate(channel, r);
					}  else if (r.allReadyOn(7, 1)) {
						/*
						 * Limpiamos la cache!
						 */
						
						r.resetCache();
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
						r.resetWatchedScores();
						r.setPeopleOnMatch( r.getNumPlayersOnSel() );
						//out.log("PASO CON FASE: " + r.getGamePhase());
//						logger.log("PASO CON FASE: " + r.getGamePhase());
						mes = new Message();
						mes.addBlock(new MessageBlockGameStart4344(r
								.getGamePhase(), 0x00004344));
						// out.streamWriteEncodedNIO(mes);
						connections.sendEncodedToNIO(r.getPlayerIds(), mes);
//						System.out.println( "EMPIEZA UN PARTIDO!!!!\n"); //$NON-NLS-1$
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!"); //$NON-NLS-1$
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!"); //$NON-NLS-1$
						out.log("PHASE 7 THE MATCH IS READY!!!!!!!"); //$NON-NLS-1$
						r.setType(7);
						r.setHalf(1);
						
						// OJO CON ESTE CAMBIO!!!!
						//sendRoomList( channel );
						sendRoomUpdate( channel, r );
						r.resetAllReady(7);
//						mes = new Message();
//						mes.addBlock(new MessageBlockGameRoomList(
//								new RoomInfo[] { r }, 0x00004306));
//						connections.sendToChannelNotInRoomEncodedNIO( channel, mes );
						//rooms.setRoom(r.getId(), r);
					}
				}

			} else if (qId == 0x00004369) {
				out.log("RECEIVING GAME START PLAYER FINAL POSITIONS: "); //$NON-NLS-1$

				
				/*
				 * Debo recuperar donde se colocaron los jugadores en la
				 * seleccion de mu�equitos.
				 * 
				 * Por lo que se ve y por este orden: - id del jugador (4 bytes) -
				 * 4 mas: primer byte: id del equipo en el que est� empezando en
				 * 0 ultimo byte: indica si es lider del equipo 1 = lider, 0 =
				 * no lider osea para un 2vs2: 00 00 00 01 y 01 00 00 01 y 00 00
				 * 00 00 y 01 00 00 00
				 * 
				 * Mmmm voy a poner los del medio a FF's a ver que pasa
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
				newdata[0] = (byte)(0x01);
				/*
				 * Ojete, en partido de grupos esto vale 1!!
				 */
				System.arraycopy(mb.getData(), 0, newdata, 1,
						mb.getData().length);
				/*
				 * He probado con 0, con 1 con 2 con 3 y con FF no aparecen
				 * trajes ni nada por el estilo.
				 */

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000436A));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				MessageBlock m = new MessageBlockVoid(0x0000436B);
				m.setData(newdata);
				mes.addBlock(m);

				connections.sendEncodedToNIO(r.getPlayerIds(), mes);
				
//				r.setGamePhase(4);
//				r.setPlayersPhase(4);

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
						.log("RECEIVING GAME SETTINGS INFO ANSWERING JUST THE SAME: "); //$NON-NLS-1$
				byte[] data = mb.getData();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000436D));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				MessageBlock m = new MessageBlockVoid(0x0000436E);
				/*
				 * Aqui suele haber un 2, será el número de jugadores?
				 *
				 * ni cero ni 1 ni 3 hacen nada especial, luego vaya vd
				 * a saber los numeritos de los huevos del final que son 
				 * Nada cambia nada.
				 */
//				data[data.length-4] = (byte)0xFF; 
//				data[data.length-3] = (byte)0xFF; 
//				data[data.length-2] = (byte)0xFF; 
//				data[data.length-1] = (byte)0xFF; 

				m.setData(data);
				mes.addBlock(m);
				RoomInfo r = rooms.searchPlayer(player.getId());
				connections.sendEncodedToNIO(r.getPlayerIds(), mes);

				System.out.println( "Match settings: " + Util.toHex(data));
				
				r.setMatchSettings(data);
				//rooms.setRoom(r.getId(), r);

				/*
				 * Si estabamos en fase 4 debo cambiar el roomtype a 0x0400
				 */
				//if (r.getGamePhase() == 4) {
					r.setType(4);
					// r.setLastBytes(0x00020100);
					r.setLastBytes(0x00020100L);
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
				//}

			} else if (qId == 0x00003087) {
				out.log("BEFORE SAVING USER SETTINGS: "); //$NON-NLS-1$

//				byte[] data = mb.getData();
//				long pid = Util.word2Long(data[4], data[5], data[6], data[7]);
				player.resetSettings();
			} else if (qId == 0x00003088) {
				out.log("ME ENVIAN LA LISTA DE MENSAJETES: "); //$NON-NLS-1$
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
						.log("HAN TERMINADO DE ENVIARME LOS MENSAJES RESPONDO 308B con zeros"); //$NON-NLS-1$
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
					if( r.cacheClean() ) {
//						System.out.println( "Soy: " + player.getName() + " y no tenía cache hasta ahora!");
						r.saveCache();
					}
					//r = r.getCache();
					/*
					 * Ahora vamos con los puntos en si, se los actualizamos
					 * a todo dios, y si hay que hacerlo mas de una vez se hace.
					 */
					r.setReadyStatus(player.getId(),-1);
					r.setUseCacheData();
					
					long ids[] = r.getPlayerIdsOnSel( );
					for( int i = 0; i < ids.length; i++ ) {
						PlayerInfo p = r.getPlayerFromPid( ids[i] );
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
						/* 
						 * Hablamos en teoría del cache luego esto debe ser correcto
						 */
						if( r.getNumPlayersOnSel() == 2 ) {
							ids = r.getPlayerIdsOnSelExcept( player.getId() );
							PlayerInfo p2 = r.getPlayerFromPid( ids[0] );
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
					
					/*
					 * No puedo hacer esto porque no se pueden computar las categorías despues
					 */
					//r.resetAllSels();
					//
//					System.out.println( "voy a enviar el mensaje de los huev***s");
//					System.out.println( "num players on sel: " + r.getNumPlayersOnSel());
					mes = new Message();
					mes.addBlock(new MessageBlockGameEndInfo(r, 0x00004384));
					out.streamWriteEncodedNIO(mes);
					r.setUseRealData();
//					System.out.println( player.getName() + " HA LLEGADO A VER LOS RESULTADOS"); //$NON-NLS-1$
					//r.setPlayerSel(player.getId(), 0x00FF);
					//System.out.println( "MALDITA SEA DESELECCIONO, SOY: " +player.getName() + " Y AHORA SEL VALE " +
					//		r.getPlayerSelFromPid(player.getId()));
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
				out.log("READ KEEP ALIVE, ANSWER YES YOU ARE ALIVE:"); //$NON-NLS-1$
				mes = new Message();
				mes.addBlock(new MessageBlockVoid(0x00000005));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000437B) {
				out
						.log("READ 437B, SOMEONE LAGGED OUT OF THE MATCH (MAYBE NOT LEAVING THE SERVER)"); //$NON-NLS-1$
				RoomInfo r = rooms.searchPlayer(player.getId());
				if (r != null) {
					out.log( "SOMEONE LAGGED OUT AND ITS NOT ME: " + player.getName() ); //$NON-NLS-1$
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
						r.setPlayerFell(player.getId(), true );
						/*
						 * Informamos por privado de los KA times del resto
						 * 
						 */
						
//						long ids[] = r.getPlayerIdsExcept(player.getId());
//						long now = new Date().getTime();
//						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//						String time = sdf.format(now);
//						for( int i = 0; i < ids.length; i++ ) {
//							PlayerInfo p = players.getPlayerInfo(ids[i]);
//							if( p == null ) continue;
//							mes = new Message();
//							long secondsElapsed = ((now - p.getConnection().getLastKATime())/1000);
//							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),  
//									(byte)0x00, (byte)0x02, p.getName() + "["+time+"] last alive time: " + 
//									secondsElapsed + " secs." + (secondsElapsed > 60 ? " Probably legal lag" : " Probably still online"),
//									0x00004402));
//							out.streamWriteEncodedNIO(mes);
//							//connections.sendEncodedToNIO(r.getPlayerIdsExcept(player.getId()), mes);
//						}
						/*
						 * Si no había cache cacheo
						 */
						if( r.cacheClean() && r.getTimestampLagged() == 0 && r.getNumPlayersOnSel() > 1) {
							r.saveCache();
						}
						if( !r.cacheClean() ) {
							r.setUseCacheData();
							if ( r.getTimestampLagged() == 0 && r.allPlayersOnSelOnline() ) r.setTimestamplagged(new Date().getTime());
							//r.setUseCacheData();
							if( r.getPinCode(player.getId()) == null && r.allPlayersOnSelOnline() ) {
//								System.out.println( "Sending PINCODE TO: " + player.getName());
								long now = new Date().getTime();
								SimpleDateFormat sdf = new SimpleDateFormat("HH:mm"); //$NON-NLS-1$
								String time = sdf.format(now);
								String pinCode = r.generatePinCode(player.getId());
								mes = new Message();
								mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
								(byte)0x00, (byte)0x02, Messages.getString(player.getLang(), "PES6JGameServer.148"), //$NON-NLS-1$
								true, 0x00004402));
								mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
								(byte)0x00, (byte)0x02, Messages.getString(player.getLang(), "PES6JGameServer.150") + pinCode, //$NON-NLS-1$
								true, 0x00004402));
								mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
										(byte)0x00, (byte)0x02, Messages.getString(player.getLang(), "PES6JGameServer.152") + pinCode +Messages.getString(player.getLang(), "PES6JGameServer.153"), //$NON-NLS-1$ //$NON-NLS-2$
										true, 0x00004402));
								out.streamWriteEncodedNIO(mes);
							}
							r.setUseRealData();
						}
						
						/*
						 * LUEGO LLEGA UN 436F con 0 que debe deseleccionar al jugador, no debo hacerlo
						 * aqui pero igual me dejan tieso...
						 */
						r.setPlayerSel(player.getId(), 0x00FF );
						
						if( r.allPlayersFell()) {
//							System.out.println( "RESETEO LA SALA MENOS EL CACHE SE HAN CAIDO LOS DOS!");
							r.setGamePhase(0x01);
							r.setType(1);
							r.resetAllButCache();
						}
//							long pids[] = r.getPlayerIdsOnSel();
//							for( int i = 0; i < pids.length; i++ ) {
//								PlayerInfo p = players.getPlayerInfo(pids[i]);
//								mes = new Message();
//								mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
//								(byte)0x00, (byte)0x02, Messages.getString(p.getLang(), "PES6JGameServer.188"), //$NON-NLS-1$
//								true, 0x00004402));
//								p.getConnection().streamWriteEncodedNIO(mes);
//							}
//							r.setGamePhase(0x01);
//							r.setType(1);
//							r.resetAll();
//						}
					} else {
//						System.out.println( "NO ESTABAN EN FASE 7 RESETEO LA SALA MENOS EL CACHE!");
						r.setGamePhase(0x01);
						r.setType(1);
						r.resetAllButCache();
						//r.setType(1);
					}
					//r.setType(1);
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
					out.log( "THE ROOM DID NOT EXIST, RETURNING"); //$NON-NLS-1$
					return;
				}

			} else if (qId == 0x00004377) {
				out.log("NEW QUERY, 4377 ON WHICH HALF ARE WE?????"); //$NON-NLS-1$
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
				out.log("RECEIVE 4385 TIME ELAPSES AS HE SAYS!!!"); //$NON-NLS-1$
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
				out.log("RECEIVE 4375 GOAL CLIENT QUERY!!!!!!!"); //$NON-NLS-1$
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
				out.log( "READ 5047 CANCELED INVITATION TO GROUP?"); //$NON-NLS-1$
//				mes = new Message();
//				mes.addBlock(new MessageBlockZero(0x00005048));
//				out.streamWriteEncodedNIO(mes);
				
				mes = new Message();
				mes.addBlock(new MessageBlockLong(0xfffffd00 + 245, 0x00005048));
				out.streamWriteEncodedNIO(mes);
				
				out.log( "Pero yo quien soy?: " + player.getName());
				
				
				/*
				 * Esto produce unicamente el efecto de cerrarle al otro tio
				 * la ventana de invitación al grupo
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockLong(0xfffffd00 + 245, 0x00005045));
				connections.searchConnection(out.getRecipient()).streamWriteEncodedNIO(mes);
				
//				mes = new Message();
//				mes.addBlock(new MessageBlockLong(0xfffffd + 245, 0x00005048));
//				connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
			} else if( qId == 0x00005043 ) {
				/*
				 * Respuesta a invitación de grupo
				 */
				
				byte ret = mb.getData()[0];
				out.log( "READ 5043, THE USER CHOOSES TO ACCEPT OR DECLINE GROUP INVITATION: " + ret + " requester: " + out.getRequester() ); //$NON-NLS-1$ //$NON-NLS-2$
				out.log( "RET VALE: " + ret );
				long reqId = out.getRequester();
				PlayerInfo pg = players.getPlayerInfo(reqId);
				if( pg != null && pg.getGid() != 0 ) { 
				
					if( ret == (byte)0x01 ) {
						/*
						 * Ha aceptado a unirse, le metemos en el grupo
						 */
						//pg.getGroupInfo().addPlayer(player);
						
						try {
//							Tools.dbAddPlayerToGroup(db, pg.getGroupInfo().getId(), player.getId());
							
							if(!Tools.dbGetGroupFull(db, pg.getGid())) {
								Tools.dbUpdatePlayerGidAndInvitation(db, player.getId(), pg.getGid(), 1);
								player.setGid(pg.getGid());
								player.setGroupName(pg.getGroupName());
								player.setInvitation(1);
								
								mes = new Message();
								MessageBlock mb1 = new MessageBlockZero( 0x00005044 );
								//mb1.setData(new byte[] { (byte)0x00, (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 });
//								byte[] gid = Util.long2Word(pg.getGid());
//								mb1.setData(new byte[] { (byte)ret, (byte)gid[0],(byte)gid[1],(byte)gid[2],(byte)gid[3] });
								mes.addBlock(mb1);
								out.streamWriteEncodedNIO(mes);
			
								mes = new Message();
								mes.addBlock(new MessageBlockByte( (byte)0x00, 0x00005046));
								connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
	
								sendPlayerList( channel );
							} else {
								/* Error, el grupo está lleno! */
//								mes = new Message();
//								
////								mb1.setData(new byte[] { (byte)gid[0],(byte)gid[1],(byte)gid[2],(byte)gid[3],(byte)0x00 });
//								for( int i = 0; i < 255; i++ ) {
//									MessageBlock mb1 = new MessageBlockLong(0xfffffd + i, 0x00005045);
//									mes.addBlock(mb1);
//									MessageBlock mb2 = new MessageBlockLong(0xfffffd + i, 0x00005046);
//									mes.addBlock(mb2);
//								}
//								out.streamWriteEncodedNIO(mes);
			
								/*
								 * A mi un 5044
								 */
								mes = new Message();
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x00005044));
								out.streamWriteEncodedNIO(mes);
								
								mes = new Message();
								//mes.addBlock(new MessageBlockByte( (byte)0xFF, 0x00005046));
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x00005046));
								connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
							}
							
							//Tools.dbSavePlayerInfo(db, player);
						} catch( SQLException ex ) {
							out.log( "Unable to add player to the group"); //$NON-NLS-1$
							out.log( ex.getMessage() );
						}
						

					}
					if( ret == (byte)0x00 ) {
						/*
						 * Tanto el 5046 con algo que no sea un cero como el 5045 producen el
						 * mismo efecto en el cliente: "Como el rival no pudo responder.... etc"
						 */
						
						/*
						 * A mi un 5044
						 */
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00005044));
						out.streamWriteEncodedNIO(mes);
						
						mes = new Message();
						mes.addBlock(new MessageBlockLong(0xfffffd00 + 245, 0x00005045));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
//					if( ret == (byte)0x02 ) {
//						mes = new Message();
//						mes.addBlock(new MessageBlockByte(ret, 0x00005045));
//						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
//					}
				} else {
					out.log( "The player which offered group membership to me, no longer exists (or has no group at all)"); //$NON-NLS-1$
				}
				

				
			} else if (qId == 0x00005040) {
				/*
				 * Invitar a alguien al grupo!
				 */
				out.log( "Received 5040 Invite to group"); //$NON-NLS-1$
				long plid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				try {
					if(!Tools.dbGetGroupFull(db, player.getGid())) {
						mes = new Message();
						mes.addBlock(new MessageBlockLong(player.getId(), 0x00005042));
						connections.searchConnection(plid).setRequester(player.getId());
						out.setRecipient(plid);
						connections.searchConnection(plid).streamWriteEncodedNIO(mes);
						
						mes = new Message();
						mes.addBlock(new MessageBlockZero( 0x00005041));
						out.streamWriteEncodedNIO(mes);
					} else {
						/*
						 * Un 52 no dice nada!!!!
						 * 245 Cancelado!!!
						 * 
						 */
//						if( out.getDebugErrorCode() != -1 ) {
//							mes = new Message();
//							mes.addBlock(new MessageBlockLong( 0xfffffd00 + out.getDebugErrorCode(), 0x00005041));
//							out.streamWriteEncodedNIO(mes);
//							sendRoomMessageToPlayer( player, 1, 8, "Sent " + out.getDebugErrorCode() + " ERROR." );
//							out.setDebugErrorCode(out.getDebugErrorCode()+1);
//						} else {
//							mes = new Message();
//							mes.addBlock(new MessageBlockLong( 0xffffffff, 0x00005041));
//							out.streamWriteEncodedNIO(mes);
//						}
					
						mes = new Message();
						mes.addBlock(new MessageBlockLong( 0xFFFFFD00 + 245, 0x00005041));
						out.streamWriteEncodedNIO(mes);
					}
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong( 0xFFFFFFFF, 0x00005041));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x000050F0) {
				/*
				 * Crear un grupo, no tengo logs voy a responder que 50F1 con
				 * zeros OJO es excesivamente largo el mensaje para enviar solo
				 * en nombre, sin embargo en la prueba realizada todo lo demas
				 * que viene son cerotes
				 */
				String grName = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				out.log("RECEIVE 50F0 CREATE GROUP CLIENT QUERY!!! NAME: " //$NON-NLS-1$
						+ grName);

				try {
					long gid = Tools.dbCreateGroup(db, player, grName);
					player.setGid( gid );
					player.setGroupName( grName );
					player.setInvitation( 3 );
					
//					System.out.println( "Despues de crear ahora mi gid es: " + player.getGroupInfo().getId());
//					System.out.println( "Despues de crear ahora mi gid name es: " + player.getGroupInfo().getName());
//					logger.log("GROUP CREATED AND NO EXCEPTION!");
					mes = new Message();
					mes.addBlock(new MessageBlockZero(0x000050F1));
					out.streamWriteEncodedNIO(mes);
					/*
					 * Es impepinable actualizar la lista de jugadores
					 */
					//connections.sendToChannelNotInGameEncodedNIO( channel, mes );
					sendPlayerList( channel );
				} catch (SQLException ex) {
					ex.printStackTrace();
					/*
					 * Enviar esto no parece funcionar para generar un error, no
					 * enviar nada a ojos del cliente tambien es como si lo
					 * hubiera creado perfectamente
					 */
					mes = new Message();
					
//					answer = 0xfffffefc; // Ya existe un usuario con ese nombre
//					mes.addBlock(new MessageBlockLong(answer, 0x00003022));
					
					//mes.addBlock(new MessageBlockLong(0xFFFFFFEBL, 0x000050F1));
					mes.addBlock(new MessageBlockLong(0xfffffd42L, 0x000050F1));
					//mes.addBlock(new MessageBlockLong(0xfffffefcL, 0x000050F1));
					
					out.streamWriteEncodedNIO(mes);
				}

			} else if (qId == 0x00005024) {

				/*
				 * Pregunta sobre el grupo, de momento desconozco a que se
				 * refiere, envia el identificador del grupo
				 */
				long grid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);

				out.log("RECEIVE 5024 INFO GROUP CLIENT QUERY!!! GROUP ID: " //$NON-NLS-1$
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
					mes = new Message();
					mes
							.addBlock(new MessageBlockLong(0xFFFFFFFF,
									0x00005025));
					out.streamWriteEncodedNIO(mes);
				}

			} else if (qId == 0x00005084) {
				/*
				 * Dejar un mensaje en el tablon de un grupo
				 */
				long grid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				String msg = new String(mb.getData(), 4, Util.strlen(mb
						.getData(), 4));

				out.log("RECEIVE 5084 SEND MESSAGE TO GROUP!!!"); //$NON-NLS-1$
//				FileOutputStream fout = new FileOutputStream("5084_mio.dec"); //$NON-NLS-1$
//				fout.write(mb.getBytes());
//				fout.close();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005085));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x0000508E) {
				/*
				 * Lista de mensajes de prohibicion
				 */

				out.log("RECEIVE 508E GROUP BAN MESSAGE LIST!!!"); //$NON-NLS-1$

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000508F));
				out.streamWriteEncodedNIO(mes);

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005091));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00005060) {

				out.log("RECEIVE 5060 GROUP MEMBERS: "); //$NON-NLS-1$

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005061));
				out.streamWriteEncodedNIO(mes);

				try {
					PlayerInfo[] pl = Tools.dbLoadGroupMembers(db, player.getGid());
					mes = new Message();
					mes.addBlock(new MessageBlockGameGroupMembers(pl,
							0x00005062));
					out.streamWriteEncodedNIO(mes);
				} catch (SQLException ex) {
					ex.printStackTrace();
				}

				
//				FileOutputStream fo = new FileOutputStream("5062_mio.dec");
//				fo.write(mes.getBytes());
//				fo.close();

				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00005063));
				out.streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004580) {

				out.log("RECEIVE 4580 PLAYER PUSHED THE FRIENDS BUTTON: "); //$NON-NLS-1$
				
				/*
				 * Inventando!
				 */
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004581));

				try {
					PlayerInfo[] pl = Tools.dbLoadPlayerFriends(db, out.getPlayerInfo().getId());

					if( pl != null ) {
						mes.addBlock(new MessageBlockFriendsList( pl, 0x00004582
						 ));
					}
				} catch( SQLException ex ) {
					
				}
				// out.streamWriteEncodedNIO(mes);
				mes.addBlock(new MessageBlockVoid(0x00004583));
				out.streamWriteEncodedNIO(mes);
//				sendFriendList(out, out.getPlayerInfo().getId());

			} else if( qId == 0x00004510) {
				/*
				 * 
				 */
				out.log( "REAL 4510 REMOVE FRIEND");
				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				
				try {
					Tools.dbRemovePlayerFriend( db, player.getId(), pid );
					Tools.dbRemovePlayerFriend( db, pid, player.getId() );
					mes = new Message();
					mes.addBlock( new MessageBlockZero( 0x00004512));
					out.streamWriteEncodedNIO(mes);
					sendFriendList(out, player.getId());
					PESConnection c;
					if( (c = connections.searchConnection(pid)) != null ) {
						sendFriendList( c, pid );
					}
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock( new MessageBlockLong( 0xFFFFFFFF, 0x00004512));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x00004520) {
				/*
				 * Bloquear charla a usuario
				 */
				out.log( "READ 4520 Block chat from player");
				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				try {
					if( Tools.dbGetBlockListFull(db, player.getId())) {
						/*
						 * Lista de bloqueo llena!!!
						 */
						mes = new Message();
						mes.addBlock( new MessageBlockLong( 0xFFFFFFFF, 0x00004522));
						out.streamWriteEncodedNIO(mes);
					} else {
						Tools.dbInsertPlayerFriend(db, player.getId(), pid, 1);
						mes = new Message();
						mes.addBlock( new MessageBlockLong( 0x00000000, 0x00004521));
						out.streamWriteEncodedNIO(mes);
						sendFriendList(out, player.getId());
					}
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock( new MessageBlockLong( 0xFFFFFFFF, 0x00004522));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x00004522 ) {
				/*
				 * Eliminar de la lista de bloqueo
				 */
				out.log( "READ 0x00004522 Remove a player from block list");
				DataId did = new DataId(mb.getData());
				long pid = did.getId();
				try {
					Tools.dbRemovePlayerFriend(db, player.getId(), pid);
					mes = new Message();
					mes.addBlock( new MessageBlockLong( 0x00000000, 0x00004523));
					out.streamWriteEncodedNIO(mes);
					sendFriendList(out, player.getId());
				} catch( SQLException ex ) {
					mes = new Message();
					mes.addBlock( new MessageBlockLong( 0xFFFFFFFF, 0x00004524));
					out.streamWriteEncodedNIO(mes);
				}
			} else if (qId == 0x0000450B) {
				/*
				 * Cancela la invitación de amigo
				 */
				out.log( "READ 450B CANCELED FRIEND INVITE?"); //$NON-NLS-1$
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x0000450C));
				out.streamWriteEncodedNIO(mes);
				
				/*
				 * Esto produce unicamente el efecto de cerrarle al otro tio
				 * la ventana de invitación al grupo
				 */
				/*
				 * Será un A???, probando... un 09 no funciona.
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockLong(0xfffffd00 + 245, 0x0000450A));
				connections.searchConnection(out.getRecipient()).streamWriteEncodedNIO(mes);
			} else if (qId == 0x00004780 ) {
				out.log( "READ 4780 MESSAGES INBOX!");
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004781));
				mes.addBlock(new MessageBlockZero(0x00004783));
				out.streamWriteEncodedNIO(mes);
			} else if( qId == 0x00004508) {
				out.log( "READ 4508 USER ACCEPTS OR DENIES TO ADD FRIEND" );
				/*
				 * Respuesta a invitación de grupo
				 */
				
				byte ret = mb.getData()[0];
				long reqId = out.getRequester();
				PlayerInfo pg = players.getPlayerInfo(reqId);
				if( pg != null ) { 
				
					if( ret == (byte)0x01 ) {
						/*
						 * Ha aceptado a unirse, le metemos en el grupo
						 */
						//pg.getGroupInfo().addPlayer(player);
						
						try {		
							if( Tools.dbGetFriendListFull(db, player.getId() )) {
								/*
								 * Error mi lista esta llena
								 */
								/*
								 * A mi un 4509
								 */
								mes = new Message();
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x00004509));
								out.streamWriteEncodedNIO(mes);
								
								mes = new Message();
								//mes.addBlock(new MessageBlockByte( (byte)0xFF, 0x00005046));
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x0000450A));
								connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
							} else
							if( Tools.dbGetFriendListFull(db, pg.getId() )) {
								/*
								 * Error, su lista esta llena
								 */
								mes = new Message();
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x00004509));
								out.streamWriteEncodedNIO(mes);
								
								mes = new Message();
								//mes.addBlock(new MessageBlockByte( (byte)0xFF, 0x00005046));
								mes.addBlock(new MessageBlockLong(0xfffffd00 + 166, 0x0000450A));
								connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
							} else {
								try {
									Tools.dbInsertPlayerFriend(db, player.getId(), pg.getId(), 0);
									Tools.dbInsertPlayerFriend(db, pg.getId(), player.getId(), 0);
									
									mes = new Message();
									MessageBlock mb1 = new MessageBlockZero( 0x00004509 );
									mes.addBlock(mb1);
									out.streamWriteEncodedNIO(mes);

									/*
									 * Esto devuelve que el tipo no pudo responder, lo cual es
									 * incorrecto lo hace con 450A
									 * 
									 * 4509 4508 450B y 450C no producen ningún efecto.
									 */
//									mes = new Message();
//									mes.addBlock(new MessageBlockZero( 0x0000450A));
//									connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
									mes = new Message();
									mes.addBlock(new MessageBlockZero( 0x00004506));
									connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
									
									sendFriendList(out, out.getPid());
									sendFriendList(pg.getConnection(), pg.getId());
		
								} catch( SQLException ex ) {
									/*
									 * Error, no se pudo :/
									 */
									mes = new Message();
									MessageBlock mb1 = new MessageBlockLong( 0xFFFFFFFF, 0x0000450A );
									mes.addBlock(mb1);
									out.streamWriteEncodedNIO(mes);
				
									mes = new Message();
									mes.addBlock(new MessageBlockLong( 0xFFFFFFFF, 0x0000450A));
									connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
								}
	
								/*
								 * Enviar la lista de amigos aqui!.
								 */
								//sendPlayerList( channel );
							} 
							
							//Tools.dbSavePlayerInfo(db, player);
						} catch( SQLException ex ) {
							out.log( "Unable to add player to friends list"); //$NON-NLS-1$
							out.log( ex.getMessage() );
						}
						

					}
					if( ret == (byte)0x00 ) {
						/*
						 * Tanto el 5046 con algo que no sea un cero como el 5045 producen el
						 * mismo efecto en el cliente: "Como el rival no pudo responder.... etc"
						 */
						
						/*
						 * A mi un 5044
						 */
						mes = new Message();
						mes.addBlock(new MessageBlockZero(0x00004509));
						out.streamWriteEncodedNIO(mes);
						
						mes = new Message();
						mes.addBlock(new MessageBlockLong(0xfffffd00 + 245, 0x0000450A));
						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
					}
//					if( ret == (byte)0x02 ) {
//						mes = new Message();
//						mes.addBlock(new MessageBlockByte(ret, 0x00005045));
//						connections.searchConnection(reqId).streamWriteEncodedNIO(mes);
//					}
				} else {
					out.log( "The player which offered friendship to me, no longer exists"); //$NON-NLS-1$
				}
			} else if (qId == 0x00004504) {

				long plid = Util.word2Long(mb.getData()[0], mb.getData()[1], mb
						.getData()[2], mb.getData()[3]);
				out.log("RECEIVE 4504 ADD FRIEND! FRIEND ID: " + plid); //$NON-NLS-1$
				
			
				connections.searchConnection(plid).setRequester(player.getId());
				out.setRecipient(plid);
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero( 0x00004505));
				out.streamWriteEncodedNIO(mes);
				
				mes = new Message();
				mes.addBlock(new MessageBlockLong(player.getId(), 0x00004507));
				connections.searchConnection(plid).streamWriteEncodedNIO(mes);

				/*
				 * Si no puedo, le envio esto
				 */
//				mes = new Message();
//				mes.addBlock(new MessageBlockLong( 0xfffffdf3, 0x00004506));
//				out.streamWriteEncodedNIO(mes);

			} else if (qId == 0x00000003) {
				/*
				 * Adios muy buenas que me vooooy!!! mide 1 y me viene un 0x01
				 */
				/*
				 * Mal hecho pero para arreglar lo del pedete de momento sirve
				 * forzamos la desconexion
				 */
				out.log("RECEIVE 00 03 disconnect!!! "); //$NON-NLS-1$
				out.logLastSentMessage("***Dump of last sent message***"); //$NON-NLS-1$
				throw new IOException("Forced shutdown of client!"); //$NON-NLS-1$
			} else if (qId == 0x000050B0) {
				/*
				 * Retirada del grupo, viene con un motivo
				 */
				String reason = new String(mb.getData(), 0, Util.strlen(mb
						.getData(), 0));
				out.log("RECEIVE 50 B0 Group disband!!!: " + reason); //$NON-NLS-1$

				/*
				 * Me invento la respuesta quiza sea esto
				 */
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x000050B1));
				out.streamWriteEncodedNIO(mes);
				try {
					int numPlayers = Tools.dbRemovePlayerFromGroup(db, player.getId(), player.getGid());
					player.setGid(0);
					player.setGroupName("");
					player.setInvitation(0);
					/*
					 * Hay que actualizar la lista de jugadores por que este tio
					 * ya no es de ningun grupo
					 */
					sendPlayerList( channel );
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			} else if (qId == 0x00004600) {
				/*
				 * Busqueda de un rival, viene con un nombre de jugador y un
				 * tipo de busqueda, por defecto coincidencia parcial
				 * 
				 * Primer byte indica el tipo de busqueda. 0x01 = Coincidencia
				 * parcial 0x02 = exacta
				 */
				byte queryType = mb.getData()[0];
				String queryText = new String(mb.getData(), 1, Util.strlen(mb
						.getData(), 1));
				out.log("RECEIVE 46 00 Player search!!!: " + queryText); //$NON-NLS-1$
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004601));
				out.streamWriteEncodedNIO(mes);
				
				try {
					GroupInfo[] groups = Tools.dbSearchGroups(db, queryType, queryText);

					if (groups != null && groups.length > 0) {
						mes = new Message();
						mes.addBlock(new MessageBlockGameGroupList(groups,
								0x00004602));
						out.streamWriteEncodedNIO(mes);
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				
				mes = new Message();
				mes.addBlock(new MessageBlockZero(0x00004603));
				out.streamWriteEncodedNIO(mes);
				
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
				out.log("RECEIVE 50 20 Group search!!!: " + queryText); //$NON-NLS-1$

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
				out.log("UNKNOWN QUERY!!!!! ************************"); //$NON-NLS-1$
			}
		}
		
		public void sendFriendList( PESConnection out, long pid ) throws IOException {
			Message mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00003082));

			try {
				PlayerInfo[] pl = Tools.dbLoadPlayerFriends(db, pid);
				/*
				 * Lo de abajo es perfectamente correcto, pero como no podemos
				 * a�adir amigos de momento enviamos una lista vacia
				 */
				if( pl != null ) {
					 mes.addBlock(new MessageBlockMenuFriends( pl, 0x00003084
					 ));
				}
			} catch( SQLException ex ) {
				
			}
			// out.streamWriteEncodedNIO(mes);
			mes.addBlock(new MessageBlockVoid(0x00003086));
			out.streamWriteEncodedNIO(mes);
		}
		
		public void sendRoomList( ChannelInfo channel ) throws IOException {
			Message mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004301));
			// Cambiamos de not in game a not in room EXPERIMENTAL
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
			// Cambiamos de not in game a not in room EXPERIMENTAL
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );

			mes = new Message();
			mes.addBlock(new MessageBlockZero(0x00004303));
			// Cambiamos de not in game a not in room EXPERIMENTAL
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
		}
		
		public void sendRoomUpdate( ChannelInfo channel, RoomInfo r ) throws IOException {
			Message mes = new Message();

			mes.addBlock(new MessageBlockGameRoomList(new RoomInfo[] { r }, 0x00004306));
			connections.sendToChannelNotInGameEncodedNIO( channel, mes );
		}

		public void createRoom( PlayerInfo player, ChannelInfo channel, String name,
				boolean passworded, String password ) throws IOException {
			RoomInfo r = new RoomInfo(roomIdx++, channel, 1, passworded,
					password, name, player );
			RoomList rooms = channel.getRoomList();
			PESConnection out = player.getConnection();
			rooms.addRoom(r);
			Message mes = new Message();
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
		}
		
		public void enterRoom( PlayerInfo player, RoomList rooms, PlayerList players, long rid, String password ) throws IOException {

			RoomInfo r = rooms.getRoom(rid);
			PESConnection out = player.getConnection();
			Message mes;
			
			if( r == null ) {
				out.log( "THE ROOM NO LONGER EXISTS, RETURNING"); //$NON-NLS-1$
				return;
			}
			
			if (r.getNumPlayers() == 4) {
				mes = new Message();
				mes.addBlock(new MessageBlockGameEnterRoomFailFull(
						0x00004321));
				out.streamWriteEncodedNIO(mes);
			} else

			if (r.hasPassword()
					&& !r.getPassword().equals(password)) {
				mes = new Message();
				mes.addBlock(new MessageBlockGameEnterRoomFailPass(
						0x00004321));
				out.streamWriteEncodedNIO(mes);

			} else {

				r.addPlayer( player );

				/*
				 * NO ES NECESARIO ENVIAR NADA DE ESTO, COMPROBADO A 31 - 05 - 2009
				 */
//				mes = new Message();
//
//				mes
//						.addBlock(new MessageBlockGameRoomList(
//								new RoomInfo[] { r }, 0x00004306));
				//out.streamWriteEncodedNIO(mes);
				/*
				 * Prueba, voy a enviar la información de la sala a tods
				 * la sala
				 */
				//connections.sendEncodedToNIO( r.getPlayerIds(), mes );
				
				for (int i = 0; i < r.getNumPlayers(); i++) {
					PlayerInfo p = players.getPlayerInfo(r.getPlayerId(i));
					if( p != null ) {
						mes = new Message();
						mes.addBlock(new MessageBlockMenuPlayerGroup(p
								, r.getId(),
								0x00004222));
						out.streamWriteEncodedNIO(mes);
						//connections.sendEncodedToNIO( r.getPlayerIds(), mes );
					} else {
						out.log( "No existe el jugador " + i + " en la sala --> p = null"); //$NON-NLS-1$ //$NON-NLS-2$
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
						out.log( "Bugged room, some player doesn't exist here, we will force a disconnect here!" ); //$NON-NLS-1$
						throw new IOException( "Trying to join a bugged room, disconnecting"); //$NON-NLS-1$
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
//				if ((data = r.getMatchSettings()) != null) {
//					mes = new Message();
//					MessageBlock m = new MessageBlockVoid(0x0000436E);
//					m.setData(data);
//					mes.addBlock(m);
//					out.streamWriteEncodedNIO(mes);
//				}
//				sendRoomList( channel );
			}
		}
		
		public void sendPlayerList( ChannelInfo channel ) throws IOException {
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
			if( r.getChannel().getType() == ChannelInfo.TYPE_COMMON_FRIENDLY ) return;
			
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
			if( r.getChannel().getType() == ChannelInfo.TYPE_COMMON_FRIENDLY) return;
			/*
			 * The same client can call this function twice when someone lagged out
			 * that would count as 2 matches, thats not acceptable.
			 */
			if( player.getOldPoints() != player.getPoints() ) return;
			
//			System.out.println( "ACTUALIZO LOS PUNTOS A " + player.getName() + " porque sus anteriores son diferentes."); //$NON-NLS-1$ //$NON-NLS-2$
//			System.out.println( "EQUIPOS ERAN: " + r.getTeam1() +" - " + r.getTeam2()); //$NON-NLS-1$ //$NON-NLS-2$
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
			
			/*
			 * Es posible que sea un jugador cacheado, si existe un original con el
			 * mismo pid lo uso
			 */
			return;
		}
		
		public void processLaggedGame( RoomInfo r ) throws IOException {
			if( r.getTimestampLagged() != 0  ) {
				r.setUseCacheData();
				long[] pids = r.getPlayerIdsOnSel();
				
//				System.out.println( "en sel tengo: " + pids.length); //$NON-NLS-1$
				
				if( r.teamSentMessage(0) && r.teamSentMessage(1)) {
					/*
					 * Enviar mensaje todos respondieron, el partido no cuenta
					 */
					for( int i = 0; i < pids.length; i++ ) {
						PESConnection c = r.getPlayerFromPid(pids[i]).getConnection();
						if( c!= null ) {
							Message mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
							(byte)0x00, (byte)0x02, Messages.getString(c.getPlayerInfo().getLang(), "PES6JGameServer.186"), //$NON-NLS-1$
							true, 0x00004402));
							c.streamWriteEncodedNIO(mes);
						} 
					}
				} else 
				if( !r.teamSentMessage(0) && !r.teamSentMessage(1) ) {
					for( int i = 0; i < pids.length; i++ ) {

						PESConnection c = r.getPlayerFromPid(pids[i]).getConnection();
						if( c!= null ) {
							Message mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
							(byte)0x00, (byte)0x02, Messages.getString(c.getPlayerInfo().getLang(), "PES6JGameServer.188"), //$NON-NLS-1$
							true, 0x00004402));
							c.streamWriteEncodedNIO(mes);
						}
					}
				} else { 
					r.backupGoals();

					if( r.teamSentMessage(0) ) {
						/*
						 * Ganador el equipo 0 (que es el 2) por 3 cero
						 */
						
						r.setTeam1Goals( 0 );
						r.setTeam2Goals( 3 );
					} else if (r.teamSentMessage(1)){
						r.setTeam1Goals( 3 );
						r.setTeam2Goals( 0 );
					} 
					for( int k = 0; k < pids.length; k++ ) {
						PlayerInfo p = r.getPlayerFromPid(pids[k]);

						if( p != null ) {
							updateProfileAndPoints(p, r);
							Message mes = new Message();
							String message;
							if( r.getWinner() == r.getPlayerTeam(p.getId()) )
								message = Messages.getString(p.getLang(), "PES6JGameServer.189"); //$NON-NLS-1$
							else
								message = Messages.getString(p.getLang(), "PES6JGameServer.190"); //$NON-NLS-1$
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
							(byte)0x00, (byte)0x02, message,
							true, 0x00004402));
							if( p.getConnection() != null ) p.getConnection().streamWriteEncodedNIO(mes);
						}

					}
					if( pids.length == 2 ) {
						PlayerInfo p1 = r.getPlayerFromPid(pids[0]);
						PlayerInfo p2 = r.getPlayerFromPid(pids[1]);
						if( p1 != null && p2 != null ) {
							updateCategory(p1, p2, r);
							/*
							 * Enviar mensaje categoría
							 */
							Message mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
							(byte)0x00, (byte)0x02, Messages.getString(p1.getLang(), "PES6JGameServer.193") + p1.getOldCategory() +  //$NON-NLS-1$
							Messages.getString(p1.getLang(), "PES6JGameServer.1") + p1.getCategory(), //$NON-NLS-1$
							true, 0x00004402));
							if( p1.getConnection() != null ) p1.getConnection().streamWriteEncodedNIO(mes);
							mes = new Message();
							mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo( Long.MAX_VALUE, "System"),   //$NON-NLS-1$
							(byte)0x00, (byte)0x02, Messages.getString(p2.getLang(), "PES6JGameServer.196") + p2.getOldCategory() +  //$NON-NLS-1$
							Messages.getString(p2.getLang(), "PES6JGameServer.197") + p2.getCategory(), //$NON-NLS-1$
							true, 0x00004402));
							if( p2.getConnection() != null ) p2.getConnection().streamWriteEncodedNIO(mes);
						}

					}
					r.restoreGoals();
				}
				r.setUseRealData();
				// OJO CON ESTO!
				//r.resetCache();
				r.setTimestamplagged(0);
				//sendRoomList(r.getChannel());
				sendPlayerList(r.getChannel());
			}
		}
		
		public void processLaggedGames() throws IOException {
			/*
			 * Comprobamos si hay juegos lageados pendientes de comprobar sus pincodes
			 *
			 */
			long now = (new Date()).getTime();
			
			for( int i = 0; i < channels.size(); i++ ) {
				RoomList rl = channels.getChannel(i).getRoomList();
				RoomInfo[] chanRooms = rl.getRoomInfoArray();
				if( chanRooms != null ) {
					for( int j = 0; j < chanRooms.length; j++ ) {
						RoomInfo r = chanRooms[j];
						//System.out.println( "el timestamp de la sala es: " + r.getTimestampLagged());
						if( r.getTimestampLagged() != 0 && 
								r.getTimestampLagged() + RoomInfo.TIME_TO_SEND_PIN_CODES <= now ) {
							processLaggedGame(r);
						}
					}
				}
			}
		}
		
		public void sendRoomMessageToPlayer( PlayerInfo player, int place, int channel, String message ) throws IOException {
			Message mes;
			mes = new Message();
			mes.addBlock(new MessageBlockGameChatMessage(new PlayerInfo(Long.MAX_VALUE, "System"),   //$NON-NLS-1$
					(byte)place, (byte)channel, message,
					true, 0x00004402));
			player.getConnection().streamWriteEncodedNIO(mes);
			return;
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
					players.removePlayer(player.getId());
					/*
					 * Intentamos grabar la info del jugador
					 */
					
					con
							.log("I WAS LOGGED IN, REMOVING MYSELF FROM EVERYWHERE!"); //$NON-NLS-1$
					//players.removePlayer(player.getId());

					RoomInfo r = rooms.searchPlayer(player.getId());
					RoomInfo rCache = rooms.searchCachedPlayer(player.getId());

					boolean punished = false;
					
					if (r != null) {
						if (rCache != null ) {
						

							/*
							 * If the player was in a started match (on phase 7)
							 * this is a deco, and we have to update his profile
							 */
							if( !shuttingDown ) {
								if( rCache.getTimestampLagged() != 0 &&
									!rCache.getCodeSent(player.getId()) &&
									rCache.getPlayerSelFromPid( player.getId() ) != 0x00FF ) {
									player.setDecos( player.getDecos() + 1 );
									
									/*
									 * Es posible que aún no haya cache, si es un ALT-F4 al otro
									 * jugador aún no le llegó el mensaje de LAG
									 */
									if( rCache.cacheClean() ) rCache.saveCache();
									rCache.setUseCacheData();
									int myteam = rCache.getPlayerTeam(player.getId());
									rCache.setTeamCodeSent(myteam, false);
									rCache.setTeamCodeSent(myteam == 1 ? 0 : 1, true);
									processLaggedGame(rCache);
									rCache.setUseRealData();
									punished = true;
								} 
							}
						} 
						/*
						 * If the player was in a started match (on phase 7)
						 * this is a deco, and we have to update his profile
						 */
						if( !shuttingDown ) {
							
							if( r.getGamePhase() >= 6 /*&& r.getType() == 7*/) {
								/*
								 * Only if he was playing!
								 */
								if( r.getPlayerSelFromPid( player.getId() ) != 0x00FF
										&& !r.getWatchedScoresFromPid( player.getId() )
										/* && 
										 Para no machacar al lagger por segunda vez
										(r != rCache) */ && !punished ) {
									
//									System.out.println( "PRIMERO SE CAE!!!!: " + player.getName() + " porque tiene en sel: " + //$NON-NLS-1$ //$NON-NLS-2$
//											r.getPlayerSelFromPid(player.getId()));
									player.setDecos( player.getDecos() + 1 );
									
									/*
									 * OJO tengo que decir que el otro equipo si que introdujo
									 * el pin
									 */
									
									/*
									 * Es posible que aún no haya cache, si es un ALT-F4 al otro
									 * jugador aún no le llegó el mensaje de LAG
									 */
									if( r.cacheClean() ) r.saveCache();
									r.setUseCacheData();
									r.setTimestamplagged(new Date().getTime());
									int myteam = r.getPlayerTeam(player.getId());
									r.setTeamCodeSent(myteam, false);
									r.setTeamCodeSent(myteam == 1 ? 0 : 1, true);
									processLaggedGame(r);
									r.setUseRealData();
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
							r.resetAllButCache();
						}
						
						r.setPlayerSel(player.getId(), 0x00FF);
						if ( r.getGamePhase() == 7
								&& r.getNumPlayersOnSel() == 0) {
							r.setType(1);
							r.setGamePhase(0x01);
							r.resetAllButCache();
						}
						//rooms.setRoom(r.getId(), r);

//						System.out.println( "DESPUES ME LO VENTILO!!!!!"); //$NON-NLS-1$
//						System.out.println( "ANTES DE VENTILARMELO TENIA: " + r.getNumPlayersOnSel() + " EN SEL"); //$NON-NLS-1$ //$NON-NLS-2$
						r.removePlayer(player.getId());
//						System.out.println( "DESPUES DE VENTILARMELO TENGO: " + r.getNumPlayersOnSel() + " EN SEL"); //$NON-NLS-1$ //$NON-NLS-2$
						//rooms.setRoom(r.getId(), r);
						r.setUseCacheData();
//						System.out.println( "PERO EN CACHE TENGO: " + r.getNumPlayersOnSel() + " EN SEL"); //$NON-NLS-1$ //$NON-NLS-2$
						r.setUseRealData();
						if (r.getNumPlayers() == 0) {
							con.log("Removing room"); //$NON-NLS-1$
							rooms.removeRoom(r.getId());

							Message mes = new Message();
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

							Message mes = new Message();
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
						//sendRoomList( channel );
					}
					/*
					 * OJO Mirar bug del 16 de abril de 2009 hora: 00:09:30
					 */
					try {
						Tools.dbSavePlayerInfo( db, player );
						con.log( "USER PROFILE SAVED!" ); //$NON-NLS-1$
					} catch( SQLException ex ) {
						con.log( "UNABLE TO SAVE USER PROFILE!!!!" + ex.getMessage()); //$NON-NLS-1$
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
