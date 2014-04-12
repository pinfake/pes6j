package pes6j.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import pes6j.datablocks.UserInfo;

public class AuthThread extends Thread {
	
	UserInfo uInfo;
	String url;
	String ip_address;
	boolean ready;
	
	AuthThread( String url, String ip_address) {
		ready = false;
		this.url = url;
		this.ip_address = ip_address;
	}
	
	public void getUserInfo() {
		URL u;
		URLConnection uc;
		BufferedReader in = null;
		UserInfo uInfo = new UserInfo( "", 0 );
		try {
			u = new URL(url + "?ip_address=" + ip_address);
			uc = u.openConnection();
			uc.setConnectTimeout(2500);
			uc.setReadTimeout(2500);
	        in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));
			String s;

			s = in.readLine();

			int idx = s.lastIndexOf('|');
			String username = s.substring(0, idx);
			int access = Integer.parseInt(s.substring(idx+1));
			uInfo.setUsername( username );
			uInfo.setAccess( access );
		} catch(MalformedURLException ex ) {
			ex.printStackTrace();
		} catch(IOException ex) {
			ex.printStackTrace();
		} catch(Exception ex ) {
			ex.printStackTrace();
		} finally { 
			try {
				if( in != null ) in.close();
			} catch (IOException ioe) {
			}
		}
	}
	
	public boolean isReady() {
		
		return( ready );
	}
}
