package pes6j.WEBMethods;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import pes6j.datablocks.ChannelList;
import pes6j.datablocks.PlayerInfo;
import pes6j.datablocks.PlayerList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PES6JWebHandler implements HttpHandler {
	ChannelList channels;
	
	public PES6JWebHandler( ChannelList channels ) {
		this.channels = channels;
	}
	
	public Map<String, String> getQueryMap(String query)  
	{  
	     String[] params = query.split("&");  
	     Map<String, String> map = new HashMap<String, String>();  
	     for (String param : params)  
	     {  
	         String name = param.split("=")[0];  
	         String value = param.split("=")[1];  
	         map.put(name, value);  
	     }  
	     return map;
	}  
	
    public void handle(HttpExchange t) throws IOException {
//        InputStream is = t.getRequestBody();
//        read( is );
    	String response = "";
    	URI uri = t.getRequestURI();
    	Map params = getQueryMap(uri.getQuery());
    	if( params.containsKey("cmd") ) {
    		String cmd = (String)params.get("cmd");
    		if( cmd == null ) {
    			response = "ERROR! no command specified";
    			sendOutput( t, response );
    			return;
    		}
    		if( cmd.equals( "getNumPlayers")) {
    			response = "" + channels.getNumPlayers();
    			sendOutput( t, response );
    			return;
    		}
    		if( cmd.equals( "getLobbyList")) {
    			for( int i = 0; i < channels.size(); i++ ) {
    				response += i + "|" + new Integer(channels.getChannel(i).getType()) + "|" + channels.getChannel(i).getName() + "|" +
    					channels.getChannel(i).getPlayerList().getNumPlayers() + "\n";
    			}
    			sendOutput( t, response );
    			return;
    		}
    		if( cmd.equals( "getPlayerList")) {
    			if( params.containsKey("lobby")) {
    				int lobbyId = Integer.parseInt((String)params.get("lobby"));
    				if( lobbyId >= channels.size() ) {
    					response += "That lobby doesn't exist!";
    				}
    				PlayerList pl = channels.getChannel(lobbyId).getPlayerList();
    				for( int i = 0; i < pl.getNumPlayers(); i++ ) {
    					PlayerInfo p = pl.getPlayer(i);
    					response += i + "|" + buildPlayerInfoOutput( p ) + "\n";
    				}
    			} else {
    				int count = 0;
    				for( int j = 0; j < channels.size(); j++ ) {
    					PlayerList pl = channels.getChannel(j).getPlayerList();
        				for( int i = 0; i < pl.getNumPlayers(); i++ ) {
        					PlayerInfo p = pl.getPlayer(i);
        					response += count + "|" + buildPlayerInfoOutput( p ) + "\n";
        					count++;
        				}
    				}
    			}
    		}
    	}
        sendOutput( t, response );
    }

    public String buildPlayerInfoOutput( PlayerInfo p ) {
    	return( p.getId() + "|" + p.getName() + "|" + p.getGid() + "|" + p.getGroupName() +
				"|" + p.getCategory() + "|" + p.getDivision() );
    }
    
    public void sendOutput(HttpExchange t, String output) throws IOException {
    	t.sendResponseHeaders(200, output.length());
        OutputStream os = t.getResponseBody();
        os.write(output.getBytes());
        os.close();
    }
}
