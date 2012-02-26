package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;

@SuppressWarnings("serial")
public class ArtistContextMenu extends ContextMenu {

	private JTable playlistTable;
	

	public ArtistContextMenu(JTable thisTable, Point point, ListenerFor menuFor,AudioPlayer audioPlayer, JTable playlistTable) {
		super(thisTable, point, menuFor, audioPlayer);

		this.playlistTable = playlistTable;

		JMenuItem item = newMenuItem("Create Playlist From...",ListenerFor.newPlaylist);
		this.add(item);
		item = newMenuItem("Create Smart Playlist From...",ListenerFor.newSmartPlaylist);
		this.add(item);
		this.add(getPlaylistAdder(playlistTable,ListenerFor.Artist));
		if(Preferences.getScmsPreference() && Preferences.getSpotifyEnabled()){
			this.add(getSpotifyPlaylist(playlistTable, ListenerFor.Artist));
		}
		showMenu();
	}

	private JMenuItem newMenuItem(String itemLabel, ListenerFor listensTo) {
		JMenuItem newMenuItem = new JMenuItem(itemLabel);
		newMenuItem.addActionListener(new ArtistMenuListener());
		newMenuItem.setActionCommand(listensTo.toString());
		return newMenuItem;
	}

	private class ArtistMenuListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			ListenerFor listensTo = ListenerFor.valueOf(e.getActionCommand());
			PlaylistTableModel playlistTableModel = (PlaylistTableModel)playlistTable.getModel();
			
			int artistID = (Integer) ((ArtistTableModel)thisTable.getModel()).getValueAt(thisTable.rowAtPoint(point), -1);
			if(artistID==0){
				return;
			}
			
			switch (listensTo) {
			case newPlaylist:
			    playlistTableModel.createNewPlaylist(db.getTrackIDsByArtist(artistID),"New Playlist");
			    playlistTableModel.fireTableDataChanged();
				break;
			case newSmartPlaylist:
				//Check SCMS data is available for each track. If it isn't, then set to null. Make sure all track's SCMS data isn't
				//null before attempting build
				ClientTrack[] selectedTracks = db.getTracksByArtist(artistID);
				PlaylistGenerator.generateSmart(selectedTracks, db, playlistTableModel);
				break;
			default:
				Dbg.syserr("Unexpected command for artist table context menu");
				break;
			}

		}

	}

}
