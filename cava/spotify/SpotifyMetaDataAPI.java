package cava.spotify;

import cava.BuildDatabase;
import cava.Constants;

import com.google.code.jspot.*;
import java.lang.Thread;
import java.util.Arrays;

/**
 * This is just a placeholder so I can work on the server Database stuff. It doesn't work!
 * @author Ben
 *
 */
public class SpotifyMetaDataAPI {
    private String actualArtistName;
    private String actualTrackName;
    private String spotifyLink;
    private Boolean matched;
    private Boolean attempted = false;
    private Boolean connected;
    private long time;
    
    public static void main(String[] args) {
        // If you run the main function then it checks for the song "August and Everything After" by Counting Crows. The name of the artist is spelt wrong so you can see that the removePunc method is working
        String input = "Coun..ting. Crow!!s";
        
        SpotifyMetaDataAPI smd = new SpotifyMetaDataAPI();
        smd.checkTrack( input, "August and Everything After" );
        System.out.println( smd.actualArtistName );
        System.out.println( smd.actualTrackName );
        System.out.println( smd.spotifyLink );
        smd.checkTrack( input, "Hanging Around" );
        System.out.println( smd.actualArtistName );
        System.out.println( smd.actualTrackName );
        System.out.println( smd.spotifyLink );
    }
    
    //Removing punctuation isn't actually a great idea. Searching for I dont want to miss a think fails (needs to be don't); likewise if you
    //remove apostrophe from la femme d'argent. Same goes for something like Radioheads's 2+2=5. 
//    public static String removePunc(String s) {
//       String r = "";
//       s = s.toLowerCase();
//       for (int i = 0; i < s.length(); i ++) {
//           if (Character.isLetter(s.charAt(i)) || s.charAt(i) == ' ' ) r += s.charAt(i);
//          }
//       return r;
//    }
    
    /**
     * Begin a check with Spotify
     * @param artist
     * @param track
     */
    public void checkTrack(String artist, String trackname){
        long currenttime = System.currentTimeMillis();
        
        if( currenttime - time < 100 ) {
            try {
                System.out.println( "Too many requests, pausing for " + (currenttime - time) + " milliseconds." );
                Thread.sleep(currenttime - time);
                System.out.println( "Unpaused" );
            } catch(InterruptedException e) {
                // Ignore
            }
        } else {
            System.out.println( "There was no need to pause, last time we sent a query was " + (currenttime - time) + " milliseconds ago" );
        }
        
        time = currenttime;
        
        //Check with spotify if track is available
        //and write results into actual track & artist names (this will put things back in proper case and re-include the 'the' if present,
        //which was previously stripped out
        
        // Set matched status to false by default
        matched = false;
        
        connected = false;
        
        //String artistTemp = removePunc(artist);
        
        //Remove any 'the's and convert to lower case
        artist = BuildDatabase.createSortByArtist(artist).toLowerCase();
        trackname = trackname.toLowerCase();
        
        
        try {
            Spotify spotify = new Spotify();
            attempted = true;
            int i=0;
            Results<Track> results = spotify.searchTrack(trackname+" "+artist);
            System.out.println("Match against: " +trackname + " // " + artist);
            for (Track track : results.getItems()) {
            	if(Arrays.binarySearch(track.getAlbum().getAvailableTerritories(),"GB") < 0){
            		if(Constants.DEBUG){
            			System.out.println("Track unavailable in GB territory");
            		}
            		continue;
            	}
               // String outputtemp = removePunc(track.getArtistName());
                System.out.println( cleanSpotifyTrackForCmp(track.getName()) + " // " + cleanSpotifyArtistForCmp(track.getArtistName()) );
                if( artist.equals(cleanSpotifyArtistForCmp(track.getArtistName())) && trackname.equals(cleanSpotifyTrackForCmp(track.getName())) ) {
                	matched = true;
                    actualArtistName = track.getArtistName();
                    actualTrackName = track.getName();
                    spotifyLink = track.getId();
                    break;//Break out once we've found a match
                }
                i++;
            }
            connected = true;
        } catch (java.io.IOException e) {
            connected = false;
            System.err.println( "Failed to create a spotify object" );
            return;
        }
        
        if (matched == true) {
            System.out.println( "Match!" );
        } else {
            System.out.println( "No match" );
            actualArtistName = null;
            actualTrackName = null;
            spotifyLink = null;
        }
        
    }
    
    /**
     * Clean the artist name returned from the spotify search by removing any 'the's,
     * removing punctuation and converting to lower case. This allows comparison to be a 
     * bit looser
     * @param artist
     * @return
     */
    private String cleanSpotifyArtistForCmp(String artist){
    	return BuildDatabase.createSortByArtist(artist).toLowerCase();
    }
    
    /**
     * Clean the track name returned from the spotify search by removing any punctuation
     * and converting to lower case. This allows the comparison to be a bit looser
     * @param track
     * @return
     */
    private String cleanSpotifyTrackForCmp(String track){
    	return track.toLowerCase();
    }
    
    
    /**
     * Should return false until we've attempted to make a request to Spotify and the information has been retrieved or until
     * we know Spotify is down.
     * @return
     */
    public boolean connectionAttempted(){
        return attempted;
    }
    
    /**
     * Were we actually able to check with Spotify?
     * @return
     */
    public boolean connectionMade(){
        return connected;
    }
    
    /**
     * Is the track available at Spotify?
     * @return
     */
    public boolean isAvailable(){
        return matched;
    }
    
    /**
     * Return the artist name as per Spotify
     * @return
     */
    public String getActualArtistName(){
        return actualArtistName;
    }

    /**
     * Return the track name as per Spotify
     * @return
     */
    public String getActualTrackName(){
        return actualTrackName;
    }
    
    /**
     * Returns Spotify Link such as 6U5hUX40OWY6l0HaPUWElZ
     * we assume all links are tracks, so omits the "spotify:track:" part to reduce storage requirements
     * @param spotiftyLink
     * @return
     */
    public String getSpotifyLink(){
        return spotifyLink.substring(14,spotifyLink.length());
    }
}