package pes6j.datablocks;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import com.maxmind.geoip.LookupService;

public class ChannelList {
	Hashtable<Integer,ChannelInfo> channels;
	
	public ChannelList( ) {
		channels = new Hashtable<Integer,ChannelInfo>();
	}
	
	public void addChannel( ChannelInfo ch ) {
		channels.put( ch.getId(), ch );
	}
	
	public ChannelInfo getChannel( int idx ) {
		return( channels.get( idx ));
	}
	
	public int size() {
		return( channels.size() );
	}
	
	public int getNumPlayers() {
		int ret = 0;
		for( int i = 0; i < channels.size(); i++ ) {
			ret += getChannel(i).getPlayerList().getNumPlayers();
		}
		return( ret );
	}
}
