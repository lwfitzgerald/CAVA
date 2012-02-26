package cava;

import cava.lyrics.LyricMatcher;
import cava.lyrics.Matcher;

/**
 * Controls the data for the track table in the browser class
 * @author Ben
 */
@SuppressWarnings("serial")
public class TrackTableModel extends BrowserTableModel {
	private static String[] defaultColumnHeaders = {"Track", "Name","Artist","Album","Length"}; 
	private TrackDatabase db;
	private Track[] tracks=null;
	private int currentArtistID = 0;
	private int currentAlbumID = 0;
	private ArtistContainer artists;
	private AlbumContainer albums;

	/**
	 * Create a new model of the table based on the supplied artist and album containers
	 * @param artists the artist container assoicated with the artist table model
	 * @param albums the album container associated with the album table model
	 */
	public TrackTableModel(ArtistContainer artists, AlbumContainer albums){
		db = new TrackDatabase();
		this.artists = artists;
		this.albums = albums;

		if(db.isConnected()){
			//override column names in parent class
			columnNames = defaultColumnHeaders;
			setTracks(0,0,null,false);
		}
	}

	/**
	 * Update the artist ID to narrow the display of music. If set to 0, all will be displayed
	 * @param artistID the artist ID to narrow the search by
	 */
	public void updateArtistID(int artistID){
		//	if(currentArtistID != artistID){
		this.currentArtistID = artistID;
		setTracks(currentArtistID, currentAlbumID,null,false);
		//	}
	}

	/**
	 * Update the album ID to narrow the display of music. If set to 0, all will be displayed
	 * @param albumID the album ID to narrow the search by
	 */
	public void updateAlbumID(int albumID){
		//if(currentAlbumID != albumID){
		this.currentAlbumID = albumID;
		setTracks(currentArtistID, currentAlbumID,null,false);
		//	}
	}

	public ClientTrack[] updateSearchString(String searchString, int artistID, int albumID){
		currentArtistID = artistID;
		currentAlbumID = albumID;
		setTracks(artistID, albumID,searchString,false);
		return (ClientTrack[]) tracks;
	}
	
	public ClientTrack[] runLyricSearch(String searchString, int artistID, int albumID){
		currentArtistID = artistID;
		currentAlbumID = albumID;
		int maxLength = Matcher.Match_MaxBits;
		if(searchString.length() > maxLength){
			searchString  = searchString.substring(0,maxLength);
		}
		System.out.println("Running lyric search with string "+searchString);
		setTracks(artistID,albumID,searchString,true);
		return (ClientTrack[]) tracks;
	}

	/**
	 * Update both the album and artist ID at the same time. If either are set to 0, all of the
	 * relevent items will be displayed
	 * @param artistID the updated artistID
	 * @param albumID the updated albumID
	 */
	public void updateArtistIDAndAlbumID(int artistID,int albumID){
		//if(currentArtistID != artistID || currentAlbumID != albumID){
		this.currentArtistID = artistID;
		this.currentAlbumID = albumID;
		setTracks(currentArtistID, currentAlbumID,null,false);

		//}
	}

	/**
	 * Internal method to set the tracks used by the table
	 * @param artistID the artistID to limit by
	 * @param albumID the albumID to limit by
	 */
	private void setTracks(int artistID, int albumID,String searchString,boolean lyricSearch) {
		//First wipe the tracks;
		tracks = null;
		
		if(lyricSearch){
			//Run a lyric search instead
			LyricMatcher matcher = new LyricMatcher();
			tracks = (ClientTrack[]) matcher.matchLyrics(searchString, artistID, albumID).toArray(new ClientTrack[0]);			
		}else if(searchString != null){
			tracks = db.getTracksLike(searchString,artistID,albumID);
		}else if(artistID==0 && albumID ==0){
			tracks = db.getAllTracks();
		}else if(albumID==0){
			//albumID = 0, but artist does not. load based on artist
			tracks = db.getTracksByArtist(artistID);
		}else if(artistID==0){
			//artistID = 0, but album does not. load based on album
			tracks = db.getTracksByAlbum(albumID);
		}else{
			//neither 0. Load based on that
			tracks = db.getTracksByArtistAndAlbum(artistID,albumID);		
		}
		setNumTracks();
	}

	/** Track display should be updated, but current selections and
	 * searches shouldn't be affected. If possible?
	 */
	public void tracksImported(){
		setTracks(currentArtistID, currentAlbumID, null,false);
	}

	/**
	 * Internal method to set the number of rows in the table
	 * @param artistID the artistID to limit by
	 * @param albumID the albumID to limit by
	 */
	private void setNumTracks(){
		if(tracks == null){
			setRowCount(0);
		}else{
			setRowCount(tracks.length);
		}
	}

	/**
	 * Get an array of the tracks currently being displayed. Used by the
	 * Browser class in searching. 
	 * @return the tracks currently displayed in the browser
	 */
	public Track[] getTracks(){
		return this.tracks;
	}

	/**
	 * Explicitly set the tracks to be displayed. Used when a search is in progress
	 * and the "all" button is clicked.
	 * @param tracks an array of tracks to be displayed
	 */
	public void setTracks(ClientTrack[] tracks){
		//First wipe the tracks
		tracks = null;
		this.tracks = tracks;
		setNumTracks();
	}

	@SuppressWarnings("unchecked")
    @Override
	public Class getColumnClass(int columnIndex){
		switch(columnIndex){
		case 0:
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
				case -1:
					return tracks[rowIndex];
				case 0:
					if(tracks[rowIndex] instanceof ClientTrack){
						return ((ClientTrack)tracks[rowIndex]).getTrackNo();
					}
				case 1:
					return tracks[rowIndex].getTrackName();
				case 2:
					if(tracks[rowIndex] instanceof ClientTrack){
						return artists.getArtistByID(((ClientTrack)tracks[rowIndex]).getArtistID()).getArtistName();
					}else{
						return -1;
					}
				case 3:
					if(tracks[rowIndex] instanceof ClientTrack){
						return albums.getAlbumByID(((ClientTrack)tracks[rowIndex]).getAlbumID()).getAlbumName();
					}else{
						return -1;
					}
				case 4:
					return tracks[rowIndex].getLengthAsString();
				}
			}catch(Exception e){
				return " ";
			}
		}
		return " ";
	}
}
