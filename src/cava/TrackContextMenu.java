package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class TrackContextMenu extends ContextMenu {

	private JTable playlistTable;
	

	public TrackContextMenu(JTable thisTable, Point point, ListenerFor menuFor,AudioPlayer audioPlayer, JTable playlistTable) {
		super(thisTable, point, menuFor, audioPlayer);

		this.playlistTable = playlistTable;

		JMenuItem item = newMenuItem("Create Playlist From...",ListenerFor.newPlaylist);
		this.add(item);
		item = newMenuItem("Create Smart Playlist From...",ListenerFor.newSmartPlaylist);
		this.add(item);
		this.add(getPlaylistAdder(playlistTable));
		if(Preferences.getScmsPreference() && Preferences.getSpotifyEnabled()){
			this.add(getSpotifyPlaylist(playlistTable, ListenerFor.Track));
		}
		showMenu();
	}

	private JMenuItem newMenuItem(String itemLabel, ListenerFor listensTo) {
		JMenuItem newMenuItem = new JMenuItem(itemLabel);
		newMenuItem.addActionListener(new TrackMenuListener());
		newMenuItem.setActionCommand(listensTo.toString());
		return newMenuItem;
	}

	private class TrackMenuListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			ListenerFor listensTo = ListenerFor.valueOf(e.getActionCommand());
            PlaylistTableModel playlistTableModel = (PlaylistTableModel)playlistTable.getModel();
            
			int[] selectedRows  = thisTable.getSelectedRows();
			ClientTrack[] selectedTracks = new ClientTrack[selectedRows.length];
			for(int i=0;i<selectedRows.length;i++){
				selectedTracks[i] = (ClientTrack) ((AbstractTableModel)thisTable.getModel()).getValueAt(selectedRows[i],-1);
			}
			//Track track =  (Track) ((TrackTableModel)thisTable.getModel()).getValueAt(thisTable.rowAtPoint(point),-1);
			switch (listensTo) {
			case newPlaylist:
			    playlistTableModel.createNewPlaylist(selectedTracks,"New Playlist");
			    playlistTableModel.fireTableDataChanged();
				break;
			case newSmartPlaylist:
				PlaylistGenerator.generateSmart(selectedTracks, db, playlistTableModel);
				break; 
			default:
				Dbg.syserr("Unexpected command for track table context menu");
				break;
			}

		}

	}

}
