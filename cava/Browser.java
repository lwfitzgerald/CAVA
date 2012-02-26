package cava;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class Browser {

	private JTable artistTable;
	private JTable albumTable;
	private JTable trackTable;
	private JTable playlistTable;
	private JTable playlistTracksTable;
	private JTextField searchBox;
	private ArtistContainer artists;
	private AlbumContainer albums;
	private ArtistTableModel artistTableModel;
	private AlbumTableModel albumTableModel;
	private TrackTableModel trackTableModel;
	private PlaylistTableModel playlistTableModel;
	private PlaylistTracksTableModel playlistTracksTableModel;
	private AudioPlayer audioPlayer;
	private NowPlaying nowPlaying;

	public static void main(String args[]) {
		new Browser();
	}

	@SuppressWarnings("serial")
    class CavaTableRenderer extends JLabel implements TableCellRenderer {

		public CavaTableRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			this.setText((value==null)? "Null Value" : value.toString());

			if (isSelected) {
				setBackground(new Color(62, 62, 62));
				setForeground(new Color(150, 150, 150));
			} else {
				setForeground(Color.WHITE);
				setBackground(new Color(102, 102, 102));
			}


			this.setFont(Cava.Ari);

			//If the table is either the playlist tracks table or the track table, then colour tracks that are now playing
			if(table==playlistTracksTable){
				if((Boolean) ((PlaylistTracksTableModel)table.getModel()).getValueAt(row, -3)){
					setForeground(new Color(196, 188, 150));
					this.setFont(Cava.AriItalic);
				}
			}
			if(table==trackTable){
				try{
					if(nowPlaying.isPlaying((ClientTrack)((TrackTableModel)table.getModel()).getValueAt(row, -1))){
						setForeground(new Color(196, 188, 150));
						this.setFont(Cava.AriItalic);
					}
				}catch(ClassCastException e){
				    Dbg.syserr("Get value returned null on track table");
				}
			}

			// Add some padding
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			return this;
		}
	}

	Browser() {
		setUpBrowser(null, null);
	}

	Browser(AudioPlayer audioPlayer, NowPlaying nowPlaying) {
		setUpBrowser(audioPlayer, nowPlaying);
	}

	void setUpBrowser(AudioPlayer audioPlayer, NowPlaying nowPlaying) {
		this.audioPlayer = audioPlayer;
		this.nowPlaying = nowPlaying;
		//Create new containers for artists and albums
		artists = new ArtistContainer();
		albums = new AlbumContainer();
		//Set Tables and their models
		artistTableModel = new ArtistTableModel(artists);
		albumTableModel = new AlbumTableModel(albums);
		trackTableModel = new TrackTableModel(artists, albums);
		playlistTableModel = new PlaylistTableModel(nowPlaying);
		playlistTracksTableModel = new PlaylistTracksTableModel(playlistTableModel,nowPlaying);
		playlistTableModel.setPlaylistTracksTableModel(playlistTracksTableModel);
		artistTable = new JTable(artistTableModel);
		albumTable = new JTable(albumTableModel);
		trackTable = new JTable(trackTableModel);
		playlistTable = new JTable(playlistTableModel);
		playlistTracksTable = new JTable(playlistTracksTableModel);
		searchBox = new JTextField(" Search...");
		setUpTable(artistTable);
		setUpTable(albumTable);
		setUpTable(trackTable);
		setUpTable(playlistTable);
		setUpTable(playlistTracksTable);
		if (audioPlayer == null) {
			JFrame frames = new JFrame();
			frames.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frames.setTitle("Test Music Browser for CAVA");
			frames.add(display_mainPanel());
			//frames.pack();
			frames.setLocationByPlatform(true);
			frames.setVisible(true);
		}
	}

	public JPanel display_mainPanel() {
		return  this.mainPanel();
	}

	public JPanel display_searchPanel() {
		return this.searchPanel();
	}

	public JPanel searchPanel() {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		BrowserListener listener = new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.SearchBox, audioPlayer, nowPlaying);

		searchBox.addFocusListener(listener);
		searchBox.getDocument().addDocumentListener(listener);

		searchBox.setBackground(Color.BLACK);
		searchBox.setForeground(Cava.lightish);
		searchBox.setBorder(BorderFactory.createLineBorder(Cava.lightbordercolor, 1));
		searchBox.setFont(Cava.Ari);
		topPanel.add(searchBox);

		/*      killSearchButton = new JButton("Cancel");
        killSearchButton.addActionListener(new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.CancelButton, audioPlayer, nowPlaying));
        topPanel.add(killSearchButton);
		 */
		return topPanel;
	}

	private JPanel artistAlbumPanel() {
		JPanel middlePanel = new JPanel();
		middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.X_AXIS));
		middlePanel.add(getJScrollPane(artistTable));
		middlePanel.add(getJScrollPane(albumTable));
		return middlePanel;
	}

	private JPanel trackPanel() {
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(getJScrollPane(trackTable));
		return bottomPanel;
	}

	public JPanel mainPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.add(leftPanel());
		mainPanel.add(rightPanel());
		return mainPanel;
	}

	private JPanel leftPanel() {
		JPanel leftPanel = new JPanel();
		leftPanel.setSize(0, 10);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(getJScrollPane(playlistTable));
		leftPanel.add(getJScrollPane(playlistTracksTable));
		return leftPanel;
	}

	private JPanel rightPanel() {
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(artistAlbumPanel());
		rightPanel.add(trackPanel());
		return rightPanel;
	}

	private JScrollPane getJScrollPane(JTable table) {
		JScrollPane scroll = new JScrollPane(table);
		if (table == playlistTable || table == artistTable || table == albumTable) {
			scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, 200));
		}
		scroll.setBackground(Color.BLACK);
		scroll.setOpaque(true);
		scroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 8, 8, 8, new Color(20, 20, 20)), BorderFactory.createEmptyBorder(1, 1, 1, 1)));
		return scroll;
	}

	private void setUpTable(JTable table) {
		table.setFillsViewportHeight(true);
		if(table != trackTable && table != playlistTracksTable){
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		table.setFont(Cava.Ari);

		int numCols = table.getColumnCount();

		for (int i = 0; i < numCols; i++) {
			TableColumn col = table.getColumnModel().getColumn(i);
			col.setCellRenderer(new CavaTableRenderer());
		}

		table.setBackground(new Color(102, 102, 102));
		table.setShowGrid(false);

		if (table == artistTable) {
			BrowserListener listener = new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.Artist, audioPlayer, nowPlaying);
			artistTable.addMouseListener(listener);
			artistTable.addKeyListener(listener);
			artistTable.getSelectionModel().addListSelectionListener(listener);
			artistTable.setFont(Cava.Ari);
			artistTable.setDragEnabled(true);
			artistTable.setTransferHandler(new ArtistTransferHandler());
		}

		if (table == albumTable) {
			BrowserListener listener = new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.Album, audioPlayer, nowPlaying);
			albumTable.addMouseListener(listener);
			albumTable.addKeyListener(listener);
			albumTable.getSelectionModel().addListSelectionListener(listener);
			albumTable.setFont(Cava.Ari);
			albumTable.setDragEnabled(true);
			albumTable.setTransferHandler(new AlbumTransferHandler());
		}

		if (table == trackTable) {
			trackTable.addMouseListener(new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.Track, audioPlayer, nowPlaying));
			trackTable.getColumnModel().getColumn(0).setMaxWidth(100);
			trackTable.getColumnModel().getColumn(4).setMaxWidth(100);
			trackTable.setDragEnabled(true);
			trackTable.setTransferHandler(new TrackTransferHandler());
		}

		if (table == playlistTable) {
			BrowserListener listener = new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.Playlist, audioPlayer, nowPlaying);
			playlistTable.addMouseListener(listener);
			playlistTable.getSelectionModel().addListSelectionListener(listener);
		}

		if (table == playlistTracksTable) {
			BrowserListener listener = new BrowserListener(artistTable, albumTable, trackTable, playlistTable, playlistTracksTable, searchBox, ListenerFor.PlaylistTrack, audioPlayer, nowPlaying);
			playlistTracksTable.addMouseListener(listener);
			playlistTracksTable.getSelectionModel().addListSelectionListener(listener);
			playlistTracksTable.setTransferHandler(new PlaylistTracksTransferHandler(playlistTable));
			playlistTracksTable.setDropMode(DropMode.INSERT_ROWS);
			playlistTracksTable.setDragEnabled(true);
		}

	}

	public ArtistContainer getArtistContainer() {
		return this.artists;
	}

	public void refreshTables(){
		ListenerFor[] allTables = {ListenerFor.Artist,ListenerFor.Album,ListenerFor.Track,ListenerFor.Playlist,ListenerFor.PlaylistTrack};
		refreshTables(allTables);
	}

	public void refreshTables(ListenerFor[] whichTables){
		for(ListenerFor table: whichTables){
			if(table==ListenerFor.Artist){
				artistTableModel.fireTableDataChanged();
			}
			if(table==ListenerFor.Album){
				albumTableModel.fireTableDataChanged();
			}
			if(table==ListenerFor.Track){
				trackTableModel.fireTableDataChanged();
			}
			if(table==ListenerFor.Playlist){
				playlistTableModel.fireTableDataChanged();
			}
			if(table==ListenerFor.PlaylistTrack){
				playlistTableModel.fireTableDataChanged();
			}
		}
	}

	public void showImportChanges(){
		//First check if a search is running
		String currentSearchText = searchBox.getText().trim();
		if(!currentSearchText.equals("") && !currentSearchText.contains("Search...")){
			//If it is don't refresh the tables. Doing so would be quite complicated.
			//    		
			//searchBox.setText(currentSearchText);  		

		}else{
			//Otherwise, we want to refresh the tables. Also need to reset selections for artists and albums
			int artistRow = artistTable.getSelectedRow();

			String currentArtist = (String) ((ArtistTableModel)artistTable.getModel()).getValueAt(artistRow, 0);
			if(currentArtist != null){
				currentArtist =  BuildDatabase.createSortByArtist(currentArtist.toLowerCase());
			}
			int albumRow = albumTable.getSelectedRow();
			String currentAlbum = (String) ((AlbumTableModel)albumTable.getModel()).getValueAt(albumRow, 0);
			if(currentAlbum != null){
				currentAlbum = currentAlbum.toLowerCase();
			}
			
			//Notify tables that rows have been added, and refresh them. 
			artistTableModel.tracksImported();
			albumTableModel.tracksImported();
			trackTableModel.tracksImported();
		
			ListenerFor[] tables = {ListenerFor.Artist,ListenerFor.Album,ListenerFor.Track};
			refreshTables(tables);
			
			//Short delay seems to be required to ensure that tables have been updated before changing row selection
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(artistRow != 0 && currentArtist != null){
				int row = ((ArtistTableModel)artistTable.getModel()).getRowNumberToJumpTo(currentArtist);
				if(row != -1){
					artistTable.changeSelection(row, 1, false, false);
				}
			}
	
			if(albumRow != 0 && currentAlbum != null){
				int row = ((AlbumTableModel)albumTable.getModel()).getRowNumberToJumpTo(currentAlbum);
				if(row != -1){
					albumTable.changeSelection(row, 1, false, false);
				}
			}
				
			Cava.mainCavaFrame.validate();
		}
	}
}
