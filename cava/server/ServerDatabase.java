package cava.server;

import cava.Database.CavaResultSet;
import cava.spotify.*;
import java.sql.PreparedStatement;
import java.util.Arrays;

import cava.*;

public class ServerDatabase extends TrackDatabase {
	
	private SpotifyMetaDataAPI spotifyAPI;
	
	
	public ServerDatabase() {
		//Make connection to database
        super();
        if(!db.isConnected()){
        	if(Constants.DEBUG){
        		db.printLastError();
        	}
        	Dbg.syserr("Could not connect to server database. Exiting");
        	System.exit(1);
        }
		spotifyAPI = new SpotifyMetaDataAPI();
	}
	
	@Override
    public String getDBName(){
    	return "CAVAserverDB";
    }

	
	/**
	 * Check if the track already exists in the uploads based on artist and trackname
	 * @param track Track to check
	 * @return Tracks if there is a match based on trackname and artist name, or null if one doesn't exist
	 */
	public cava.server.ServerTrack[] getTracksFromUploads(cava.server.ServerTrack track){
		String trackName = escapeTrackName(track.getTrackName());
		String artistName = escapeArtistName(track.getArtistName());
		int numMatches = getNumTracksFromUploads(trackName, artistName);
		if(numMatches < 1){
			return null;
		}
		cava.server.ServerTrack[] tracks = new cava.server.ServerTrack[numMatches];
		CavaResultSet rs;
		if((rs=db.select("SELECT trackid,trackname,artistname,scms,scmsGroup FROM tracksUploaded WHERE trackname='"+trackName+"' AND artistname='"+artistName+"'"))!=null){
			int i=0;
			while(i < numMatches) {
				if(!rs.fetchRow()){
					return null;
				}
				tracks[i++] = new cava.server.ServerTrack(rs.fetchInt(1), rs.fetchString(2), rs.fetchString(3), rs.fetchBlob(4),rs.fetchInt(5));
			}
			return tracks;
		}
		return null;
	}
	
	public cava.server.ServerTrack[] getTracksFromGroup(int scmsGroup){ 
		int numMatches = getNumTracksInGroup(scmsGroup);
		if(numMatches < 1){
			return null;
		}
		cava.server.ServerTrack[] tracks = new cava.server.ServerTrack[numMatches];
		CavaResultSet rs;
		if((rs=db.select("SELECT trackid,trackname,artistname,scms,scmsGroup FROM tracksUploaded WHERE scmsGroup="+scmsGroup))!=null){
			int i=0;
			while(i < numMatches) {
				if(!rs.fetchRow()){
					return null;
				}
				tracks[i++] = new cava.server.ServerTrack(rs.fetchInt(1), rs.fetchString(2), rs.fetchString(3), rs.fetchBlob(4),rs.fetchInt(5));
			}
			return tracks;
		}
		return null;
	}
	
	private int getNumTracksInGroup(int scmsGroup){
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM tracksUploaded WHERE scmsGroup="+scmsGroup))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		return -1;
	}
	
	
	
	/**
	 * Find the number of matches from the uploads table based on trackName and artistName. Used to pre-allocate array
	 * @param trackName
	 * @param artistName
	 * @return
	 */
	private int getNumTracksFromUploads(String trackName, String artistName){
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM tracksUploaded WHERE trackname='"+trackName+"' AND artistname='"+artistName+"'"))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}
		}
		return -1;
	}
	
	/**
	 * Move a track from tracksUploaded to tracksAvailable. Checks with Spotify if it's available
	 * Also removes tracks and updates group IDs from tracksUploaded as necessary
	 * @param trackID
	 */
	public void MakeTrackAvailable(cava.server.ServerTrack track){
		//Check track is in uploads first
		CavaResultSet rs;
		
		String artistName = track.getArtistName();
		String trackName = track.getTrackName();
		int trackID = track.getTrackID();
		byte[] scms = track.getScms();
		
		if((rs=db.select("SELECT COUNT(*) FROM tracksUploaded WHERE trackID="+trackID))!=null){
			if(rs.fetchRow()){
				if(rs.fetchInt(1) != 1){
					if(Constants.DEBUG){
						System.err.println("Track with ID: " + trackID + " isn't in the tracksUploaded table!");
					}
					return;
				}
			}
		}
		
		//Check a track and wait for a reply
		spotifyAPI.checkTrack(artistName, trackName);
		while(!spotifyAPI.connectionAttempted()){
			//Do nothing
		}
		
		if(spotifyAPI.connectionMade()){
			//If the track is available at Spotify, remove from uploads and add to available
			if(spotifyAPI.isAvailable()){
				trackName = spotifyAPI.getActualTrackName();
				artistName = spotifyAPI.getActualArtistName();
				String spotifyLink = spotifyAPI.getSpotifyLink();
				
				//If we add successfully, then delete from the uploaded tracks, keeping this track and marking it as available
				if(addToAvailableTracks(trackName,artistName,scms,spotifyLink)){
					removeGroupFromUploadedTracks(track,true,SCMSGroupCodes.madeAvailable);
				}else{
					System.err.println("Failed to add to available tracks");
					db.printLastError();
				}
			}else{
				System.err.println("Track unavailable");
				//Track is unavailable, so just remove all from the uploads.
				removeGroupFromUploadedTracks(track, false, null);
			}
		}else{			
			//mark group as awaiting Spotify test
			System.err.println("Mark as waiting");
			removeGroupFromUploadedTracks(track, true, SCMSGroupCodes.awaitingSpotifyCheck);
		}

	}
	

	/**
	 * Deletes the group from the uploaded tracks. 
	 * @param trackID the track ID which indicates the group to delete
	 * @param keepTrack if true, the track with the ID specified will be kept. This should be the case if the track's available at Spotify
	 * or Spotify were uncontactable
	 * @param groupFlag specifies what the new group should be set to. Set to null if keeptrack=false
	 */
	private void removeGroupFromUploadedTracks(cava.server.ServerTrack track, boolean keepTrack, SCMSGroupCodes groupFlag) {
		int trackID = track.getTrackID();
		String trackName = db.escapeString(track.getTrackName(),100);
		String artistName = db.escapeString(track.getArtistName(),100);
		System.out.println("Removing group. SCMS flag: " + groupFlag + ". Keeping track: " + keepTrack);
		CavaResultSet rs;
		if((rs=db.select("SELECT scmsGroup FROM tracksUploaded WHERE trackid=" +trackID))!=null){
			if(rs.fetchRow()){
				int scmsGroup = rs.fetchInt(1);
				if(scmsGroup == -1){
					if(Constants.DEBUG){
						System.err.println("Removing group from database, but group number is: " + scmsGroup);
						return;
					}
				}else{
					System.out.println("Removing group number: " + scmsGroup);
					//If track wasn't available, remove the whole lot from the tracks uploaded table
					if(!keepTrack){
						if(scmsGroup!=-2){
							db.delete("DELETE FROM tracksUploaded WHERE scmsGroup="+scmsGroup);
						}else{
							db.delete("DELETE FROM tracksUploaded WHERE trackid="+trackID);
						}
						//Also delete any aliases with this trackID; they might be present because of aliases added before
						//spotify check was possible
						db.delete("DELETE FROM aliases WHERE tracksUploadedID="+trackID);
					}else{
						//Otherwise, move all other entries to the alias table and delete them from the tracksUploaded table
						if((rs=db.select("SELECT DISTINCT artistname,trackname FROM tracksUploaded WHERE scmsGroup="+scmsGroup+" AND (" +
								"trackname!='"+trackName+"' OR artistname!='"+artistName+"')"))!=null){
							while(rs.fetchRow()){
								System.out.println("Adding alias: " + trackName);
								addAlias(rs.fetchString(1), rs.fetchString(2), trackID);
							}
						}else{
							System.err.println("Query failed. ");
							db.printLastError();
						}
						
						db.delete("DELETE FROM tracksUploaded WHERE scmsGroup="+scmsGroup+" AND trackID !=" + trackID);
						//Finally, set the group SCMS flag on the specified track
						db.update("UPDATE tracksUploaded SET scmsGroup="+groupFlag.getIntValue()+" WHERE trackID="+trackID);
						
					}				
				}
			}else{
				System.err.println("Could not fetch row");
				db.printLastError();
			}
		}else{
			System.err.println("Could not select group from table");
			db.printLastError();
		}
		
	}

	/**
	 * Add a track with the given data to the table of tracks available for smart-playlist generation
	 * @param trackName
	 * @param artistName
	 * @param scms
	 * @param spotifyLink
	 * @return
	 */
	private boolean addToAvailableTracks(String trackName, String artistName,
			byte[] scms, String spotifyLink) {
		//Make sure spotify link isn't in the database
		spotifyLink = db.escapeString(spotifyLink, 100);
		CavaResultSet rs;
		if((rs=db.select("SELECT COUNT(*) FROM tracksAvailable WHERE spotifylink='"+spotifyLink+"'"))!=null){
			if(rs.fetchRow()){
				if(rs.fetchInt(1) != 0){
					if(Constants.DEBUG){
						System.err.println("Spotify link:"+spotifyLink + "already exists in the db. Track: ("+trackName+"), Artist: ("+artistName+")");
					}
					return false;
				}
			}else{
				if(Constants.DEBUG){
					System.err.println("Unable to select row when checking count from the available tracks");
				}
				return false;
			}	
		}else{
			if(Constants.DEBUG){
				System.err.println("Unable to query db when checkiong count from the available tracks");
			}
			return false;
		}
		
		//Check lengths
		artistName = db.checkLength(artistName, 100);
		trackName = db.checkLength(trackName, 100);
		
		int artistID = getArtistID(artistName);
		
		PreparedStatement statement = db.createStatement("INSERT INTO tracksAvailable (artistID,trackname,spotifylink,scms) VALUES(?,?,?,?)");
		
		if(statement != null){
			try{
			statement.setInt(1, artistID);
			statement.setString(2, trackName);
			statement.setString(3, spotifyLink);
			statement.setBytes(4, scms);
			return db.executeStatement(statement);
			}catch (Exception e) {
				db.printLastError();
				return false;
			}
		}

		return false;
		
	}
	
	/**
	 * Get the ariistID for a particular artistName. If it exists in the table already, the corresponding ID is returned
	 * Otherwise, the artist is added and the last inserted ID is returned
	 * @param artistName
	 * @return
	 */
	private int getArtistID(String artistName){
		artistName = db.escapeString(artistName, 100);
		CavaResultSet rs;
		if((rs=db.select("SELECT artistID FROM artist WHERE artistName='"+artistName+"'"))!=null){
			if(rs.fetchRow()){
				return rs.fetchInt(1);
			}else{
				//Track not in database, so add
				if(db.insert("INSERT INTO artist (artistName) VALUES ('"+artistName+"')") == 1){ 
					return getArtistID(artistName);
				}else{
					if(Constants.DEBUG){
						System.err.println("Could not insert artist into server table");
					}
				}
			}
		}
		
		return -1;
	}

	/**
	 * Get an array of tracks from the tracksAvailable table where the artist and track name of the provided track is
	 * a known alias of that in the tracks available table
	 * @param track
	 * @return an array of matching tracks based on the known-alias table, or null if there are no matches
	 */
    public cava.server.ServerTrack[] getTracksFromAliases(cava.server.ServerTrack track) {
		String trackName = escapeTrackName(track.getTrackName());
		String artistName = escapeArtistName(track.getArtistName());
		int numMatches = getNumTracksFromAliases(trackName, artistName);
		
		if(numMatches < 1){
			return null;
		}
		
		cava.server.ServerTrack[] tracks = new cava.server.ServerTrack[numMatches];
		CavaResultSet rs;
		if((rs=db.select("SELECT trackid,tracksUploaded.trackname,tracksUploaded.artistname,scms,scmsGroup FROM (aliases LEFT JOIN tracksUploaded ON trackid=tracksUploadedID)  WHERE aliases.trackname='"+trackName+"' AND aliases.artistname='"+artistName+"'"))!=null){
			int i=0;
			while(i < numMatches) {
				if(!rs.fetchRow()){
					return null;
				}
				tracks[i++] = new cava.server.ServerTrack(rs.fetchInt(1), rs.fetchString(2), rs.fetchString(3), rs.fetchBlob(4), rs.fetchInt(5));
			}
			return tracks;
		}
		return null;
    }
    
    private int getNumTracksFromAliases(String trackName, String artistName){
    	CavaResultSet rs;
    	if((rs=db.select("SELECT COUNT(*) FROM aliases WHERE artistname='"+artistName+"' AND trackname='"+trackName+"'"))!=null){
    		if(rs.fetchRow()){
    			return rs.fetchInt(1);
    		}
    	}
    	return -1;
    }
    
    private String escapeArtistName(String artistName){
    	return db.escapeString(BuildDatabase.createSortByArtist(artistName), 100).toLowerCase();
    }
    
    private String escapeTrackName(String trackName){
    	return db.escapeString(trackName, 100).toLowerCase();
    }
    
    private String checkArtistName(String artistName){
    	return db.checkLength(BuildDatabase.createSortByArtist(artistName), 100).toLowerCase();
    }
    
    private String checkTrackName(String trackName){
    	return db.checkLength(trackName, 100).toLowerCase();
    }
    
    /**
     * Add a track to the uploads table, creating a new SCMS group
     * @param track
     */
    public void addToUploadsTable(cava.server.ServerTrack track){
    	addToUploadsTable(track, -1);
    }
    
    /**
     * Add a track to the uploads table, with the specified scmsGroup. If
     * scmsGroup < 0, create a new group. 
     * @param track
     * @param scmsGroup
     * @return the ID of the inserted track, or -1 on failure
     */
    public void addToUploadsTable(cava.server.ServerTrack track, int scmsGroup){
    	CavaResultSet rs;
    	if(scmsGroup==-1){
    		if((rs=db.select("SELECT MAX(scmsGROUP) FROM tracksUploaded"))!=null){
    			if(rs.fetchRow()){
    				scmsGroup = rs.fetchInt(1) +1;
    			}else{
    				return;
    			}
    		}else{
    			return;
    		}
    	}
    	String sql = "INSERT INTO tracksUploaded (trackname,artistname,scms,scmsGROUP) VALUES(?,?,?,"+scmsGroup+")";
    	PreparedStatement statement = db.createStatement(sql);
    	try{
    		statement.setString(1, checkTrackName(track.getTrackName()));
    		statement.setString(2, checkArtistName(track.getArtistName()));
    		statement.setBytes(3, track.getScms());
    		if(!db.executeStatement(statement)){
    			db.printLastError();
    		}
    	}catch(Exception e){
    		if(Constants.DEBUG){
    			System.err.println("Unable to add to uploads table. Query was: " + sql + ".Error:");
    			db.printLastError();
    		}
    			
    	}
    	return;
    }
    
    public void addAlias(String artistName,String trackName,int tracksUploadedID){
    	artistName = escapeArtistName(artistName);
    	trackName = escapeTrackName(trackName);
    	//First check count
    	CavaResultSet rs;
    	if((rs=db.select("SELECT tracksUploadedID FROM aliases WHERE trackname='"+trackName+"' AND artistname='"+artistName+"'"))!=null){
    		if(rs.fetchRow()){
    			if(Constants.DEBUG){
    				System.err.println("Alias with artist: ("+artistName+") and track name: ("+trackName+") already exists");
    				if(rs.fetchInt(1) != tracksUploadedID){
    					System.err.println("Existig alias with different tracksUploadedID!. Current ID: " + rs.fetchInt(1) + ", other ID: " + tracksUploadedID);
    				}
    			}
    			return;
    		}
    	}
    	
    	if(db.insert("INSERT INTO aliases (trackname,artistname,tracksUploadedID) VALUES ('"+trackName+"','"+artistName+"',"+tracksUploadedID+") ") != 1){
    		if(Constants.DEBUG){
    			db.printLastError();
    		}
    	}
    }
    
	public void addTracksToTempTable(cava.server.ServerTrack[] tracks){
		String sql = "INSERT INTO temp (trackname,artistname,scms) VALUES (?,?,?)";
		for(cava.server.ServerTrack t: tracks){
			try{
				PreparedStatement statement = db.createStatement(sql);
				statement.setString(1, db.checkLength(t.getTrackName(),100));
				statement.setString(2, db.checkLength(t.getArtistName(),100));
				statement.setBytes(3, t.getScms());
				db.executeStatement(statement);
			}catch(Exception e){
				if(Constants.DEBUG){
					System.err.println("Could not insert track: "+t.getTrackName());
					db.printLastError();
				}
			}
		}
	}
	
	/**
	 * Loads the oldest track in the temporary table. You should delete it when you're finished
	 * @return the oldest track, or null on error. 
	 */
	public cava.server.ServerTrack getFirstNewTrack(){
		CavaResultSet rs;
		if((rs=db.select("SELECT id,trackname,artistname,scms FROM temp ORDER BY id ASC"))!=null){
			if(rs.fetchRow()){
				return new cava.server.ServerTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchBlob(4));
			}
		}
		return null;
	}
	
	/**
	 * Delete an item from the temporary table. 
	 * @param id the id of the item you want to delete
	 */
	public void deleteFromTemp(int id){
		db.delete("DELETE FROM temp WHERE id="+id);
	}
    
    public cava.server.ServerTrack[] getLimitedUploadedTracks(int start, int end, ServerTrack[] exclude){
    	return getLimitedTracks(false, start, end, exclude);
    }
    
    public cava.server.ServerTrack[] getLimitedAvailableTracks(int start,int end, ServerTrack[] exclude){
    	return getLimitedTracks(true, start, end, exclude);
    	
    }
        
    private cava.server.ServerTrack[] getLimitedTracks(boolean fromAvailable,int start,int end, ServerTrack[] exclude){
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
        cava.server.ServerTrack[] tracks = new cava.server.ServerTrack[trackCount];
        //Slightly different queries:
        String sql;
        if(fromAvailable){
        	sql = "SELECT * FROM (SELECT trackid,trackname,artistname,scms,spotifylink, ROW_NUMBER() OVER () AS R " +
    		"FROM (tracksAvailable LEFT JOIN artist ON tracksAvailable.artistID=artist.artistID) " + excludesql + " )AS " +
    		"tmp WHERE R >= "+ start + " AND R <=" + end;
        }else{
        	sql = "SELECT * FROM (SELECT trackid,trackname,artistname,scms,scmsGroup, ROW_NUMBER() OVER () AS R " +
    		"FROM tracksUploaded " + excludesql + " )AS tmp WHERE R >= "+ start + " AND R <=" + end;
        }
        CavaResultSet rs;
        if((rs=db.select(sql))!=null){
            while(rs.fetchRow()){
            	
            	if(fromAvailable){
            		tracks[i] = new cava.server.ServerTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchBlob(4),rs.fetchString(5));
            	}else{
            		tracks[i] = new cava.server.ServerTrack(rs.fetchInt(1),rs.fetchString(2),rs.fetchString(3),rs.fetchBlob(4),rs.fetchInt(5));
            	}
            	
                i++;
            }
            
            //if i=0, no rows where returned so return null
            if(i==0){
            	return null;
            }
            
            //Make sure all index have been filled
            if(i != trackCount){
            	Dbg.sysout("Downsizing array so there are no null indexes(trackcount: "+trackCount+".Rows: "+i+")");
            	tracks = Arrays.copyOf(tracks, i);
            }
            return tracks;
        }
        if(Constants.DEBUG){
        	db.printLastError();
        }
        return null;
        
        
    }
    
    public cava.server.ServerTrack[] getTracksAwaitingSpotifyCheck(){
    	return getTracksFromGroup(SCMSGroupCodes.awaitingSpotifyCheck.getIntValue());
    }
    
    public static void main (String args[]){
//        ServerDatabase db = new ServerDatabase();
//
//        cava.server.Track[] tracks = new cava.server.Track[4];
//        ArrayList<cava.server.Track> tracksToAdd = new ArrayList<cava.server.Track>();
//        tracksToAdd.add( new cava.server.Track(0,"Mr. Brightside","The Killers",new byte[1]));
//        tracksToAdd.add( new cava.server.Track(1,"Mr brightSide","killers",new byte[2]));
//        tracksToAdd.add( new cava.server.Track(2,"Mr brightSide","Killers",new byte[3]));
//        tracksToAdd.add( new cava.server.Track(3,"mr brightSide","killers",new byte[3]));
//
//        db.addTracksToTempTable(tracksToAdd);
//        cava.server.Track t;
//        while( (t = db.getFirstNewTrack()) != null){
//            System.out.println("temp: " + t.getTrackName() + "//" + t.getArtistName());
//            db.addToUploadsTable(t,1);
//            db.deleteFromTemp(t.getTrackID());	
//        }
//
//        //    	cava.server.Track track = new cava.server.Track(2,"te'st","real''name",new byte[3]);
//
//        tracks = db.getTracksFromGroup(1);
//        if(tracks!=null){
//            for(cava.server.Track temp: tracks){
//                System.out.println(temp.getTrackName() + "//" + temp.getArtistName() + "//" + temp.getScmsGroup());
//            }
//        }else{
//            System.out.println("No tracks available");
//        }
//        if(tracks!=null){
//            db.MakeTrackAvailable(tracks[0]);
//        }else{
//            System.out.println("No matching tracks!");
//        }
//
//        //	tracks = db.getLimitedAvailableTracks(0, 5, null);
    }
    
    
}


