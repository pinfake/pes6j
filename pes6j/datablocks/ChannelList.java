package pes6j.datablocks;

import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import com.maxmind.geoip.LookupService;

public class ChannelList {
	Vector<ChannelInfo> channels;
	
	public ChannelList( ) {
		channels = new Vector<ChannelInfo>();
	}
	
	public void addChannel( ChannelInfo ch ) {
		channels.add( ch );
	}
	
	public ChannelInfo getChannel( int idx ) {
		return( channels.get( idx ));
	}
	
	public int size() {
		return( channels.size() );
	}
	
	public int getChannelForPlayer( long pid ) {
		for( int i = 0; i < size(); i++ ) {
			ChannelInfo chan = getChannel( i );
			if( chan.getPlayerList().getPlayerInfo( pid ) != null ) return( i );
		}
		return( -1 );
	}
	
	public ChannelList getChannelListForPlayer( PESConnection c, Properties country2continent, LookupService cl ) {
		ChannelList list = new ChannelList();
		String countryCode = cl.getCountry(c.getSocket().getInetAddress().getHostAddress()).getCode();
		String continent = country2continent.getProperty( countryCode );
		for( int i = 0; i < size(); i++ ) {
			try {
				c.log( "countryCode: " + countryCode + " continent: " + continent );
			} catch( IOException e ) {
				return list;
			}
			if( continent == null || c.getUserInfo().getAccess() > 1 || getChannel(i).inContinentList( continent )) {
				list.addChannel( getChannel(i));
			}
		}
		return( list );
	}
	
	public int getNumPlayers() {
		int ret = 0;
		for( int i = 0; i < channels.size(); i++ ) {
			ret += getChannel(i).getPlayerList().getNumPlayers();
		}
		return( ret );
	}
}
