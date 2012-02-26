package cava;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class PlaylistSelection implements Transferable {
	
	int[] playlistIDs;

	public PlaylistSelection(int[] selectedRows) {
		this.playlistIDs = selectedRows;
	}

	@Override
	public Object getTransferData(DataFlavor arg0)
			throws UnsupportedFlavorException, IOException {
		return playlistIDs;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] flavors = {Cava.playlistItemFlavor};
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if(flavor.equals(Cava.playlistItemFlavor)){
			return true;
		}else{
			Dbg.syserr("Not supported");
			return false;
		}
	}

}
