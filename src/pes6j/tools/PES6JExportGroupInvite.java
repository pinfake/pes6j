package pes6j.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import pes6j.database.Db;
import pes6j.database.Table;
import pes6j.servers.Logger;
import pes6j.servers.Tools;


public class PES6JExportGroupInvite {
	
	private Db db;
	Properties properties;
	private Logger logger;

	void loadProperties() throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream("config/properties")); //$NON-NLS-1$
	}
	
	void initDB() throws IOException {
		db = new Db(properties);
	}
		
	void initialize() throws IOException {
		logger = new Logger( "log/PES6JExportGroupInvite.log");
		loadProperties();
		initDB();
	}
	
	public static void main(String[] arg) {
		new PES6JExportGroupInvite();
	}
	
	PES6JExportGroupInvite() {
		try {
			initialize();
		} catch( IOException ex ) {
			System.err.println( "Unable to initialize: " + ex.getMessage() );
			ex.printStackTrace();
		}
		try {
			db.connect();
			String outputFile = "export/grpinvitelist.txt";
			logger.log( "Starting Export GroupInviteList process..." );
			Table t = Tools.dbLoadGroupInviteList( db );
			
			File f = new File( outputFile );
			FileWriter fw = new FileWriter( f );
			fw.write("" + t.size() +"\n");
			for( int i = 0; i < t.size(); i++ ) {
				fw.write( "" + t.getLong(i, "gid") + "," + t.get(i, "name") + "," + t.getInt(i, "num_players")+"," +
						t.getInt(i,"slots") + "," + t.getLong(i, "level") + "," + t.get(i, "recruit_comment") + "\n");
			}
			fw.close();
			logger.log( "Export GroupInviteList process finished.");
			db.disconnect();
			
		} catch( Exception ex ) {
			logger.log( "Exception while updating: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
