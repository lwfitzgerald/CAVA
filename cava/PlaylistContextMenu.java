package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;

@SuppressWarnings("serial")
public class PlaylistContextMenu extends ContextMenu {

	private JTable playlistTracksTable;
	

	public PlaylistContextMenu(JTable thisTable, Point point, ListenerFor menuFor,AudioPlayer audioPlayer, JTable playlistTracksTable) {
		super(thisTable, point, menuFor, audioPlayer);

		this.playlistTracksTable = playlistTracksTable;
		JMenuItem item = newMenuItem("Delete Playlist",ListenerFor.deletePlaylist);
		this.add(item);
		item = newMenuItem("Rename Playlist",ListenerFor.renamePlaylist);
		this.add(item);
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
			int row = thisTable.rowAtPoint(point);
			int playlistID = (Integer) ((PlaylistTableModel)thisTable.getModel()).getValueAt(row, -2);
			switch (listensTo) {
			case deletePlaylist:
				((PlaylistTableModel)thisTable.getModel()).deletePlaylist(playlistID);
				((PlaylistTableModel)thisTable.getModel()).fireTableDataChanged();
				((PlaylistTracksTableModel)playlistTracksTable.getModel()).playlistDeleted(playlistID);
				((PlaylistTracksTableModel)playlistTracksTable.getModel()).fireTableDataChanged();
				break;
			case renamePlaylist:
				String newPlaylistName = (String) JOptionPane.showInputDialog(Cava.mainCavaFrame,"New Name For \""+ ((PlaylistTableModel)thisTable.getModel()).getValueAt(row, -3) +"\"","Rename Playlist",JOptionPane.PLAIN_MESSAGE);
				if(newPlaylistName != null && newPlaylistName.length() > 0){
					((PlaylistTableModel)thisTable.getModel()).renamePlaylist(playlistID,newPlaylistName);
					((PlaylistTracksTableModel)playlistTracksTable.getModel()).fireTableDataChanged();
				}
				break; 
			default:
				Dbg.syserr("Unexpected command for playlist table context menu");
				break;
			}

		}

	}

}
