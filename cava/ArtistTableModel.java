package cava;
/**
 * Controls the data for the artist table in the browser class.
 * @author Ben
 */
@SuppressWarnings("serial")
public class ArtistTableModel extends BrowserTableModel {
	private static String[] defaultColumnHeaders = {"Artist"}; 
	private TrackDatabase db;
	//private Artist[] artists=null;
	private ArtistContainer artists;
	
	/**
	 * Create a new model of the table based on the supplied artist container
	 * @param artists an ArtistContainer object; the artists to be displayed
	 */
	public ArtistTableModel(ArtistContainer artists){
		this.artists = artists;
		db = new TrackDatabase();
		if(db.isConnected()){
			//override column names in parent class
			columnNames = defaultColumnHeaders;
			showArtistsWithIDs(null);
		}
	}
	
	//Refresh list of artists, but don't change anything else. 
	public void tracksImported(){
		showArtistsWithIDs(null);
	}

	public void showArtistsWithIDs(Integer[] artistIDs){
		if(artistIDs==null){
			artists.updateArtists();
		}else{
			artists.getArtistsWithIDs(artistIDs);
		}
		//Plus one for "All"
		setRowCount(artists.getNumArtists()+1);
	}
	
	public int getRowNumberToJumpTo(String jumpTo){
		return artists.getFirstRowMatching(jumpTo.toLowerCase());
	}
	
	@SuppressWarnings("unchecked")
    @Override
	public Class getColumnClass(int columnIndex){
		switch(columnIndex){
		case -1:
			return Integer.class;
		case 0:
			return String.class;
		default:
			return Integer.class;
		}
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		//If row = -1, then the row doesn't exist
		if(rowIndex==-1){
			return null;
		}
		try{
			switch (columnIndex) {
			case -1:
				return (rowIndex == 0) ? 0  : artists.getArtistByRowNumber(rowIndex-1).getArtistID();
			case 0:
				return (rowIndex == 0) ? "All"  : artists.getArtistByRowNumber(rowIndex-1).toString();
			default:
				return (rowIndex == 0) ? 0  : artists.getArtistByRowNumber(rowIndex-1).getArtistID();
			}
		}catch(Exception e){
			return null;
		}
	}


}
