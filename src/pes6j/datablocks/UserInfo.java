package pes6j.datablocks;

public class UserInfo {
	String username;
	int access;
	
	public UserInfo( String username, int access ) {
		this.username = username;
		this.access = access;
	}
	
	public String getUsername() {
		return( username );
	}
	
	public int getAccess() {
		return( access );
	}
	
	public void setUsername( String username ) {
		this.username = username;
	}
	
	public void setAccess( int access ) {
		this.access = access;
	}
}
