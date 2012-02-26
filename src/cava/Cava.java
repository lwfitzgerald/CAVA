package cava;

import java.awt.datatransfer.DataFlavor;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.Border;

import cava.lyrics.LyricsScrubber;
import cava.scrobbler.CavaScrobbler;
import cava.server.Client;
import javax.swing.border.LineBorder;

public class Cava implements ActionListener {

    private AudioPlayer audioPlayer;
    private NowPlaying nowPlaying;
    private TrackDatabase db;
    private CavaScrobbler scrobbler;
    private Seekbar seekBar;
    private Volumebar volumeBar;
    private Progressbar progressBar;

    JLabel currentTrack = new JLabel();
    JLabel currentArtist = new JLabel();
    //Made static for pop-ups
    public static JFrame mainCavaFrame;
    JLabel nextTrack;
    //Theme Colours
    public static Color lightish = new Color(196, 188, 150);
    public static Color darkish = new Color(20, 20, 20);
    //public static Color darkish = Color.WHITE;
    public static Color silverish = new Color(191, 191, 191);
    public static Color lightbordercolor = new Color(64, 64, 64);
    public static Color textcolor = new Color(255,255,255);
    //Fonts Used
    public static Font BigNoodle = null;
    public static Font MoviePoster = null;
    public static Font MoviePosterS = null;
    public static Font MoviePosterSS = null;
    public static Font Cali = null;
    public static Font CaliItalic = null;
    public static Font Ari = new Font("Arial", Font.PLAIN, 11);
    public static Font AriItalic = null;
    /*public static Font BigNoodle = new Font("BigNoodleTitling", Font.PLAIN, 14);
    public static Font MoviePoster = new Font("SF Movie Poster", Font.PLAIN, 24);
    public static Font MoviePosterS = new Font("SF Movie Poster", Font.PLAIN, 15);
    public static Font MoviePosterSS = new Font("SF Movie Poster", Font.PLAIN, 17);
    public static Font Cali = new Font("Calibri", Font.PLAIN, 11);
    public static Font CaliItalic = new Font("Calibri", Font.ITALIC, 11);
    public static Font Ari = new Font("Arial", Font.PLAIN, 10);
    public static Font AriItalic = new Font("Arial", Font.ITALIC, 10);
     */
    public static DataFlavor trackDataFlavor;
    public static DataFlavor playlistItemFlavor;
    private ComponentMover cm;
    private ComponentResizer cr;
    private JPanel top;
    private Rectangle oldBounds;
    private Boolean Maxed; // True if window is maximised
    //private Task task;
    public ImageButton bVol;

    public Cava() {
        // Load the preferences for the application
        Preferences.loadPreferences();
        System.setProperty("derby.stream.error.file", System.getProperty("user.home") + "/.cava/derby.log");
        System.setProperty("derby.locks.monitor", "true");
        System.setProperty("derby.locks.deadlockTrace", "true");
        final SplashScreen splash = SplashScreen.getSplashScreen();
        // Load the database
        db = new TrackDatabase();
        //If we couldn't connect, then try and create the database. 
        //If it still fails, error and exit. 
        if (!db.isConnected()) {
        	Graphics2D g = null;
            if (splash != null) {
                g = splash.createGraphics();
                g.drawString("Creating Database", 40, splash.getSize().height-50);
                splash.update();
            }
            if (!BuildDatabase.createDBInHomeFolder()) {
            	fatalLoadError(g,splash,"Fatal database error. Exiting");
            }

            db = new TrackDatabase();
            if (!db.isConnected()) {
                fatalLoadError(g,splash,"Fatal database error. Exiting");
            }
        }

        // Set up Last.fm scrobbling
        scrobbler = new CavaScrobbler(Preferences.getLastfmUsername(), Preferences.getLastfmPassword(), db);
        Preferences.setScrobbler(scrobbler);

        try {
            trackDataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + Track[].class.getName() + "\"");
            playlistItemFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + int[].class.getName() + "\"");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Load the required fonts
        InputStream bnt = null;
        InputStream mpt = null;
        InputStream mpts = null;
        InputStream mptss = null;
        InputStream ari = null;
        InputStream arii = null;
        InputStream cali = null;
        InputStream calii = null;

        try {
            bnt = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/big_noodle_titling.ttf"));
            mpt = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/sf movie poster2.ttf"));
            mpts = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/sf movie poster2.ttf"));
            mptss = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/sf movie poster2.ttf"));
            ari = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/arial.ttf"));
            arii = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/ariali.ttf"));
            cali = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/calibri.ttf"));
            calii = new BufferedInputStream(this.getClass().getResourceAsStream("Fonts/calibrii.ttf"));
        } catch (NullPointerException ex) {
            Logger.getLogger(Cava.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            BigNoodle = Font.createFont(Font.PLAIN, bnt).deriveFont(14f);
            MoviePoster = Font.createFont(Font.PLAIN, mpt).deriveFont(24f);
            MoviePosterS = Font.createFont(Font.PLAIN, mpts).deriveFont(15f);
            MoviePosterSS = Font.createFont(Font.PLAIN, mptss).deriveFont(17f);
            Ari = Font.createFont(Font.PLAIN, ari).deriveFont(10f);
            AriItalic = Font.createFont(Font.PLAIN, arii).deriveFont(10f);
            Cali = Font.createFont(Font.PLAIN, cali).deriveFont(11f);
            CaliItalic = Font.createFont(Font.PLAIN, calii).deriveFont(11f);
        } catch (FontFormatException ex) {
            Logger.getLogger(Cava.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Cava.class.getName()).log(Level.SEVERE, null, ex);
        }


        // ----------------------------------------------------------------------------
        // Setting Colours for specified colour theme
        // ----------------------------------------------------------------------------

        //Scrollbar

        UIManager.put("ScrollBar.background", Color.BLACK);
        UIManager.put("ScrollBar.thumb", lightish);
        UIManager.put("ScrollBar.thumbShadow", lightish);
        UIManager.put("ScrollBar.thumbDarkShadow", Color.BLACK);
        UIManager.put("ScrollBar.shadow", lightish);
        UIManager.put("ScrollBar.darkShadow", lightish);
        UIManager.put("ScrollBar.thumbHighlight", lightish);
        UIManager.put("ScrollBar.track", Color.BLACK);
        UIManager.put("ScrollBar.trackHighlight", lightbordercolor);
        UIManager.put("ScrollBar.border", BorderFactory.createLineBorder(lightbordercolor, 1));
        UIManager.put("ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI");

        //Table
        UIManager.put("TableHeaderUI", "javax.swing.plaf.basic.BasicTableHeaderUI");
        UIManager.put("TableHeader.background", Color.BLACK);
        UIManager.put("TableHeader.foreground", lightish);
        UIManager.put("TableHeader.font", BigNoodle);
        UIManager.put("TableHeader.cellBorder", BorderFactory.createLineBorder(lightbordercolor, 1));
        UIManager.put("Panel.background", darkish);
        UIManager.put("ScrollPane.background", Color.BLACK);
        UIManager.put("Viewport.background", Color.BLACK); // Colours the background seen when dragging columns
        UIManager.put("Slider.background", darkish);

        //OptionPane
        UIManager.put("OptionPane.foreground", Color.WHITE);

        //Color theme = new Color(102, 102, 102);
        Color theme = lightbordercolor;
        Color textcolor = Color.WHITE;

        //Menu
        UIManager.put("List.focusCellHighlightBorder", BorderFactory.createLineBorder(Color.WHITE, 1));
        UIManager.put("Label.foreground", lightish);
        UIManager.put("Label.font", Cali);
        UIManager.put("List.background", theme);
        UIManager.put("List.foreground", textcolor);
        UIManager.put("List.font", Cali);
        UIManager.put("TextField.background", theme);
        UIManager.put("TextField.foreground", textcolor);
        UIManager.put("TextField.font", Cali);
        UIManager.put("ComboBox.background", theme);
        UIManager.put("ComboBox.foreground", textcolor);
        UIManager.put("ComboBox.thumb", Color.RED);
        UIManager.put("ComboBox.font", Cali);
        UIManager.put("ComboBox.selectionBackground", silverish);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBoxMenuItem.background", silverish);
        UIManager.put("CheckBoxMenuItem.foreground", Color.BLACK);
        UIManager.put("CheckBoxMenuItem.background", silverish);
        UIManager.put("CheckBoxMenuItem.font", Cali);


        UIManager.put("MenuItem.background", silverish);
        UIManager.put("MenuItem.foreground", Color.black);
        UIManager.put("MenuItem.acceleratorForeground", Color.yellow);
        UIManager.put("MenuItem.font", Cali);
        UIManager.put("PopupMenu.font", Cali);
        UIManager.put("Menu.font", Cali);
        UIManager.put("MenuBar.font", Cali);
        UIManager.put("Menu.foreground", Color.BLACK);
        UIManager.put("Menu.background", Color.yellow);
        UIManager.put("Menu.selectionBackground", Color.gray);
        UIManager.put("Menu.selectionForeground", Color.white);
        UIManager.put("MenuItem.selectionBackground", Color.gray);
        UIManager.put("MenuItem.selectionForeground", Color.white);
        UIManager.put("MenuItem.selectionBorder", new LineBorder(Color.YELLOW));
        UIManager.put("PopupMenu.background", silverish);
        UIManager.put("PopupMenu.foreground", Color.BLACK);
        UIManager.put("Tree.textForeground", Color.BLACK);
        UIManager.put("PopupMenu.border", new LineBorder(Color.BLACK));

        UIManager.put("Button.background", Color.darkGray);
        UIManager.put("Button.foreground", Color.white);
        UIManager.put("Button.select", darkish);
        UIManager.put("Button.focus", darkish);
        

        UIManager.put("OptionPane.background", darkish);
        UIManager.put("OptionPane.messageForeground", Color.white);


        //Refer to http://devdaily.com/java/java-uimanager-color-keys-list for more info on UIManager Color Keys

        nowPlaying = new NowPlaying(currentTrack, currentArtist);
        audioPlayer = new AudioPlayer(nowPlaying, db, scrobbler);
        seekBar = new Seekbar(audioPlayer);
        audioPlayer.setSeekBar(seekBar);

        volumeBar = new Volumebar(audioPlayer, bVol);
        volumeBar.setVolume(audioPlayer, Preferences.getVolume());
        audioPlayer.setVolumeBar(volumeBar);

        progressBar = new Progressbar();

        mainCavaFrame = new JFrame();
        mainCavaFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainCavaFrame.setTitle("     CAVA     ");


        mainCavaFrame.setLayout(new GridBagLayout());
        Browser browser = new Browser(audioPlayer, nowPlaying);
        MenuBar menubar = new MenuBar();
        audioPlayer.setBrowser(browser);
        BuildDatabase.browser = browser;//Pass the bowser so the tables can be refreshed
        nowPlaying.setArtistContainer(browser.getArtistContainer());


        //Call display function and set main panel properties
        display(mainCavaFrame.getContentPane(), menubar.setupMenuBar(), browser.display_mainPanel(), browser.display_searchPanel());
        mainCavaFrame.setUndecorated(true);
        mainCavaFrame.setBackground(Color.BLACK);
        mainCavaFrame.pack();
        mainCavaFrame.setSize(900, 600);
        mainCavaFrame.setMinimumSize(new Dimension(800, 400));
        BufferedImage taskbarIcon;



        //Set taskbar Icon for program
        try {
            taskbarIcon = ImageIO.read(this.getClass().getResource("images/play.png"));
            mainCavaFrame.setIconImage(taskbarIcon);
        } catch (IOException e) {
            if (Constants.DEBUG) {
                Dbg.syserr("Unable to load taskbar image");
            }
        }

        //Make the frame resizable without the need for a decorated window
        cr = new ComponentResizer();
        cr.setMinimumSize(new Dimension(800, 400));
        cr.setSnapSize(new Dimension(10, 10));
        cr.setDragInsets(new Insets(10, 10, 10, 10));
        cr.registerComponent(mainCavaFrame);

        Maxed = false;

        mainCavaFrame.setLocationRelativeTo(null);
        mainCavaFrame.setVisible(true);
        mainCavaFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
        runBackgoundThreads();
    }
    
    /**
     * Runs anything that needs to be done in the background on start-up, such as SCMS data generation
     * or uploading
     */
    private void runBackgoundThreads(){
    	if(!SCMSGenerator.generateSCMS()){
    		Client client =new Client(5000, new TrackDatabase());
    		client.submitTracksFromDB();
    	}
    	
    	if(Preferences.getLyricsScrubberStarted()){
    		new LyricsScrubber().start();
    	}
    }

    // test client
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        new Cava();
    }

    private void display(Container pane, Container mpanel, Container bpanel, Container spanel) {


        // ----------------------------------------------------------------------------
        // Loading Required Images
        // ----------------------------------------------------------------------------

        BufferedImage imgPlay = null;
        BufferedImage imgPause = null;
        BufferedImage imgStop = null;
        BufferedImage imgNext = null;
        BufferedImage imgPrevious = null;
        BufferedImage imgPlayH = null;
        BufferedImage imgPauseH = null;
        BufferedImage imgStopH = null;
        BufferedImage imgNextH = null;
        BufferedImage imgPreviousH = null;
        BufferedImage imgMax = null;
        BufferedImage imgMin = null;
        BufferedImage imgQuit = null;
        BufferedImage imgLogo = null;
        BufferedImage imgResize = null;
        BufferedImage imgVol = null;
        BufferedImage imgMute = null;
        BufferedImage imgVolH = null;
        BufferedImage imgMuteH = null;

        try {
            imgPlay = ImageIO.read(this.getClass().getResource("images/play.png"));
            imgPause = ImageIO.read(this.getClass().getResource("images/pause.png"));
            imgStop = ImageIO.read(this.getClass().getResource("images/stop.png"));
            imgNext = ImageIO.read(this.getClass().getResource("images/next.png"));
            imgPrevious = ImageIO.read(this.getClass().getResource("images/previous.png"));
            imgPlayH = ImageIO.read(this.getClass().getResource("images/play_hold.png"));
            imgPauseH = ImageIO.read(this.getClass().getResource("images/pause_hold.png"));
            imgStopH = ImageIO.read(this.getClass().getResource("images/stop_hold.png"));
            imgNextH = ImageIO.read(this.getClass().getResource("images/next_hold.png"));
            imgPreviousH = ImageIO.read(this.getClass().getResource("images/previous_hold.png"));
            imgMax = ImageIO.read(this.getClass().getResource("images/maximise.png"));
            imgMin = ImageIO.read(this.getClass().getResource("images/minimise.png"));
            imgQuit = ImageIO.read(this.getClass().getResource("images/quit.png"));
            imgLogo = ImageIO.read(this.getClass().getResource("images/logoS.png"));
            imgResize = ImageIO.read(this.getClass().getResource("images/resize.png"));
            imgVol = ImageIO.read(this.getClass().getResource("images/volume.png"));
            imgVolH = ImageIO.read(this.getClass().getResource("images/volume_hold.png"));
            imgMute = ImageIO.read(this.getClass().getResource("images/mute.png"));
            imgMuteH = ImageIO.read(this.getClass().getResource("images/mute_hold.png"));
        } catch (IOException ex) {
            Logger.getLogger(Cava.class.getName()).log(Level.SEVERE, null, ex);
        }

        // ----------------------------------------------------------------------------
        // Creating custom class ImageButtons from loaded Images
        // ----------------------------------------------------------------------------

        ImageButton bPlay = new ImageButton(imgPlay, imgPlayH, imgPause, imgPauseH, audioPlayer, AudioAction.Resume);
        audioPlayer.setPlayButton(bPlay);
        ImageButton bStop = new ImageButton(imgStop, imgStopH, audioPlayer, AudioAction.Stop);
        ImageButton bNext = new ImageButton(imgNext, imgNextH, audioPlayer, AudioAction.SkipForward);
        ImageButton bPrevious = new ImageButton(imgPrevious, imgPreviousH, audioPlayer, AudioAction.SkipBack);
        bVol = new ImageButton(imgVol, imgVolH, imgMute, imgMuteH, audioPlayer, AudioAction.Mute);
        ImageIcon iconQuit = new ImageIcon(imgQuit);
        JLabel bQuit = new JLabel(iconQuit, JLabel.CENTER);
        bQuit.addMouseListener(new WindowControlListener(WindowButtonType.Exit));

        ImageIcon iconMax = new ImageIcon(imgMax);
        ImageIcon iconMin = new ImageIcon(imgMin);
        ImageIcon iconLogo = new ImageIcon(imgLogo);
        ImageIcon iconResize = new ImageIcon(imgResize);

        volumeBar = new Volumebar(audioPlayer, bVol);
        volumeBar.setVolume(audioPlayer, Preferences.getVolume());
        audioPlayer.setSeekBar(seekBar);
        audioPlayer.setVolumeBar(volumeBar);

        JLabel bMax = new JLabel();
        bMax.setIcon(iconMax);

        JLabel bMin = new JLabel();
        bMin.setIcon(iconMin);

        JLabel logo = new JLabel();
        logo.setIcon(iconLogo);

        JLabel resize = new JLabel();
        resize.setIcon(iconResize);

        currentTrack.setText("No Song Playing");            // Show song filename
        currentTrack.setSize(200, 10);
        currentTrack.setFont(MoviePoster);
        currentTrack.setForeground(Color.WHITE);
        currentTrack.setVisible(true);

        currentArtist.setText("   ");
        currentArtist.setSize(200, 10);
        currentArtist.setFont(MoviePosterS);
        currentArtist.setForeground(Color.WHITE);
        currentArtist.setVisible(true);

        // ----------------------------------------------------------------------------
        // Creating all required panels for main body of panel
        // ----------------------------------------------------------------------------

        JPanel AudioControlPanel = new JPanel();
        AudioControlPanel.setLayout(new GridBagLayout());
        AudioControlPanel.setSize(400, 50);
        AudioControlPanel.setBackground(Color.BLACK);

        JPanel SongDisplay = new JPanel();
        SongDisplay.setLayout(new GridBagLayout());
        SongDisplay.setSize(500, 50);
        SongDisplay.setOpaque(false);

        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        Border grayline = BorderFactory.createLineBorder(lightbordercolor, 1);
        AudioControlPanel.setBorder(grayline);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(0, 2, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = -5;
        c.ipady = 0;
        c.gridwidth = 1;   //1 columns wide
        AudioControlPanel.add(bPrevious, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 0;
        AudioControlPanel.add(bPlay, c);

        c.gridx = 2;
        AudioControlPanel.add(bStop, c);

        c.gridx = 3;
        AudioControlPanel.add(bNext, c);

        // Now add the AudioControlPanel of buttons to final frame
        //c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(60, 10, 50, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.ipadx = 3;
        c.ipady = 0;
        c.gridheight = 30;
        c.weightx = 1;
        c.weighty = 1;
        pane.add(AudioControlPanel, c);

        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(-25, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        c.ipady = 10;
        c.weightx = 0;
        c.weighty = 0;
        c.gridheight = 10;
        c.gridwidth = 0;
        SongDisplay.add(currentTrack, c);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, -5, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = 10;
        c.ipady = 10;
        c.weightx = 0;
        c.weighty = 0;
        c.gridheight = 10;
        c.gridwidth = 0;
        SongDisplay.add(currentArtist, c);

        //pane.add(nextTrack);
        //c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(-10, 0, -22, 0);
        c.anchor = GridBagConstraints.PAGE_END;
        c.gridx = 5;
        c.gridy = 3;
        c.ipadx = 200;
        c.ipady = 0;
        //c.gridwidth = 40;
        SongDisplay.add(seekBar, c);

        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(40, 80, 50, 50);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 50;
        c.weightx = 1;
        c.weighty = 1;
        pane.add(SongDisplay, c);

        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(140, 2, 2, 2);
        c.ipady = 400;
        c.ipadx = 600;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 100;
        c.gridwidth = 50;
        c.gridheight = 1;
        pane.add(bpanel, c);

        // ----------------------------------------------------------------------------
        // This is where the Menu Bar's position gets set
        // ----------------------------------------------------------------------------


        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 80, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 150;
        c.ipady = 18;
        c.gridwidth = 0;
        c.gridheight = 0;
        c.weightx = 1;
        c.weighty = 1;
        mpanel.setVisible(true);
        pane.add(mpanel, c);

        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(100, 0, 0, 20);
        c.gridx = 1;
        c.gridy = 0;
        c.ipadx = 200;
        c.ipady = 5;
        c.weightx = 1;
        c.weighty = 1;
        pane.add(spanel, c);

        c.insets = new Insets(2, 10, 2, 2);
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 0;
        c.gridheight = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 0;
        pane.add(bQuit, c);

        c.insets = new Insets(2, 10, 2, 20);
        c.ipadx = 0;
        c.ipady = 0;
        bMax.addMouseListener(new WindowControlListener(WindowButtonType.Maximize));
        pane.add(bMax, c);

        c.insets = new Insets(2, 10, 2, 40);
        bMin.addMouseListener(new WindowControlListener(WindowButtonType.Minimize));
        pane.add(bMin, c);

        c.insets = new Insets(4, -1, 0, 0);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 0;
        c.gridheight = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 0;
        pane.add(logo, c);

        c.insets = new Insets(1, 1, 1, 1);
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 0;
        c.gridheight = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(resize, c);

        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(62, 0, 0, 190);
        c.gridx = 1;
        c.gridy = 0;
        c.ipadx = -2;
        c.ipady = -2;
        c.gridheight = 1;
        c.gridwidth = 1;
        pane.add(bVol, c);


        c.insets = new Insets(67, 0, 0, 10);
        c.gridx = 1;
        c.gridy = 0;
        c.ipadx = 150;
        c.ipady = 10;
        c.gridheight = 1;
        c.gridwidth = 1;
        pane.add(volumeBar, c);


        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(35, 0, 2, 0);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 100;
        c.gridwidth = 100;
        c.ipadx = 0;
        c.ipady = 5;
        pane.add(progressBar, c);

        // ----------------------------------------------------------------------------
        // Dealing with Borders
        // ----------------------------------------------------------------------------


        top = new JPanel();
        JPanel left = new JPanel();
        JPanel right = new JPanel();
        JPanel bottom = new JPanel();
        JPanel inner = new JPanel();
        JPanel outer = new JPanel();

        top.setBackground(silverish);
        left.setBackground(silverish);
        right.setBackground(silverish);
        bottom.setBackground(silverish);

        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, -1);
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.ipadx = 0;
        c.ipady = 15;
        c.weightx = 0;
        c.weighty = 0;
        cm = new ComponentMover(mainCavaFrame, top);
        pane.add(top, c);

        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1000;
        c.gridwidth = 1;
        c.ipady = 0;
        c.ipadx = -3;
        pane.add(left, c);

        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 500;
        c.gridy = 0;
        c.gridheight = 1000;
        c.gridwidth = 1;
        c.ipady = 0;
        c.ipadx = -3;
        pane.add(right, c);

        c.anchor = GridBagConstraints.PAGE_END;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 4;
        c.gridheight = 100;
        c.gridwidth = 600;
        c.ipadx = 0;
        c.ipady = 0;
        pane.add(bottom, c);

        //Add border lines on both the inside and outside of border panels

        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(26, 8, 0, 8);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 0;
        inner.setOpaque(false);
        inner.setBorder(new LineBorder(lightbordercolor, 1));
        //pane.add(inner, c);

        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(0, 0, -11, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        outer.setOpaque(false);
        outer.setBorder(new LineBorder(lightbordercolor, 1));
        pane.add(outer, c);
	
	//Setup the system tray icon - note that if your system doesn't support this, it will just do nothing
	Tray tray = new Tray(imgPlay, mainCavaFrame, audioPlayer, bPlay, this);
	tray.setupTray();

    }
    
    public void trayCleanup() {
	    cleanup();	    
    }

    /**
     * Should be called when the program is exiting
     */
    private void cleanup() {
        BuildDatabase.cancelTrackImporting();
        Client.cancelUploading();
        SCMSGenerator.stopGeneration();
        System.exit(0);
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void fatalLoadError(Graphics2D g, SplashScreen splash,String error){
        g.drawString(error, 40, splash.getSize().height-30);
        splash.update();    	
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.exit(1);
    }

    private class WindowControlListener extends MouseAdapter {

        private WindowButtonType buttonType;

        public WindowControlListener(WindowButtonType buttonType) {
            this.buttonType = buttonType;
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            switch (buttonType) {
                case Minimize:
                    mainCavaFrame.setState(JFrame.ICONIFIED);
                    return;
                case Maximize:
                    if (!Maxed) {
                        oldBounds = mainCavaFrame.getBounds();
                        Maxed = true;
                        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                        mainCavaFrame.setBounds(maxBounds);
                        cm.deregisterComponent(top);
                        cr.deregisterComponent(mainCavaFrame);
                    } else {
                        Maxed = false;
                        mainCavaFrame.setBounds(oldBounds);
                        cm = new ComponentMover(mainCavaFrame, top);
                        cr.registerComponent(mainCavaFrame);
                    }
                    return;
                case Exit:
                    cleanup();
                    return;
            }
        }
    }

    private enum WindowButtonType {

        Minimize, Maximize, Exit;
    }
}


