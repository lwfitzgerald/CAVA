package cava;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;


/**
* FileChooser loads the file explorer so users can import their tracks by graphically selecting folders or files. It loads a separate
* popup window, in the form of a JFileChooser. It also makes use of the FileFilter class so that the only items displayed in the
* JFileChooser are directories and specified files.
*
* @author   Dave Glencross
*/
class FileChooser {
	
	/**
	* This is the only method, and does what is described above.
	*
	* @param addfolder	this is a boolean which specifies whether the user has requested to add a single file or a whole
	* 			folder to the library. If it is a single song, nothing changes. If it is a whole track, the
	*			file chooser does not display files, only directories.
	* @return		an array of strings is returned with the full path of the selected files or directories. If there is	*			nothing to return, most likely because the user pressed cancel, it returns null.
	*/
	public String[] openFileChooser(Boolean addfolder) {

		JPanel parent = new JPanel();
		parent = null;
		JFileChooser chooser = new JFileChooser();
		
		String directory = Preferences.getDirPreference();		
		if (directory!= null && !directory.trim().isEmpty()){
			chooser.setCurrentDirectory(new File(directory));
		}
		
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		// If we have requested to open a folder rather than file, make the file chooser aware of this
		if(addfolder == true) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		
		//Allow user to select multiple files at once (or folders)
		chooser.setMultiSelectionEnabled(true);
		chooser.setDragEnabled(false);
		
		// Create the filter so only music files are displayed
		FileFilter filter = new FileFilter();
		chooser.setFileFilter(filter);
		
		// Make sure if we press cancel it doesn't screw up
		int returnVal = chooser.showOpenDialog(parent);
		
		// If the user pressed OK then go!
		if(returnVal == JFileChooser.APPROVE_OPTION) {

			File[] files = chooser.getSelectedFiles();

			ArrayList<String> pathsList = new ArrayList<String>();
			int i=0;
			for( i=0; i<files.length; i++ ) {
				pathsList.add(files[i].getPath());
			}
			
			String []paths = new String[i];
			pathsList.toArray(paths);
			
			return paths;

		}

		// If the user pressed cancel (or for whatever other reason failed to select a song), we just return null
		return null;

    }
		
}
