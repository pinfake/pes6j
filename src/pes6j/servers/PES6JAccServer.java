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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import pes6j.database.Db;
import pes6j.datablocks.ChannelInfo;
import pes6j.datablocks.ChannelList;
import pes6j.datablocks.DataCrtPlayer;
import pes6j.datablocks.DataId;
import pes6j.datablocks.GroupInfo;
import pes6j.datablocks.Message;
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

public class PES6JAccServer {
	// the socket used by the server

	private ServerSocketChannel ssChannel;
	PESConnectionList connections = new PESConnectionList();
	byte[] default_settings = null;
	private Logger logger;
	ChannelList channels = new ChannelList();
	Selector selector;
	private Db db;
	boolean debug = false;
	Vector<SelectionKey> pendingKeys;
	
	void initDB() throws IOException, SQLException {
		db = new Db(Configuration.authProperties);
		db.connect();
	}

	void initialize() throws IOException, SQLException {
		Configuration.initializeAuthProperties();
		debug = Configuration.authProperties.getProperty("debug").trim().toLowerCase().equals("on");
		channels.addChannel(new ChannelInfo(
				ChannelInfo.TYPE_MENU,
				"MENU03-SP/", null));
		initDB();
	}
	// server constructor

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
			con.log( "[ACCSERVER] connection accepted.");
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
				if( pendingKeys.indexOf(key) != -1 ) {
					pendingKeys.remove( key );
				}
				key.cancel();
				socketChannel.close();
				return;
			}
			
//			if( con.isFetchingUserInfo() ) {
//				
//				if( pendingKeys.indexOf(key) == -1 ) {
//					con.log( "numero de keys: " + pendingKeys.size() + " numero de conexiones: " + connections.size());
//					pendingKeys.add( key );
//					con.log( "[ACCSERVER] añado key");
//				}
//				return;
//			} else if( pendingKeys.indexOf(key) != -1 ) {
//				pendingKeys.remove( key );
//				con.log( "[ACCSERVER] borro key");
//			}
//			if( con.userInfoFetched() ) {
//				con.fetchUserInfo();
//				UserInfo uInfo = con.getUserInfo();
//				
//				if( uInfo.getAccess() < 1 ) {
//					throw new IOException( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " +
//							uInfo.getAccess() );
//				}
//				try {
//					Tools.dbSaveUserInfo( db, uInfo, con.getSocket().getInetAddress().getHostAddress() );
//				} catch( SQLException ex ) {
//					con.log( "EXCEPTION TRYING TO SAVE USER INFO: " );
//					con.log( ex );
//					throw new IOException( "UNABLE TO SAVE USER INFO!!!");
//				}
//				con.log( "[ACCSERVER] fetched user info");
//			}
			try {
				con.log( "[ACCSERVER] read going to process block");
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
	  
	  	public void retryLastMessageBlock( PESConnection con ) {
	  		Message mes = con.getLastReadMessage();
	  		if( mes != null ) {
	  			MetaMessageBlock mb = mes.getBlock(mes.getNumBlocks()-1);
	  			try {
	  				processMessageBlock( con, mb );
	  			} catch( IOException ex ) {
	  				ex.printStackTrace();
	  			}
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
	  
		public void disconnectPlayer( PESConnection con ) {
			
			/*
			 * Necesitamos las conexiones del canal de este jugador
			 * suponiendo que el jugador ya tenga IP
			 */
			

			try {

				
				connections.removeConnection(con);
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
	  
	PES6JAccServer(int port) {

		/* create socket server and wait for connection requests */
		try

		{
			logger = new Logger( "log/pes6j_ACC_SERVER.log" );
			initialize();
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(port));
			
			selector = Selector.open();
			SelectionKey acceptKey = ssChannel.register(selector, SelectionKey.OP_ACCEPT);

			
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
			
			pendingKeys = new Vector<SelectionKey>();
			while (true)
			{
				Enumeration<SelectionKey> pendKeys = pendingKeys.elements();
				while( pendKeys.hasMoreElements() ) {
					SelectionKey key = pendKeys.nextElement();
					try {
						checkFetchUserInfo(key);
						this.read(key);
					} catch( IOException e ) {
						key.cancel();
					}
				}
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
							checkFetchUserInfo(sk);
							try {
								this.read(sk);
							} catch( IOException e ) {
								sk.cancel();
							}
						} else if (sk.isWritable()) {
							checkFetchUserInfo(sk);
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
					checkFetchUserInfo(sk1);	
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
			}

		}

		catch (Exception e ) {
			
			System.err.println( "Exception on new ServerSocket: " + e );
			e.printStackTrace();
		}

	}
	
	public void checkFetchUserInfo( SelectionKey sk1 ) {
		
		try {
			PESConnection con1 = connections.searchConnection(((SocketChannel) sk1.channel()).socket());
			if( con1.isFetchingUserInfo() ) {
				
				if( pendingKeys.indexOf(sk1) == -1 ) {
					con1.log( "numero de keys: " + pendingKeys.size() + " numero de conexiones: " + connections.size());
					pendingKeys.add( sk1 );
					con1.log( "[ACCSERVER] añado key");
				}
				return;
			} else if( pendingKeys.indexOf(sk1) != -1 ) {
				pendingKeys.remove( sk1 );
				con1.log( "[ACCSERVER] borro key");
			}
			
			if( con1.userInfoFetched() ) {
				con1.fetchUserInfo();
				UserInfo uInfo = con1.getUserInfo();
				
				if( uInfo.getAccess() < 1 ) {
					con1.log( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " +
							uInfo.getAccess() );
				}
				try {
					Tools.dbSaveUserInfo( db, uInfo, con1.getSocket().getInetAddress().getHostAddress() );
				} catch( SQLException ex ) {
					con1.log( "EXCEPTION TRYING TO SAVE USER INFO: " );
					con1.log( ex );
					//throw new IOException( "UNABLE TO SAVE USER INFO!!!");
				}
				con1.log( "[ACCSERVER] fetched user info");
				retryLastMessageBlock(con1);
			}
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
	}

	// you must "run" server to have the server run as a console application

	public static void main(String[] arg) {
		// start server on port 1500
		new PES6JAccServer(12881);
	}

	void processMessageBlock(PESConnection out, MetaMessageBlock mb) throws IOException {
		
		//DataCdKeyAndPass cdkey = out.getCdkey();
		PlayerInfo player = out.getPlayerInfo();
		UserInfo uInfo = out.getUserInfo();
		
		String address = out.getSocket().getInetAddress().getHostAddress();
		
		/*
		 * Necesitamos las conexiones del canal de este jugador
		 * suponiendo que el jugador ya tenga IP
		 */
		
		byte[] enc = Util.konamiEncode(mb.getBytes());
		byte[] enccab = new byte[4];
		System.arraycopy(enc, 0, enccab, 0, 4);
		out.log("HEX ENC CAB: " + Util.toHex(enccab));

		int qId = ((MetaMessageBlock) mb).getHeader().getQuery();

		Message mes;
		
		if( out.getUserInfo() == null && out.getHTTPThread() == null ) {
			HTTPFetchUserInfoThread uInfoThread = new HTTPFetchUserInfoThread( Configuration.authProperties.getProperty("auth-url"), address );
			uInfoThread.start();
			out.setHTTPThread( uInfoThread );
		}

		try {
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
//			try {
//				if( out.getUserInfo() == null ) {
//					HTTPFetchUserInfoThread uInfoThread = new HTTPFetchUserInfoThread( properties.getProperty("auth-url"), address );
//					uInfoThread.start();
//					out.setHTTPThread( uInfoThread );
//				}
				//uInfo = Tools.getUserInfo( properties.getProperty("auth-url"), address );
//			} catch( Exception ex ) {
//				out.log( "EXCEPTION TRYING TO FETCH USER INFO: " );
//				out.log( ex );
//				throw new IOException( "UNABLE TO FETCH USER INFO!!!");
//			}
//			if( uInfo.getAccess() < 1 ) {
//				throw new IOException( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " +
//						uInfo.getAccess() );
//			}
//			try {
//				Tools.dbSaveUserInfo( db, uInfo, address );
//			} catch( SQLException ex ) {
//				out.log( "EXCEPTION TRYING TO SAVE USER INFO: " );
//				out.log( ex );
//				throw new IOException( "UNABLE TO SAVE USER INFO!!!");
//			}
		} else if (qId == 0x00003003) {
			out
					.log("MATCHES LOGIN LOGIN CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");

			//cdkey = new DataCdKeyAndPass(mb.getData());

			// As reddwarf says i can check whether they have the right patch or not
			// I don't know if transfer patches apply here.
			
			if( uInfo != null ) {
				if( uInfo.getAccess() < 1) {
					mes = new Message();
					mes.addBlock(new MessageBlockLong(0xffffff10, 0x00003004));
					out.streamWriteEncodedNIO(mes);
				} else {
					mes = new Message();
					mes.addBlock(new MessageBlockZero(++qId));
					out.streamWriteEncodedNIO(mes);
				} 
			}
			/*
			 * Si no tengo la información de usuario no pasamos de aqui.
			 */
			
		} else if (qId == QUERIES.LOGIN_PLAYERS_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN PLAYERS CLIENT QUERY ANSWERING PLAYERS SERVER:");

			try {
				
//				uInfo 
//				if( uInfo == null ) {
//					try {
//						uInfo = Tools.getUserInfo(properties
//							.getProperty("auth-url"), address);
//					} catch( Exception ex ) {
//						out.log( "EXCEPTION TRYING TO FETCH USER INFO: " );
//						out.log( ex );
//						throw new IOException( "UNABLE TO FETCH USER INFO!!!");
//					}
//					if (uInfo.getAccess() < 1) {
//						throw new IOException("THE USER \""
//								+ uInfo.getUsername()
//								+ "\" DOESNT HAVE ENOUGH ACCESS: "
//								+ uInfo.getAccess());
//					}
//					out.setUserInfo( uInfo );
//				}
				out.log( "Voy a listar sus jugadores, pero antes, mi username es: " + uInfo.getUsername() + " "
						+ " mi ip es: " + out.getSocket().getInetAddress().getHostAddress() );
				PlayerInfo plInfo[] = Tools.dbLoadPlayers(db, uInfo.getUsername());
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

			DataCrtPlayer crtPly = new DataCrtPlayer(mb.getData());
			boolean ok = false;
			try {
				Tools.dbCreatePlayer(db, uInfo.getUsername(), crtPly, default_settings);
				ok = true;
			} catch (SQLException ex) {
				ex.printStackTrace();
			}


			mes = new Message();
			long answer = 0x00000000;
			if (!ok) {
				answer = 0xfffffefc; // Ya existe un usuario con ese nombre
				mes.addBlock(new MessageBlockLong(answer, 0x00003022));
			} else {
				mes.addBlock(new MessageBlockLong(answer, 0x00003022));
			}
			out.streamWriteEncodedNIO(mes);


		} else if (qId == QUERIES.LOGIN_PLYGRP_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN PLYGRP CLIENT QUERY ANSWERING LOGIN PLYGRP SERVER:");

			DataId pid = new DataId(mb.getData());
			try {
				player = Tools.dbLoadPlayerInfo(db, pid.getId(), default_settings);
				out.setPlayerInfo( player );
				player.setConnection( out );
				qId++;
				mes = new Message();
				mes.addBlock(new MessageBlockLoginPlayerGroup(
						player,
						++qId));
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
			mes.addBlock(new MessageBlockLoginDateTimeInfo(
					(new Date()).getTime(), ++qId));
			out.streamWriteEncodedNIO(mes);
		} else if (qId == QUERIES.LOGIN_Q10_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN Q10 CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");
			mes = new Message();

			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
			qId++;
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
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
		}else if (qId == 0x0000308A) {
			out
					.log("MATCHES LOGIN MESSAGES CLIENT QUERY ANSWERING LOGIN MESSAGES SERVER & LOGIN MESGLST SERVER:");

			mes = new Message();
			mes.addBlock(new MessageBlockLoginMessages(player
					.getId(), 0x00003087));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(0));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

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
			//seq += 2;
			// mes.addBlock( new MessageBlockVoid( 0x00000005, seq++
			// ));
			mes
					.addBlock(new MessageBlockZero(
							QUERIES.LOGIN_MESSAGES_SERVER_QUERY + 1));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(1));
			mes
					.addBlock(new MessageBlockVoid(
							QUERIES.LOGIN_MESSAGES_SERVER_QUERY + 2));
			// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(2));

			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			// out.write(LOGIN_R1123);

		} else if (qId == QUERIES.LOGIN_Q12_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN Q12 CLIENT QUERY ANSWERING BLOCK ZERO:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		} else if (qId == QUERIES.LOGIN_Q13_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN Q13 CLIENT QUERY ANSWERING BLOCK ZERO:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		} else if (qId == QUERIES.LOGIN_Q14_CLIENT_QUERY) {
			out
					.log("MATCHES LOGIN Q14 CLIENT QUERY ANSWERING BLOCK ZERO:");

			DataId did = new DataId(mb.getData());
			try {
				Tools.dbSetPlayerSel(db, uInfo.getUsername(), did.getId());
				mes = new Message();
				mes.addBlock(new MessageBlockZero(
						QUERIES.LOGIN_Q14_1_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
				mes = new Message();
				mes.addBlock(new MessageBlockZero(
						QUERIES.LOGIN_Q14_2_SERVER_QUERY));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
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
				player = Tools.dbGetPlayerSel(db, uInfo, default_settings );
				out.setPlayerInfo( player );
				player.setConnection( out );
				mes = new Message();
				mes.addBlock(new MessageBlockMenuGroupInfo(player,
						++qId));
				out.streamWriteEncodedNIO(mes);
				// Util.streamWriteEncodedNIO(out, mes.getBytes());
			} catch (SQLException ex) {
				out.log(ex);
				ex.printStackTrace();
			}
		} else if (qId == 0x00004200) {
			out
					.log("MATCHES MENU MENUS CLIENT QUERY ANSWERING MENU MENU LIST SERVER:");
			mes = new Message();
			mes
					.addBlock(new MessageBlockMenuMenuList(
							channels, player, ++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		} else if (qId == QUERIES.MENU_MYIP_CLIENT_QUERY) {
			out
					.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING MENU PLAYER AND GROUP SERVER:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		} else if (qId == QUERIES.MENU_MYPLID_CLIENT_QUERY) {
			out
					.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING FRIEND LIST AND GROUP???:");
			mes = new Message();

			// StefenG
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes.addBlock(new MessageBlockZero(qId + 2));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			/*
			 * By now, there are no friends
			 */
			
//			mes = new Message();
//			mes.addBlock(new MessageBlockMenuFriends(
//					new PlayerInfo[] { new PlayerInfo(0x00000077L,
//							"Amigo") }, qId + 4, seq++));
//			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes.addBlock(new MessageBlockVoid(qId + 6));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			// ST-Lyon
			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
					QUERIES.MENU_PLYGRP_SERVER_QUERY));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes.addBlock(new MessageBlockMenuPlayerId(player
					.getId(), QUERIES.MENU_PLYGRP_SERVER_QUERY + 1));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		}
		} catch( Exception ex ) {
			out.log( ex );
		}
	}
}

	/** One instance of this thread will run for each client */

