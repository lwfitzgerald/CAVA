package cava;

/**
 * @author Peter
 * This class is the Album structure, stores the information read from the track database.
 */
public class Album {
    private String albumName;
    private int artistID;
    private int albumID;
    private String artLocation;

    /**
     * Create a new album object
     * @param albumName - the album's title
     * @param albumID - the unique key for the album
     * @param artistID - the unique key for the artist of the album
     * @param artLocation - path to the location of the album
     */
    public Album(String albumName, int albumID, int artistID, String artLocation){
        this.albumName = albumName;
        this.albumID = albumID;
        this.artistID = artistID;
        this.artLocation = artLocation;
    }

    /**
     * @return the title of the album
     */
    public String getAlbumName(){
        return albumName;
    }

    /**
     * @return the key of the album
     */
    public int getAlbumID(){
        return albumID;
    }

    /**
     * @return the artist ID of the album's artist
     */
    public int getArtistID(){
        return artistID;
    }

    /**
     * @return the path to the location of the album
     */
    public String getArtLocation(){
        return artLocation;
    }
    
    @Override
    /**
     * Overridden method returns the album name
     */
    public String toString(){
    	return this.albumName;
    }

}
