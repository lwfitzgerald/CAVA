package cava;

/**
 * 
 * @author Ben
 * This class is effectively an Artists datastructure. It stores the information read from the database.
 * It means that DB queries can be done using the indexed integers, but other parts of the program can
 * refer to artists by their name
 */
public class Artist {
    private String artistName;
    private int artistID;
    private String sortByName;

    /**
     * Create a new artist object
     * @param artistID - the unique identifier for the artist
     * @param artistName - the artist's name
     * @param sortByName - the name used to sort the artist; e.g. without 'the'.
     */
    public Artist(int artistID, String artistName, String sortByName){
        this.artistID = artistID;
        this.artistName = artistName;
        this.sortByName = sortByName;
    }

    /**
     * 
     * @return the artist name
     */
    public String getArtistName(){
        return artistName;
    }

    /**
     * 
     * @return the artist ID
     */
    public int getArtistID(){
        return artistID;
    }

    /**
     * 
     * @return the artist's sort name
     */
    public String getsortByName(){
        return sortByName;
    }
    
    @Override
    /**
     * Overridden method returns the artist name
     */
    public String toString(){
    	return this.artistName;
    }


}
