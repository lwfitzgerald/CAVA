package cava;

/**
 * Controls the data for the album table in the browser class
 * @author Ben
 */
@SuppressWarnings("serial")
public class AlbumTableModel extends BrowserTableModel {
	private static String[] defaultColumnHeaders = {"Album"}; 
	private TrackDatabase db;
	private AlbumContainer albums=null;
	int currentArtistID=-1;
	
	/**
	 * Create a new model of the table based on the supplied album container
	 * @param albums an AlbumContainer object; the albums to be displayed
	 */
	public AlbumTableModel(AlbumContainer albums){
		db = new TrackDatabase();
		this.albums = albums;
		if(db.isConnected()){
			//override column names in parent class
			columnNames = defaultColumnHeaders;
			updateArtistID(0);
		}
	}
	
	/**
	 * Update the artistID to narrow the album display. If set to 0, all albums will be displayed.
	 * If a search is in progress, only albums from that search will be shown.
	 * @param artistID the artistID to narrow the search by
	 */
	public void updateArtistID(int artistID){
		//if(currentArtistID != artistID){
			albums.updateArtistID(artistID);
			//Plus one for "All"
			setRowCount(albums.getNumAlbums() + 1);
			
			currentArtistID=artistID;
		//}
	}
	
	/**
	 * Tracks have been imported, so reload the albums with the currently
	 * selected abum
	 */
	public void tracksImported(){
		updateArtistID(currentArtistID);
	}
	
	
	/**
	 * Show only albums with an ID in the given array. Used by the search feature.
	 * @param albumIDs The array of IDs to show.
	 */
	public void showAlbumsWithIDs(Integer[] albumIDs){
		if(albumIDs==null){
			albums.updateArtistID(0);
		}else{
			albums.getAlbumsWithIDs(albumIDs);
		}
		//Plus one for "All"
		setRowCount(albums.getNumAlbums()+1);
	}
	
	public int getRowNumberToJumpTo(String jumpTo){
		return albums.getFirstRowMatching(jumpTo.toLowerCase());
	}
	
	public int getCurrentArtistID(){
		return this.currentArtistID;
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
				return (rowIndex == 0) ? 0  : albums.getAlbumByRowNumber(rowIndex-1).getAlbumID();
			case 0:
				return (rowIndex == 0) ? "All"  : albums.getAlbumByRowNumber(rowIndex-1).toString();
			default:
				return (rowIndex == 0) ? 0  : albums.getAlbumByRowNumber(rowIndex-1).getAlbumID();
			}
		}catch(Exception e){
			return null;
		}
	}	

}
