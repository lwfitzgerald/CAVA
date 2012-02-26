package cava;

/**
 * Stores the data read by the TagReader. Basically a datastructure to pass to database functions
 * @author Ben
 */
public class TagData {
	private String trackName;
	private String artistName;
	private String albumName;
	private String path;
	private int trackLength;
	private String bitRate;
	private String codec;
	private int trackNo;
	
	/**
	 * Create a new TagData object. Truncates string fields to the correct length
	 * @param artistName
	 * @param albumName
	 * @param trackName
	 * @param trackNo
	 * @param path
	 * @param trackLength
	 * @param bitRate
	 * @param codec
	 */
	public TagData(String artistName, String albumName, String trackName,
			int trackNo, String path, int trackLength, String bitRate,
			String codec) {
		this.artistName = artistName;
		this.albumName = albumName;
		this.trackName = (trackName.length() <= 90) ? trackName : trackName.substring(0, 89);
		this.trackNo = trackNo;
		this.path = path;
		this.trackLength = trackLength;
		this.bitRate = bitRate;
		this.codec = codec;
	}
	
	/**
	 * Get the artist name
	 * @return the artist name
	 */
	public String getArtistName(){
		return this.artistName;
	}
	
	/**
	 * Get the album name
	 * @return the album name
	 */
	public String getAlbumName(){
		return this.albumName;
	}
	
	/**
	 * Get the track name
	 * @return the track name
	 */
	public String getTrackName(){
		return this.trackName;
	}
	
	/**
	 * Get the path to the track
	 * @return the path to the track
	 */
	public String getPath(){
		return this.path;
	}
	
	/**
	 * Get the length of the track in seconds
	 * @return the length of the track
	 */
	public int getTrackLength(){
		return this.trackLength;
	}
	
	/**
	 * Get the file extension used. May be improved to give actual codec.
	 * @return the file type
	 */
	public String getCodec(){
		return this.codec;
	}
	
	/**
	 * Get the track number on the CD
	 * @return the track number
	 */
	public int getTrackNo(){
		return this.trackNo;
	}
	
	/**
	 * Get the bit rate as a string. May be VBR etc
	 * @return the bit rate
	 */
	public String getBitRate(){
		return this.bitRate;
	}
}
