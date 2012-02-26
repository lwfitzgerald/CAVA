package cava;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;

/**
 * Uses the jaudiotagger library (http://www.jthink.net/jaudiotagger/index.jsp) to read the
 * tag data from music tracks
 * @author Ben
 */
public class TagReader {
	
	/**
	 * Constructor. Turns off the logging of data for jaudiotagger. Without this, myraid information
	 * is sent to stderr.
	 */
	TagReader(){
		//ByteArrayInputStream(theString.getBytes())
		String s = "org.jaudiotagger.level = OFF";
		try {
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(s.getBytes()));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the tag data for the specified file (either fully qualified or relative(fully qualified required
	 * for actual use, relative for testing)). File should be a valid music file.  
	 * @param path the path to the file
	 * @return a TagData object or null on error. 
	 */
	public TagData getTagData(String path){
		File file = new File(path);
		String codec = path.substring(path.lastIndexOf('.') + 1);
		if(codec.equalsIgnoreCase("mp3")){
			try {
				MP3File f = (MP3File) AudioFileIO.read(file);
				if(f.hasID3v2Tag()){
					return getMP3TagData(f, path, codec);
				}else{
					return getDefaultTagData(file, path, codec);
				}
			} catch (CannotReadException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (TagException e) {
				e.printStackTrace();
				return null;
			} catch (ReadOnlyFileException e) {
				e.printStackTrace();
				return null;
			} catch (InvalidAudioFrameException e) {
				e.printStackTrace();
				return null;
			} catch(Exception e){
				if(Constants.DEBUG){
					e.printStackTrace();
				}
				return null;
			}
		}else{
			return getDefaultTagData(file, path, codec);
		}
	}
	
	private TagData getMP3TagData(MP3File f,String path,String codec){
		try{
			ID3v24Tag tag = f.getID3v2TagAsv24();
			AudioHeader header = f.getAudioHeader();
			String artist = getValueFromID3v24Tag(tag, FieldKey.ARTIST, "Unknown Artist");
			String album = getValueFromID3v24Tag(tag, FieldKey.ALBUM, "Unknown Album");
			String trackName = getValueFromID3v24Tag(tag, FieldKey.TITLE, "Unknown Song");
			
			if(artist.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty artist");
				artist = "Unknown Artist";
			}
			if(album.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty album");
				album = "Unknown Album";
			}
			if(trackName.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty track name");
				trackName = "Unknown Song";
			}
			
			
			int trackLength = header.getTrackLength();
			String bitRate = header.getBitRate();
			int trackNo;
			String trackNoAsString = tag.getFirst(ID3v24Frames.FRAME_ID_TRACK);
			try{
				trackNo = Integer.parseInt(trackNoAsString);
			}catch(NumberFormatException e){
				trackNo = 0;
			}			
			return (new TagData(artist,album,trackName,trackNo,path,trackLength,bitRate,codec));
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private TagData getDefaultTagData(File file,String path,String codec){
		try {
			AudioFile f =  AudioFileIO.read(file);
			Tag tag = f.getTag();
			AudioHeader header = f.getAudioHeader();
			String artist = getValueFromTag(tag, FieldKey.ARTIST, "Unknown Artist");
			String album = getValueFromTag(tag, FieldKey.ALBUM, "Unknown Album");
			
			String trackName = getValueFromTag(tag, FieldKey.TITLE, "Unknown Song");
			
			if(artist.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty artist");
				artist = "Unknown Artist";
			}
			if(album.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty album");
				album = "Unknown Album";
			}
			if(trackName.trim().isEmpty()){
				Dbg.sysout("Track with path: " + path + "has empty track name");
				trackName = "Unknown Song";
			}
						
			int trackLength = header.getTrackLength();
			String bitRate = header.getBitRate();
			int trackNo;
			String trackNoAsString = getValueFromTag(tag, FieldKey.TRACK, "0");
			try{
				trackNo = Integer.parseInt(trackNoAsString);
			}catch(NumberFormatException e){
				trackNo = 0;//set a default value
			}			
			return (new TagData(artist,album,trackName,trackNo,path,trackLength,bitRate,codec));			
			
		} catch (CannotReadException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (TagException e) {
			e.printStackTrace();
			return null;
		} catch (ReadOnlyFileException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidAudioFrameException e) {
			e.printStackTrace();
			return null;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private String getValueFromTag(Tag tag,FieldKey fieldKey,String failureMessage){
		try{
			return tag.getFirst(fieldKey);
		}catch (Exception e) {
			return failureMessage;
		}
	}
	
	private String getValueFromID3v24Tag(ID3v24Tag tag,FieldKey fieldKey,String failureMessage){
		try{
			return tag.getFirst(fieldKey);
		}catch (Exception e) {
			return failureMessage;
		}
	}
	
	/**
	 * Print the same information retrieved by getTagData to stdout. Used for testing
	 * @param path the path to the file.
	 */
	public void printTagData(String path){
		//TagData data = getTagData(path);
		//System.out.println("Artist Name: " + data.getArtistName());
		//System.out.println("Album Name: " + data.getAlbumName());
		//System.out.println("Track Name: " + data.getTrackName());
	}
	
	public static void main(String[] args){
		//TagReader program = new TagReader();
		//String file = args[0];
		//System.out.println("File: " + file);
		//program.printTagData(file);
		
	}
			
}
