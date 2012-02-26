package cava;

import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class TrackTransferHandler extends TransferHandler {
	
	public int getSourceActions(JComponent c) {
	    return COPY_OR_MOVE;
	}

	protected Transferable createTransferable(JComponent c) {
		//System.out.println("Dragging...");
		ClientTrack[] selectedTracks = getSelectedTracks(c);
		//return new StringSelection("Test");
		return new TrackSelection(selectedTracks);
	}

	protected void exportDone(JComponent c, Transferable t, int action) {
		//Do nothing to original table
	}
	
	protected ClientTrack[] getSelectedTracks(JComponent c){
		JTable table = (JTable) c;
		int[] selectedRows = table.getSelectedRows();
		ClientTrack[] selectedTracks = new ClientTrack[selectedRows.length];
		for(int i =0;i<selectedRows.length;i++){
			//System.out.println("Dragging row: " + i);
			selectedTracks[i] = (ClientTrack) table.getValueAt(selectedRows[i], -1);
		}
		return selectedTracks;
	}

}
