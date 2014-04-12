package pes6j.servers;

//The server code Server.java:
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;

import pes6j.datablocks.Message;
import pes6j.datablocks.MessageBlock;
import pes6j.datablocks.MessageBlockInfo;
import pes6j.datablocks.MessageBlockQ2;
import pes6j.datablocks.MessageBlockQ6;
import pes6j.datablocks.MessageBlockRnkUrl;
import pes6j.datablocks.MessageBlockRnkUrlLst;
import pes6j.datablocks.MessageBlockServers;
import pes6j.datablocks.MessageBlockVoid;
import pes6j.datablocks.MessageBlockZero;
import pes6j.datablocks.MetaMessageBlock;
import pes6j.datablocks.PESConnection;
import pes6j.datablocks.QUERIES;
import pes6j.datablocks.RankUrlInfo;
import pes6j.datablocks.ServerInfo;
import pes6j.datablocks.Util;

/**
 * This is to help people to write Client server application I tried to make it
 * as simple as possible... the client connect to the server the client send a
 * String to the server the server returns it in UPPERCASE thats all
 */

public class PES6JServerThreaded {
	// the socket used by the server

	private ServerSocket serverSocket;
	private Properties properties;
	private Logger logger;
	
	void loadProperties() throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream("config/properties")); //$NON-NLS-1$
	}

	void loadPackets() throws IOException {

	}

	// server constructor

	PES6JServerThreaded(int port) {

		
		/* create socket server and wait for connection requests */
		try

		{
			logger = new Logger( "log/pes6j_SERVER.log" );
			loadPackets();
			loadProperties();
			serverSocket = new ServerSocket(port);

			logger.log("PES6JServer waiting for client on port "
					+ serverSocket.getLocalPort());

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
			System.err.println("Exception on new ServerSocket: " + e);
			e.printStackTrace();
		}

	}

	// you must "run" server to have the server run as a console application

	public static void main(String[] arg) {
		// start server on port 1500
		/*
		 * Pruebecillas
		 */

		new PES6JServerThreaded(10881);
	}

	/** One instance of this thread will run for each client */

	public class TcpThread extends Thread {

		// the socket where to listen/talk

		Socket socket;
		String address;

		InputStream in;
		PESConnection out;

		TcpThread(Socket socket) {
			this.socket = socket;
			this.address = socket.getInetAddress().getHostAddress();
		}

		public void run() {

			try {
				// create output first
				out = new PESConnection(socket, false);
				// out = socket.getOutputStream();
				// out.flush();
				in = socket.getInputStream();
			}

			catch (IOException e) {

				logger.log("Exception creating new Input/output Streams: "
								+ e);
				return;
			}

			logger.log("[" + address + "] Waiting for command...");

			// read a String (which is an object)

			try {
				Message mes;
				MetaMessageBlock mb;
				int seq = 1;
				byte[] input;

				while ((input = Util.streamReadDecoded(in)) != null) {
					mes = new Message(input);
					mb = mes.getBlock(0);

					int qId = ((MessageBlock) mb).getHeader().getQuery();
					long sq = ((MessageBlock) mb).getHeader().getSeq();
					/*
					 * Init, first stage
					 */
					if (qId == QUERIES.INIT_CLIENT_QUERY) {
						out.log("MATCHES INIT CLIENT QUERY ANSWERING INIT SERVER & INFO:");
						mes = new Message();
						mes.addBlock(new MessageBlockZero(++qId));

						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes
								.addBlock(new MessageBlockInfo(
										properties.getProperty( "welcome-msg-title" ),
										properties.getProperty( "welcome-msg-body" ),
										++qId));
//						mes.addBlock(new MessageBlockInfo(
//								"About discontinuing service:",
//								"I really need some sleep...", qId, seq++));
						mes.addBlock(new MessageBlockZero(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.Q2_CLIENT_QUERY /*
																 * &&
																 * ((MessageBlock)mb).getHeader().getSeq() ==
																 * 2
																 */) {
						out.log( "MATCHES Q2 CLIENT QUERY ANSWERING Q2 SERVER:");
						mes = new Message();
						mes.addBlock(new MessageBlockQ6((new Date().getTime()),
								++qId));
						// mes.addBlock( new MessageBlockQ2( ++qId, seq++ ));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());
					} else if (qId == QUERIES.CLOSE_CLIENT_QUERY) {
						logger.log("MATCHES CLOSE CLIENT QUERY CLOSING CONECTION!");
						break;
					} else if (qId == QUERIES.SRV_CLIENT_QUERY) {
						logger.log("MATCHES SRV CLIENT QUERY ANSWERING SRV SERVER QUERY & SRVLST SERVER:");
						mes = new Message();
						qId = QUERIES.VOID_SERVER_QUERY;
						mes.addBlock(new MessageBlockVoid(qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

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
						// }, ++qId, seq++ ));
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
						// }, qId, seq++ ));
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
						// }, qId, seq++ ));
						// mes.addBlock( new MessageBlockServers(new
						// ServerInfo[] {
						// new ServerInfo( 2, "ACCT03-SP/", "192.168.1.10",
						// 12881 ), // 210.249.144.204:12881
						// new ServerInfo( 1, "GATE-SP/", "10.11.133.203", 0 )
						// }, qId, seq++ ));

						mes.addBlock(new MessageBlockServers(new ServerInfo[] {
								new ServerInfo(7, "GROUP-SP/", properties
										.getProperty("server-ip"), 10887, 0),
								new ServerInfo(6, "SCHE-SP/", properties
										.getProperty("server-ip"), 10887, 0),
								new ServerInfo(4, "QUICK0-SP/", properties
										.getProperty("server-ip"), 10887, 0),
								new ServerInfo(4, "QUICK1-SP/", properties
										.getProperty("server-ip"), 10887, 0),
								new ServerInfo(8, "MENU03-SP/", properties
										.getProperty("server-ip"), 12881, 0), // 210.249.144.205:12881
								new ServerInfo(3, properties.getProperty("server-name"), properties
										.getProperty("server-ip"), 10887, 0),
								new ServerInfo(5, "CUP0-SP/",
										"210.249.144.198", 0, 0),
								// new ServerInfo( 5, "CUP1-SP/",
								// "210.249.144.199", 0 ),
								// new ServerInfo( 5, "CUP2-SP/",
								// "210.249.144.200", 0 ),
								// new ServerInfo( 5, "CUP3-SP/",
								// "210.249.144.201", 0 ),
								// new ServerInfo( 5, "CUP4-SP/",
								// "210.249.144.202", 0 ),
								// new ServerInfo( 5, "CUP5-SP/",
								// "210.249.144.198", 0 ),
								// new ServerInfo( 5, "CUP6-SP/",
								// "210.249.144.199", 0 ),
								// new ServerInfo( 5, "CUP7-SP/",
								// "210.249.144.200", 0 ),
								// new ServerInfo( 5, "CUP8-SP/",
								// "210.249.144.201", 0 ),
								new ServerInfo(2, "ACCT03-SP/", properties
										.getProperty("server-ip"), 12881, 0), // 210.249.144.204:12881
								new ServerInfo(1, "GATE-SP/", properties
										.getProperty("server-ip"), 10887, 0) },
								++qId));
						mes.addBlock(new MessageBlockVoid(++qId));
						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						// out.write(RESPUESTA_423);
						// seq += 5;
					} else if (qId == 0x00002200) {
						logger.log("MATCHES RNKURL CLIENT QUERY ANSWERING RNKURL SERVER QUERY & RNKURLLST SERVER:");

						mes = new Message();
						mes.addBlock(new MessageBlockRnkUrl(0x00002201));

						out.streamWriteEncoded(mes);
						// Util.streamWriteEncoded(out, mes.getBytes());

						mes = new Message();
						mes
								.addBlock(new MessageBlockRnkUrlLst(
										new RankUrlInfo[] {
												new RankUrlInfo(
														RankUrlInfo.TYPE_0,
														"http://pes6web.winning-eleven.net/pes6e2/ranking/we10getrank.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_1,
														"https://pes6web.winning-eleven.net/pes6e2/ranking/we10getgrprank.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_2,
														"http://pes6web.winning-eleven.net/pes6e2/ranking/we10RankingWeek.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_3,
														"https://pes6web.winning-eleven.net/pes6e2/ranking/we10GrpRankingWeek.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_4,
														"https://pes6web.winning-eleven.net/pes6e2/ranking/we10RankingCup.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_5,
														"http://www.pes6j.net/server/we10getgrpboard.html"),
												// new RankUrlInfo(
												// RankUrlInfo.TYPE_5,
												// "https://pes6web.winning-eleven.net/pes6e2/grpboard/we10getgrpboard.html"),
												new RankUrlInfo(
														RankUrlInfo.TYPE_6,
														"http://www.pes6j.net/server/we10getgrpinvitelist.html")
										// new RankUrlInfo( RankUrlInfo.TYPE_6,
										// "https://pes6web.winning-eleven.net/pes6e2/group/we10getgrpinvitelist.html")
										}, 0x00002202));

						mes.addBlock(new MessageBlockZero(0x00002203));
						FileOutputStream fout = new FileOutputStream(
								"2202-2203_mio.dec");
						fout.write(mes.getBytes());
						fout.close();
						out.streamWriteEncoded(mes);
					}

					else {
						out.log("no es nada previsto pepe");
					}
				}
			}

			catch (IOException e) {

				logger.log("Exception reading/writing  Streams: " + e);

				return;

			}

			finally {
				try {
					out.close();
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}
}