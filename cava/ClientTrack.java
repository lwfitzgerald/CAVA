package cava;

/**
 * This class is effectively a track data structure. It stores information read from the database about each
 * track.
 * @author Ben
 *
 */
public class ClientTrack extends Track {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 7723327010558855521L;
	private String path = null;
    private int albumID;
    private int artistID;
    private int trackNo = 0;
    

    /**
     * Create a new track object
     */
    public ClientTrack(int trackID, String trackName, String path, int albumID, int artistID,int length, int trackNo) {
    	//Path not set to save space
        this.trackID = trackID;
        this.trackName = trackName;
        this.albumID = albumID;
        this.artistID = artistID;
        this.trackLength = length;
        this.trackNo = trackNo;
    }
    
    /**
     * Create a new track object with a playlist track ID provided
     */
    public ClientTrack(int trackID, String trackName, String path, int albumID, int artistID,int length, int trackNo, int playlistTrackID) {
    	//Path not set to save space
        this.trackID = trackID;
        this.trackName = trackName;
        this.albumID = albumID;
        this.artistID = artistID;
        this.trackLength = length;
        this.trackNo = trackNo;
        this.playlistTrackID = playlistTrackID;
    }

    /**
     * Create a new track object with scms provided
     */
    public ClientTrack(int trackID, byte[] scms) {
    	this.trackID = trackID;
    	this.scms = scms;
    }
    
    /**
     * Create a new track object with path information only.
     */
    public ClientTrack(int trackID, String path) {
    	this.trackID = trackID;
    	this.path = path;
    }

    /**
     * Get the path. This method loads the path from the database. Once used, the path should be unset
     * @param db a track database to connect to
     * @return the path of the track or null on failure.
     */
    public String getPath(TrackDatabase db) {
    	if(path==null){
    		return db.getTrackPath(trackID);
    	}else{
    		return path;
    	}
    }

    /**
     * Get the album ID
     * @return the album ID
     */
    public int getAlbumID() {
        return this.albumID;
    }

    /**
     * Get the artist ID -- this is the original artist, not the album artist
     * @return the artist ID
     */
    public int getArtistID() {
        return this.artistID;
    }
    
    public void unsetScms(){
    	//System.out.println("Unsetting SCMS data");
    	this.scms = null;
    }
    
    public int getTrackNo(){
    	return this.trackNo;
    }

    @Override
	public String getArtistName(TrackDatabase db) {
		return db.getArtistName(artistID);
	}
	
	public String getAlbumName(TrackDatabase db) {
	    return db.getAlbumName(albumID);
	}
}