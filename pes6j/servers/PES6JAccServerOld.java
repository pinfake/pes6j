package pes6j.servers;

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
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import pes6j.database.Db;
import pes6j.datablocks.ChannelInfo;
import pes6j.datablocks.ChannelList;
import pes6j.datablocks.DataCdKeyAndPass;
import pes6j.datablocks.DataCrtPlayer;
import pes6j.datablocks.DataId;
import pes6j.datablocks.Message;
import pes6j.datablocks.MessageBlock;
import pes6j.datablocks.MessageBlockLoginDateTimeInfo;
import pes6j.datablocks.MessageBlockLoginInit;
import pes6j.datablocks.MessageBlockLoginMessages;
import pes6j.datablocks.MessageBlockLoginMessagesList;
import pes6j.datablocks.MessageBlockLoginPlayerGroup;
import pes6j.datablocks.MessageBlockLoginPlayers;
import pes6j.datablocks.MessageBlockLoginQ4;
import pes6j.datablocks.MessageBlockLong;
import pes6j.datablocks.MessageBlockMenuGroupInfo;
import pes6j.datablocks.MessageBlockMenuMenuList;
import pes6j.datablocks.MessageBlockMenuPlayerGroup;
import pes6j.datablocks.MessageBlockMenuPlayerId;
import pes6j.datablocks.MessageBlockVoid;
import pes6j.datablocks.MessageBlockZero;
import pes6j.datablocks.MetaMessageBlock;
import pes6j.datablocks.PESConnection;
import pes6j.datablocks.PESConnectionList;
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.QUERIES;
import pes6j.datablocks.UserInfo;
import pes6j.datablocks.Util;

/**
 * This is to help people to write Client server application I tried to make it
 * as simple as possible... the client connect to the server the client send a
 * String to the server the server returns it in UPPERCASE thats all
 */

public class PES6JAccServerOld {
	// the socket used by the server

	private ServerSocketChannel ssChannel;
	Selector selector;

	PESConnectionList connections = new PESConnectionList();
	
	Properties properties;
	byte[] default_settings = null;
	private Logger logger;
	ChannelList channels = new ChannelList();
	private Db db;
	
	
	void loadProperties() throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream("config/properties")); //$NON-NLS-1$
		String allow_keys = properties.getProperty("allow-new-keys");
	}

	void initialize() throws IOException {
		
		channels.addChannel(new ChannelInfo(
				ChannelInfo.TYPE_MENU,
				"MENU03-SP/", null));
		initDB();
	}
	// server constructor

	PES6JAccServerOld(int port) {
		
		/* create socket server and wait for connection requests */
		try
		
		{
			logger = new Logger( "log/pes6j_ACC_SERVER.log" );
			loadProperties();
			initialize();
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(port));
			
			selector = Selector.open();
			ssChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			logger.log("PES6JAccServer waiting for client on port "
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
				while ((selector.select(5000)) > 0) {
					// Someone is ready for I/O, get the ready keys
					Set readyKeys = selector.selectedKeys();
					System.out.println( "tengo: " + readyKeys.size() + " keys... ");
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
				            }
						/*
						 * Comprobamos si tenemos a algun jugador zombie
						 */
						long now = (new Date()).getTime();
						System.out.println( "Tengo " + connections.size() + " conexiones.");
						Enumeration<PESConnection> cons = connections.elements();
						while( cons.hasMoreElements() ) {
							PESConnection con1 = cons.nextElement();
							if( now - con1.getLastKATime() > 60000L ) {
								SelectionKey sk1 = con1.getSocketChannel().keyFor( selector );
								if( sk1 != null ) {
									sk1.cancel();
								}
								con1.log( "TIMED OUT, NO KEEP ALIVE REPLY FOR ONE MINUTE" );
								con1.close();
								connections.removeConnection(con1);
							}
						}
					}
				}
			}
		}
		
		catch (IOException e) {
			System.err.println( "Exception on new ServerSocket: " + e );
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
		socketChannel.register(this.selector, SelectionKey.OP_READ);
		
		try {
			PESConnection con = new PESConnection( socket, false );
			connections.addConnection(con);
		}
		
		catch (IOException e) {
			logger.log("Exception creating new Input/output Streams: "
					+ e);
			return;
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
		}
		//
		catch (IOException e) {
			try {
				con.log("Exception reading/writing  Streams: " + e);
				key.cancel();
				socketChannel.close();
				con.close();
				connections.removeConnection( con );
			} catch (IOException ex) {
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	} 


	void initDB() throws IOException {
		db = new Db(properties);
	}
	
	public static void main(String[] arg) {
		// start server on port 1500
		new PES6JAccServerOld(12881);
	}

	void processMessageBlock(PESConnection out, MetaMessageBlock mb)
	throws IOException {
		
		DataCdKeyAndPass cdkey = null;
		PlayerInfo player = out.getPlayerInfo();
		UserInfo uInfo = out.getUserInfo();
		
		String address = out.getSocket().getInetAddress().getHostAddress();
		
		// read a String (which is an object)
		
		
		Message mes;
		
		byte[] enc = Util.konamiEncode(mb.getBytes());
		byte[] enccab = new byte[4];
		System.arraycopy(enc, 0, enccab, 0, 4);
		out.log("HEX ENC CAB: " + Util.toHex(enccab));
		
		int qId = ((MessageBlock) mb).getHeader().getQuery();
		
		if (qId == QUERIES.LOGIN_INIT_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN INIT CLIENT QUERY ANSWERING LOGIN INIT SERVER:");
			mes = new Message();
			mes.addBlock(new MessageBlockLoginInit(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			/*
			 * Recuperamos su nombre de usuario
			 */
			
			uInfo = Tools.getUserInfo(properties
					.getProperty("auth-url"), address);
			if (uInfo.getAccess() < 1) {
				throw new IOException("THE USER \""
						+ uInfo.getUsername()
						+ "\" DOESNT HAVE ENOUGH ACCESS: "
						+ uInfo.getAccess());
			}
			out.setUserInfo( uInfo );
			
		} else if (qId == 0x00003003) {
			out
			.log("MATCHES LOGIN LOGIN CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");
			
			cdkey = new DataCdKeyAndPass(mb.getData());
			
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			
			out.streamWriteEncodedNIO(mes);
		} else if (qId == QUERIES.LOGIN_PLAYERS_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN PLAYERS CLIENT QUERY ANSWERING PLAYERS SERVER:");
			
			try {
				if( uInfo == null ) {
					uInfo = Tools.getUserInfo(properties
							.getProperty("auth-url"), address);
					if (uInfo.getAccess() < 1) {
						throw new IOException("THE USER \""
								+ uInfo.getUsername()
								+ "\" DOESNT HAVE ENOUGH ACCESS: "
								+ uInfo.getAccess());
					}
					out.setUserInfo( uInfo );
				}
				PlayerInfo plInfo[] = Tools.dbLoadPlayers(db, uInfo
						.getUsername());
				qId++;
				mes = new Message();
				mes.addBlock(new MessageBlockLoginPlayers(plInfo,
						++qId));
				out.streamWriteEncodedNIO(mes);
				
			} catch (SQLException ex) {
				throw new IOException(ex.getMessage());
			}
		} else if (qId == QUERIES.LOGIN_Q4_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN Q4 CLIENT QUERY ANSWERING LOGIN Q4 SERVER:");
			mes = new Message();
			++qId;
			mes.addBlock(new MessageBlockLoginQ4(++qId));
			
			out.streamWriteEncodedNIO(mes);
		} else if (qId == QUERIES.LOGIN_CRTPLY_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN CRTPLY CLIENT QUERY ANSWERING BLOCK ZERO SERVER & PLAYER LIST:");
			
			DataCrtPlayer crtPly = new DataCrtPlayer(mb
					.getData());
			boolean ok = false;
			try {
				Tools.dbCreatePlayer(db, uInfo.getUsername(), crtPly,
						default_settings);
				ok = true;
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			
			try {
				PlayerInfo plInfo[] = Tools.dbLoadPlayers(db, uInfo
						.getUsername());
				
				mes = new Message();
				long answer = 0x00000000;
				if (!ok)
					answer = 0x000000FF; // No se cual es la
				// respuesta si el tema
				// es incorrecto
				mes
				.addBlock(new MessageBlockLong(answer, ++qId));
				
				out.streamWriteEncodedNIO(mes);
				
				mes = new Message();
				mes.addBlock(new MessageBlockLoginPlayers(plInfo,
						++qId));
				
				out.streamWriteEncodedNIO(mes);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			
		} else if (qId == QUERIES.LOGIN_PLYGRP_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN PLYGRP CLIENT QUERY ANSWERING LOGIN PLYGRP SERVER:");
			
			DataId pid = new DataId(mb.getData());
			try {
				player = Tools.dbLoadPlayerAndGroupInfo(db,
						pid.getId(), default_settings);
				out.setPlayerInfo( player );
				player.setConnection( out );
				qId++;
				mes = new Message();
				mes.addBlock(new MessageBlockLoginPlayerGroup(player,
						player.getGroupInfo(), ++qId));
				out.streamWriteEncodedNIO(mes);
				
			} catch (SQLException ex) {
				
			}
			
		} else if (qId == QUERIES.LOGIN_GRPNFO_CLIENT_QUERY) {
			/*
			 * Si si, pero es informacion del grupo
			 */
			out
			.log("MATCHES LOGIN GET DATE AND TIME ANSWERING DATE AND TIME SERVER:");
			qId++;
			
			mes = new Message();
			mes.addBlock(new MessageBlockLoginDateTimeInfo((new Date())
					.getTime(), ++qId));
			out.streamWriteEncodedNIO(mes);
		} else if (qId == QUERIES.LOGIN_Q10_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN Q10 CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");
			mes = new Message();
			
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			qId++;
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		} else if (qId == 0x0000308A) {
			out
			.log("MATCHES LOGIN MESSAGES CLIENT QUERY ANSWERING LOGIN MESSAGES SERVER & LOGIN MESGLST SERVER:");
			
			mes = new Message();
			mes.addBlock(new MessageBlockLoginMessages(player.getId(),
					0x00003087));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(0));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			mes = new Message();
			mes.addBlock(new MessageBlockLoginMessagesList(player
					.getSettings()));
			// mes.addBlock( new MessageBlockLoginMessagesList(
			// new MessagesInfo(
			// "Largo 1",
			// "Largo 2",
			// "Largo 3",
			// "Largo 4",
			// "Largo 5",
			// "Largo 6",
			// "Largo 7",
			// "Largo 8",
			// "Largo 9",
			// "Largo 10",
			// "Corto 1",
			// "Corto 2",
			// "Corto 3",
			// "Corto 4",
			// "Corto 5",
			// "Corto 6",
			// "Corto 7",
			// "Corto 8"), QUERIES.LOGIN_MESSAGES_SERVER_QUERY+1,
			// seq));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(0));
			// mes.addBlock( new MessageBlockVoid( 0x00000005, 0
			// ));
			mes.addBlock(new MessageBlockZero(
					QUERIES.LOGIN_MESSAGES_SERVER_QUERY + 1));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(1));
			mes.addBlock(new MessageBlockVoid(
					QUERIES.LOGIN_MESSAGES_SERVER_QUERY + 2));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(2));
			
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			// out.write(LOGIN_R1123);
			
		} else if (qId == QUERIES.LOGIN_Q12_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN Q12 CLIENT QUERY ANSWERING BLOCK ZERO:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		} else if (qId == QUERIES.LOGIN_Q13_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN Q13 CLIENT QUERY ANSWERING BLOCK ZERO:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		} else if (qId == QUERIES.LOGIN_Q14_CLIENT_QUERY) {
			out
			.log("MATCHES LOGIN Q14 CLIENT QUERY ANSWERING BLOCK ZERO:");
			
			DataId did = new DataId(mb.getData());
			try {
				Tools.dbSetPlayerSel(db, uInfo.getUsername(), did
						.getId());
				mes = new Message();
				mes.addBlock(new MessageBlockZero(
						QUERIES.LOGIN_Q14_1_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncoded(out, mes.getBytes());
				mes = new Message();
				mes.addBlock(new MessageBlockZero(
						QUERIES.LOGIN_Q14_2_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncoded(out, mes.getBytes());
			} catch (SQLException ex) {
				
			}
		}
		
		/*
		 * A PARTIR DE AQUI SERIA EL MENU SERVER!!!
		 */
		else if (qId == QUERIES.MENU_GROUP_CLIENT_QUERY) {
			out
			.log("MATCHES MENU GROUP CLIENT QUERY ANSWERING MENU GROUP INFO SERVER:");
			try {
				player = Tools.dbGetPlayerSel(db, uInfo,
						default_settings);
				out.setPlayerInfo( player );
				player.setConnection( out );
				
				mes = new Message();
				mes.addBlock(new MessageBlockMenuGroupInfo(player
						.getId(), player.getGroupInfo(), ++qId));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncoded(out, mes.getBytes());
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		} else if (qId == 0x00004200) {
			out
			.log("MATCHES MENU MENUS CLIENT QUERY ANSWERING MENU MENU LIST SERVER:");
			mes = new Message();
			mes.addBlock(new MessageBlockMenuMenuList(channels, player, ++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		} else if (qId == QUERIES.MENU_MYIP_CLIENT_QUERY) {
			out
			.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING MENU PLAYER AND GROUP SERVER:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		} else if (qId == QUERIES.MENU_MYPLID_CLIENT_QUERY) {
			out
			.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING FRIEND LIST AND GROUP???:");
			mes = new Message();
			
			// StefenG
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			mes = new Message();
			mes.addBlock(new MessageBlockZero(qId + 2));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			/*
			 * By now, there are no friends
			 */
			
			//						mes = new Message();
			//						mes.addBlock(new MessageBlockMenuFriends(
			//								new PlayerInfo[] { new PlayerInfo(0x00000077L,
			//										"Amigo") }, qId + 4, 0));
			//						out.streamWriteEncoded(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			mes = new Message();
			mes.addBlock(new MessageBlockVoid(qId + 6));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			// ST-Lyon
			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
			
			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerId(player.getId(),
					QUERIES.MENU_PLYGRP_SERVER_QUERY + 1));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncoded(out, mes.getBytes());
		}
		
		
	}
		
	
}
