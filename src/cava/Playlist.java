package cava;

import java.util.Arrays;

public class Playlist {
	protected String playlistName;
	protected int numTracks;
	protected int playlistID;	
	
	Playlist(int playlistID, String playlistName, int numTracks){
		this.playlistName = playlistName;
		this.numTracks = numTracks;
		this.playlistID = playlistID;
	}
	
	public String getPlaylistName(){
		return this.playlistName;
	}
	
	public int getNumTracks(){
		return this.numTracks;
	}
	
	public int getPlaylistID(){
		return this.playlistID;
	}
	
	public Track[] getTracksInPlaylist(TrackDatabase db){
		return db.getTracksInPlaylist(this.playlistID);
	}
	
	public void addTracks(ClientTrack[] tracksToAdd, int insertionPoint, TrackDatabase db){
		Track[] oldPlaylist = getTracksInPlaylist(db);
		int[] newTrackIDs = new int [oldPlaylist.length+tracksToAdd.length];
		TrackType[] trackTypes = new TrackType[oldPlaylist.length+tracksToAdd.length];
		boolean allLocal = true;
		boolean allSpotify = true;
		
		int addAt = 0;
		
		for(int i=0;i<=oldPlaylist.length;i++){
			if(i==insertionPoint){
				for(int j=0;j<tracksToAdd.length;j++){
					newTrackIDs[addAt] = tracksToAdd[j].getTrackID();
					if(tracksToAdd[j] instanceof ClientTrack){
						trackTypes[addAt] = TrackType.Local;
						allSpotify = false;
					}else{
						trackTypes[addAt] = TrackType.Spotify;
						allLocal = false;						
					}
					addAt++;
				}
			}
			
			if(i< oldPlaylist.length){
				newTrackIDs[addAt] = oldPlaylist[i].getTrackID();
				if(oldPlaylist[i] instanceof ClientTrack){
					trackTypes[addAt] = TrackType.Local;
					allSpotify = false;
				}else{
					trackTypes[addAt] = TrackType.Spotify;
					allLocal = false;						
				}
				addAt++;
			}
			
		}
		db.recreatePlaylist(newTrackIDs, this.playlistID,allLocal,allSpotify,trackTypes);
	}
	
	public void moveTracks(int[] indexesToMove, int insertionPoint, TrackDatabase db) {
		Arrays.sort(indexesToMove);
		Track[] oldPlaylist = getTracksInPlaylist(db);
		
		int[] newTrackIDs = new int[oldPlaylist.length];
		TrackType[] trackTypes = new TrackType[oldPlaylist.length];
		boolean allLocal = true;
		boolean allSpotify = true;
		//Create the array of track IDs based on the insertion point and indexes to move
		int addAt = 0;
		for(int i=0;i<=oldPlaylist.length;i++){
			//if i = row, this is the insertion point. cyle through indexesToMove and copy them across
			if(i==insertionPoint){
				for(int j=0;j<indexesToMove.length;j++){
					newTrackIDs[addAt] = oldPlaylist[indexesToMove[j]].getTrackID();
					if(oldPlaylist[indexesToMove[j]] instanceof ClientTrack){
						trackTypes[addAt] = TrackType.Local;
						allSpotify = false;
					}else{
						trackTypes[addAt] = TrackType.Spotify;
						allLocal = false;						
					}
					addAt++;
				}
			}
			
			//Add track.get(i) to new tracks if it's not in the list of indexes to move
			//Also check array bounds -- we have to cycle with i untill one after the length, in case we drop at the end of the list.
			
			if(i < oldPlaylist.length && Arrays.binarySearch(indexesToMove, i) < 0){
				newTrackIDs[addAt] = oldPlaylist[i].getTrackID();
				if(oldPlaylist[i].isClientTrack()){
					trackTypes[addAt] = TrackType.Local;
					allSpotify = false;
				}else{
					trackTypes[addAt] = TrackType.Spotify;
					allLocal = false;						
				}
				addAt++;
			}
		}
		//Recreate the playlist with the new trackIDs
		db.recreatePlaylist(newTrackIDs, this.playlistID,allLocal,allSpotify,trackTypes);
		
	}
	
}
