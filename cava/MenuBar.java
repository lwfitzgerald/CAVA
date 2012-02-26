/**
 * The MenuBar is, as the name suggests, the menu bar displayed at the top left-hand corner of the main Cava window. Pressing the menu
 * items generally triggers a new window with something to be loaded in. The specifics are dealt with in other classes, which are
 * called by actionListeners.
 * 
 * @author Dave Glencross
 */

package cava;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class MenuBar implements ActionListener, ItemListener {

	String add[] = { "" };
	JMenuBar menuBar;
	//JPanel tabbedPane;
	BuildDatabase database;

	public static void main(String args[]) {
		new MenuBar();
	}
	
	/**
	* The constructor for the menu bar
	*/
	public MenuBar() {
		setupMenuBar();
	}
	
	public JMenuBar setupMenuBar() {
	//Where the GUI is created:
	JMenu menu, submenu;
	JMenuItem menuItem;
	JCheckBoxMenuItem CB;
	
	//Create the menu bar.
	menuBar = new JMenuBar();
        menuBar.setBackground(Cava.darkish);
        menuBar.setOpaque(false);

        Color fontc = Color.BLACK;
        Font dafont = Cava.Cali;     
        
	//Build the FILE menu.
	menu = new JMenu("File");
        menu.setFont(dafont);
        menu.setForeground(fontc);
        menu.setMnemonic(KeyEvent.VK_A);
	menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
	menuBar.add(menu);
	
	//Add a submenu called ADD
	submenu = new JMenu("Add to library");
	submenu.setMnemonic(KeyEvent.VK_S);
	
	//Have the ADD FILE button
	menuItem = new JMenuItem("File",KeyEvent.VK_T);
	menuItem.setAccelerator(KeyStroke.getKeyStroke('O', ActionEvent.CTRL_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("Select a file to add to the library");
	menuItem.addActionListener(this);
	submenu.add(menuItem);
	
	//Have the ADD FOLDER button
	menuItem = new JMenuItem("Folder",KeyEvent.VK_T);
	menuItem.getAccessibleContext().setAccessibleDescription("Select a folder to add to the library");
	menuItem.addActionListener(this);
	submenu.add(menuItem);
	
	menu.add(submenu);
	
	//Have the ADD EXIT button
	menuItem = new JMenuItem("Exit",KeyEvent.VK_T);
	//menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("Quit the program");
	menuItem.addActionListener(this);
	menu.add(menuItem);
	
	//Build EDIT menu in the menu bar.
	menu = new JMenu("Edit");
        menu.setFont(dafont);
        menu.setForeground(fontc);
	menu.setMnemonic(KeyEvent.VK_N);
	menu.getAccessibleContext().setAccessibleDescription(
		"This menu does nothing");
	
	//Have the PREFERENCES button
	menuItem = new JMenuItem("Preferences",KeyEvent.VK_T);
	menuItem.setAccelerator(KeyStroke.getKeyStroke('P', ActionEvent.CTRL_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
	menuItem.addActionListener(this);
        menu.add(menuItem);
	menuBar.add(menu);
	
	//Build PLAYBACK menu in the menu bar
	menu = new JMenu("Playback");
        menu.setFont(dafont);
        menu.setForeground(fontc);
	menu.setMnemonic(KeyEvent.VK_N);
	menu.getAccessibleContext().setAccessibleDescription(
		"This menu changes playback options");
		
	//check box menu items for SHUFFLE, REPEAT
	CB = new JCheckBoxMenuItem("Shuffle");
	CB.setMnemonic(KeyEvent.VK_C);
	CB.addItemListener(this);
	menu.add(CB);
	
	CB = new JCheckBoxMenuItem("Repeat");
	CB.setMnemonic(KeyEvent.VK_H);
	CB.addItemListener(this);
	menu.add(CB);
	
	menuBar.add(menu);
	
	//Build TOOLS menu in the menu bar.
	menu = new JMenu("Tools");
        menu.setFont(dafont);
        menu.setForeground(fontc);
	menu.setMnemonic(KeyEvent.VK_N);
	menu.getAccessibleContext().setAccessibleDescription(
		"This menu does nothing");
	
	//Have the PREFERENCES button
	menuItem = new JMenuItem("Preferences",KeyEvent.VK_T);
	//menuItem.setAccelerator(KeyStroke.getKeyStroke('P', ActionEvent.CTRL_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
	menuItem.addActionListener(this);
        menu.add(menuItem);
	menuBar.add(menu);

	return menuBar;
	}
	
	/**
	* Deals with JMenuItems being pressed, but not the JCheckboxes
	*/
	public void actionPerformed(ActionEvent e) {
		JMenuItem source = (JMenuItem)(e.getSource());
		//System.out.println(e.getSource());
		String s = source.getText();
		// Add file
		if ( s.equals("File") ) {
			FileChooser fc = new FileChooser();
			add = fc.openFileChooser(false);
			if( add != null) { 
				database = new BuildDatabase(null);
				database.addSongToLibrary(add);
			}
		}
		// Add folder
		if ( s.equals("Folder") ) {
			FileChooser fc = new FileChooser();
			add = fc.openFileChooser(true);
			
			if( add != null) { 
				database = new BuildDatabase(null);
				database.addNewFolder(add);
			}
		}
		// Exit
		if ( s.equals("Exit") ) {
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(Cava.mainCavaFrame, WindowEvent.WINDOW_CLOSING));
		}
		// Load preferences window
		if ( s.equals("Preferences") ) {
			Preferences.createAndShowGUI();
		}
	}
	
	/**
	* itemStateChanged deals with the JCheckBoxes being changed
	*/
	public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
	String s = source.getText();
	if( s.equals("Shuffle")) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				NowPlaying.shuffle = true;
			} else {
				NowPlaying.shuffle = false;
			}
			
			//System.out.println( NowPlaying.shuffle );
	}
	if( s.equals("Repeat")) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				NowPlaying.repeat = true;
			} else {
				NowPlaying.repeat = false;
			}
			
			//System.out.println( NowPlaying.repeat );
	}
        //System.out.println(s);
	}
	
	// Returns just the class name -- no package info.
	protected String getClassName(Object o) {
		String classString = o.getClass().getName();
		int dotIndex = classString.lastIndexOf(".");
		return classString.substring(dotIndex+1);
	}
	
}