package pes6j.datablocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Stack;

import pes6j.servers.Configuration;
import pes6j.servers.HTTPFetchUserInfoThread;
import pes6j.servers.Logger;


public class PESConnection {

	public static final int DEBUG_MESSAGES_SAVED = 4;
	
	long pid;
	SocketChannel outChannel;
	OutputStream out;
	InputStream in;
	long seq;
	Socket socket;
	Logger logger;
	long requester;
	long recipient;

	Stack<Message> lastMessages;
	PlayerInfo pInfo;
	UserInfo uInfo;
	DataCdKeyAndPass cdkey;
	long lastKATime;
	byte[] readBuffer;
	MyByteBuffer writeBuffer;
	HTTPFetchUserInfoThread uInfoThread;
	boolean logEnabled;
	Message lastReadMessage;
	int debugErrorCode = -1;
	
	public PESConnection(Socket socket, boolean debug) throws IOException {
		seq = 1;
		requester = 0L;
		this.socket = socket;

		outChannel = 
			socket.getChannel();
		
		this.out = socket.getOutputStream();
		this.in = socket.getInputStream();

		logEnabled = debug;
		
		if( logEnabled ) { 
			logger = new Logger("log/pes6j_"
					+ socket.getInetAddress().getHostAddress());
		}
		lastMessages = new Stack<Message>();
		pInfo = null;
		cdkey = null;
		uInfo = null;
		readBuffer = new byte[0];
		lastKATime = (new Date()).getTime();
		pid = 0;
		writeBuffer = new MyByteBuffer( );
		uInfoThread = null;
		lastReadMessage = null;
	}
	
	public long getRecipient() {
		return recipient;
	}

	public void setRecipient(long recipient) {
		this.recipient = recipient;
	}

	public Message getLastReadMessage() {
		return( this.lastReadMessage );
	}
	
	public int getDebugErrorCode() {
		return debugErrorCode;
	}

	public void setDebugErrorCode(int debugErrorCode) {
		this.debugErrorCode = debugErrorCode;
	}

	public void setHTTPThread( HTTPFetchUserInfoThread uInfoThread ) {
		this.uInfoThread = uInfoThread;
	}
	
	public HTTPFetchUserInfoThread getHTTPThread( ) {
		return( uInfoThread );
	}
	
	public boolean isFetchingUserInfo() {
		if( uInfoThread == null ) return false;
		if( uInfoThread.getStatus() == HTTPFetchUserInfoThread.FETCHING ) return( true );
		return( false );
	}
	
	public boolean userInfoFetched() {
		if( uInfoThread == null ) return false;
		if( uInfoThread.getStatus() == HTTPFetchUserInfoThread.FETCHED ) return( true );
		return( false );
	}
	
	public void fetchUserInfo() {
		if( uInfoThread != null && uInfoThread.getStatus() == HTTPFetchUserInfoThread.FETCHED ) 
			uInfo = uInfoThread.getUserInfo();
		uInfoThread = null;
	}
	
	public SocketChannel getSocketChannel() {
		return( this.outChannel );
	}
	
	public UserInfo getUserInfo() {
		return( uInfo );
	}
	
	public void setUserInfo( UserInfo uInfo ) {
		this.uInfo = uInfo;
	}
	
	public long getLastKATime( ) {
		return( lastKATime );
	}
	
	public void setLastKATime(long time) {
		lastKATime = time;
	}
	
	public void setPlayerInfo( PlayerInfo pg ) {
		pInfo = pg;
	}
	
	public PlayerInfo getPlayerInfo( ) {
		return( pInfo );
	}
	
	public void setCdkey( DataCdKeyAndPass cdkey ) {
		this.cdkey = cdkey;
	}
	
	public DataCdKeyAndPass getCdkey( ) {
		return( this.cdkey );
	}
	
	public synchronized Socket getSocket() {
		return( socket );
	}
	
	public synchronized void setRequester( long pid ) {
		requester = pid;
	}
	
	public synchronized long getRequester( ) {
		return( requester );
	}

	public synchronized void incSeq() {
		seq++;
	}

	public synchronized long getSeqPP() {
		return (seq++);
	}

	public synchronized OutputStream getOutputStream() {
		return (out);
	}

	public synchronized void streamWriteEncoded(Message mes) throws IOException {
		for (int i = 0; i < mes.getNumBlocks(); i++) {
			MetaMessageBlock mb = mes.getBlock(i);
			if (mb instanceof MessageBlockMulti) {
				for (int j = 0; j < ((MessageBlockMulti) mb).getNumBlocks(); j++) {
					MessageBlock b = ((MessageBlockMulti) mb).getBlock(j);
					GenericHeader h = b.getHeader();
					h.setSeq(seq++);
					b.setHeader(h);
					((MessageBlockMulti) mb).setBlock(j, b);
				}
			} else {
				GenericHeader h = ((MessageBlock) mb).getHeader();
				h.setSeq(seq++);
				((MessageBlock) mb).setHeader(h);
			}
			mes.setBlock(i, mb);
		}
		if( lastMessages.size() >= DEBUG_MESSAGES_SAVED )
			lastMessages.remove(0);
		
		lastMessages.push(mes);
		
		//Arrays.push( lastMessages = mes;
		Util.streamWriteEncoded(out, mes.getBytes());
		/*
		 * Es muy salvaje, no podemos hacer esto.
		 */
		//log( "WRITE: ", mes );
	}
	
	public boolean hasRemaining() {
		return writeBuffer.hasRemaining();
	}
	
	public boolean canWrite() {
		return( (outChannel.validOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE );
	}
	
	public int write() throws IOException {
		
		
		if( outChannel.isConnected() && canWrite() ) {	
			ByteBuffer buf = ByteBuffer.wrap( writeBuffer.get() );
			//writeBuffer.flip();
			
			/*
			 * TENGO MULTIPLES EXCEPCIONES DE BROKEN PIPE, DEBERÍA CONTROLARLAS
			 */
	
			int ret = outChannel.write( buf );
			if( ret > 0 ) writeBuffer.removeHead( ret );
			return( ret );
		} else {
			log( "Can't send data, either lost connection or just can't write to him" );
			return( 0 );
		}
	}
	
	public void writeToBuffer( byte[] data ) throws IOException {
		writeBuffer.put( data );
	}
	
	public synchronized void streamWriteEncodedNIO(Message mes) throws IOException {		
		for (int i = 0; i < mes.getNumBlocks(); i++) {
			MetaMessageBlock mb = mes.getBlock(i);
			if (mb instanceof MessageBlockMulti) {
				for (int j = 0; j < ((MessageBlockMulti) mb).getNumBlocks(); j++) {
					MessageBlock b = ((MessageBlockMulti) mb).getBlock(j);
					GenericHeader h = b.getHeader();
					h.setSeq(seq++);
					writeToBuffer( Util.konamiEncode(b.getBytes()) );
				}
			} else {
				GenericHeader h = ((MessageBlock) mb).getHeader();
				h.setSeq(seq++);

				writeToBuffer( Util.konamiEncode(mb.getBytes()) );
			}
		}
		if( lastMessages.size() >= DEBUG_MESSAGES_SAVED )
			lastMessages.remove(0);
		
		lastMessages.push(mes);
	}

	public synchronized void logLastSentMessage(String message) throws IOException {
		if( logEnabled ) logger.log( message, lastMessages );
	}
	
	public synchronized void log(String message, Message mes) throws IOException {
		if( logEnabled ) logger.log(message, mes);
	}
	
	public synchronized void log( Exception ex ) throws IOException {
		if( logEnabled ) logger.log( ex );
	}
	
	public synchronized void log(String message) throws IOException {
		if( logEnabled ) logger.log( message );
	}

	public synchronized void streamWriteBlockEncoded(MessageBlock mb) throws IOException {
		GenericHeader h = mb.getHeader();
		h.setSeq(seq++);
		mb.setHeader(h);
		Util.streamWriteEncoded(out, mb.getBytes());
	}
	
	public synchronized byte[] streamReadDecoded() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
			//byte buf[] = new byte[4096];
			int len = outChannel.read(buf);

			if (len == -1) throw new IOException( "Read -1 bytes, connection closed" );

			
			byte ret[] = new byte[len];
			System.arraycopy( buf.array(), 0, ret, 0, len);
			
			return (Util.konamiDecode(ret));
	}
	
	public synchronized byte[] streamRead() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
			//byte buf[] = new byte[4096];
			int len = outChannel.read(buf);

			if (len == -1) throw new IOException( "Read -1 bytes, connection closed" );

			
			byte ret[] = new byte[len];
			System.arraycopy( buf.array(), 0, ret, 0, len);
			
			return (ret);
	}
	
//	public synchronized Message networkReadMessage() throws IOException {
//		byte[] buf = streamReadDecoded();
//		
//		log("READ:");
//		log(Util.toHex(buf));
//		/*
//		 * Si teniamos algo leido anteriormente en el buffer interno lo
//		 * concatenamos
//		 */
//		byte[] full = new byte[readBuffer.length + buf.length];
//		System.arraycopy( readBuffer, 0, full, 0, readBuffer.length );
//		System.arraycopy( buf, 0, full, readBuffer.length, buf.length );
//		
//		Message mes = Message.getInstance(full);
//		if( mes != null ) {
//			readBuffer = new byte[0];
//		} else
//			readBuffer = full;
//		return( mes );
//	}
	
	public synchronized Message networkReadMessage() throws IOException {
		byte[] buf = streamRead();
		byte[] fullDecoded;
		

		/*
		 * Si teniamos algo leido anteriormente en el buffer interno lo
		 * concatenamos
		 */
		byte[] full = new byte[readBuffer.length + buf.length];
		System.arraycopy( readBuffer, 0, full, 0, readBuffer.length );
		System.arraycopy( buf, 0, full, readBuffer.length, buf.length );
		
		fullDecoded = Util.konamiDecode(full);
		
		Message mes = Message.getInstance(fullDecoded);
		if( mes != null ) {
			readBuffer = new byte[0];
			log("READ:");
			log(Util.toHex(fullDecoded));
			this.lastReadMessage = mes;
		} else
			readBuffer = full;
		return( mes );	
	}
	
	

	public synchronized void close() throws IOException {
		try {
			out.close();
			outChannel.close();
			if( logEnabled ) logger.close();
		} catch( Exception ex ) {
			
		}
	}

	public synchronized void setPid(long pid) {
		this.pid = pid;
	}

	public synchronized long getPid() {
		return (pid);
	}
}
