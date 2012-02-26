package cava;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TrackSelection implements Transferable {
	
	ClientTrack[] selectedTracks;

	public TrackSelection(ClientTrack[] selectedTracks) {
		this.selectedTracks = selectedTracks;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)throws UnsupportedFlavorException, IOException {
		//System.out.println("Attempting to return transferrable data");
		return selectedTracks;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] flavors = {Cava.trackDataFlavor};
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if(flavor.equals(Cava.trackDataFlavor)){
			return true;
		}else{
			Dbg.syserr("Not supported");
			return false;
		}
	}
	
	public ClientTrack[] getTracks(){
		return selectedTracks;
	}

}
