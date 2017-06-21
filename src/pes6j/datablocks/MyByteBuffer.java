package pes6j.datablocks;

public class MyByteBuffer {
	byte[] data;
	
	public MyByteBuffer( ) {
		data = new byte[0];
	}
	
	public void put( byte[] buf ) {
		byte[] newdata = new byte[data.length+buf.length];
		System.arraycopy( data, 0, newdata, 0, data.length);
		System.arraycopy( buf, 0, newdata, data.length, buf.length );
		data = newdata;
	}
	
	public void removeHead( int bytes ) {
		byte[] newdata = new byte[data.length-bytes];
		System.arraycopy( data, bytes, newdata, 0, data.length - bytes );
		data = newdata;
	}
	
	public byte[] get( ) {
		return( data );
	}
	
	public boolean hasRemaining() {
		return( data.length > 0 );
	}
}
