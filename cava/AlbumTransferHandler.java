package cava;

import javax.swing.JComponent;
import javax.swing.JTable;

@SuppressWarnings("serial")
public class AlbumTransferHandler extends TrackTransferHandler {
	
	TrackDatabase db;
	AlbumTransferHandler(){
		this.db = new TrackDatabase();
	}
	
	protected ClientTrack[] getSelectedTracks(JComponent c){
		JTable albumTable = (JTable)c;
		int albumID = (Integer) ((AlbumTableModel)albumTable.getModel()).getValueAt(albumTable.getSelectedRow(), -1);
		int artistID = ((AlbumTableModel)albumTable.getModel()).getCurrentArtistID();
		return db.getTracksByArtistAndAlbum(artistID, albumID);
		
	}
}
