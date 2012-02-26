package cava;

import cava.server.ServerTrack;

/**
 * Controls the data for the track table in the browser class
 * @author Ben
 */
@SuppressWarnings("serial")
public class PlaylistTracksTableModel extends BrowserTableModel {
	private static String[] defaultColumnHeaders = {"Name","Artist"}; 
	private TrackDatabase db;
	private Track[] tracks=null;
	private PlaylistTableModel playlists;
	private int playlistID;
	private NowPlaying nowPlaying;
	
	/**
	 * Create a new model of the table based on the supplied artist and album containers
	 * @param artists the artist container assoicated with the artist table model
	 * @param albums the album container associated with the album table model
	 */
	public PlaylistTracksTableModel(PlaylistTableModel playlists,NowPlaying nowPlaying){
		db = new TrackDatabase();
		this.playlists = playlists;
		if(db.isConnected()){
			//override column names in parent class
			columnNames = defaultColumnHeaders;
			setPlaylistID(-1);
		}
		this.nowPlaying = nowPlaying;
	}
	
	/**
	 * Internal method to set the tracks used by the table
	 * @param artistID the artistID to limit by
	 * @param albumID the albumID to limit by
	 */
	public void setPlaylistID(int playlistID) {
		this.playlistID = playlistID;
		//System.out.println("Playlist id set to: " + playlistID);
		//Now Playing
		if(playlistID == -1){
			tracks = ((NowPlaying)playlists.getNowPlaying()).getTracksInPlaylist();
		}else{
			tracks = db.getTracksInPlaylist(playlistID);
		}
		setNumTracks();
	}

	private void setNumTracks(){
		if(tracks==null){
			setRowCount(0);
		}else{
			setRowCount(tracks.length);
		}
	}
	
	public void playlistDeleted(int playlistID){
		if(this.playlistID==playlistID){;
			tracks= new ClientTrack[0];
			setNumTracks();
		}
	}
	
	/**
	 * Explicitly set the tracks to be displayed
	 */
	public void setTracks(Track[] tracks){
		this.tracks = tracks;
		setNumTracks();
	}
	
	public void moveTracks(int[] indexesToMove, int insertionPoint) {
		//System.out.println("Current playlist:" + playlistID);
		if(playlistID==-1){
			((NowPlaying)playlists.getNowPlaying()).moveTracks(indexesToMove,insertionPoint);
		}else{
			playlists.getCurrentPlaylist().moveTracks(indexesToMove, insertionPoint, db);	
			//System.err.println("Re-ordering only supported on now playing at the moment");
		}
		//Re-set playlist id to force regeneration of array of tracks
		//First need to notify playlist table of changes
		setPlaylistID(playlistID);
	}
	
	public void addTracks(ClientTrack[] tracks, int indexToAddAt) {
		if(playlistID==-1){
			((NowPlaying)playlists.getNowPlaying()).addTracks(tracks, indexToAddAt);
		}else{
			playlists.getCurrentPlaylist().addTracks(tracks, indexToAddAt, db);
			//Have to reload playlists to regenerate counts
			playlists.setPlaylists();
		}
		//Re-set playlist id to force regeneration of array of tracks
		setPlaylistID(playlistID);
		//Update counts on playlist table
		playlists.fireTableDataChanged();
	}	
	
	@SuppressWarnings("unchecked")
    @Override
	public Class getColumnClass(int columnIndex){
		switch(columnIndex){
		case -3:
			return Boolean.class;
		case -2:
			return Integer.class;
		case -1:
			return Track.class;
		default:
			return String.class;
		}
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(tracks!=null){
			try{
				switch(columnIndex){
				case -3:
					if(playlistID==-1){
						return nowPlaying.isPlaying(rowIndex);
					}else{
						return nowPlaying.isPlaying(tracks[rowIndex]);
					}
				case -2:
					//For nowPlaying, the row Index == playlist track ID.
					if(playlistID==-1){
						return rowIndex;
					}else{
						return tracks[rowIndex].getPlaylistTrackID();
					}
				case -1:
					return tracks[rowIndex];
				case 0:
					return tracks[rowIndex].getTrackName();
				case 1:
					if(tracks[rowIndex] instanceof ClientTrack){
						return ((ClientTrack) tracks[rowIndex]).getArtistName(db);
					}else{
						return ((ServerTrack) tracks[rowIndex]).getArtistName();
					}
				}
			}catch(Exception e){
				e.printStackTrace();
				return "error";
			}
		}
		return "error";
	}
}
