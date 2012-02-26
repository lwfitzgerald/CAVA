package cava.server;

import java.util.Hashtable;

import cava.miraje.Mir;

/**
 * Class for processing recently added data
 */
public class Processor extends Thread {
    /**
     * Server Database
     */
    private ServerDatabase db;
    
    /**
     * Reference to server object
     */
    private CavaServer server;
    
    /**
     * Whether to end the thread
     */
    private boolean quit;
    
    
    /**
     * Constant defining how many entries with the same tagging are required
     * before making a track available
     */
    private static final int promoteCeiling = 2;
    
    /**
     * Creates a new Processor thread
     * @param db Server Database
     * @param server Reference to server object
     */
    public Processor(ServerDatabase db, CavaServer server) {
        this.db = db;
        this.server = server;
        this.quit = false;
    }
    
    /**
     * Recheck tracks that were unable to be verified to be available from
     * Spotify
     */
    private void recheckTracksAwaitingSpotify() {
    	cava.server.ServerTrack[] tracks = db.getTracksAwaitingSpotifyCheck();
    	if(tracks != null) {
    		for(cava.server.ServerTrack track : tracks) {
    			db.MakeTrackAvailable(track);
    		}
    	}
    }
    
    public void quit(){
    	quit=true;
    }
    
    
    /**
     * Start the thread
     */
    @Override
    public void run() {
        while(!quit) {
            if(server.getProcess()) {
            	//First check for any tracks awaiting a spotify check
            	recheckTracksAwaitingSpotify();
                
                // Fetch a track from the temp table
            	ServerTrack track;
                Integer groupid;
                
                while((track = db.getFirstNewTrack()) != null && !quit) {
                    // Look for an existing Scms group
                    if((groupid = Mir.getGroupID(track, db, 0.20f)) != null) {
                        // There's an existing group so check for promotion
                    	
                    	// If the groupID is less than 0, group has either been promoted 
                    	// or is awaiting a Spotfy check. Hence, throw away
                    	if(groupid < 0){
                            // Remove the track from the temp table as it's now processed
                            db.deleteFromTemp(track.getTrackID());
                            continue;
                    	}
                    	// Add this track to the uploadsTable
                    	db.addToUploadsTable(track, groupid);
                    	
                        ServerTrack[] grouptracks = db.getTracksFromGroup(groupid);
                        
                        Hashtable<String, Integer> table = new Hashtable<String, Integer>();
                        ServerTrack maxtrack = null;
                        int maxcount = 0;
                        
                        for(int i=0; i < grouptracks.length; i++) {
                            String hashstr = grouptracks[i].getArtistName() + '\0' + grouptracks[i].getTrackName();
                            Integer counter;
                            
                            if((counter = table.get(hashstr)) != null) {
                                // Counter for tags exists so increment
                                if(++counter > maxcount) {
                                    maxtrack = grouptracks[i];
                                    maxcount = counter;
                                }
                            } else {
                                // No counter for tags so create one
                                table.put(hashstr, Integer.valueOf(1));
                                
                                if(maxcount == 0) {
                                    maxtrack = grouptracks[i];
                                    maxcount = 1;
                                }
                            }
                        }
                        
                        if(maxcount >= promoteCeiling) {
                            // We've found enough existing entries to verify these tags
                            // so promote the correct tagging to the available table and
                            // store aliases for fast lookup later
                            db.MakeTrackAvailable(maxtrack);
                        }
                    } else {
                        // No existing Scms group so create one
                        db.addToUploadsTable(track);
                    }
                    
                    // Remove the track from the temp table as it's now processed
                    db.deleteFromTemp(track.getTrackID());
                }
                
                server.setProcess(false);
            }else{
            	//Sleep for 10 seconds
            	try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
    }
}
