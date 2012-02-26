package cava;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class PlaylistTracksTransferHandler extends TransferHandler {

	JTable playlistTable;

	public PlaylistTracksTransferHandler(JTable playlistTable) {
		this.playlistTable = playlistTable;
	}

	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	protected Transferable createTransferable(JComponent c) {
		//System.out.println("Dragging...");
		JTable table = (JTable) c;
		int[] selectedRows = table.getSelectedRows();
		return new PlaylistSelection(selectedRows);
	}

	public boolean canImport(TransferHandler.TransferSupport info){
		if(info.isDataFlavorSupported(Cava.trackDataFlavor)){
			return true;
		}
		if(info.isDataFlavorSupported(Cava.playlistItemFlavor)){
			return true;
		}
		Dbg.syserr("Unsupported flavour type (PlaylistTracksTrasferHandler.canImport())");
		return false;
	}


	public boolean importData(TransferHandler.TransferSupport info) {
		Dbg.syserr("Importing Data");

		if(!info.isDrop()){
			return false;
		}

		DataFlavor[] dataFlavors = info.getDataFlavors();
		if(dataFlavors.length != 1){
			Dbg.syserr("Unexpected multiple flavours for drop action");
			return false;
		}

		JTable playlistTable = (JTable) info.getComponent();
		JTable.DropLocation dropLocation = (JTable.DropLocation) info.getDropLocation();
		//System.out.println("Dropping at: " + dropLocation.getRow());

		if(dataFlavors[0]==Cava.trackDataFlavor){
			//System.out.println("Adding Tracks");
			try {
				ClientTrack[] tracksToAdd = (ClientTrack[]) info.getTransferable().getTransferData(Cava.trackDataFlavor);
				((PlaylistTracksTableModel)playlistTable.getModel()).addTracks(tracksToAdd,dropLocation.getRow());
				//System.out.println("After moving tracks");
				((PlaylistTracksTableModel)playlistTable.getModel()).fireTableDataChanged();
			} catch (UnsupportedFlavorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		if(dataFlavors[0]==Cava.playlistItemFlavor){
			try {
				//System.out.println("Re-ordering Playlist");
				int[] rowsToMove = (int[]) info.getTransferable().getTransferData(Cava.playlistItemFlavor);
				//				Arrays.sort(rowsToMove);
				//				int[] playlistTrackIDsToMove = new int[rowsToMove.length];
				//				for(int i=0;i<rowsToMove.length;i++){
				//					playlistTrackIDsToMove[i] = (Integer) ((PlaylistTracksTableModel)playlistTable.getModel()).getValueAt(rowsToMove[i], -2);
				//				}
				//System.out.println("Moving tracks in transfer class");
				//PlaylistTracksTableModel model = (PlaylistTracksTableModel) playlistTable.getModel();
				((PlaylistTracksTableModel)playlistTable.getModel()).moveTracks(rowsToMove,dropLocation.getRow());
				//System.out.println("After moving tracks");
				//this.playlistTable.setRowSelectionInterval(this.playlistTable.getSelectedRow(),this.playlistTable.getSelectedRow());
				//((PlaylistTableModel)this.playlistTable.getModel()).fireTableDataChanged();
				//((PlaylistTracksTableModel)playlistTable.getModel()).setPlaylistID(((PlaylistTableModel)this.playlistTable.getModel()).getCurrentPlaylist().getPlaylistID());
				((PlaylistTracksTableModel)playlistTable.getModel()).fireTableDataChanged();
			} catch (UnsupportedFlavorException e) {
				System.err.println("Unsupported Flavour");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("IO Exception");
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			} catch (Error e){
				e.printStackTrace();
			}
		}
		return false;
	}

}
