package cava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import cava.Database.CavaResultSet;
import cava.server.ServerTrack;

/**
 * 
 * @author Ben & Peter
 * Provides access to the user's track database
 */
public class TrackDatabase {
    protected Database db;
    private boolean debugging = true;
    int maxInserts = 1000; //Number of inserts allowed before creating a new statement

    /**
     * Connect to the track database
     */
    public TrackDatabase(){
    	//Connect to the track database in the user's home folder
        db = new Database(System.getProperty("user.home") + "/.cava/"+getDBName());
        if(debugging){
        	if(!db.isConnected()){
        		db.printLastError();
        	}
        }
    }
    
    public String getDBName(){
    	return "trackDB";
    }

    /**
     * Gets all albums in the track database
     * @return an album array containing all the albums, null if empty
     */
    public Album[] getAllAlbums(){
        int Albums = getAlbumCount();
        if(Albums < 1){
            return null;
        }
        Album[] allalbums = new Album[Albums];
        CavaResultSet rs = db.select("SELECT albumname,albumid,artistid,artlocation FROM album ORDER BY albumname");
        if(rs!=null){
            int counter = 0;
            while ( counter < Albums){
                if ( !rs.fetchRow() ){
                    return null;
                }
                allalbums[counter] = new Album(rs.fetchString(1),rs.fetchInt(2),rs.fetchInt(3),rs.fetchString(4));
                counter++;
            }
            return allalbums;
        }
        return null;
    }

    /**
     * Count the number of albums in database
     * @return the number of albums listed (albumid), -1 on failure
     */
    public int getAlbumCount(){
    	CavaResultSet rs; 
        if((rs = db.select("SELECT COUNT(*) FROM album")) != null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
    }

    /**
     * Finds all tracks on an album
     * @param albumID the key of the album searched for
     * @return an array of Track objects, null if empty
     */
    public ClientTrack[] getTracksByAlbum( int albumID ){
    	return getTracks(null, -1, albumID);
    }

    /**
     * finds how many tracks there are for a given albumid
     * @param albumid the unique key for the album
     * @return integer, no. of tracks under albumid
     */
    public int getTracksByAlbumCount( int albumid ){
    	return getTrackCount(null, -1, albumid);
    }


    /**
     * Get all artists in the user's track database
     * @return an artist array containing all the artists.
     */
    public Artist[] getAllArtists(){
        int numArtists = getArtistCount();
        if(numArtists < 1){
            return null;
        }
        Artist[] artists = new Artist[numArtists];
        CavaResultSet rs;
        if((rs = db.select("SELECT artistid,artistname,sortbyname FROM artist ORDER BY sortbyname")) != null ){
            int i=0;
            while(i<numArtists){
                if(!rs.fetchRow()){
                    return null;
                }
                artists[i] = new Artist(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3));
                i++;
            }
            return artists;
        }
        return null;
    }

    /**
     * Count the total number of artists
     * @return the number of rows in the artist name, or -1 on failure
     */
    public int getArtistCount(){
    	CavaResultSet rs;
        if((rs = db.select("SELECT COUNT(*) FROM artist")) != null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
    }

    /**
     * Count the total number of tracks stored in the database
     * @return the number of rows in the track database, or -1 on failure
     */
    public int getTrackCount(){
    	return getTrackCount(null,-1,-1);
    }

    /**
     * Count the total number of tracks for a given artist using an artist ID
     * @param artistID the ID of the artist to query by
     * @return the number of tracks by the given artist, or -1 on failure
     */
    public int getTracksByArtistCount(int artistID){
    	return getTrackCount(null, artistID, -1);
    }

    /**
     * Get all the tracks by a given artist
     * @param artistID the ID of the artist to query by
     * @return an array of tracks containing the track information
     */
    public ClientTrack[] getTracksByArtist(int artistID){
    	return getTracks(null, artistID, -1);
    }
    
    /**
     * Get all the track IDs by a given artist. Used for playlist generation
     * @param artistID the ID of the artist to query by
     * @return an array of integers containing the track id. 
     */
    public int[] getTrackIDsByArtist(int artistID){
        int numTracksByArtist = getTracksByArtistCount(artistID);
        if(numTracksByArtist < 1){
            return null;
        }
        int[] trackIDs = new int[numTracksByArtist];
        CavaResultSet rs;
        if((rs=db.select("SELECT trackid " +
        		"FROM (track LEFT JOIN album ON track.albumid=album.albumid) LEFT JOIN artist ON " +
        		"track.artistid=artist.artistid WHERE track.artistid=" + artistID
                + " ORDER BY artistname ASC, albumname ASC,trackno ASC"))!= null){
            int i =0;
            while(i < numTracksByArtist){
                if(!rs.fetchRow()){
                    return null;
                }
                trackIDs[i] = rs.fetchInt(1);
                i++;
            }
            return trackIDs;
        }
        return null;
    }
    
    /**
     * Finds the number of tracks limited by artist and album ID
     * @param artistID the artist ID to limit by
     * @param albumID the album ID to limit by
     * @return the number of tracks by the specified artist on the given album
     */
    public int getNumTracksByArtistAndAlbum(int artistID, int albumID){
    	return getTrackCount(null,artistID,albumID);
    }
    
    /**
     * Returns tracks limited by artist and album ID
     * @param artistID the artist ID to limit by, or -1 to not limit
     * @param albumID the album ID to limit by, or -1 to not limit
     * @return a Track array containg tracks by the given artist on the given album
     */
    public ClientTrack[] getTracksByArtistAndAlbum(int artistID, int albumID){
    	return getTracks(null,artistID,albumID);
    }

    /**
     * Get all the tracks in the user's track database
     * @return an array of all the user's tracks order by artist, album and then track number
     */
    public ClientTrack[] getAllTracks() {
        return getAllTracks(null);
    }
    
    /**
     * Get all the tracks in the user's track database excluding those specified
     * @param exclude Tracks to exclude
     * @return an array of all the user's tracks order by artist, album and then track number
     */
    public ClientTrack[] getAllTracks(ClientTrack[] exclude){
    	return getTracks(exclude,-1,-1);
    }
    
    
    private ClientTrack[] getTracks(ClientTrack[] exclude,int artistID, int albumID){
        String excludedIDs;
        if(exclude != null) {
            StringBuilder builder = new StringBuilder();
            int i;
            for(i=0; i < exclude.length-1; i++) {
                builder.append(exclude[i].getTrackID() + ", ");
            }
            builder.append(exclude[i].getTrackID());
            excludedIDs = builder.toString();
        } else {
            excludedIDs = null;
        }
        
        int numTracks = getTrackCount(excludedIDs,artistID,albumID);
        if(numTracks < 1){
            return null;
        }
        int i = 0;
        ClientTrack[] tracks = new ClientTrack[numTracks];
        CavaResultSet rs;
        if((rs = db.select("SELECT trackid,trackname,path,track.albumid,track.artistid,length,trackno " +
		"FROM (track LEFT JOIN album ON track.albumid=album.albumid) LEFT JOIN artist ON " +
		"track.artistid=artist.artistid " + buildWhereClauseForTracks(excludedIDs, artistID, albumID) + " ORDER BY artistname ASC, albumname ASC,trackno ASC"))!=null){
            while(i < numTracks){
                if(!rs.fetchRow()){
                    return null;
                }
                tracks[i] = new ClientTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchInt(4),rs.fetchInt(5),rs.fetchInt(6),rs.fetchInt(7));
                i++;
            }
            
            return tracks;
        }
        return null;
    }
    
    
    private int getTrackCount(String excludedIDsAsString, int artistID, int albumID){
    	CavaResultSet rs;
    	if((rs=db.select("SELECT COUNT(*) FROM track " + buildWhereClauseForTracks(excludedIDsAsString, artistID, albumID))) != null){
    		if(rs.fetchRow()){
    			return rs.fetchInt(1);
    		}
    	}
    	return -1;
    }
    
    private String buildWhereClauseForTracks(String excludedIDsAsString, int artistID, int albumID){
    	boolean conditionFound = false;
    	StringBuilder whereClause = new StringBuilder(" ");
    	if(excludedIDsAsString != null){
    		whereClause.append("WHERE trackid NOT in ("+excludedIDsAsString+") ");
    		conditionFound=true;
    	}
    	if(artistID > 0){
    		if(!conditionFound){
    			whereClause.append("WHERE track.artistID="+artistID+" ");
    			conditionFound = true;
    		}else{
    			whereClause.append("AND track.artistID="+artistID+" ");
    		}
    	}
    	if(albumID > 0){
    		if(!conditionFound){
    			whereClause.append("WHERE track.albumID="+albumID+" ");
    			conditionFound = true;
    		}else{
    			whereClause.append("AND track.albumID="+albumID+" ");
    		}
    	}
    	return whereClause.toString();
    }
    
    
    /**
     * Get all the tracks in the user's track database excluding those specified
     * @param exclude Tracks to exclude
     * @return an array of all the user's tracks order by artist, album and then track number
     */
    public ClientTrack[] getLimitedTracks(int start,int end,ClientTrack[] exclude){
    	//Make start 1 if it is less than 1, since this is the minimum index
    	if(start < 1){
    		start =1;
    	}
        String excludesql;
        if(exclude != null) {
            StringBuilder builder = new StringBuilder(" WHERE trackid NOT in (");
            int i;
            for(i=0; i < exclude.length-1; i++) {
                builder.append(exclude[i].getTrackID() + ", ");
            }
            builder.append(exclude[i].getTrackID());
            builder.append(")");
            excludesql = builder.toString();
        } else {
            excludesql = "";
        }
        
        int trackCount = end-start+1;
        if(trackCount < 1){
            return null;
        }
        
        int i = 0;
        ClientTrack[] tracks = new ClientTrack[trackCount];
        CavaResultSet rs;
        if((rs=db.select("SELECT * FROM (SELECT trackid,scms, ROW_NUMBER() OVER () AS R " +
		"FROM (track LEFT JOIN album ON track.albumid=album.albumid) LEFT JOIN artist ON " +
		"track.artistid=artist.artistid " + excludesql + " )AS tmp WHERE R >= "+ start + " AND R <=" + end)) != null){
            while(rs.fetchRow()){
                tracks[i] = new ClientTrack(rs.fetchInt(1),rs.fetchBlob(2));
                i++;
            }
            
            //if i=0, no rows where returned so return null
            if(i==0){
            	return null;
            }
            
            //Make sure all index have been filled
            if(i != trackCount){
                Dbg.sysout("Downsizing array so there are no null indexes(trackconnt: "+trackCount+".Rows: "+i+")");
            	tracks = Arrays.copyOf(tracks, i);
            }
            return tracks;
        }
        db.printLastError();
        return null;
    }
    
    /**
     * Find all albums by an artist
     * @param artistID the ID of the artist you want to find albums by
     * @return an Album array containg albums by the given artist
     */
    public Album[] getAlbumsByArtist(int artistID){
    	int numAlbums = getNumAlbumsByArtist(artistID);
    	if(numAlbums < 0){
    		Dbg.syserr("Count returned less than 0");
    		return null;
    	}
    	Album[] albums = new Album[numAlbums];
    	CavaResultSet rs;
    	if((rs = db.select("SELECT DISTINCT albumname,album.albumid,album.artistid,artlocation FROM album LEFT JOIN track ON track.albumid=album.albumid WHERE track.artistid="+artistID+" ORDER BY albumname")) != null){
    		int i = 0;
    		while(i < numAlbums){
    			if(!rs.fetchRow()){
    	    		Dbg.syserr("Count returned null");
    	    		db.printLastError();
    	    		return null;
    			}
    			albums[i] = new Album(rs.fetchString(1),rs.fetchInt(2),rs.fetchInt(3),rs.fetchString(4));
    			i++;
    		}
    		return albums;
    	}else{
    		db.printLastError();
    		return null;
    	}
    }
    
    /**
     * Finds the number of albums by the given artist
     * @param artistID the artist ID you wish to limit by
     * @return the number of albums by that artist
     */
    public int getNumAlbumsByArtist(int artistID){
    	CavaResultSet rs;
    	if((rs=db.select("SELECT COUNT(DISTINCT album.albumid) FROM album LEFT JOIN track ON track.albumid=album.albumid WHERE track.artistid=" + artistID))!=null){
    		if(rs.fetchRow()){
    			return rs.fetchInt(1);
    		}
    	}
    	return -1;
    }
	
	public ClientTrack[] getTracksLike(String searchString){
		return getTracksLike(searchString, 0, 0); 
	}
	
	
	public ClientTrack[] getTracksLike(String searchString,int artistID, int albumID){
		searchString = searchString.toLowerCase();
        int numTracksLike = getNumTracksLike(searchString,artistID,albumID);
        if(numTracksLike < 1){
            return null;
        }
        
        ClientTrack[] tracks = new ClientTrack[numTracksLike];
        //Build the string to limit by artist and album ID if neceessary
        String albumArtistLimit = "";
        if(artistID != 0){
        	albumArtistLimit = " track.artistID = "+artistID+" AND ";
        	if(albumID != 0){
        		albumArtistLimit = albumArtistLimit.concat("track.albumID = "+albumID+" AND ");
        	}
        }else if(albumID != 0){
        	albumArtistLimit = " track.albumID = "+albumID+" AND ";
        }
        CavaResultSet rs;
        if((rs=db.select("SELECT trackid,trackname,path,track.albumid,track.artistid,length,trackno " +
		"FROM (track LEFT JOIN album ON track.albumid=album.albumid) LEFT JOIN artist ON " +
		"track.artistid=artist.artistid WHERE "+albumArtistLimit+" (lowertrackname " +
		"LIKE '%"+searchString+"%' OR sortbyname LIKE '%"+searchString+"%' OR loweralbumname LIKE '%"+searchString+"%') ORDER BY sortbyname ASC, loweralbumname ASC,trackno ASC"))!=null){
            int i = 0;
            while(i < numTracksLike){
                if(!rs.fetchRow()){
                    return null;
                }
                tracks[i] = new ClientTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchInt(4),rs.fetchInt(5),rs.fetchInt(6),rs.fetchInt(7));
                i++;
            }
            return tracks;
        }
        db.printLastError();
        return null;
	}
	
	public int getNumTracksLike(String searchString,int artistID, int albumID){
		searchString = searchString.toLowerCase();
        //Build the string to limit by artist and album ID if neceessary
        String albumArtistLimit = "";
        if(artistID != 0){
        	albumArtistLimit = " track.artistID = "+artistID+" AND ";
        	if(albumID != 0){
        		albumArtistLimit = albumArtistLimit.concat("track.albumID = "+albumID+" AND ");
        	}
        }else if(albumID != 0){
        	albumArtistLimit = " track.albumID = "+albumID+" AND ";
        }
        CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM (track LEFT JOIN album ON track.albumid=album.albumid) LEFT JOIN artist ON " +
				"track.artistid=artist.artistid WHERE "+albumArtistLimit+" (lowertrackname LIKE '%"+searchString+"%' OR sortbyname LIKE '%"+searchString+"%' OR loweralbumname LIKE '%"+searchString+"%')"))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		db.printLastError();
		return -1;
	}
	
	public Artist[] getArtistsWithIDs(Integer[] artistIDs){
        String whereClause;
        if(artistIDs != null) {
        	if(artistIDs.length <= 0){
        		return null;
        	}
            StringBuilder builder = new StringBuilder(" WHERE artistid IN (");
            int i;
            for(i=0; i < artistIDs.length-1; i++) {
                builder.append(artistIDs[i] + ", ");
            }
            if(i<artistIDs.length){
            	builder.append(artistIDs[i]);
            }
            builder.append(")");
            whereClause = builder.toString();
        } else {
            whereClause = "";
        }
        int numArtists = getNumArtistsWithIDs(whereClause);
        if(numArtists < 1){
            return null;
        }
        Artist[] artists = new Artist[numArtists];
        CavaResultSet rs;
        if((rs = db.select("SELECT artistid,artistname,sortbyname FROM artist "+whereClause+" ORDER BY sortbyname")) != null){
            int i=0;
            while(i<numArtists){
                if(!rs.fetchRow()){
                    return null;
                }
                artists[i] = new Artist(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3));
                i++;
            }
            return artists;
        }
        return null;
	}
	
	public int getNumArtistsWithIDs(Integer[] artistIDs){
        String whereClause;
        if(artistIDs != null) {
        	if(artistIDs.length <= 0){
        		return -1;
        	}
            StringBuilder builder = new StringBuilder(" WHERE artistid IN (");
            int i;
            for(i=0; i < artistIDs.length-1; i++) {
                builder.append(artistIDs[i] + ", ");
            }
            if(i<artistIDs.length){
            	builder.append(artistIDs[i]);
            }
            builder.append(")");
            whereClause = builder.toString();
        } else {
            whereClause = "";
        }
        CavaResultSet rs;
        if((rs=db.select("SELECT COUNT(*) FROM artist" + whereClause))!=null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
        
	}
	
	
	public int getNumArtistsWithIDs(String artistIDsAsString){
		CavaResultSet rs;
        if((rs=db.select("SELECT COUNT(*) FROM artist" + artistIDsAsString))!=null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
	}
	
	public Album[] getAlbumsWithIDs(Integer[] albumIDs){
        String whereClause;
        if(albumIDs != null) {
        	if(albumIDs.length <= 0){
        		return null;
        	}
            StringBuilder builder = new StringBuilder(" WHERE albumid IN (");
            int i;
            for(i=0; i < albumIDs.length-1; i++) {
                builder.append(albumIDs[i] + ", ");
            }
            if(i<albumIDs.length){
            	builder.append(albumIDs[i]);
            }
            builder.append(")");
            whereClause = builder.toString();
        } else {
            whereClause = "";
        }
        System.out.println("Where clause: " + whereClause);
        int numAlbums = getNumAlbumsWithIDs(whereClause);
        if(numAlbums < 1){
            return null;
        }
        Album[] allalbums = new Album[numAlbums];
        CavaResultSet rs;
        if((rs = db.select("SELECT albumname,albumid,artistid,artlocation FROM album "+whereClause+" ORDER BY albumname")) != null){
            int counter = 0;
            while ( counter < numAlbums){
                if ( !rs.fetchRow() ){
                    return null;
                }
                allalbums[counter] = new Album(rs.fetchString(1),rs.fetchInt(2),rs.fetchInt(3),rs.fetchString(4));
                counter++;
            }
            return allalbums;
        }
        return null;

	}
	
	public int getNumAlbumsWithIDs(Integer[] albumIDs){
        String whereClause;
        if(albumIDs != null && albumIDs.length > 0) {
        	if(albumIDs.length <= 0){
        		return -1;
        	}
            StringBuilder builder = new StringBuilder(" WHERE albumid IN (");
            int i;
            for(i=0; i < albumIDs.length-1; i++) {
                builder.append(albumIDs[i] + ", ");
            }
            if(i<albumIDs.length){
            	builder.append(albumIDs[i]);
            }
            builder.append(")");
            whereClause = builder.toString();
        } else {
            whereClause = "";
        }
        CavaResultSet rs;
        if((rs=db.select("SELECT COUNT(*) FROM album" + whereClause))!=null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
        
	}
	
	public int getNumAlbumsWithIDs(String albumIDsAsString){
		CavaResultSet rs;
        if((rs=db.select("SELECT COUNT(*) FROM album" + albumIDsAsString))!=null){
            if(rs.fetchRow()){
                return rs.fetchInt(1);
            }
        }
        return -1;
	}
	
	public Playlist[] getAllPlaylists(){
        int numPlaylists = getPlaylistCount();
        if(numPlaylists < 1){
            return null;
        }
        
        Playlist[] playlists = new Playlist[numPlaylists];
        CavaResultSet rs;
        if((rs = db.select("SELECT id,name,(SELECT COUNT(*) FROM playlisttracks WHERE playlistid = id) AS numtracks FROM playlist ORDER BY id")) != null){
            int i=0;
            while(i<numPlaylists){
                if(!rs.fetchRow()){
                    return null;
                }
                playlists[i] = new Playlist(rs.fetchInt(1),rs.fetchString(2),rs.fetchInt(3));
                i++;
            }
            return playlists;
        }
        return null;
	}
	
	public int getPlaylistCount(){
		CavaResultSet rs;
		if((rs = db.select("SELECT COUNT(*) FROM playlist")) != null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		return -1;
	}
	
	public Track[] getTracksInPlaylist(int playlistID){
        int trackCount = getNumTracksInPlaylist(playlistID);
        if(trackCount < 1){
            return null;
        }
        CavaResultSet rs;
        Track[] tracks = new Track[trackCount];
        if((rs=db.select("SELECT playlistTracks.trackid,track.trackname,path,track.albumid,track.artistid,length,trackno,playlisttracks.ptid,playlisttracks.spotifyID,spotifyTracks.trackname,spotifyTracks.artistname,spotifylink " +
		"FROM (playlisttracks LEFT JOIN track ON track.trackid=playlisttracks.trackid) LEFT JOIN spotifyTracks ON playlistTracks.spotifyID=spotifyTracks.spotifyID " +
		"WHERE playlisttracks.playlistid = "+playlistID+" ORDER BY playlisttracks.trackorder ASC"))!=null){
            int i = 0;
            while(i < trackCount){
                if(!rs.fetchRow()){
                    return null;
                }
                if(rs.fetchInt(1) != -1){
                	tracks[i] = new ClientTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchInt(4),rs.fetchInt(5),rs.fetchInt(6),rs.fetchInt(7),rs.fetchInt(8));
                }else{
                	tracks[i] = new ServerTrack(rs.fetchInt(9),rs.fetchString(10),rs.fetchString(11),rs.fetchString(12),rs.fetchInt(8));
                	
                }
                i++;
            }
            return tracks;
        }else{
        	db.printLastError();
        }
        return null;
	}
	
	public int getNumTracksInPlaylist(int playlistID){
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM playlisttracks WHERE playlistid = " + playlistID))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		return -1;
	}
		
	public void createNewLocalPlaylist(int[] trackIDs,String playlistName){
		int playlistID = createNewPlaylist(playlistName);
		if(playlistID > 0){
			addTracksToPlaylist(trackIDs, playlistID, 0,true,false,null);
		}
	}
	
	public void recreatePlaylist(int[] trackIDs,int playlistID,boolean allLocal, boolean allSpotify,TrackType[] trackTypes){
		//Remove old tracks in playlist
		db.delete("DELETE FROM playlisttracks WHERE playlistid="+playlistID);
		//Add new ones starting order at 0
		addTracksToPlaylist(trackIDs, playlistID, 0,allLocal,allSpotify,trackTypes);
	}
	
	public void addLocalTracksToPlaylist(int[] trackIDs,int playlistID){
		addTracksToPlaylist(trackIDs, playlistID, true,false,null);
	}
	
	private void addTracksToPlaylist(int[] trackIDs,int playlistID,boolean allLocal, boolean allSpotify, TrackType[] trackTypes){
		//Make sure the playlist exists
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM playlist WHERE id = " + playlistID))!=null){
			if(rs.fetchRow()){
				if(rs.fetchInt(1) == 1){
					if((rs=db.select("SELECT MAX(trackorder) FROM playlisttracks WHERE playlistid="+playlistID))!=null){
						if(rs.fetchRow()){
							//Insert tracks starting at the next number after the maximum existing value
							int maxValue = rs.fetchInt(1);
							if(maxValue >= 0){
								addTracksToPlaylist(trackIDs, playlistID, maxValue+1,allLocal,allSpotify,trackTypes);
							}
						}
					}
				}
			}
		}	
	}
	
	/**
	 * Add tracks to a playlist starting with the given order value
	 * @param trackIDs an array of track(or spotify) ids to insert
	 * @param playlistID the playlist to add to
	 * @param minOrderValue the minimum order value. 0 for new playlists (max(ordervalue from playlist) +1) otherwise
	 * @param allLocal flag whether or not all these tracks are local tracks
	 * @param allSpotify flag whether or not all these tracks are spotify tracks
	 * @param trackTypes if there is a mixture of local and spotify tracks, use this to indicate for each track what type it is
	 */
	private void addTracksToPlaylist(int[] trackIDs, int playlistID, int minOrderValue, boolean allLocal, boolean allSpotify, TrackType[] trackTypes){
		//Flag which field to insert into
		String insertStatement;
		if(allSpotify){
			insertStatement = "INSERT INTO playlisttracks (playlistid,"+TrackType.Spotify.getPlaylistTracksField()+",trackorder) VALUES ";
		}else{
			insertStatement = "INSERT INTO playlisttracks (playlistid,"+TrackType.Local.getPlaylistTracksField()+",trackorder) VALUES ";
		}
		
		
        StringBuilder values = new StringBuilder(insertStatement);
        int i;
        ArrayList<Integer> listOfTracksToChangeToSpotify = new ArrayList<Integer>();
        for(i=minOrderValue;i<trackIDs.length-1;i++){
        	//If this is a hybrid playlist and this track is of type spotify, then add it to the list to update.
        	if(!allLocal && !allSpotify && trackTypes[i]==TrackType.Spotify){
        		listOfTracksToChangeToSpotify.add(i);
        	}
        	if(i % maxInserts == 0){
        		values.append("("+playlistID+","+trackIDs[i]+","+i+")");
        		db.insert(values.toString());
        		values = new StringBuilder(insertStatement);
        	}else{
        		values.append("("+playlistID+","+trackIDs[i]+","+i+"),");
        	}
        }
    	//If this is a hybrid playlist and this track is of type spotify, then add it to the list to update.
    	if(!allLocal && !allSpotify && trackTypes[i]==TrackType.Spotify){
    		listOfTracksToChangeToSpotify.add(i);
    	}
        values.append("("+playlistID+","+trackIDs[i]+","+i+")");
        db.insert(values.toString());
        //if this is a hybrid playlist and there are Spotify tracks, update them.
    	if(!allLocal && !allSpotify && listOfTracksToChangeToSpotify.size() > 0 ){
    		changePlaylistTracksToSpotify(playlistID,listOfTracksToChangeToSpotify);
    	}
	}
	
	public void changePlaylistTracksToSpotify(int playlistID, ArrayList<Integer> listOfTracksToChangeToSpotify){
		StringBuilder whereClause = new StringBuilder("WHERE playlistID="+playlistID+" AND trackOrder IN(");
		int i;
		for(i=0;i<listOfTracksToChangeToSpotify.size()-1;i++){
			whereClause.append(listOfTracksToChangeToSpotify.get(i) +",");
		}
		whereClause.append(listOfTracksToChangeToSpotify.get(i) +")");
		if(db.update("UPDATE playlistTracks SET spotifyID=trackID,trackID=-1 "+whereClause) != listOfTracksToChangeToSpotify.size()){
			db.printLastError();
			System.err.println("Could not mark tracks as being from spotify");
		}
		
		
	}
	
	public void deletePlaylist(int playlistID){
		System.out.println("Deleting playlist");
		db.delete("DELETE FROM playlisttracks WHERE playlistid="+playlistID);
		db.delete("DELETE FROM playlist WHERE id="+playlistID);
		db.delete("DELETE FROM spotifyTracks WHERE playlistid="+playlistID);
	}
	
	public void deleteTrackFromPlaylist(int playlistID,int playlistTrackID){
		//Ensure that the track belongs to the playlist. 
		CavaResultSet rs;
		if((rs=db.select("SELECT spotifyID FROM playlisttracks WHERE playlistid="+playlistID+" AND ptid="+playlistTrackID))!=null){
			if(rs.fetchRow()){
				db.delete("DELETE FROM playlisttracks WHERE ptid="+playlistTrackID);
				if(rs.fetchInt(1) != -1){
					//if that was a spotify track, delete track from spotify table
					db.delete("DELETE FROM spotifyTracks WHERE spotifyID="+rs.fetchInt(1));
				}
			}
		}
	}
		
	/**
	 * Get the track path from the database. 
	 * @param trackID the ID of the track you need the path for
	 * @return the path or null on failure
	 */
	public String getTrackPath(int trackID){
		CavaResultSet rs;
		if((rs=db.select("SELECT path FROM track WHERE trackid=" + trackID))!=null){
			if(rs.fetchRow()){
				return rs.fetchString(1);
			}
		}
		return null;
	}

    /**
     * Find out if a connected was successfully made to the user's track DB
     * @return true on success, false on failure
     */
    public boolean isConnected(){
        return db.isConnected();
    }

    /**
     * Get the last error encountered by the database
     * @return a string containing the error message
     */
    public String getLastError(){
        return db.getLastError();
    }

    /**
     * Print out the last error encountered by the database
     */
    public void printLastError(){
        db.printLastError();
    }

    /**
     * Testing for this class
     * @param args
     */
    public static void main (String args[]){
    	TrackDatabase db = new TrackDatabase();
    	cava.server.ServerTrack[] tracks = db.getTracksToUpload();
    	db.markAsUploaded(tracks);
    }

	public String getArtistName(int artistID) {
		CavaResultSet rs;
		if((rs=db.select("SELECT artistname FROM artist WHERE artistID=" + artistID))!=null){
			if(rs.fetchRow()){
				return rs.fetchString(1);
			}
		}
		return null;
	}
	
	public String getAlbumName(int albumID) {
		CavaResultSet rs;
	    if((rs=db.select("SELECT albumname FROM album WHERE albumid=" + albumID))!=null){
            if(rs.fetchRow()){
                return rs.fetchString(1);
            }
        }
        return null;
	}
	
	public byte[] getSCMSFromTrack(int trackID){
		CavaResultSet rs;
		if((rs=db.select("SELECT scms,scmsavailable FROM track WHERE trackid="+trackID))!=null){
			if(rs.fetchRow()){
				if(rs.fetchInt(2)==0){
					return null;
				}else{
					return rs.fetchBlob(1);
				}
			}
		}
		return null;
	}

	public void renamePlaylist(int playlistID, String newPlaylistName) {
		String playlistName = db.escapeString(newPlaylistName, 100);
		db.update("UPDATE playlist SET name='"+playlistName+"' WHERE id="+playlistID);
	}
	
	public Stack<ClientTrack> getTracksWithoutSCMSData(){
		Stack<ClientTrack> stack = new Stack<ClientTrack>();
		CavaResultSet rs;
		if((rs = db.select("SELECT trackid,path FROM track WHERE scmsavailable=0")) != null){
			while(rs.fetchRow()){
				//System.out.println("Adding item to stack");
				stack.add(new ClientTrack(rs.fetchInt(1), rs.fetchString(2)));
			}
		}else{
			Dbg.syserr("Failed to select");
		}
		return stack;
	}
	
	public void insertSCMS(int trackID, byte[] scms){
		db.addSCMSData(trackID, scms);
	}
	
//	public void insertSCMS(LinkedList<Integer> trackIDs, LinkedList<byte[]> scmsData){
//		int numTracks = trackIDs.size();
//		if(numTracks != scmsData.size()){
//			System.err.println("Fatal error. Mismatched trackID and scms data sizes");
//		}
//		PreparedStatement statement = db.createStatement(sql)
//	}
	
	public cava.server.ServerTrack[] getTracksToUpload(){
		cava.server.ServerTrack[] tracks = new cava.server.ServerTrack[Constants.tracksToUpload];
        CavaResultSet rs;
        int i = 0;
        if((rs=db.select("SELECT trackid,trackname,artistname,scms FROM (track LEFT JOIN artist ON track.artistid=artist.artistid) WHERE scmsuploaded=0 AND scmsAvailable=1" )) != null){
            while(rs.fetchRow() && i<Constants.tracksToUpload){
                tracks[i++] = new cava.server.ServerTrack(rs.fetchInt(1), rs.fetchString(2), rs.fetchString(3), rs.fetchBlob(4));
            }
            
            //if i=0, no rows where returned so return null
            if(i==0){
            	return null;
            }
            
            //Make sure all index have been filled
            if(i != Constants.tracksToUpload){
                Dbg.sysout("Downsizing array so there are no null indexes(original size: "+Constants.tracksToUpload+".Rows: "+i+")");
            	tracks = Arrays.copyOf(tracks, i);
            }
            return tracks;
        }
        db.printLastError();
        return null;
	}
	
	public void markAsUploaded(Track[] tracks){
		if(tracks==null || tracks.length==0) return;
		StringBuilder whereClause = new StringBuilder(" WHERE trackID IN(");
		int i;
		for(i=0;i<tracks.length-1;i++){
			whereClause.append(tracks[i].getTrackID() +",");
		}
		whereClause.append(tracks[i].getTrackID() +") ");
		if(db.update("UPDATE track SET scmsuploaded=1 "+ whereClause) != tracks.length){
			if(Constants.DEBUG){
				Dbg.syserr("Could not mark tracks as uploaded");
			}
		}
	}

	public void createNewSpotifyPlaylist(ServerTrack[] similarTracks) {
		//Create a new playlist
		int playlistID = createNewPlaylist("Spotify Playlist");
		if(playlistID > 0){
			//Insert tracks into spotify track table
			StringBuilder insert = new StringBuilder("");
			int i;
			for(i=0;i<similarTracks.length-1;i++){
				insert.append("("+playlistID+",'"+db.escapeString(similarTracks[i].getSpotifyLink(), 50)+"'," +
						"'"+db.escapeString(similarTracks[i].getTrackName(), 100)+"'," +
						"'"+db.escapeString(similarTracks[i].getArtistName(), 100)+"'),");
			}
			insert.append("("+playlistID+",'"+db.escapeString(similarTracks[i].getSpotifyLink(), 50)+"'," +
					"'"+db.escapeString(similarTracks[i].getTrackName(), 100)+"'," +
					"'"+db.escapeString(similarTracks[i].getArtistName(), 100)+"')");
			
			if(db.insert("INSERT INTO spotifyTracks (playlistID,spotifyLink,trackname,artistname) VALUES " + insert) != similarTracks.length){
				db.printLastError();
			}else{
				addTracksToPlaylist(getSpotifyTrackIDsFromPlaylist(playlistID), playlistID, 0, false, true, null);
			}
		}
		
		
	}
	
	private int[] getSpotifyTrackIDsFromPlaylist(int playlistID){
		int numTracks = getNumSpotifyTracksInPlaylist(playlistID);
		if(numTracks < 0){
			db.printLastError();
			return null;
		}
		int[] trackIDs = new int[numTracks];
        CavaResultSet rs;
        if((rs = db.select("SELECT spotifyID FROM spotifyTracks WHERE playlistID="+playlistID)) != null){
            int i=0;
            while(i<numTracks){
                if(!rs.fetchRow()){
                    return null;
                }
                trackIDs[i++] = rs.fetchInt(1);
            }
            return trackIDs;
        }
        return null;
	}
	
	private int getNumSpotifyTracksInPlaylist(int playlistID){
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM spotifyTracks WHERE playlistid = " + playlistID))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		return -1;
	}
	
	private int createNewPlaylist(String playlistName){
		CavaResultSet rs;
		if(db.insert("INSERT INTO playlist (name) VALUES ('"+db.escapeString(playlistName, 200)+"')") == 1){
			if((rs = db.select("SELECT IDENTITY_VAL_LOCAL() FROM playlist")) != null){
				if(rs.fetchRow()){
					return  rs.fetchInt(1);
				}
			}
		}
		db.printLastError();
		return -1;
	}
	
	
	public CavaResultSet getAllLyrics() {
		return db.select("SELECT * FROM lyrics");
	}
	
	public CavaResultSet getLyricsSmart(int artistID, int albumID) {
		String whereClause;
		if((whereClause = buildWhereClauseForTracks(null, artistID, albumID)).equals(" ")) {
			return  db.select("SELECT lyrics.lyrics, track.trackID, track.trackName, track.path, track.albumID, track.artistID,track.length, track.trackNo FROM lyrics, track WHERE track.trackID = lyrics.trackID");
		} else {
			System.out.println("######## WHERE #########\n\n|"+whereClause+"|");
			return db.select("SELECT lyrics.lyrics, track.trackID, track.trackName, track.path, track.albumID, track.artistID,track.length, track.trackNo FROM track,lyrics "+whereClause+" AND track.trackID=lyrics.trackID AND lyrics.lyrics IS NOT NULL");
		}
	}
	
	public CavaResultSet getLyricsByAlbum(String album) {
		return db.select("");
	}
	public CavaResultSet selectTracksForScrubber() {
		return db.select("SELECT track.trackname, artist.artistname, track.trackid, lyrics.lyrics FROM track, artist, lyrics WHERE track.artistid = artist.artistid AND (track.trackid = lyrics.trackid AND lyrics.lyrics IS NULL AND lyrics.lyricsUnavailable!=1)");
		//return db.select("SELECT * FROM lyrics");
	}
	
	public int addLyric(int trackID, String lyric) {
		return db.update("UPDATE lyrics SET lyrics = '"+lyric+"' WHERE trackid = "+trackID);
		//return db.insert("INSERT INTO lyrics (lyrics) VALUES ('"+lyric+"') WHERE trackid = "+trackID);
	}

	public void markLyricAsUnavailable(int trackID) {
		db.update("UPDATE lyrics SET lyricsUnavailable=1 WHERE trackid = "+trackID);
	}
	
}
