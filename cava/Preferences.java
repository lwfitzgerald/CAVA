/**
 * The Preferences class creates the tabbed Preferences pane when opened by the
 * user.
 *
 * This class is getting large and unmanageable, I may break it down into separate classes in different files
 * 
 * @author Dave Glencross
 */
package cava;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.MessageDigest;

import javax.swing.border.LineBorder;
import javax.swing.colorchooser.*;
import javax.swing.event.*;

import cava.lyrics.LyricsScrubber;
import cava.scrobbler.CavaScrobbler;
import cava.spotify.SpotifyPlayer;

@SuppressWarnings("serial")
public class Preferences extends JPanel // implements ActionListener
{
    private enum Service { LASTFM, SPOTIFY }
    
    private static String defaultDir;
    /**
     * This boolean keeps track of whether consent has been given by the user
     * for the application to take SCMS data from their songs. The option is
     * selected with use of a check box. False means no, true means yes
     */
    private static boolean scmsConsent;
    private static boolean lastfmEnabled;
    private static String lastfmUsername;
    private static String lastfmPassword;
    private static int lastfmPasswordLength;
    private static boolean spotifyEnabled;
    private static String spotifyUsername;
    private static String spotifyPassword;
    private static int spotifyPasswordLength;
    private static float volume;
    private static final String defaultdirconf = "defaultdir";
    private static final String scmsconf = "scms";
    private static final String lastfmconf = "lastfm";
    private static final String spotifyconf = "spotify";
    private static final String volumeconf = "volume";
    private static final String scrubberconf = "scrubber";
    protected JColorChooser chooser;
    
    private static CavaScrobbler scrobbler;
    private static SpotifyPlayer spotifyplayer;

    /**
     * The constructor sets out a number of JComponents into a JTabbedPane and
     * calls a method for each pane. It also specifies the size of this display.
     */
    public Preferences() {
        super(new GridLayout(1, 2));

        // Set up the different panels.
        JTabbedPane tabbedPane = new JTabbedPane();

        JComponent panel1 = general();
        tabbedPane.addTab("General", null, panel1, "General Settings");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        JComponent panel2 = service(Service.LASTFM);
        tabbedPane.addTab("Last.fm", null, panel2, "Last.fm Account Settings");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        JComponent panel3 = service(Service.SPOTIFY);
        tabbedPane.addTab("Spotify", null, panel3, "Spotify Account Settings");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        /* For whoever edits this part, the lyrics panel is created in line 221. You can change the functionality of the button there.*/
	JComponent panel4 = lyrics();
	tabbedPane.addTab("Lyrics", null,panel4, "Download lyrics for songs");
	tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);
	/*
	JComponent panel5 = themes();
	tabbedPane.addTab("Themes", null, panel5, "Theme colour chooser");
	tabbedPane.setMnemonicAt(4, KeyEvent.VK_5);*/

        // Add the tabbed pane to this panel.
        add(tabbedPane);

        // The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * This JComponent is for the first panel, which is entitled "General
     * settings". At the moment, is just keeps track of whether consent has been
     * given for SCMS functions by the user. If the status of this is changed,
     * we then save this new status to a file.
     * 
     * @return return the JPanel panel so the Preferences constructor can
     *         display it
     */
    protected JComponent general() {
        JPanel panel = new JPanel(false);
        
        JLabel chooseDir = new JLabel("Default directory for adding music:");
        chooseDir.setFont(Cava.Ari);
        chooseDir.setForeground(Color.WHITE);
        
        final JTextField directory = new JTextField(20);
        directory.setText(defaultDir);
        JButton dirButton = new JButton("Choose directory");
        
        final JCheckBox scms = new JCheckBox("Allow CAVA to upload similarity data", scmsConsent);
        scms.setOpaque(false);
        scms.setFont(Cava.Ari);
        scms.setForeground(Color.WHITE);
        
        scms.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateScmsPreference(scms.isSelected());
            }
        });

        /* Just a simple button so we can display our data 'policy' */
        JButton tacs = new JButton("Terms and Conditions");
        tacs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tandcs();
            }
        });
        
        dirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String path = directoryLookup();
                if(path != null) {
                    updateDirPreference(path);
                    directory.setText(defaultDir);
                }
            }
        });

        directory.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {}

            @Override
            public void focusLost(FocusEvent arg0) {
                updateDirPreference(directory.getText());
            }
        });

        // Add all the components to the main panel
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        panel.add(chooseDir, c);
        
        c.gridx = 1;
        c.gridy = 1;
        panel.add(directory, c);
        
        c.gridx = 1;
        c.gridy = 2;
        panel.add(dirButton, c);

        c.gridx = 0;
        c.gridy = 4;
        panel.add(scms, c);

        c.gridx = 0;
        c.gridy = 5;
        panel.add(tacs, c);

        return panel;
    }

    protected JComponent themes() {

    	JPanel panel = new JPanel();
    	JButton pb = new JButton("Player background");
    	JLabel pbcolour = new JLabel("     ");
    	pbcolour.setForeground(Color.blue);
    	pbcolour.setBackground(Color.blue);
    	JButton bb = new JButton("Browser background");
    	JButton sb = new JButton("Scroll bar");

    	panel.add(pb);
    	panel.add(pbcolour);
    	panel.add(bb);
    	panel.add(sb);
    	//panel.add();

    	return panel;
    }

    protected JComponent colour() {
    	JPanel panel = new JPanel(false);

    	//Set up color chooser for setting text color
    	chooser = new JColorChooser();
    	//chooser.getSelectionModel().addChangeListener(this);
    	chooser.setBorder(BorderFactory.createTitledBorder("Choose Colour Theme"));

    	//Remove the preview panel
    	chooser.setPreviewPanel(new JPanel());
    	//chooser.removeChooserPanel("HSB");
    	//chooser.setChooserPanels(AbstractColorChooserPanel panels[ ]);

    	// Retrieve the current set of panels 
    	AbstractColorChooserPanel[] oldPanels = chooser.getChooserPanels(); 

    	// Remove panels 
    	for (int i=0; i<oldPanels.length; i++) { 
    		String clsName = oldPanels[i].getClass().getName(); 
    		if (clsName.equals("javax.swing.colorchooser.DefaultRGBChooserPanel")) { 
    			// Remove rgb chooser if desired 	
    			chooser.removeChooserPanel(oldPanels[i]); 
    		} else if (clsName.equals("javax.swing.colorchooser.DefaultHSBChooserPanel")) { 
    			// Remove hsb chooser if desired 
    			chooser.removeChooserPanel(oldPanels[i]);
    		}
    	} 

	JButton ok = new JButton("OK");
	JButton apply = new JButton("Apply");
	JButton cancel = new JButton("Cancel");
	
	panel.add(chooser);
	panel.add(ok);
	panel.add(apply);
	panel.add(cancel);

    //panel.setPreferredSize(new Dimension(200));
	
	//panel.pack();
        return panel;
    }
    
    // Get the colour of the colour chooser
    public void stateChanged(ChangeEvent e) {
        //Color newColor = chooser.getColor();
        //banner.setForeground(newColor);
    }

    protected JComponent lyrics() {
    	JPanel panel = new JPanel(false);
    	JButton button;
    	if(!getLyricsScrubberStarted()) {
    		button = new JButton("Download lyrics");
    		button.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				JButton me = ((JButton) e.getSource());
    				updateLyricsScrubberPrefs(false);
    				LyricsScrubber ls = new LyricsScrubber();
    				ls.start();
    				me.setEnabled(false);
    				me.setText("Feature Enabled");
    			}
    		});
    		/*JLabel warning = new JLabel("This can take a long time, depending on"
		    	    + "how many tracks you have in your library."
		    	    + "(Approx 5 seconds per track)");*/
    		String text =     "\n"
    			+ "This can take a long time, depending on "
    			+ "how many tracks you have in your library. "
    			+ "(Approx 5 seconds per track) ";

    		JTextArea textArea = new JTextArea(text);
    		textArea.setSize(290, 10);
    		//frame.setSize(320, 150);
    		//frame.setResizable(false);
    		textArea.setLineWrap(true);
    		textArea.setWrapStyleWord(true);
    		textArea.setEditable(false);
    		textArea.setOpaque(false);
    		textArea.setForeground(Color.white);
    		textArea.setFont(Cava.Ari);
    		panel.setLayout(new GridBagLayout());
    		GridBagConstraints c = new GridBagConstraints();
    		//c.anchor = GridBagConstraints.FIRST_LINE_END;
    		c.fill = GridBagConstraints.NORTH;
    		c.gridx = 0;
    		c.gridy = 0;
    		panel.add(button, c);

    		c.fill = GridBagConstraints.HORIZONTAL;
    		c.gridx = 0;
    		c.gridy = 1;
    		panel.add(textArea, c);
    	} else {
    		button = new JButton("Feature Activated");
    		button.setEnabled(false);
    	}
    	panel.add(button);

    	return panel;
    }
   

    /**
     * Allows the user to specify a directory which becomes default
     * for when opening up the file explorer to add songs to the library.
     * 
     * @return the first value from the string array 'result', which is the path
     *         of the selected folder
     */
    public String directoryLookup() {
        String result[];
        FileChooser fc = new FileChooser();
        result = fc.openFileChooser(true);
        
        if(result != null) {
            return result[0];
        } else {
            return null;
        }
    }

    /**
     * Load the preferences from their respective files
     */
    public static void loadPreferences() {
        // Check the configuration folder exists and if not create it
        checkConfigDir();

        // Load the default directory preference
        if(!loadDirPreference()) {
            // If the preference could not be loaded, fall back to default and
            // write new config file
            Dbg.sysout("Default directory config file not found, creating a new one with default setting");

            updateDirPreference(System.getProperty("user.home") + "/");
        }

        // Load the Scms consent preference
        if(!loadScmsPreference()) {
            // If the preference could not be loaded, fall back to default and
            // write new config file
            Dbg.sysout("Scms approval config file not found, creating a new one with default setting");

            updateScmsPreference(false);
        }

        // Load the Last.fm preferences
        if(!loadLastfmPreferences()) {
            // If the preference could not be loaded, fall back to default and
            // write new config file
            Dbg.sysout("Last.fm config file not found, creating a new one with default settings");

            updateLastfmPreferences(false, "", "");
        }

        // Load the Spotify preferences
        if(!loadSpotifyPreferences()) {
            // If the preference could not be loaded, fall back to default and
            // write new config file
            Dbg.sysout("Spotify config file not found, creating a new one with default settings");

            updateSpotifyPreferences(false, "", "");
        }
        
        if(!loadVolumePreference()) {
            Dbg.sysout("Volume config file not found, creating a new one with default setting");
            
            updateVolumePreference(1.0f);
        }
    }
    
    /**
     * Sets the Scrobbler in use
     * 
     * @param scrobbler
     *            Scrobbler in use
     */
    public static void setScrobbler(CavaScrobbler scrobbler) {
        Preferences.scrobbler = scrobbler;
    }
    
    /**
     * Sets the Spotify player in use
     * 
     * @param spotifyplayer
     *            Spotify player in use
     */
    public static void setSpotifyPlayer(SpotifyPlayer spotifyplayer) {
        Preferences.spotifyplayer = spotifyplayer;
    }

    /**
     * Creates the configuration directory if it doesn't exist
     */
    private static void checkConfigDir() {
        if(new File(System.getProperty("user.home") + "/.cava/").mkdir()) {
            Dbg.sysout("Creating CAVA config directory " + System.getProperty("user.home") + "/.cava/");
        }
    }

    /**
     * Loads the default directory path from the config file
     * 
     * @return A boolean indicating whether loading was successful
     */
    private static boolean loadDirPreference() {
        try {
            FileInputStream fstream = new FileInputStream(System.getProperty("user.home") + "/.cava/" + defaultdirconf);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            // Store the loaded path in the private variable
            defaultDir = br.readLine();
            in.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Updates the default directory path
     * 
     * @param path
     *            New default directory path
     */
    private static void updateDirPreference(String path) {
        defaultDir = path;

        try {
            File file = new File(System.getProperty("user.home") + "/.cava/" + defaultdirconf);
            Writer output = new BufferedWriter(new FileWriter(file));
            output.write(defaultDir);
            output.flush();
            output.close();
        } catch (IOException e) {
            Dbg.syserr("Writing to default directory config file failed");
        }
    }

    /**
     * Returns the default directory path preference
     * 
     * @return Default directory path preference
     */
    public static String getDirPreference() {
        return defaultDir;
    }

    /**
     * Returns whether or not Scms consent has been given
     * 
     * @return A boolean indicating whether loading was successful
     */
    private static boolean loadScmsPreference() {
        try {
            FileInputStream fstream = new FileInputStream(System.getProperty("user.home") + "/.cava/" + scmsconf);
            DataInputStream in = new DataInputStream(fstream);

            scmsConsent = in.readBoolean();
            in.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Updates the Scms consent preference
     * 
     * @param consent
     *            Whether Scms consent has been given
     */
    private static void updateScmsPreference(boolean consent) {
        scmsConsent = consent;

        try {
            FileOutputStream fstream = new FileOutputStream(System.getProperty("user.home") + "/.cava/" + scmsconf);
            DataOutputStream out = new DataOutputStream(fstream);

            out.writeBoolean(scmsConsent);
            out.flush();
            out.close();
        } catch (IOException e) {
            Dbg.syserr("Writing to scms config file failed");
        }
    }

    /**
     * Returns whether consent has been given to submit Scms data
     * 
     * @return A boolean representing whether consent has been given
     */
    public static boolean getScmsPreference() {
        return scmsConsent;
    }

    /**
     * Loads the Last.fm preferences from the config file
     * 
     * @return A boolean representing whether loading was successful
     */
    private static boolean loadLastfmPreferences() {
        return loadServicePreferences(Service.LASTFM);
    }

    /**
     * Updates the Last.fm preferences
     * 
     * @param enabled
     *            Whether Last.fm scrobbling is enabled
     * @param username
     *            Last.fm username
     * @param password
     *            Last.fm password
     */
    private static void updateLastfmPreferences(boolean enabled, String username, String password) {
        if(enabled != lastfmEnabled || !username.equals(lastfmUsername) || password.indexOf('\0') == -1) {
            updateServicePreferences(Service.LASTFM, enabled, username, password);
        }
    }

    /**
     * Loads the Spotify preferences from the config file
     * 
     * @return A boolean representing whether loading was successful
     */
    private static boolean loadSpotifyPreferences() {
        return loadServicePreferences(Service.SPOTIFY);
    }

    /**
     * Updates the Spotify preferences
     * 
     * @param enabled
     *            Whether Spotify features are enabled
     * @param username
     *            Spotify username
     * @param password
     *            Spotify password
     */
    private static void updateSpotifyPreferences(boolean enabled, String username, String password) {
        if(enabled != spotifyEnabled || !username.equals(spotifyUsername) || password.indexOf('\0') == -1) {
            updateServicePreferences(Service.SPOTIFY, enabled, username, password);
        }
    }

    /**
     * Loads the preferences for a service
     * 
     * @param service
     *            Service to load preferences for (Service.LASTFM or
     *            Service.SPOTIFY)
     */
    private static boolean loadServicePreferences(Service service) {
        try {
            FileInputStream fstream = new FileInputStream(System.getProperty("user.home") + "/.cava/" + ((service == Service.LASTFM) ? lastfmconf : spotifyconf));
            DataInputStream in = new DataInputStream(fstream);

            if(service == Service.LASTFM) {
                lastfmEnabled = in.readBoolean();
                
                if(in.readBoolean()) {
                    lastfmUsername = in.readUTF();
                } else {
                    lastfmUsername = "";
                }

                if(in.readBoolean()) {
                    lastfmPassword = in.readUTF();
                    lastfmPasswordLength = in.readInt();
                } else {
                    lastfmPassword = "";
                    lastfmPasswordLength = 0;
                }
            } else {
                spotifyEnabled = in.readBoolean();

                if(in.readBoolean()) {
                    spotifyUsername = in.readUTF();
                } else {
                    spotifyUsername = "";
                }

                if(in.readBoolean()) {
                    spotifyPassword = in.readUTF();
                    spotifyPasswordLength = in.readInt();
                } else {
                    spotifyPassword = "";
                    spotifyPasswordLength = 0;
                }
            }

            in.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Updates the preferences for a service
     * 
     * @param service
     *            Service to update preferences for (Service.LASTFM or
     *            Service.SPOTIFY)
     * @param enabled
     *            Whether service is enabled
     * @param username
     *            Service username
     * @param password
     *            Service password
     */
    private static void updateServicePreferences(Service service, boolean enabled, String username, String password) {
        if(service == Service.LASTFM) {
            lastfmEnabled = enabled;
            lastfmUsername = username;
            
            // Only update the password if it's been changed
            if(password.indexOf('\0') == -1) {
                lastfmPassword = MD5(password);
                lastfmPasswordLength = password.length();
            }
            
            if(enabled) {
                scrobbler.updateDetails(lastfmUsername, lastfmPassword);
            }
        } else {
            spotifyEnabled = enabled;
            spotifyUsername = username;
            
            // Only update the password if it's been changed
            if(password.indexOf('\0') == -1) {
                spotifyPassword = password;
                spotifyPasswordLength = password.length();
            }
            
            if(enabled) {
                spotifyplayer.updateDetails(spotifyUsername, spotifyPassword);
            }
        }

        try {
            FileOutputStream fstream = new FileOutputStream(System.getProperty("user.home") + "/.cava/" + ((service == Service.LASTFM) ? lastfmconf : spotifyconf));
            DataOutputStream out = new DataOutputStream(fstream);

            if(service == Service.LASTFM) {
                out.writeBoolean(lastfmEnabled);

                if(lastfmUsername.equals("")) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(lastfmUsername);
                }

                if(lastfmPassword.equals("")) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(lastfmPassword);
                    out.writeInt(lastfmPasswordLength);
                }
            } else {
                out.writeBoolean(spotifyEnabled);

                if(spotifyUsername.equals("")) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(spotifyUsername);
                }

                if(spotifyPassword.equals("")) {
                    out.writeBoolean(false);
                } else {
                    out.writeBoolean(true);
                    out.writeUTF(spotifyPassword);
                    out.writeInt(spotifyPasswordLength);
                }
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            Dbg.syserr("Writing to " + ((service == Service.LASTFM) ? "Last.fm" : "Spotify") + " config file failed");
        }
    }
    
    /**
     * Get the MD5 hash of the given string
     * @param password String to hash
     * @return MD5 hash of string
     */
    private static String MD5(String password) {
        byte[] bytes = password.getBytes();
        StringBuffer hexString = new StringBuffer();
        try{
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            byte[] messageDigest = algorithm.digest(bytes);

            for(int i=0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                
                hexString.append(hex);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            Dbg.syserr(e.getMessage());
        }
        
        return hexString.toString();
    }

    /**
     * Get whether Last.fm support is enabled
     * @return Whether Last.fm support is enabled
     */
    public static boolean getLastfmEnabled() {
        return lastfmEnabled;
    }
    
    /**
     * Get the Last.fm username
     * @return Last.fm username
     */
    public static String getLastfmUsername() {
        return lastfmUsername;
    }
    
    /**
     * Get the Last.fm password (MD5 hash)
     * @return MD5 hash of Last.fm password
     */
    public static String getLastfmPassword() {
        return lastfmPassword;
    }
    
    /**
     * Get whether Spotify features are enabled
     * @return Whether spotify features are enabled
     */
    public static boolean getSpotifyEnabled() {
        return spotifyEnabled;
    }
    
    /**
     * Get the Spotify username
     * @return Spotify username
     */
    public static String getSpotifyUsername() {
        return spotifyUsername;
    }
    
    /**
     * Get the Spotify password
     * @return Spotify password
     */
    public static String getSpotifyPassword() {
        return spotifyPassword;
    }

    /**
     * Loads the volume preference
     * 
     * @return Whether load was successful
     */
    private static boolean loadVolumePreference() {
        try {
            FileInputStream fstream = new FileInputStream(System.getProperty("user.home") + "/.cava/" + volumeconf);
            DataInputStream in = new DataInputStream(fstream);

            volume = in.readFloat();
            in.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }
    
    /**
     * Update volume preference
     * 
     * @param volume
     *            New volume
     */
    public static void updateVolumePreference(float volume) {
        if(volume != Preferences.volume) {
            Preferences.volume = volume;
            
            if(spotifyplayer != null) {
                spotifyplayer.setVolume(volume);
            }
    
            try {
                FileOutputStream fstream = new FileOutputStream(System.getProperty("user.home") + "/.cava/" + volumeconf);
                DataOutputStream out = new DataOutputStream(fstream);
    
                out.writeFloat(volume);
                out.flush();
                out.close();
            } catch (IOException e) {
                Dbg.syserr("Writing to volume config file failed");
            }
        }
    }
    
    public static void updateLyricsScrubberPrefs(boolean reset) {
    	try {
    		if(!reset) {
	    		FileOutputStream fstream = new FileOutputStream(System.getProperty("user.home") + "/.cava/" + scrubberconf);
	    		DataOutputStream o = new DataOutputStream(fstream);
	    		o.writeBytes("true");
	    		o.flush();
	    		o.close();
    		} else {
    			File f = new File(System.getProperty("user.home") + "/.cava/" + scrubberconf);
    			if(!f.delete()) {
    				System.err.println("Saving scrubber prefs failed.");
    			}
    		}
    	} catch (IOException e) {
    		System.err.println("Saving scrubber prefs failed.");
    	}
    }
    
    public static boolean getLyricsScrubberStarted() {
    	File f = new File(System.getProperty("user.home") + "/.cava/" + scrubberconf);
    	return f.exists();
    }
    
    /**
     * Get the volume preference
     * 
     * @return Volume preference
     */
    public static float getVolume() {
        return Preferences.volume;
    }
    
    /**
     * Loads a separate JFrame to show a stock message about Terms and
     * Conditions to do with the SCMS data
     */
    private void tandcs() {
        JFrame frame = new JFrame("Terms and Conditions");
        String text = "We send information about the songs to our server, "
            + "where we store a database of song data. "
            + "We do this so that we can suggest songs to you. "
            + "We DO NOT store any information about you - the track data "
            + "is sent anonymously, and in fact we do not even store "
            + "your library (that's not how the song-matching works).";

        JTextArea textArea = new JTextArea(text);
        textArea.setSize(290, 10);
        frame.setSize(320, 150);
        frame.setResizable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setForeground(Color.white);
        textArea.setFont(Cava.Ari);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        frame.add(textArea, c);
        frame.setVisible(true);
    }

    private static String getFakePassword(int length) {
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i < length; i++) {
            buffer.append('\0');
        }
        
        return buffer.toString();
    }
    
    /**
     * Displays a panel containing JLabels, a JTextField and a JPasswordField,
     * so that the user's username and password can be saved for any service.
     * 
     * @param service
     *            Service (Service.LASTFM or Service.SPOTIFY)
     * @return returns the panel so it can be displayed
     */
    protected JComponent service(final Service service) {
        JPanel panel = new JPanel(false);
        
        final JCheckBox enabled;
        final JTextField username;
        String fakePassword = "";
        
        if(service == Service.LASTFM) {
            enabled = new JCheckBox("Activate Last.fm scrobbling", lastfmEnabled);
            username = new JTextField(lastfmUsername, 15);
            
            fakePassword = getFakePassword(lastfmPasswordLength);
        } else {
            enabled = new JCheckBox("Activate Spotify features", spotifyEnabled);
            username = new JTextField(spotifyUsername, 15);
            
            fakePassword = getFakePassword(spotifyPasswordLength);
        }
        
        enabled.setOpaque(false);
        enabled.setFont(Cava.Ari);
        enabled.setForeground(Color.WHITE);
        
        final JPasswordField password = new JPasswordField(fakePassword, 15);
        username.setForeground(Color.WHITE);
        password.setForeground(Color.WHITE);
        username.setBorder(new LineBorder(Color.GRAY));
        password.setBorder(new LineBorder(Color.GRAY));
        username.setCaretColor(Color.white);
        password.setCaretColor(Color.white);
        username.setOpaque(false);
        password.setOpaque(false);
        JLabel insertUsername = new JLabel("Username: ");
        JLabel insertPassword = new JLabel("Password: "); 

        insertUsername.setForeground(Color.white);
        insertPassword.setForeground(Color.white);

        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        panel.add(enabled, c);

        c.gridwidth = 1;
        c.gridy = 2;
        panel.add(insertUsername, c);

        c.gridx = 1;
        panel.add(username, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(insertPassword, c);

        c.gridx = 1;
        panel.add(password, c);

        enabled.addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if(service == Service.LASTFM) {
                    updateLastfmPreferences(enabled.isSelected(), username.getText(), String.valueOf(password.getPassword()));
                } else {
                    updateSpotifyPreferences(enabled.isSelected(), username.getText(), String.valueOf(password.getPassword()));
                }
            }
        });
        
        username.addFocusListener(new FocusListener() {  
            @Override
            public void focusLost(FocusEvent arg0) {
                if(service == Service.LASTFM) {
                    updateLastfmPreferences(enabled.isSelected(), username.getText(), String.valueOf(password.getPassword()));
                } else {
                    updateSpotifyPreferences(enabled.isSelected(), username.getText(), String.valueOf(password.getPassword()));
                }
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {}
        });
        
        password.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                if(service == Service.LASTFM) {
                    String passwordstr = String.valueOf(password.getPassword());
                    updateLastfmPreferences(enabled.isSelected(), username.getText(), passwordstr);
                    password.setText(getFakePassword(passwordstr.length()));
                } else {
                    String passwordstr = String.valueOf(password.getPassword());
                    updateSpotifyPreferences(enabled.isSelected(), username.getText(), passwordstr);
                    password.setText(getFakePassword(passwordstr.length()));
                }
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {
                // Blank the password box if the user clicks it
                password.setText("");
            }
        });

        return panel;
    }

    /**
     * Create the GUI and show it.
     */
    public static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Preferences");
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add content to the window.
        frame.add(new Preferences(), BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(400, 260));
        // Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
