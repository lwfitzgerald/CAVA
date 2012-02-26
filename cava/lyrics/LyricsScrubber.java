package cava.lyrics;

import cava.*;
import cava.Database.CavaResultSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;



public class LyricsScrubber extends Thread {
	
	private TrackDatabase trackDatabase;
	private static boolean doScrubbing;
	
	
	
	public LyricsScrubber() {
		doScrubbing = true;
		trackDatabase = new TrackDatabase();
		
	}
	
	public static void stopScrubbing() {
		doScrubbing = false;
		new ErrorMessage("Downloading of lyrics as been stopped", "Action Stopped!");
	}
	
	public void scrub() {
		String artistName;
		String trackName;
		int trackID;
		String lyrics;
		boolean someLyricsSearched = false;
		CavaResultSet rs = trackDatabase.selectTracksForScrubber();
		try {
			while(rs.fetchRow() && doScrubbing) {
				System.out.println("Trying to get lyrics");
				someLyricsSearched = true;
				trackName = rs.fetchString(1);
				artistName = rs.fetchString(2);
				trackID = rs.fetchInt(3);
				lyrics = extractLyrics(getLyricsXML_Standard(artistName, trackName));
				if(lyrics != null) {
					if(lyrics.trim().isEmpty()){
						trackDatabase.markLyricAsUnavailable(trackID);
					}else{
						//System.out.println("TRACK ID = "+trackID);
						//System.out.println("Searching for "+trackName+" by "+artistName+":");
						Dbg.sysout("Got lyrics for "+trackName+" by"+artistName);
						if(trackDatabase.addLyric(trackID, lyrics) == -1) {
							System.err.println("Error: Failed to add lyric into lyricsDB");
						} else {
							//System.out.println("Successfully added lyric");
						}
						//System.out.println("==========================");
					}
				}
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(someLyricsSearched){
			new ErrorMessage("Lyrics have finished downloading", "Action Complete!");
		}
	}
	
	public void run() {
		System.out.println("SCRUBBING");
		scrub();
	}
	
	/*public void readFile(String file) {
		//ArrayList<String> output = new ArrayList<String>();
		String trackID = "";
		String artist = "";
		String track = "";
		try {
            FileReader fin = new FileReader( file );
            BufferedReader graphFile = new BufferedReader( fin );
            String line;
            while( ( line = graphFile.readLine() ) != null ) {
            	if(line.length() != 0) { 
            		StringTokenizer st = new StringTokenizer( line );
            		if(st.countTokens() != 3) {
            			System.err.println("Skipping badly formatted line: \n\t"+line);
            		}
            		trackID = st.nextToken();
            		artist = st.nextToken().replaceAll(Pattern.quote("_"), " ");
            		track = st.nextToken().replaceAll(Pattern.quote("_"), " ");
            		System.out.println("================================================");
            		System.out.println(extractLyrics(getLyricsXML_Standard(artist, track)));
            		//output.add(line);
            		Thread.sleep(5000);
            	}
             }
         } catch( IOException e ) { 
        	 System.err.println( e ); 
         } catch (InterruptedException e) {
			e.printStackTrace();
		}

         //return output;
         //This should be the part of the code that i add the database functionality in.
	}*/
	
	public String getLyricsXML_Standard(String artist, String track){
		String url = "";
		String toReturn = "";
		URL u;
		BufferedReader in = null;
		try {
			url = Constants.API_URL+"?i="+Constants.API_KEY+"&a="+URLEncoder.encode(artist, "UTF-8")+"&t="+URLEncoder.encode(track, "UTF-8");
			u = new URL(url);
			in = new BufferedReader(new InputStreamReader(u.openStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				toReturn += inputLine;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if(in!=null){	
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return toReturn;
	}
	
	public String extractLyrics(String xml) {
		if(xml==null){
			return null;
		}
		String toReturn = "";
		try {
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(xml));

	        Document doc = db.parse(is);
	        String stat = doc.getElementsByTagName("status").item(0).getTextContent();
	        if(!stat.equals("200") && !stat.equals("300")) {
	        	return "";
	        }
	        Node n = doc.getElementsByTagName("tx").item(0);
	        toReturn = tidyLyrics(n.getTextContent());
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	    return toReturn;
	}
	
	public String tidyLyrics(String l) {
		l = l.replaceAll(Pattern.quote("[br]")+"+", "/");
		l = l.replaceAll(Pattern.quote("[")+"[^"+Pattern.quote("]")+"]*"+Pattern.quote("]"), "");
		l = l.replaceFirst("^\\s+|\\s+$+", "");
		l = l.replaceAll("\\s{2,}", " ");
		l = l.replaceAll(Pattern.quote("\"")+"|"+Pattern.quote("'"), "");
		return l.replaceAll(Pattern.quote("***")+".*"+Pattern.quote("***"), "");
	}
	
	
	/*public static void main(String[] args) {
		//readFile(args[0]);
		System.out.println("HELLO");
		LyricsScrubber ls = new LyricsScrubber();
		ls.start();
		//System.out.println(lines.toString());
		//System.out.println(extractLyrics(getLyricsXML_Standard("eminem", "puke")));
	}*/

}
