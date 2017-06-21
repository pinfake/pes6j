package pes6j.datablocks;

import java.util.Hashtable;

public class GroupList {
	Hashtable<String,GroupInfo> groups;
	
	public GroupList() {
		groups = new Hashtable<String,GroupInfo>();
	}
	
	public GroupInfo searchGroup( long id ) {
		String strId = ""+id;
		return( groups.get(strId));
	}
	
	public void insertGroup( GroupInfo g ) {
		if( searchGroup( g.getId() ) == null )
			groups.put( ""+g.getId(), g );
	}
	
	public void removeGroup( long id ) {
		groups.remove( ""+id );
	}
}
