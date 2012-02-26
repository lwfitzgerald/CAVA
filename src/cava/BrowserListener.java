package cava;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BrowserListener implements MouseListener, FocusListener, ActionListener, DocumentListener, ListSelectionListener, KeyListener{
	private JTable artistTable;
	private JTable albumTable;
	private JTable trackTable;
	private JTable playlistTable;
	private JTable playlistTracksTable;
	private JTextField searchBox;
	private ListenerFor listensTo;

	private long searchDelay = 500;

	private boolean debugging = false;
	private AudioPlayer audioPlayer;
	private NowPlaying nowPlaying;
	private boolean updatingArtist;
	private boolean updatingAlbum;
	private boolean updatingPlaylist;
	private Long lastKeyTyped;
	private String jumpTo;

	//These fields need to be static across all instances. When a search is running, all
	//instances need to be able to read from these to find out if a search has happened
	private static Integer[] allMatchingArtists = null;
	private static Integer[] matchingAlbums = null; 
	private static ClientTrack[] matchingTracks = null;
	private static int currentArtistID;
	private static int currentAlbumID;
	private static boolean searchRunning = false;
	private static SearchRunner searchRunner = null;

	BrowserListener(JTable artistTable, JTable albumTable, JTable trackTable,JTable playlistTable,JTable playlistTracksTable,JTextField searchBox, ListenerFor listensTo,AudioPlayer audioPlayer,NowPlaying nowPlaying){
		this.artistTable = artistTable;
		this.albumTable = albumTable;
		this.trackTable = trackTable;
		this.searchBox = searchBox;
		this.listensTo = listensTo;
		this.audioPlayer = audioPlayer;
		this.playlistTable = playlistTable;
		this.playlistTracksTable = playlistTracksTable;
		this.nowPlaying = nowPlaying;
		updatingArtist = false;
		updatingAlbum = false;
		updatingPlaylist = false;
		currentAlbumID = 0;
		currentArtistID = 0;
		lastKeyTyped = 0L;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		//only perform actions on left click (for the time being)
		Point point = e.getPoint();
		if(e.getButton() == MouseEvent.BUTTON1){
			int count = e.getClickCount();
			boolean doubleClick = (count == 1) ? false : true;
			switch(this.listensTo){
			case Artist:
				handleArtistClick(point, doubleClick);
				break;
			case Album:
				handleAlbumClick(point, doubleClick);
				break;
			case Track:
				handleTrackClick(point, doubleClick);
				break;
			case Playlist:
				handlePlaylistClick(point, doubleClick);
				break;
			case PlaylistTrack:
				handlePlaylistTracksClick(point, doubleClick);
				break;			
			}
		}else if(e.getButton() == MouseEvent.BUTTON3){
			switch(this.listensTo){
			case Artist:
				handleArtistRightClick(point);
				break;
			case Album:
				handleAlbumRightClick(point);
				break;
			case Track:
				handleTrackRightClick(point);
				break;
			case PlaylistTrack:
				handlePlaylistTracksRightClick(point);
				break;
			case Playlist:
				handlePlaylistRightClick(point);
			}
		}
	}

	private void handleArtistClick(Point point, boolean doubleClick) {		
		if(doubleClick && validRow(artistTable, point)){
			while(updatingArtist){
				//Wait for artist to update
				
			}
			handleTrackClick(0);
		}
	}

	private void handlePlaylistClick(Point point,boolean doubleClick){
		if(doubleClick && validRow(playlistTable, point)){
			while(updatingPlaylist){
				//Wait for display update to finish
			}
			handlePlaylistTracksClick(0);
		}
	}

	private void handleArtistRowChange(int row){
		updatingArtist = true;
		//Get artist ID. Setting column as -1 will return this.
		Integer artistID = (Integer) artistTable.getValueAt(row, -1);
		//check to make sure a valid row was clicked
		if(artistID==null){
			return;
		}
		//If there's no search, process artist clicks in the usual way

		if(allMatchingArtists==null && searchRunner!=null){
			//throw away clicks if a search if a search is in progress#
			if(debugging){
				System.out.println("ignoring artist click");
			}
			updatingArtist = false;
			return;			
		}

		currentArtistID = artistID;
		//Reset Album ID
		currentAlbumID = 0;
		//Reset matchingAlbums
		matchingAlbums = null;
		if(allMatchingArtists==null){
			if(debugging){
				System.out.println("Normal artist click");
			}
			AlbumTableModel albumModel = (AlbumTableModel) albumTable.getModel();
			albumModel.updateArtistID(artistID);
			albumModel.fireTableDataChanged();
			TrackTableModel trackModel = (TrackTableModel) trackTable.getModel();
			//Reset album
			trackModel.updateArtistIDAndAlbumID(artistID, 0);
			trackModel.fireTableDataChanged();				
		}else{
			if(debugging){
				System.out.println("Artist click as search");
			}
			//A search is in progress, so we need to run another 'search'
			//Only want to refresh album table though
			handleSearch(false,false,true);		
		}
		updatingArtist = false;
	}



	private void handleAlbumClick(Point point, boolean doubleClick) {
		if(doubleClick && validRow(albumTable, point)){
			while(updatingAlbum){
				//wait for display to finish update
			}
			handleTrackClick(0);
		}

	}

	private void handleAlbumRowChange(int row){
		updatingAlbum = true;
		Integer albumID = (Integer) albumTable.getValueAt(row, -1);
		//Check to make sure a valid row was clicked
		if(albumID==null){
			System.out.println("AlbumID is null");
			return;
		}
		
		if(matchingAlbums==null && searchRunner != null){
			//throw away clicks if a search if a search is in progress#
			if(debugging){
				System.out.println("ignoring album click");
			}
			updatingAlbum = false;
			return;			
		}
		
		currentAlbumID = albumID;
		//If there's no search, process clicks in the usual way
		if(matchingAlbums==null){
			if(debugging){
				System.out.println("Updating albums normally");
			}
			TrackTableModel trackModel = (TrackTableModel) trackTable.getModel();
			trackModel.updateAlbumID(albumID);
			trackModel.fireTableDataChanged();
		}else{
			if(debugging){
				System.out.println("Running album click as search");
			}
			//A search has been run, so need to run another 'search' to
			//process this click. Don't refresh either artist or album table
			handleSearch(false,false,false);
		}
		updatingAlbum = false;
	}

	private void handlePlaylistRowChange(int row){
		updatingPlaylist = true;
		Integer playlistID = (Integer) playlistTable.getValueAt(row, -2);
		PlaylistTableModel playlistModel = (PlaylistTableModel) playlistTable.getModel();
		//Track[] tracksInPlaylist = (Track[])playlistModel.getValueAt(row, -1);
		playlistModel.setCurrentPlaylist(row);
		((PlaylistTracksTableModel)playlistTracksTable.getModel()).setPlaylistID(playlistID);
		((PlaylistTracksTableModel)playlistTracksTable.getModel()).fireTableDataChanged();
		updatingPlaylist = false;
	}

	private void handleTrackClick(Point point, boolean doubleClick) {
		if(doubleClick && validRow(trackTable, point)){
			int row = trackTable.rowAtPoint(point);
			changeTrack(row, trackTable);			
		}

	}

	private void handleTrackClick(int row){
		changeTrack(row, trackTable);

	}

	private void handlePlaylistTracksClick(Point point, boolean doubleClick){
		if(doubleClick && validRow(playlistTracksTable, point)){
			int row = playlistTracksTable.rowAtPoint(point);
			changeTrack(row, playlistTracksTable);
		}
	}

	private void handlePlaylistTracksClick(int row){
		changeTrack(row,playlistTracksTable);
	}

	private void changeTrack(int row, JTable table){
		int numRows = table.getRowCount();
		ArrayList<Track> listOfTracksToPlay = new ArrayList<Track>();
		for(int i = 0;i<numRows;i++){
			listOfTracksToPlay.add((Track) table.getValueAt(i, -1));
		}
		nowPlaying.setTracks(listOfTracksToPlay);
		nowPlaying.setPosition(row);
		//audioPlayer.setPlayQueue(listOfTracksToPlay,trackTable.rowAtPoint(point));
		audioPlayer.DoAudioAction(AudioAction.Play);
		//((PlaylistTableModel)playlistTable.getModel()).setNowPlayingTracks(listOfTracksToPlay.toArray(new Track[0]));
		((PlaylistTableModel)playlistTable.getModel()).fireTableDataChanged();
		((TrackTableModel)trackTable.getModel()).fireTableDataChanged();		
		//Update tracks in playlist if now-playing is the currently selected playlist
		if(((PlaylistTableModel)playlistTable.getModel()).getCurrentPlaylist().getPlaylistID()==-1){
			handlePlaylistRowChange(0);
		}

	}

	private void handleArtistRightClick(Point point){
		//System.out.println("Handling track right click");
		if(validRow(artistTable, point) && artistTable.rowAtPoint(point) != 0){
			artistTable.setRowSelectionInterval(artistTable.rowAtPoint(point), artistTable.rowAtPoint(point));
			new ArtistContextMenu(artistTable, point, listensTo, audioPlayer, playlistTable);
		}

	}

	private void handleAlbumRightClick(Point point){
		//System.out.println("Handling track right click");
		if(validRow(albumTable, point) && albumTable.rowAtPoint(point) != 0){
			albumTable.setRowSelectionInterval(albumTable.rowAtPoint(point), albumTable.rowAtPoint(point));
			new AlbumContextMenu(albumTable, point, listensTo, audioPlayer, playlistTable);
		}

	}

	private void handleTrackRightClick(Point point){
		//System.out.println("Handling track right click");
		if(validRow(trackTable, point)){
			if(trackTable.isRowSelected(trackTable.rowAtPoint(point))){
				trackTable.addRowSelectionInterval(trackTable.rowAtPoint(point), trackTable.rowAtPoint(point));	
			}else{
				trackTable.setRowSelectionInterval(trackTable.rowAtPoint(point), trackTable.rowAtPoint(point));
			}
			new TrackContextMenu(trackTable, point, listensTo, audioPlayer, playlistTable);
		}

	}

	private void handlePlaylistTracksRightClick(Point point){
		//System.out.println("Handling track right click");
		if(validRow(playlistTracksTable, point)){
			if(playlistTracksTable.isRowSelected(playlistTracksTable.rowAtPoint(point))){
				playlistTracksTable.addRowSelectionInterval(playlistTracksTable.rowAtPoint(point), playlistTracksTable.rowAtPoint(point));	
			}else{
				playlistTracksTable.setRowSelectionInterval(playlistTracksTable.rowAtPoint(point), playlistTracksTable.rowAtPoint(point));
			}
			new PlaylistTracksContextMenu(playlistTracksTable, point, listensTo, audioPlayer, playlistTable);
		}

	}

	private void handlePlaylistRightClick(Point point){
		//Don't add these option to the first row, now playing:
		if(validRow(playlistTable, point) && playlistTable.rowAtPoint(point) != 0){
			playlistTable.setRowSelectionInterval(playlistTable.rowAtPoint(point), playlistTable.rowAtPoint(point));
			new PlaylistContextMenu(playlistTable, point, listensTo, audioPlayer, playlistTracksTable);
		}
	}

	/**
	 * Internal method to test if the click was on a valid row of the table (i.e. not after the end of the table)
	 * @param table the table the click happened on
	 * @param point the point the click was at
	 * @return true if this point is on a row of the table, false otherwise
	 */
	private boolean validRow(JTable table, Point point){
		return ((table.rowAtPoint(point) == -1) ? false : true);
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void focusGained(FocusEvent e) {
		if(this.listensTo==ListenerFor.SearchBox){
			if(searchBox.getText().contains("Search...")){
				searchBox.setText("");
			}
		}

	}

	@Override
	public void focusLost(FocusEvent e) {
		if(this.listensTo==ListenerFor.SearchBox){
			if(searchBox.getText().equals("")){
				searchBox.setText(" Search...");
			}
		}
	}

	/**
	 * Convert a tree to an array. Used in the searching to convert trees of artist IDs
	 * to arrays for use
	 * @param tree - the tree (T<Integer,Void)> to convert
	 * @return an array containing the keys of the tree
	 */
	private Integer[] treeKeysToArray(TreeMap<Integer,Void> tree){
		int size = tree.size();
		if(size > 0){
			Integer[] array = new Integer[size];
			Integer value = tree.firstKey();;
			int i = 0;
			array[i++] = value;
			while((value=tree.higherKey(value)) != null){
				array[i++] = value;
			}
			return array;

		}else{
			return new Integer[0];
		}
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(listensTo == ListenerFor.CancelButton){
			searchBox.setText("Search...");
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {

	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		handleSearch();	
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		handleSearch();

	}

	private void handleSearch(){
		handleSearch(true,true,true);
	}

	/**
	 * Handle a search. 
	 * @param searchTermChanged If this was a search performed because the search term was updated, this should be true
	 * if this was a 'fake' search because of artist/album filtering, this should be false
	 */
	private void handleSearch(boolean searchTermChanged,boolean refreshArtistTable,boolean refreshAlbumTable) {
		String searchString = searchBox.getText().trim();
		if(debugging){
			System.out.println("Key press detected. Contents of search: " + searchString);
		}

		//Before doing anything else, wait for any existing searches to finsh
		while(searchRunner != null && searchRunning==true){
			try {
				Thread.sleep(50);
				System.out.println("Waiting for search to finish");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//Then try and cancel any searches still running
		if(searchRunner != null && searchRunning==false){
			if(debugging){
				System.out.println("Cancelling search");
			}
			searchRunner.cancel();
		}else{
			System.out.println("Search running?     : " + searchRunning);
			System.out.println("Search runner object: " + searchRunner);
		}

		//If there is a search to run, delegate to the SearchRunner. Otherwise, just update tables.
		if(!searchString.equals("") && !searchString.contains("Search...")){

			//Reset search trackers if search term changed
			if(searchTermChanged){
				if(refreshArtistTable){
					allMatchingArtists = null;
					currentArtistID = 0;
				}
				if(refreshAlbumTable){
					currentAlbumID = 0;
					matchingAlbums = null;
				}
			}

			//Set the field so that the worker can read it			
			searchRunner = new SearchRunner(searchString);
			Timer timer = new Timer();
			if(debugging){
				System.out.println("Scheduling search");
			}
			timer.schedule(searchRunner, searchDelay);

		}else{
			//Reset all views
			allMatchingArtists = null;
			matchingAlbums = null;
			matchingTracks = null;
			searchRunner = null;
			((AlbumTableModel)albumTable.getModel()).showAlbumsWithIDs(null);
			((ArtistTableModel)artistTable.getModel()).showArtistsWithIDs(null);
			((TrackTableModel)trackTable.getModel()).updateSearchString(null,0,currentAlbumID);
			((ArtistTableModel)artistTable.getModel()).fireTableDataChanged();
			((AlbumTableModel)albumTable.getModel()).fireTableDataChanged();
			((TrackTableModel)trackTable.getModel()).fireTableDataChanged();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){

			//Perform the right action for that table
			if(listensTo==ListenerFor.Artist){
				//System.out.println("Changing Artist");
				int selectedRow = artistTable.getSelectedRow();
				//System.out.println("Selected artist row: " + selectedRow);
				if(selectedRow != -1){
					//System.out.println("Updating Artist Table");
					handleArtistRowChange(selectedRow);
				}
			}else if(listensTo==ListenerFor.Album){
				int selectedRow = albumTable.getSelectedRow();
				if(selectedRow != -1){
					handleAlbumRowChange(selectedRow);
				}
			}else if(listensTo == ListenerFor.Playlist){
				int selectedRow = playlistTable.getSelectedRow();
				if(selectedRow != -1){
					handlePlaylistRowChange(selectedRow);
				}
			}
		}

	}


	private class SearchRunner extends TimerTask{
		private String searchString;

		SearchRunner(String searchString){
			this.searchString = searchString;
		}

		@Override
		public void run() {
			searchRunning=true;
			if(debugging){
				Dbg.sysout("Starting search");
			}
			//First find all tracks where the track name, album name or artist name matches
			long start = System.currentTimeMillis();
			System.out.println("Searchstring: " + searchString);
			if(searchString.toLowerCase().startsWith("lyric:")){
				matchingTracks = ((TrackTableModel)trackTable.getModel()).runLyricSearch(searchString.substring(searchString.indexOf(':')+1).trim(),currentArtistID,currentAlbumID);
			}else{
				matchingTracks = ((TrackTableModel)trackTable.getModel()).updateSearchString(searchString,currentArtistID,currentAlbumID);
			}
			long t1 = System.currentTimeMillis();
			System.out.println("Finished track search");

			
			//Create trees for artist and album IDs.
			TreeMap<Integer, Void> matchingArtistTree = new TreeMap<Integer, Void>();
			TreeMap<Integer, Void> matchingAlbumTree = new TreeMap<Integer, Void>();
			if(matchingTracks != null && matchingTracks.length > 0){
				for(ClientTrack t: matchingTracks){
					if(!matchingArtistTree.containsKey(t.getArtistID())){
						matchingArtistTree.put(t.getArtistID(),null);
					}
					if(!matchingAlbumTree.containsKey(t.getAlbumID())){
						matchingAlbumTree.put(t.getAlbumID(),null);
					}

				}
			}
			long t2 = System.currentTimeMillis();

			//Convert them to arrays for use

			//Integer[] matchingArtists = treeKeysToArray(matchingArtistTree);
			//We keep track of all the artists so that we can display them all -- we shouldn't filter the shown artists
			if(allMatchingArtists == null){
				allMatchingArtists = treeKeysToArray(matchingArtistTree);
				((ArtistTableModel)artistTable.getModel()).showArtistsWithIDs(allMatchingArtists);
				((ArtistTableModel)artistTable.getModel()).fireTableDataChanged();
			}

			//We only update this when the artist changes. It will be set to null when that happens
			if(matchingAlbums==null){
				matchingAlbums = treeKeysToArray(matchingAlbumTree);
				((AlbumTableModel)albumTable.getModel()).showAlbumsWithIDs(matchingAlbums);
				((AlbumTableModel)albumTable.getModel()).fireTableDataChanged();
			}
			long t3 = System.currentTimeMillis();


			long t4 = System.currentTimeMillis();


			((TrackTableModel)trackTable.getModel()).fireTableDataChanged();
			long t5 = System.currentTimeMillis();
			searchRunning=false;

			if(debugging){

				long firstSearchTime = t1 - start;
				long listBuildingTime = t2 - t1;
				long arrayConversionTime = t3 - t2;
				long secondSearchTime = t4 - t3;
				long updateTime = t5 - t4;
				
				Dbg.syserr("First Search : " + firstSearchTime);
				Dbg.syserr("List Building: " + listBuildingTime);
				Dbg.syserr("Array Convert: " + arrayConversionTime);
				Dbg.syserr("2nd Search   : " + secondSearchTime);
				Dbg.syserr("Update       : " + updateTime);
				Dbg.syserr("Search finished");

			}
		}

		@Override
		public boolean cancel() {
			return super.cancel();
		}

	}


	@Override
	public void keyPressed(KeyEvent arg0) {
	    // Ignore
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// Ignore
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		if(this.listensTo==ListenerFor.Artist || this.listensTo==ListenerFor.Album){
			if(System.currentTimeMillis() - lastKeyTyped < 400){
				jumpTo = jumpTo.concat(((Character)arg0.getKeyChar()).toString());
			}else{
				jumpTo = ((Character)arg0.getKeyChar()).toString();
			}
			lastKeyTyped = System.currentTimeMillis();
			System.out.println("Jump to:" + jumpTo);
			if(jumpTo != null){
				if(this.listensTo==ListenerFor.Artist){
					int row = ((ArtistTableModel)artistTable.getModel()).getRowNumberToJumpTo(jumpTo);
					System.out.println("Row: " + row);
					if(row != -1){
						artistTable.changeSelection(row, 1, false, false);
					}
				}else if(this.listensTo==ListenerFor.Album){
					int row = ((AlbumTableModel)albumTable.getModel()).getRowNumberToJumpTo(jumpTo);
					if(row != -1){
						albumTable.changeSelection(row, 1, false, false);
					}
				}
			}
		}

	}


}

