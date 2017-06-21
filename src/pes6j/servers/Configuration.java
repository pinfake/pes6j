package pes6j.servers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

	public static Properties serverProperties;
	public static Properties country2Continent;
	public static Properties authProperties;
	public static Properties gameServerProperties;
	
	public static void initializeServerProperties() throws IOException {
		serverProperties = new Properties();
		serverProperties.load(new FileInputStream("config/server.properties")); //$NON-NLS-1$

	}
	
	public static void initializeAuthProperties() throws IOException {
		authProperties = new Properties();
		authProperties.load(new FileInputStream("config/auth.properties")); //$NON-NLS-1$

	}
	
	public static void initializeGameServerProperties() throws IOException {
		gameServerProperties = new Properties();
		gameServerProperties.load(new FileInputStream("config/gameserver.properties")); //$NON-NLS-1$
		country2Continent = new Properties();
		country2Continent.load(new FileInputStream("config/country2continent.properties"));
	}
	
	public static void initializeGameServerProperties( String configFile ) throws IOException {
		gameServerProperties = new Properties();
		gameServerProperties.load(new FileInputStream(configFile)); //$NON-NLS-1$
		country2Continent = new Properties();
		country2Continent.load(new FileInputStream("config/country2continent.properties"));
	}
}
