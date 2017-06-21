package pes6j.datablocks;

public class ChannelInfo {
	byte type;
	String name;
	PlayerList playerList;
	RoomList roomList;
	String continent;
	int id;
	
	public static byte TYPE_MENU = (byte) 0x1F;
	public static byte TYPE_PC_CHANNEL = (byte) 0x5F;
	public static byte TYPE_COMMON_FRIENDLY = (byte) 0x3F; 
	public static byte TYPE_PS2_CHANNEL = (byte) 0X9F;

	public ChannelInfo(byte type, String name, String continentCode ) {
		this.name = name;
		this.type = type;
		this.playerList = new PlayerList();
		this.roomList = new RoomList();
		this.continent = continentCode;
	}

	public void setId( int id ) {
		this.id = id;
	}
	
	public int getId() {
		return( this.id );
	}
	
	public String getName() {
		return (name);
	}

	public byte getType() {
		return (type);
	}
	
	public PlayerList getPlayerList() {
		return( playerList );
	}
	
	public RoomList getRoomList() {
		return( roomList );
	}
	
	public boolean inContinentList( String countryCode ) {
		if( continent == null ) return true;
		return( continent.indexOf( countryCode ) != -1 );
	}
}
