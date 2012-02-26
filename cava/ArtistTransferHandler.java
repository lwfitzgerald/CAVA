package cava;

import javax.swing.JComponent;
import javax.swing.JTable;

@SuppressWarnings("serial")
public class ArtistTransferHandler extends TrackTransferHandler {
	
	TrackDatabase db;
	ArtistTransferHandler(){
		this.db = new TrackDatabase();
	}
	
	protected ClientTrack[] getSelectedTracks(JComponent c){
		JTable artistTable = (JTable)c;
		int artistID = (Integer) ((ArtistTableModel)artistTable.getModel()).getValueAt(artistTable.getSelectedRow(), -1);
		return db.getTracksByArtist(artistID);
		
	}
}