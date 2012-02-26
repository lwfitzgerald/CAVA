package cava;

import java.io.File;

/**
 * FileFilter is a custom filter, applied by the FileChooser class to make sure when navigating for songs, only the specified formats
 * are displayed.At time of writing this, these are just mp3 and flac. This may be increased as Xuggler is capable of far more formats,
 * but at the moment these are the only ones which the database parses.
 * @auther  Dave Glencross
 */
class FileFilter extends javax.swing.filechooser.FileFilter {
    /**
     * Checks whether any given file is either a directory, or of the specified file formats.
     *
     * @param file	the file being checked
     * @return	<code>true</code> if the file is a directory or specified file format
     *		<code>false</code> otherwise
     */
    @Override
    public boolean accept(File file) {
        // Allow only directories, or files with an extension indicating it's audio
        return file.isDirectory() || file.getAbsolutePath().endsWith(".mp3") || file.getAbsolutePath().endsWith(".flac");
    }
    /**
     * Gives the description of what files formats are displayed.
     * @return	a string with the description
     */
    @Override
    public String getDescription() {
        // This description will be displayed in the dialog,
        // hard-coded = ugly, should be done via I18N
        return "Music files (*.mp3, *.flac)";
    }
} 
