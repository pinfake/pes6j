package pes6j.servers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

import pes6j.datablocks.Message;
import pes6j.datablocks.Util;

public class Logger {
	File f;
	String fileName;
	FileWriter writer;
	
	public Logger( File f ) throws IOException {
		this.f = f;
		fileName = f.getPath();
		open();
	}
	
	public Logger( String fileName ) throws IOException {
		this.fileName = fileName;
		this.f = new File( fileName );
		open();
	}
	
	synchronized void open() throws IOException {
		writer = new FileWriter(f, true);
	}
	
	public synchronized void log(String message) {
		try {
			writer.write("[" + (new Date()).toString() + "] " + message + "\n");
			writer.flush();
		} catch( IOException ex ) {
			System.err.println( "ERROR!: Coultn't write to log - " + fileName );
			ex.printStackTrace();
		}
	}
	
	public synchronized void log(Exception ex) {
		try {
			writer.write("[" + (new Date()).toString() + "] ");
			PrintWriter pw = new PrintWriter( writer );
			ex.printStackTrace( pw );
			writer.write( "\n");
			writer.flush();
		} catch( IOException e ) {
			System.err.println( "ERROR!: Coultn't write to log - " + fileName );
			e.printStackTrace();
		}
	}
	
	public synchronized void log(String message, Message mes) {
		try {
			writer.write("[" + (new Date()).toString() + "] " + message + " DUMP:\n");
			writer.write( Util.toHex(mes.getBytes()) + "\n");
			writer.flush();
		} catch( IOException ex ) {
			System.err.println( "ERROR!: Couldn't write to log - " + fileName );
			ex.printStackTrace();
		}
	}
	
	public synchronized void log(String message, Vector<Message> mes) {
		try {
			writer.write("[" + (new Date()).toString() + "] " + message + " DUMPS:\n");
			for( int i = 0; i < mes.size(); i++ )
				writer.write( Util.toHex(mes.get(i).getBytes()) + "\n");
			writer.flush();
		} catch( IOException ex ) {
			System.err.println( "ERROR!: Couldn't write to log - " + fileName );
			ex.printStackTrace();
		}
	}
	
	public synchronized void close() throws IOException {
		writer.flush();
		writer.close();
	}
}
