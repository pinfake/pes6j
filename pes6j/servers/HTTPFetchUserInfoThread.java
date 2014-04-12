package pes6j.servers;

import pes6j.datablocks.UserInfo;


public class HTTPFetchUserInfoThread extends Thread {
	public final static int FETCHED = 2;
	public final static int FETCHING = 1;
	public final static int IDLE = 0;
	String ip_address;
	String auth_url;
	UserInfo uInfo;
	int status;
	
	HTTPFetchUserInfoThread( String auth_url, String ip_address) {
		this.ip_address = ip_address;
		this.auth_url = auth_url;
		this.uInfo = null;
		this.status = IDLE;
	}
	
	public void run() {
		this.status = FETCHING;
		this.uInfo = Tools.getUserInfo( auth_url, ip_address );
		this.status = FETCHED;
	}
	
	public UserInfo getUserInfo() {
		return( this.uInfo );
	}
	
	public int getStatus() {
		return( this.status );
	}
}
