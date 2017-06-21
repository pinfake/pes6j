package pes6j.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import pes6j.database.Db;
import pes6j.servers.Logger;
import pes6j.servers.Tools;


public class PES6JRankUpdater {
	
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
		logger = new Logger( "log/PES6JRankUpdater.log");
		loadProperties();
		initDB();
	}
	
	public static void main(String[] arg) {
		new PES6JRankUpdater();
	}
	
	PES6JRankUpdater() {
		try {
			initialize();
		} catch( IOException ex ) {
			System.err.println( "Unable to initialize: " + ex.getMessage() );
			ex.printStackTrace();
		}
		try {
			db.connect();
			logger.log( "Starting Rank Update process..." );
			Tools.dbUpdateDivisionAndPosition( db );
			logger.log( "Rank Update process finished.");
			db.disconnect();
		} catch( SQLException ex ) {
			logger.log( "Exception while updating: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
