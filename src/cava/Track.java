package cava;

import java.io.Serializable;

import cava.server.ServerTrack;

public abstract class Track implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8029976985258647956L;
	protected int trackID;
	protected String trackName;
	protected byte[] scms = null;
	protected int trackLength = 0;
	protected int playlistTrackID = -1;//Only used for tracks with a playlist ID
	
	protected Track(){
		
	}
	
	public int getTrackID(){
		return this.trackID;
	}
	
	public String getTrackName(){
		return this.trackName;
	}
	
	public boolean isClientTrack(){
		if(this instanceof ClientTrack){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean isServerTrack(){
		return !isClientTrack();
	}
	
	public byte[] getScms(){
		return scms;
	}
	
	public byte[] getScms(TrackDatabase db){
		if(this instanceof ClientTrack && scms==null){
			scms = db.getSCMSFromTrack(trackID);
		}
		return getScms();
	}
	
	public String getLengthAsString(){
    	int mins = (int) Math.floor(trackLength/60);
    	int seconds = trackLength % 60;
    	return mins + ":" + ((seconds < 10) ? "0" + seconds : seconds);     
	}
	
	public int getLength(){
		return this.trackLength;
	}
	
	public int getPlaylistTrackID(){
		return this.playlistTrackID;
	}
	
	public abstract String getArtistName(TrackDatabase db);
	
	public static boolean TracksAreSame(Track a, Track b){
		if(a instanceof ClientTrack && b instanceof ClientTrack){
			return true;
		}
		if(a instanceof ServerTrack && b instanceof ServerTrack){
			return true;
		}		
		return false;
		
	}
	
	
}
