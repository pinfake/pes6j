package pes6j.datablocks;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

public class RoomPlayerData implements Cloneable {
	PlayerInfo player;
	int[] readyStatus;
	int team;
	int selOrder;
	int teamPos;
	int spect;
	boolean fell;
	int playerPhase;
	String pinCode;
	boolean codeSent;
	boolean watchedScores;
	static final Random randomGenerator = new Random();
	static final DecimalFormat pinCodeFormatter = new DecimalFormat("0000");

	public RoomPlayerData( PlayerInfo p ) {
		player = p;
		reset();
	}
	
	public Object clone() {
		RoomPlayerData rpd = new RoomPlayerData(this.player);
		rpd.setReadyStatus(readyStatus);
		rpd.setTeam(team);
		rpd.setSelOrder(selOrder);
		rpd.setTeamPos(teamPos);
		rpd.setSpect(spect);
		rpd.setPinCode( pinCode );
		rpd.setCodeSent( codeSent );
		rpd.setWatchedScores( watchedScores );
		rpd.setFell( fell );
		rpd.setPlayerPhase(playerPhase);
		return( rpd );
	}
	
	public void setFell( boolean fell ) {
		this.fell = fell;
	}
	
	public boolean getFell( ) {
		return( this.fell );
	}
	
	public void setWatchedScores( boolean val ) {
		this.watchedScores = val;
	}
	
	public boolean getWatchedScores( ) {
		return( this.watchedScores );
	}
	
	public int getPlayerPhase() {
		return ( this.playerPhase );
	}
	
	public void setPlayerPhase( int playerPhase ) {
		this.playerPhase = playerPhase;
	}
	
	public void reset() {
		readyStatus = new int[8];
		resetAllReadyStatus();
		team = 0x000000FF;
		selOrder = 0x00FF;
		teamPos = 0;
		spect = 0;
		pinCode = null;
		codeSent = false;
		watchedScores = false;
		playerPhase = 1;
		fell = false;
	}
	
	public String getPinCode() {
		return( pinCode );
	}
	
	public String generatePinCode() {
		int number = randomGenerator.nextInt(10000);
		pinCode = pinCodeFormatter.format(number);
		return( pinCode );
	}
	
	public void setPinCode( String pinCode ) {
		this.pinCode = pinCode;
	}
	
	public boolean getCodeSent() {
		return( codeSent );
	}
	
	public void setCodeSent( boolean sent) {
		codeSent = sent;
	}
	
	public PlayerInfo getPlayer() {
		return player;
	}
	public void setPlayer(PlayerInfo player) {
		this.player = player;
	}
	public int[] getReadyStatus() {
		return readyStatus;
	}
	public void setReadyStatus(int[] readyStatus) {
		this.readyStatus = readyStatus;
	}
	
	public void setReadyStatusPhase( int readyStatus ) {
		this.readyStatus[playerPhase] = readyStatus;
	}
	
	public void setReadyStatusPhase( int playerPhase, int readyStatus ) {
		this.readyStatus[playerPhase] = readyStatus;
	}
	
	public int getReadyStatusPhase( int phase ) {
		return( this.readyStatus[phase] );		
	}
	
	public void resetAllReadyStatus( ) {
		Arrays.fill(readyStatus, -1);
	}
	public int getTeam() {
		return team;
	}
	public void setTeam(int team) {
		this.team = team;
	}
	public int getSelOrder() {
		return selOrder;
	}
	public void setSelOrder(int selOrder) {
		this.selOrder = selOrder;
	}
	public int getTeamPos() {
		return teamPos;
	}
	public void setTeamPos(int teamPos) {
		this.teamPos = teamPos;
	}
	public int getSpect() {
		return spect;
	}
	public void setSpect(int spect) {
		this.spect = spect;
	}
	
}
