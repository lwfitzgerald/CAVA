package cava;
/**
 * Controls the data for the playlist table in the browser class
 * @author Ben
 */
@SuppressWarnings("serial")
public class PlaylistTableModel extends BrowserTableModel{
	private static String[] defaultColumnHeaders = {"Playlists"}; 
	private TrackDatabase db;
	private Playlist[] playlists=null;
	private NowPlaying nowPlaying;
	private Playlist currentPlaylist;
	private PlaylistTracksTableModel playlistTracks;

	/**
	 * Create a new model of the playlist
	 * @param artists the artist container assoicated with the artist table model
	 * @param albums the album container associated with the album table model
	 * @param tracks the track table model associated with the track table
	 */
	public PlaylistTableModel(NowPlaying nowPlaying){
		db = new TrackDatabase();
		this.nowPlaying = nowPlaying;
		if(db.isConnected()){
			//override column names in parent class
			columnNames = defaultColumnHeaders;
			setPlaylists();
		}
		currentPlaylist = nowPlaying;
	}

	public void setNowPlayingTracks(ClientTrack[] tracks){
		nowPlaying.setTracks(tracks);
	}

	public Playlist getNowPlaying(){
		return nowPlaying;
	}

	/**
	 * Internal method to set the playlists used by the table
	 */
	public void setPlaylists() {
		//System.out.println("Setting playlists");
		playlists = db.getAllPlaylists();
		setNumPlaylists();
	}

	/**
	 * Get all the playlists being displayed
	 * @return an array of playlists currently being displayed
	 */
	public Playlist[] getPlaylists(){
		return playlists;
	}

	/**
	 * Get the ID of the playlist
	 * @param row the row of the table the playlist is in
	 * @return the playlist ID or -1 for "All" and "Now Playing"
	 */
	public int getPlaylistIDFromRow(int row){
		Playlist p = getPlaylistFromRow(row);
		if(p==null){
			return -1;
		}else{
			return p.getPlaylistID();
		}
	}

	/**
	 * Get the playlist at the given row
	 * @param row the row of the table the playlist is in
	 * @return the playlist or NULL for "All" and "Now Playing"
	 */
	public Playlist getPlaylistFromRow(int row){
		if(row == 0){
			return nowPlaying;
		}
		try{
			return playlists[row-1];
		}catch(ArrayIndexOutOfBoundsException e){
			return null;
		}
	}

	/**
	 * Internal method to set the number of rows in the table
	 */
	private void setNumPlaylists(){
		//Plus one for now playing
		if(playlists == null){
			setRowCount(1);
		}else{
			setRowCount(playlists.length + 1);	
		}
	}

	public void addToPlaylist(int playlistID, ClientTrack[] tracks) {
		if(playlistID==-1){
			nowPlaying.addTracks(tracks);
		}else{
			int[] trackIDs = new int[tracks.length];
			for(int i=0; i < tracks.length ; i++){
				trackIDs[i] = tracks[i].getTrackID();
			}
			db.addLocalTracksToPlaylist(trackIDs,playlistID);
			//TODO: A better way of refreshing the view would be nicer. Currently this queries the database for all playlists.
			setPlaylists();
		}

	}
	
	@Override
	public void fireTableDataChanged(){
		super.fireTableDataChanged();
//		System.out.println("Playlist ID: " + currentPlaylist.getPlaylistID());
		playlistTracks.setTracks(currentPlaylist.getTracksInPlaylist(db));
		playlistTracks.fireTableDataChanged();
	}

	public void createNewPlaylist(int[] trackIDs,String playlistName){
		db.createNewLocalPlaylist(trackIDs, playlistName);
		//TODO: A better way of refreshing the view would be nicer. Currently this queries the database for all playlists.
		setPlaylists();
	}
	
	public void createNewPlaylist(ClientTrack[] tracks,String playlistName){
		int[] trackIDs = new int[tracks.length];
		for(int i=0;i<tracks.length;i++){
			trackIDs[i] = tracks[i].getTrackID();
		}
		createNewPlaylist(trackIDs, playlistName);
	}

	//remove from playlist. If currently playing, then return true so that playback can be stopped.
	public boolean removeFromPlaylist(int playlistID, int row) {
		//TODO: This needs to be improved to support re-ordering of the tracks. It shouldn't be based on row, but basing on
		//trackID isn't sufficient as a track could be in the playlist twice
		if(playlistID==-1){
			setPlaylists();
			return nowPlaying.removeTrack(row);
		}
		setPlaylists();
		return false;

	}
	
	public boolean removeFromPlaylist(int playlistID, int[] rows) {
		//TODO: This needs to be improved to support re-ordering of the tracks. It shouldn't be based on row, but basing on
		//trackID isn't sufficient as a track could be in the playlist twice
		if(playlistID==-1){
			setPlaylists();
			return nowPlaying.removeTrack(rows);
		}else{
			for(int row: rows){
				db.deleteTrackFromPlaylist(playlistID,(Integer)playlistTracks.getValueAt(row, -2));
			}
		}
		setPlaylists();
		return false;

	}

	public void deletePlaylist(int playlistID){
		if(playlistID==-1){
			if(Constants.DEBUG){
				//System.out.println("Cannot delete Now Playing playlist");
			}
			return;
		}
		db.deletePlaylist(playlistID);
		setPlaylists();
	}

	/**
	 * Gets the text to display in the table by showing the number of tracks in the playlist
	 * @param playlist the playlist to get the text to display for
	 * @return the playlist name followed by the number of tracks in the playlist.
	 */
	private String getTextToDisplay(Playlist playlist){
		String text = playlist.getPlaylistName();
		text = text.concat(" ("+playlist.getNumTracks()+")");
		return text;
	}	

	@SuppressWarnings("unchecked")
    @Override
	public Class getColumnClass(int columnIndex){
		switch(columnIndex){
		case -1:
			return ClientTrack[].class;
		case -2:
			return Integer.class;
		case 0:
			return Integer.class;
		default:
			return String.class;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(playlists !=null || nowPlaying != null){
			try{
				switch(columnIndex){
				case -3:
					if(rowIndex == 0){
						return nowPlaying.getPlaylistName();
					}else{
						return playlists[rowIndex-1].getPlaylistName();
					}
				case -2:
					if(rowIndex == 0){
						return -1;
					}else{
						return playlists[rowIndex-1].getPlaylistID();
					}
				case -1:
					if(rowIndex == 0){
						return nowPlaying.getTracksInPlaylist(db);
					}else{
						return playlists[rowIndex-1].getTracksInPlaylist(db);
					}
				case 0:
					if(rowIndex == 0){
						return getTextToDisplay(nowPlaying);
					}else{
						return getTextToDisplay(playlists[rowIndex-1]);
					}
				}
			}catch(Exception e){
				return " ";
			}
		}
		return "";
	}

	public void setCurrentPlaylist(int row) {
		this.currentPlaylist = getPlaylistFromRow(row);

	}

	public Playlist getCurrentPlaylist() {
		return this.currentPlaylist;
	}

	public void setPlaylistTracksTableModel(PlaylistTracksTableModel playlistTracksTableModel) {
		playlistTracks = playlistTracksTableModel;
		
	}

	public void renamePlaylist(int playlistID, String newPlaylistName) {
		if(playlistID != -1){
			db.renamePlaylist(playlistID,newPlaylistName);
			setPlaylists();
		}
	}
}
