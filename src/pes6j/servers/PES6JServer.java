package pes6j.servers;

//The server code Server.java:
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
import java.util.Set;
import java.util.Vector;

import pes6j.database.Db;
import pes6j.datablocks.Message;
import pes6j.datablocks.MessageBlock;
import pes6j.datablocks.MessageBlockInfo;
import pes6j.datablocks.MessageBlockQ6;
import pes6j.datablocks.MessageBlockRnkUrl;
import pes6j.datablocks.MessageBlockRnkUrlLst;
import pes6j.datablocks.MessageBlockServers;
import pes6j.datablocks.MessageBlockVoid;
import pes6j.datablocks.MessageBlockZero;
import pes6j.datablocks.MetaMessageBlock;
import pes6j.datablocks.PESConnection;
import pes6j.datablocks.PESConnectionList;
import pes6j.datablocks.QUERIES;
import pes6j.datablocks.RankUrlInfo;
import pes6j.datablocks.ServerInfo;
import pes6j.datablocks.Util;

/**
 * This is to help people to write Client server application I tried to make it
 * as simple as possible... the client connect to the server the client send a
 * String to the server the server returns it in UPPERCASE thats all
 */

public class PES6JServer {
	// the socket used by the server

	private ServerSocketChannel ssChannel;
	Selector selector;
	boolean debug = false;
	PESConnectionList connections = new PESConnectionList();
	
	private Logger logger;
	private Db db;
	
	ServerInfo[] servers;
	
	// server constructor

	PES6JServer(int port) {
		
		
		/* create socket server and wait for connection requests */
		try
		
		{
			logger = new Logger( "log/pes6j_SERVER.log" );
			Configuration.initializeServerProperties();
			debug = Configuration.serverProperties.getProperty("debug").trim().toLowerCase().equals("on");
			
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(port));
			
			selector = Selector.open();
			ssChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			logger.log("PES6JServer waiting for client on port "
					+ port);
			
			try {
				initDB();
			} catch( SQLException ex ) {
				ex.printStackTrace();
				return;
			}
			while (true) {
				while ((selector.select(5000)) > 0) {
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
					
					/*
					 * Comprobamos si tenemos a algun jugador zombie
					 */
					long now = (new Date()).getTime();
					Enumeration<PESConnection> cons = connections.elements();
					while( cons.hasMoreElements() ) {
						PESConnection con1 = cons.nextElement();
						SelectionKey sk1 = con1.getSocketChannel().keyFor( selector );
						if( now - con1.getLastKATime() > 60000L ) {
							connections.removeConnection(con1);				
							if( sk1 != null ) {
								sk1.cancel();
							}
							con1.log( "TIMED OUT, NO KEEP ALIVE REPLY ONE MINUTE" );
							con1.close();
							
						} else {
							if( con1.hasRemaining() ) con1.write();
							if(!con1.hasRemaining()) sk1.interestOps(SelectionKey.OP_READ);
							else sk1.interestOps(SelectionKey.OP_WRITE);
						}
					}
				}
			}
			
		}
		
		catch (IOException e) {
			System.err.println("Exception on new ServerSocket: " + e);
			e.printStackTrace();
		}
		
	}
	
	void initDB() throws IOException, SQLException {
		db = new Db(Configuration.serverProperties);
		db.connect();
		servers = Tools.dbLoadServers(db);
		db.disconnect();
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		PESConnection con = connections.searchConnection(socketChannel.socket());
		con.write();
		if(!con.hasRemaining())key.interestOps(SelectionKey.OP_READ);
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
			PESConnection con = new PESConnection( socket, debug );
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
			//System.out.println(e.getMessage());
			try {
				con.log("Exception reading/writing  Streams: " + e);
				key.cancel();
				socketChannel.close();
				connections.removeConnection( con );
				con.close();
			} catch (Exception ex) {
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	} 
	
	// you must "run" server to have the server run as a console application

	public static void main(String[] arg) {
		// start server on port 1500
		/*
		 * Pruebecillas
		 */

		new PES6JServer(10881);
	}

	/** One instance of this thread will run for each client */

	void processMessageBlock(PESConnection out, MetaMessageBlock mb)
			throws IOException {

		// the socket where to listen/talk

		String address = out.getSocket().getInetAddress().getHostAddress();

		logger.log("[" + address + "] Waiting for command...");

		// read a String (which is an object)

		Message mes;

		int qId = ((MessageBlock) mb).getHeader().getQuery();
		/*
		 * Init, first stage
		 */
		if (qId == 0x00002008 ) {
			out.log("MATCHES INIT CLIENT QUERY ANSWERING INIT SERVER & INFO:");
			mes = new Message();
			mes.addBlock(new MessageBlockZero(++qId));

			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes.addBlock(new MessageBlockInfo(Configuration.serverProperties
					.getProperty("welcome-msg-title"), Configuration.serverProperties
					.getProperty("welcome-msg-body"), ++qId));

			mes.addBlock(new MessageBlockZero(++qId));
			out.streamWriteEncodedNIO(mes);
		} else if (qId == QUERIES.Q2_CLIENT_QUERY /*
		 * &&
		 * ((MessageBlock)mb).getHeader().getSeq() ==
		 * 2
		 */) {
			out.log("MATCHES Q2 CLIENT QUERY ANSWERING Q2 SERVER:");
			mes = new Message();
			mes.addBlock(new MessageBlockQ6((new Date().getTime()), ++qId));
			// mes.addBlock( new MessageBlockQ2( ++qId, 0 ));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());
		} else if (qId == QUERIES.CLOSE_CLIENT_QUERY) {
			logger.log("MATCHES CLOSE CLIENT QUERY CLOSING CONECTION!");
			throw new IOException("Connection closed by client.");
		} else if (qId == QUERIES.SRV_CLIENT_QUERY) {
			logger
					.log("MATCHES SRV CLIENT QUERY ANSWERING SRV SERVER QUERY & SRVLST SERVER:");
			mes = new Message();
			qId = QUERIES.VOID_SERVER_QUERY;
			mes.addBlock(new MessageBlockVoid(qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			// mes.addBlock( new MessageBlockServers(
			// new ServerInfo[] {
			// new ServerInfo( 7, "GROUP-SP/", "10.11.133.16", 0 ),
			// new ServerInfo( 6, "SCHE-SP/", "10.11.133.16", 0 ),
			// new ServerInfo( 4, "QUICK0-SP/", "10.11.133.15", 0 ),
			// new ServerInfo( 4, "QUICK1-SP/", "10.11.133.22", 0 ),
			// new ServerInfo( 8, "MENU03-SP/", "192.168.1.10",
			// 12882 ), // 210.249.144.205:12881
			// new ServerInfo( 3, "Inglees/PS2", "210.249.144.193",
			// 0 ),
			// new ServerInfo( 3, "Italiano/Polaco",
			// "210.249.144.202", 0 )
			// }, ++qId, 0 ));
			// mes.addBlock( new MessageBlockServers(new
			// ServerInfo[] {
			// new ServerInfo( 3, "Inglees/PC", "210.249.144.193", 0
			// ),
			// new ServerInfo( 3, "Cualquiera/PS2",
			// "210.249.144.202", 0 ),
			// new ServerInfo( 3, "Inglees/Francees",
			// "210.249.144.193", 0 ),
			// new ServerInfo( 3, "Cualquiera/PC",
			// "210.249.144.202", 0 ),
			// new ServerInfo( 3, "Francees/PS2", "210.249.144.193",
			// 0 ),
			// new ServerInfo( 3, "Francees/PC", "210.249.144.193",
			// 0 ),
			// new ServerInfo( 3, "Alemaan", "210.249.144.193", 0 ),
			// new ServerInfo( 3, "Alemaan/PC", "210.249.144.193", 0
			// ),
			// new ServerInfo( 3, "Espannol", "210.249.144.193", 0
			// ),
			// new ServerInfo( 5, "CUP0-SP/", "210.249.144.198", 0 )
			// }, qId, 0 ));
			// mes.addBlock( new MessageBlockServers(new
			// ServerInfo[] {
			// new ServerInfo( 5, "CUP1-SP/", "210.249.144.199", 0
			// ),
			// new ServerInfo( 5, "CUP2-SP/", "210.249.144.200", 0
			// ),
			// new ServerInfo( 5, "CUP3-SP/", "210.249.144.201", 0
			// ),
			// new ServerInfo( 5, "CUP4-SP/", "210.249.144.202", 0
			// ),
			// new ServerInfo( 5, "CUP5-SP/", "210.249.144.198", 0
			// ),
			// new ServerInfo( 5, "CUP6-SP/", "210.249.144.199", 0
			// ),
			// new ServerInfo( 5, "CUP7-SP/", "210.249.144.200", 0
			// ),
			// new ServerInfo( 5, "CUP8-SP/", "210.249.144.201", 0 )
			// }, qId, 0 ));
			// mes.addBlock( new MessageBlockServers(new
			// ServerInfo[] {
			// new ServerInfo( 2, "ACCT03-SP/", "192.168.1.10",
			// 12881 ), // 210.249.144.204:12881
			// new ServerInfo( 1, "GATE-SP/", "10.11.133.203", 0 )
			// }, qId, 0 ));
			
			Vector<ServerInfo> serverList = new Vector<ServerInfo>();
			serverList.add( new ServerInfo(7, 7, "GROUP-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 10887));
			serverList.add( new ServerInfo(6, 6, "SCHE-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 10887));
			serverList.add( new ServerInfo(0x0190, 4, "QUICK0-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 10887));
			serverList.add( new ServerInfo(0x0191, 4, "QUICK1-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 10887));
			serverList.add( new ServerInfo(0x0323, 8, "MENU03-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 12881));
			
			for( int i = 0; i < servers.length; i++ ) {
					String numPlayersStr = Util.fetchURLContents("http://localhost:"+servers[i].getWPort()+"/query?cmd=getNumPlayers");

				int numPlayers = 0;
				if( numPlayersStr != null ) {
					numPlayers = Integer.parseInt( numPlayersStr );
					servers[i].setNumPlayers(numPlayers);
				}
				serverList.add( servers[i]);
			}
			
//			int i = 0;
//			String serverName;
//			
//			while( (serverName = Configuration.serverProperties.getProperty("server-" + i + "-name")) != null ) {
//				String serverIp = Configuration.serverProperties.getProperty("server-" + i + "-ip");
//				int serverPort = Integer.parseInt( Configuration.serverProperties.getProperty("server-" + i + "-port") );
//				int serverWPort = Integer.parseInt( Configuration.serverProperties.getProperty("server-" + i + "-wport") );
//				int serverId = Integer.parseInt( Configuration.serverProperties.getProperty("server-" + i + "-id"));
//				System.out.println("antes de fetch");
//				String numPlayersStr = Util.fetchURLContents("http://localhost:"+serverWPort+"/query?cmd=getNumPlayers");
////				System.out.println("serverIp: " + serverIp);
////				System.out.println("serverName: " + serverName);
////				System.out.println("serverPort: " + serverPort);
//				int numPlayers = 0;
//				if( numPlayersStr != null ) {
//					numPlayers = Integer.parseInt( numPlayersStr );
////					System.out.println("players: " + numPlayers );
//				} else {
////					System.out.println("no conn, numplayers es: " + numPlayers);
//				}
//				serverList.add( new ServerInfo(serverId, 3, serverName, serverIp, serverPort, numPlayers));
//				i++;
//			}
			
			serverList.add( new ServerInfo(0x00CB, 2, "ACCT03-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 12881));
			serverList.add( new ServerInfo(0x0000, 1, "GATE-SP/", Configuration.serverProperties
							.getProperty("server-ip"), 10887));
			
			mes.addBlock(new MessageBlockServers((ServerInfo[])serverList.toArray(new ServerInfo[1]), ++qId));
			
//			mes.addBlock(new MessageBlockServers(new ServerInfo[] {
//					new ServerInfo(7, "GROUP-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 10887, 0),
//					new ServerInfo(6, "SCHE-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 10887, 0),
//					new ServerInfo(4, "QUICK0-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 10887, 0),
//					new ServerInfo(4, "QUICK1-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 10887, 0),
//					new ServerInfo(8, "MENU03-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 12881, 0), // 210.249.144.205:12881
//					
//					new ServerInfo(3, Configuration.serverProperties.getProperty("server-name"),
//							Configuration.serverProperties.getProperty("server-ip"), Integer.parseInt( Configuration.serverProperties.getProperty("server-port")), numPlayers ),
//					new ServerInfo(5, "CUP0-SP/", "210.249.144.198", 0, 0),
//					// new ServerInfo( 5, "CUP1-SP/",
//					// "210.249.144.199", 0 ),
//					// new ServerInfo( 5, "CUP2-SP/",
//					// "210.249.144.200", 0 ),
//					// new ServerInfo( 5, "CUP3-SP/",
//					// "210.249.144.201", 0 ),
//					// new ServerInfo( 5, "CUP4-SP/",
//					// "210.249.144.202", 0 ),
//					// new ServerInfo( 5, "CUP5-SP/",
//					// "210.249.144.198", 0 ),
//					// new ServerInfo( 5, "CUP6-SP/",
//					// "210.249.144.199", 0 ),
//					// new ServerInfo( 5, "CUP7-SP/",
//					// "210.249.144.200", 0 ),
//					// new ServerInfo( 5, "CUP8-SP/",
//					// "210.249.144.201", 0 ),
//					new ServerInfo(2, "ACCT03-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 12881, 0), // 210.249.144.204:12881
//					new ServerInfo(1, "GATE-SP/", Configuration.serverProperties
//							.getProperty("server-ip"), 10887, 0) }, ++qId));
			mes.addBlock(new MessageBlockVoid(++qId));
			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			// out.write(RESPUESTA_423);
			// seq += 5;
		} else if (qId == 0x00002200) {
			logger
					.log("MATCHES RNKURL CLIENT QUERY ANSWERING RNKURL SERVER QUERY & RNKURLLST SERVER:");

			mes = new Message();
			mes.addBlock(new MessageBlockRnkUrl(0x00002201));

			out.streamWriteEncodedNIO(mes);
			// Util.streamWriteEncodedNIO(out, mes.getBytes());

			mes = new Message();
			mes
					.addBlock(new MessageBlockRnkUrlLst(
							new RankUrlInfo[] {
//									new RankUrlInfo(RankUrlInfo.TYPE_0,
//											"http://pes6web.winning-eleven.net/pes6e2/ranking/we10getrank.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_1,
//											"https://pes6web.winning-eleven.net/pes6e2/ranking/we10getgrprank.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_2,
//											"http://pes6web.winning-eleven.net/pes6e2/ranking/we10RankingWeek.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_3,
//											"https://pes6web.winning-eleven.net/pes6e2/ranking/we10GrpRankingWeek.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_4,
//											"https://pes6web.winning-eleven.net/pes6e2/ranking/we10RankingCup.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_5,
//											"http://www.pes6j.net/server/we10getgrpboard.html"),
//									// new RankUrlInfo(
//									// RankUrlInfo.TYPE_5,
//									// "https://pes6web.winning-eleven.net/pes6e2/grpboard/we10getgrpboard.html"),
//									new RankUrlInfo(RankUrlInfo.TYPE_6,
//											"http://www.pes6j.net/server/we10getgrpinvitelist.html")
									
									new RankUrlInfo(RankUrlInfo.TYPE_0,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10getrank.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_1,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10getgrprank.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_2,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10RankingWeek.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_3,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10GrpRankingWeek.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_4,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10RankingCup.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_5,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10getgrpboard.html"),
									// new RankUrlInfo(
									// RankUrlInfo.TYPE_5,
									// "https://pes6web.winning-eleven.net/pes6e2/grpboard/we10getgrpboard.html"),
									new RankUrlInfo(RankUrlInfo.TYPE_6,
											"http://"+Configuration.serverProperties
											.getProperty("server-ip")+"/PES6J/we10getgrpinvitelist.php")
									
							// new RankUrlInfo( RankUrlInfo.TYPE_6,
							// "https://pes6web.winning-eleven.net/pes6e2/group/we10getgrpinvitelist.html")
							}, 0x00002202));

			mes.addBlock(new MessageBlockZero(0x00002203));
			out.streamWriteEncodedNIO(mes);
		}

		else {
			out.log("UNKNOWN QUERY!");
		}

	}
}