package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class ContextMenu extends JPopupMenu {

	protected JTable thisTable;
	protected Point point;
	protected TrackDatabase db;
	protected AudioPlayer audioPlayer;
	protected ListenerFor menuFor;

	public ContextMenu(JTable thisTable, Point point, ListenerFor menuFor, AudioPlayer audioPlayer) {
		this.thisTable = thisTable;
		this.point = point;
		this.audioPlayer = audioPlayer;
		this.menuFor = menuFor;
		db = new TrackDatabase();
	}

	public void showMenu(){
		super.show(thisTable.findComponentAt(point),point.x,point.y);
	}
	
	protected JMenu getPlaylistAdder(JTable playlistTable){
		//Use the default for playlisttracks and tracks tables
		return getPlaylistAdder(playlistTable,null);
	}

	protected JMenu getPlaylistAdder(JTable playlistTable, ListenerFor listensTo){
		JMenu playlistSubMenu = new JMenu("Add To Playlist");
		PlaylistTableModel playlistTableModel = (PlaylistTableModel)playlistTable.getModel();
		Playlist nowPlaying = playlistTableModel.getNowPlaying();
		JMenuItem item = playlistItem(nowPlaying.getPlaylistName(),nowPlaying.getPlaylistID(),playlistTableModel,listensTo);
		playlistSubMenu.add(item);
		Playlist[] playlists = playlistTableModel.getPlaylists();
		if(playlists!=null){
			for(Playlist p : playlists){
				item = playlistItem(p.getPlaylistName(),p.getPlaylistID(),playlistTableModel,listensTo);
				playlistSubMenu.add(item);

			}
		}
		return playlistSubMenu;
	}


	protected JMenuItem playlistItem(String playlistName,int playlistID,PlaylistTableModel playlistTableModel,ListenerFor listensTo){
		JMenuItem newMenuItem = new JMenuItem(playlistName);
		newMenuItem.addActionListener(new PlaylistAdderListener(playlistTableModel,listensTo));
		newMenuItem.setActionCommand(Integer.toString(playlistID));
		return newMenuItem;
	}
	
	protected JMenuItem getSpotifyPlaylist(JTable playlistTable, ListenerFor listensTo){
		JMenuItem spotifyPlaylist = new JMenuItem("Create Spotify Playlist");
		PlaylistTableModel playlistTableModel = ((PlaylistTableModel)playlistTable.getModel());
		spotifyPlaylist.addActionListener(new SpotifyPlaylistCreator(getSelectedTracks(listensTo, playlistTableModel), playlistTableModel));
		return spotifyPlaylist;
	}
	
	private class PlaylistAdderListener implements ActionListener{

		private PlaylistTableModel playlistTableModel;
		private ListenerFor listensTo;
		
		public PlaylistAdderListener(PlaylistTableModel playlistTableModel, ListenerFor listensTo) {
			this.playlistTableModel = playlistTableModel;
			this.listensTo = listensTo;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			//Default option for track and playlist tracks table
			int playlistID = Integer.parseInt(e.getActionCommand());
			playlistTableModel.addToPlaylist(playlistID,getSelectedTracks(listensTo, playlistTableModel));
			playlistTableModel.fireTableDataChanged();
		}
		
	}
	
	private class SpotifyPlaylistCreator implements ActionListener {
        private ClientTrack[] selectedTracks;
		private PlaylistTableModel playlistTableModel;
		
		public SpotifyPlaylistCreator(ClientTrack[] selectedTracks, PlaylistTableModel playlistTableModel) {
			this.selectedTracks = selectedTracks;
			this.playlistTableModel = playlistTableModel;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			PlaylistGenerator.generateSpotify(selectedTracks, db, playlistTableModel);
		}
	}
	
	private ClientTrack[] getSelectedTracks(ListenerFor listensTo, PlaylistTableModel playlistTableModel){
		if(listensTo==ListenerFor.Artist){
			int artistID = (Integer) ((ArtistTableModel)thisTable.getModel()).getValueAt(thisTable.rowAtPoint(point), -1);
			if(artistID==0){
				return null;
			}else{
				return db.getTracksByArtist(artistID);
			}
		}else if(listensTo==ListenerFor.Album){
			int albumID = (Integer) ((AlbumTableModel)thisTable.getModel()).getValueAt(thisTable.rowAtPoint(point), -1);
			int artistID = ((AlbumTableModel)thisTable.getModel()).getCurrentArtistID();
			if(albumID==0){
				return null;
			}else{
				return db.getTracksByArtistAndAlbum(artistID, albumID);
			}
		}else{
			int[] selectedRows  = thisTable.getSelectedRows();
			ClientTrack[] selectedTracks = new ClientTrack[selectedRows.length];
			for(int i=0;i<selectedRows.length;i++){
				selectedTracks[i] = (ClientTrack) ((AbstractTableModel)thisTable.getModel()).getValueAt(selectedRows[i],-1);
			}
			//Track track = (Track) ((AbstractTableModel)thisTable.getModel()).getValueAt(thisTable.rowAtPoint(point),-1);
			return selectedTracks;
		}
	}

}
