package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class PlaylistTracksContextMenu extends ContextMenu {

	private JTable playlistTable;

	public PlaylistTracksContextMenu(JTable thisTable, Point point, ListenerFor menuFor,AudioPlayer audioPlayer, JTable playlistTable) {
		super(thisTable, point, menuFor, audioPlayer);

		this.playlistTable = playlistTable;

		JMenuItem item = newMenuItem("Remove From Playlist",ListenerFor.RemoveFromPlaylist);
		this.add(item);
		showMenu();
	}

	private JMenuItem newMenuItem(String itemLabel, ListenerFor listensTo) {
		JMenuItem newMenuItem = new JMenuItem(itemLabel);
		newMenuItem.addActionListener(new PlaylistTracksMenuListener());
		newMenuItem.setActionCommand(listensTo.toString());
		return newMenuItem;
	}

	private class PlaylistTracksMenuListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			ListenerFor listensTo = ListenerFor.valueOf(e.getActionCommand());
			PlaylistTableModel playlistTableModel = (PlaylistTableModel)playlistTable.getModel();
			
			int[] selectedRows  = thisTable.getSelectedRows();
			Track[] selectedTracks = new Track[selectedRows.length];
			for(int i=0;i<selectedRows.length;i++){
				selectedTracks[i] = (Track) ((AbstractTableModel)thisTable.getModel()).getValueAt(selectedRows[i],-1);
			}
			//int row = thisTable.rowAtPoint(point);
			switch (listensTo) {
			case RemoveFromPlaylist:
				int playlistID = playlistTableModel.getCurrentPlaylist().getPlaylistID();
				if(playlistTableModel.removeFromPlaylist(playlistID,selectedRows)){
					audioPlayer.DoAudioAction(AudioAction.CurrentTrackRemoved);
				}
				playlistTableModel.fireTableDataChanged();
				((PlaylistTracksTableModel)thisTable.getModel()).setPlaylistID(playlistTableModel.getCurrentPlaylist().getPlaylistID());
				((PlaylistTracksTableModel)thisTable.getModel()).fireTableDataChanged();
				break;
			default:
				Dbg.syserr("Unexpected command for playlist track table context menu");
				break;
			}

		}

	}

}
