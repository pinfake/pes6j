package pes6j.servers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

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
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.QUERIES;
import pes6j.datablocks.UserInfo;
import pes6j.datablocks.Util;

/**
 * This is to help people to write Client server application I tried to make it
 * as simple as possible... the client connect to the server the client send a
 * String to the server the server returns it in UPPERCASE thats all
 */

public class PES6JAccServerThreaded {
	// the socket used by the server

	private ServerSocket serverSocket;
	Properties properties;
	byte[] default_settings = null;
	private Logger logger;
	ChannelList channels = new ChannelList();

	void loadProperties() throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream("config/properties")); //$NON-NLS-1$
		String allow_keys = properties.getProperty("allow-new-keys");
	}

	void initialize() throws IOException {
		
		channels.addChannel(new ChannelInfo(
				ChannelInfo.TYPE_MENU,
				"MENU03-SP/", null));
	}
	// server constructor

	PES6JAccServerThreaded(int port) {

		/* create socket server and wait for connection requests */
		try

		{
			logger = new Logger( "log/pes6j_ACC_SERVER.log" );
			loadProperties();
			initialize();
			serverSocket = new ServerSocket(port);

			logger.log("PES6JAccServer waiting for client on port "
					+ serverSocket.getLocalPort());

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
			
			while (true)

			{

				Socket socket = serverSocket.accept(); // accept connection
				socket.setTcpNoDelay(true);
				logger.log("New client asked for a connection ["
						+ socket.getInetAddress().getHostAddress() + "]");

				TcpThread t = new TcpThread(socket); // make a thread of it

				logger.log("Starting a thread for ["
						+ socket.getInetAddress().getHostAddress() + "]");

				t.start();

			}

		}

		catch (IOException e) {
			System.err.println( "Exception on new ServerSocket: " + e );
		}

	}

	// you must "run" server to have the server run as a console application

	public static void main(String[] arg) {
		// start server on port 1500
		new PES6JAccServerThreaded(12881);
	}

	/** One instance of this thread will run for each client */

	class TcpThread extends Thread {

		// the socket where to listen/talk

		DataCdKeyAndPass cdkey = null;
		PlayerInfo player = null;
		UserInfo uInfo = null;
		
		Socket socket;
		String address;

		private Db db;
		
		InputStream in;
		OutputStream out;

		TcpThread(Socket socket) {
			this.socket = socket;
			this.address = socket.getInetAddress().getHostAddress();
		}

		void initDB() throws IOException, SQLException {
			db = new Db(properties);
			db.connect();
		}
		
		public void run() {

			PESConnection out;
			
			try {
				initDB();
				out = new PESConnection(socket, false);
				in = socket.getInputStream();
				out.log("[" + address + "] Waiting for command...");
			}

			catch (IOException e) {

				logger.log("Exception creating new Input/output Streams: "
								+ e);
				return;
			} catch( SQLException e ) {
				logger.log("Exception connecting to DB: "
						+ e);
				return;
			}

			// read a String (which is an object)

			try {

				Message mes;
				MetaMessageBlock mb;
				int seq = 1;
				byte[] input;

				while ((input = Util.streamReadDecoded(in)) != null) {
					out.log("READ:");
					out.log(Util.toHex(input));
					mes = new Message(input);
					mb = mes.getBlock(0);

					int qId = ((MessageBlock) mb).getHeader().getQuery();

					if (qId == QUERIES.LOGIN_INIT_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN INIT CLIENT QUERY ANSWERING LOGIN INIT SERVER:");
						mes = new Message();
						mes.addBlock(new MessageBlockLoginInit(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
						
						/*
						 * Recuperamos su nombre de usuario
						 */
						try {
							uInfo = Tools.getUserInfo( properties.getProperty("auth-url"), address );
						} catch( Exception ex ) {
							out.log( "EXCEPTION TRYING TO FETCH USER INFO: " );
							out.log( ex );
							throw new IOException( "UNABLE TO FETCH USER INFO!!!");
						}
						if( uInfo.getAccess() < 1 ) {
							throw new IOException( "THE USER \"" + uInfo.getUsername() + "\" DOESNT HAVE ENOUGH ACCESS: " +
									uInfo.getAccess() );
						}
						try {
							Tools.dbSaveUserInfo( db, uInfo, address );
						} catch( SQLException ex ) {
							out.log( "EXCEPTION TRYING TO SAVE USER INFO: " );
							out.log( ex );
							throw new IOException( "UNABLE TO SAVE USER INFO!!!");
						}
					} else if (qId == 0x00003003) {
						out
								.log("MATCHES LOGIN LOGIN CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");

						cdkey = new DataCdKeyAndPass(mes.getBlock(0).getData());

						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));

						out.streamWriteEncoded(mes);
					} else if (qId == QUERIES.LOGIN_PLAYERS_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN PLAYERS CLIENT QUERY ANSWERING PLAYERS SERVER:");

						try {
							if( uInfo == null ) {
								try {
									uInfo = Tools.getUserInfo(properties
										.getProperty("auth-url"), address);
								} catch( Exception ex ) {
									out.log( "EXCEPTION TRYING TO FETCH USER INFO: " );
									out.log( ex );
									throw new IOException( "UNABLE TO FETCH USER INFO!!!");
								}
								if (uInfo.getAccess() < 1) {
									throw new IOException("THE USER \""
											+ uInfo.getUsername()
											+ "\" DOESNT HAVE ENOUGH ACCESS: "
											+ uInfo.getAccess());
								}
								out.setUserInfo( uInfo );
							}
							PlayerInfo plInfo[] = Tools.dbLoadPlayers(db, uInfo.getUsername());
							qId++;
							mes = new Message();
							mes.addBlock(new MessageBlockLoginPlayers(plInfo,
									++qId));
							out.streamWriteEncoded(mes);

						} catch (SQLException ex) {
							throw new IOException(ex.getMessage());
						}
					} else if (qId == QUERIES.LOGIN_Q4_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN Q4 CLIENT QUERY ANSWERING LOGIN Q4 SERVER:");
						mes = new Message();
						++qId;
						mes.addBlock(new MessageBlockLoginQ4(++qId));

						out.streamWriteEncoded(mes);
					} else if (qId == QUERIES.LOGIN_CRTPLY_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN CRTPLY CLIENT QUERY ANSWERING BLOCK ZERO SERVER & PLAYER LIST:");

						DataCrtPlayer crtPly = new DataCrtPlayer(mes
								.getBlock(0).getData());
						boolean ok = false;
						try {
							Tools.dbCreatePlayer(db, uInfo.getUsername(), crtPly, default_settings);
							ok = true;
						} catch (SQLException ex) {
							ex.printStackTrace();
						}

						try {
							PlayerInfo plInfo[] = Tools.dbLoadPlayers(db, uInfo.getUsername());

							mes = new Message();
							long answer = 0x00000000;
							if (!ok)
								answer = 0x000000FF; // No se cual es la
														// respuesta si el tema
														// es incorrecto
							mes.addBlock(new MessageBlockLong(answer, ++qId));

							out.streamWriteEncoded(mes);

							mes = new Message();
							mes.addBlock(new MessageBlockLoginPlayers(plInfo,
									++qId));

							out.streamWriteEncoded(mes);
						} catch (SQLException ex) {
							ex.printStackTrace();
						}

					} else if (qId == QUERIES.LOGIN_PLYGRP_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN PLYGRP CLIENT QUERY ANSWERING LOGIN PLYGRP SERVER:");

						DataId pid = new DataId(mes.getBlock(0).getData());
						try {
							player = Tools.dbLoadPlayerAndGroupInfo(db, pid.getId(), default_settings);
							qId++;
							mes = new Message();
							mes.addBlock(new MessageBlockLoginPlayerGroup(
									player, player.getGroupInfo(),
									++qId));
							out.streamWriteEncoded(mes);

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
						out.streamWriteEncoded(mes);
					} else if (qId == QUERIES.LOGIN_Q10_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN Q10 CLIENT QUERY ANSWERING BLOCK ZERO SERVER:");
						mes = new Message();

						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
						qId++;
						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == 0x0000308A) {
						out
								.log("MATCHES LOGIN MESSAGES CLIENT QUERY ANSWERING LOGIN MESSAGES SERVER & LOGIN MESGLST SERVER:");

						mes = new Message();
						mes.addBlock(new MessageBlockLoginMessages(player
								.getId(), 0x00003087));
						// out.streamWriteBlockEncoded((MessageBlock)mes.getBlock(0));
						out.streamWriteEncoded(mes);
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
						seq += 2;
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

						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						// out.write(LOGIN_R1123);

					} else if (qId == QUERIES.LOGIN_Q12_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN Q12 CLIENT QUERY ANSWERING BLOCK ZERO:");
						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.LOGIN_Q13_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN Q13 CLIENT QUERY ANSWERING BLOCK ZERO:");
						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.LOGIN_Q14_CLIENT_QUERY) {
						out
								.log("MATCHES LOGIN Q14 CLIENT QUERY ANSWERING BLOCK ZERO:");

						DataId did = new DataId(mes.getBlock(0).getData());
						try {
							Tools.dbSetPlayerSel(db, uInfo.getUsername(), did.getId());
							mes = new Message();
							mes.addBlock(new MessageBlockZero(
									QUERIES.LOGIN_Q14_1_SERVER_QUERY));
							out.streamWriteEncoded(mes);
							// Util.streamWriteEncoded(out, mes.getBytes());
							mes = new Message();
							mes.addBlock(new MessageBlockZero(
									QUERIES.LOGIN_Q14_2_SERVER_QUERY));
							out.streamWriteEncoded(mes);
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
							player = Tools.dbGetPlayerSel(db, uInfo, default_settings );
							mes = new Message();
							mes.addBlock(new MessageBlockMenuGroupInfo(player
									.getId(), player.getGroupInfo(),
									++qId));
							out.streamWriteEncoded(mes);
							// Util.streamWriteEncoded(out, mes.getBytes());
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
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.MENU_MYIP_CLIENT_QUERY) {
						out
								.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING MENU PLAYER AND GROUP SERVER:");
						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
								QUERIES.MENU_PLYGRP_SERVER_QUERY));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.MENU_MYPLID_CLIENT_QUERY) {
						out
								.log("MATCHES MENU MYIP CLIENT QUERY ANSWERING FRIEND LIST AND GROUP???:");
						mes = new Message();

						// StefenG
						mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
								QUERIES.MENU_PLYGRP_SERVER_QUERY));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes.addBlock(new MessageBlockZero(qId + 2));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						/*
						 * By now, there are no friends
						 */
						
//						mes = new Message();
//						mes.addBlock(new MessageBlockMenuFriends(
//								new PlayerInfo[] { new PlayerInfo(0x00000077L,
//										"Amigo") }, qId + 4, seq++));
//						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes.addBlock(new MessageBlockVoid(qId + 6));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						// ST-Lyon
						mes = new Message();
						mes.addBlock(new MessageBlockMenuPlayerGroup(player, 0,
								QUERIES.MENU_PLYGRP_SERVER_QUERY));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes.addBlock(new MessageBlockMenuPlayerId(player
								.getId(), QUERIES.MENU_PLYGRP_SERVER_QUERY + 1));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					}

				}
			}

			catch (IOException e) {
				try {
					out.log("Exception reading/writing  Streams: " + e);
				} catch (IOException ex) {

				}
				return;

			}

			finally {
				try {
					out.close();
					in.close();
					db.disconnect();
				} catch (Exception e) {
				}
			}
		}
	}
}
