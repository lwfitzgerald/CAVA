package cava;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Tray {
	
	private Image icon;
	private JFrame frame;
	private Boolean maxed;
	private AudioPlayer audioPlayer;
	private ImageButton button;
	private Cava cava;

	public Tray(Image imgPlay, JFrame mainFrame, AudioPlayer audioPlayer, ImageButton button, Cava cava) {
		this.icon = imgPlay;
		this.frame = mainFrame;
		this.audioPlayer = audioPlayer;
		this.button = button;
		this.cava = cava;
		maxed = true;
	}
	
	public void setupTray() {
		//Make program minimise to tray
		//Check the SystemTray is supported
		if (!SystemTray.isSupported()) {
		    System.out.println("SystemTray is not supported");
		    return;
		}
		
		final TrayIcon trayIcon = new TrayIcon(icon);
		trayIcon.setImageAutoSize(true);
		final SystemTray tray = SystemTray.getSystemTray();
		
		// Add mouse listener
		MouseListener mouseListener = new MouseListener() {
                
			public void mouseClicked(MouseEvent e) {
				// This modifier is just to make sure it is not the right mouse button triggering the max/min
				if((e.getModifiers() & InputEvent.BUTTON3_MASK) != InputEvent.BUTTON3_MASK) {
					if( maxed == true ) { 
						frame.setVisible(false);
						maxed = false;
					} else {
						frame.setVisible(true);
						maxed = true;
					}
				}
			}
		
			public void mouseEntered(MouseEvent e) {               
			}
		
			public void mouseExited(MouseEvent e) {               
			}
		
			public void mousePressed(MouseEvent e) {            
			}
		
			public void mouseReleased(MouseEvent e) {                
			}
		    };
		
		// Create a popup menu components
		final PopupMenu popup = new PopupMenu();
		
		// Make play/pause button and add action listener
		MenuItem playPause = new MenuItem("Play / pause");
		ActionListener playListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( audioPlayer.isPlaying() ) {
					if( audioPlayer.isPaused() == false ) {
						audioPlayer.DoAudioAction(AudioAction.Pause);
						button.setPlayPauseButton(AudioAction.Resume);
					} else {
						audioPlayer.DoAudioAction(AudioAction.Resume);
						button.setPlayPauseButton(AudioAction.Pause);
					}
				}
			}
		    };
		playPause.addActionListener(playListener);

		// Make skip forward button and add action listener
		MenuItem forward = new MenuItem("Forward >>");
		ActionListener forwardListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    audioPlayer.DoAudioAction(AudioAction.SkipForward);
			}
		    };
		    forward.addActionListener(forwardListener);
		    
		    // Make skip back button and add action listener
		MenuItem back = new MenuItem(   "Back    <<");
		ActionListener backListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    audioPlayer.DoAudioAction(AudioAction.SkipBack);
			}
		    };
		    back.addActionListener(backListener);
		    
		    // Make exit button and add action listener
		MenuItem exit = new MenuItem("Quit Cava");
		    ActionListener exitListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    cava.trayCleanup();
			}
		    };
		    exit.addActionListener(exitListener);
	       
		//Add components to popup menu
		popup.add(playPause);
		popup.add(forward);
		popup.add(back);
		popup.addSeparator();
		popup.add(exit);
		//popup.setBorder(new LineBorder(lightbordercolor, 1));
		//popup.setBackground(Color.black);
		    
		
		trayIcon.addMouseListener(mouseListener);
		trayIcon.setPopupMenu(popup);
	       
		try {
		    tray.add(trayIcon);
		} catch (AWTException e) {
		    Dbg.syserr("TrayIcon could not be added.");
		}
	}
	
}
