package cava.lyrics;

import cava.*;
import cava.Database.CavaResultSet;
import java.util.ArrayList;

public class LyricMatcher {
	
	private TrackDatabase trackDatabase;
	private Matcher dmp;
	public LyricMatcher() {
		// constructor
		dmp = new Matcher();
		trackDatabase = new TrackDatabase();
	}
	// ClientTrack(int trackID, String trackName, String path, int albumID, int artistID,int length, int trackNo) {
	public ArrayList<ClientTrack> matchLyrics(String searchLyrics, int artistID, int albumID ) {
	    CavaResultSet rs = trackDatabase.getLyricsSmart(artistID, albumID);
	    ArrayList<ClientTrack> toReturn = new ArrayList<ClientTrack>();
	    ClientTrack tempTrack;
	    String lyrics, trackName, path;
	    int trackID, _albumID, _artistID, length, trackNo;
	    searchLyrics = cleanSearchTerm(searchLyrics);
	    while(rs.fetchRow()) {
	        lyrics    = rs.fetchString(1);
	        trackID   = rs.fetchInt(2);
	        trackName = rs.fetchString(3);
	        path      = rs.fetchString(4);
	        _albumID   = rs.fetchInt(5);
	        _artistID  = rs.fetchInt(6);
	        length    = rs.fetchInt(7);
	        trackNo   = rs.fetchInt(8);

	        //System.out.println("LYRICS:\n|"+lyrics+"|");
	        if(lyrics != null) {
	            if(dmp.fuzzyMatch(searchLyrics, lyrics, 0) != -1 && lyrics != null) {
	                //System.out.println(lyrics);
	                tempTrack = new ClientTrack(trackID, trackName, path, _albumID, _artistID, length, trackNo);
	                toReturn.add(tempTrack);
	            }
	        }
	    }
	    return toReturn;
	}
	
	
	/*public ArrayList<String> matchLyrics(String searchLyrics) {
		CavaResultSet rs = 
		ArrayList<String> toReturn = new ArrayList<String>();
		String lyrics;
		String trackID;
		returnedLyrics = getAllLyrics();
		search = cleanSearchTerm(search);
		try {
			while(returnedLyrics.next()) {
				trackID = returnedLyrics.getObject(1).toString();
				lyrics = returnedLyrics.getObject(2).toString();
				System.out.println("ID:"+trackID+"\n"+lyrics+"\n\n");
				if(dmp.fuzzyMatch(search, lyrics, 0) != -1) {
					toReturn.add(trackID);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return toReturn;
		
	}*/
	
	/*public static void main(String[] args) {
		LyricMatcher lm = new LyricMatcher();
		
		 ArrayList<ClientTrack> picka = lm.matchLyrics("brother", -1, -1);
		System.out.println(picka.toString());
	}*/
	
	public static String cleanSearchTerm(String st) {
		return st.replaceAll(" \\? ", " ");
	}
	
}

