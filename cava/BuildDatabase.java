package cava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import cava.Database.CavaResultSet;

/**
 * Build the user's track Database from scratch by searching their computer and/or 
 * using the specified folder
 * @author Ben
 *
 */
public class BuildDatabase {

    private Database db;
    private String databaseName = "trackDB";
    private TagReader tagReader;
    private ArrayList<String> listOfFailedSongs = new ArrayList<String>();
    private ArrayList<String> listOfIgnoredSongs = new ArrayList<String>();
    private ArrayList<String> listOfSongsWithoutSCMS = new ArrayList<String>();
//	private ArrayList<String> listOfAddedSongs = new ArrayList<String>();
//	private StringBuilder trackAddSQL = new StringBuilder("INSERT INTO track (trackname,lowertrackname,albumid,artistid,path,bitrate,codec,length,trackno) VALUES");
//	private int maxWaitBeforeAdding = 500;
    //private String trackAddSQL = "INSERT INTO track (trackname,lowertrackname,albumid,artistid,path,bitrate,codec,length,trackno) VALUES";
    private int addedSongs;
    private boolean debugging = true;
//	private HashMap<String, Integer> artistIDs = new HashMap<String, Integer>(256);
//	private HashMap<String, Integer> albumStore = new HashMap<String, Integer>(256);
    private TreeMap<String, Integer> artistIDs = new TreeMap<String, Integer>();
    private TreeMap<String, Integer> albumStore = new TreeMap<String, Integer>();
    private boolean skipPathCheck;
//	private int numTrackSinceLastInsert;
    private boolean tablesCreatedSuccessfully;
    public static Browser browser = null;
    //These must be static to ensure we don't end up with multiple adders running
    private static TrackAdder trackAdder;
    private static boolean creatingTables = false;

    /**
     * Convenience method. Automatically specifies create as false.
     * Connects to the database in the given location
     * @param path the path to the database (excluding database name) you wish to connect to
     */
    BuildDatabase(String path) {
        if (path == null) {
            path = getDefaultDBLocation();
        }
        tagReader = new TagReader();
        db = new Database(path + "/" + databaseName);
        addedSongs = 0;
    }

    /**
     * Connects (and optionally creates) the database in the location given
     * @param path the path to the database (excluding database name) you wish to connect to
     * @param create if true, the database is first created.
     */
    BuildDatabase(String path, boolean create) {
        if (path == null) {
            path = getDefaultDBLocation();
        }
        tagReader = new TagReader();
        tablesCreatedSuccessfully = false;
        db = new Database(path + "/" + databaseName, create);
        addedSongs = 0;
        if (create == true) {
            buildTables();
        }
    }

    /**
     * Builds the required tables for the track database (artist,album and track)
     */
    private synchronized void buildTables() {
        creatingTables = true;
        tablesCreatedSuccessfully = true;
        if (!db.isConnected()) {
            Dbg.syserr("Could not connect to database!");
            tablesCreatedSuccessfully = false;
        }
        String trackTable = "CREATE TABLE track (trackid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY "
                + "(START WITH 1, INCREMENT BY 1),trackname VARCHAR(100) NOT NULL,lowertrackname VARCHAR(100) NOT NULL, albumid INTEGER NOT NULL, "
                + "artistid INTEGER NOT NULL, path VARCHAR(350) NOT NULL, bitrate VARCHAR(12) "
                + "NOT NULL, codec VARCHAR(10) NOT NULL, length SMALLINT NOT NULL, trackno SMALLINT "
                + "NOT NULL, scms BLOB(2147483647),scmsavailable SMALLINT NOT NULL DEFAULT 0,scmsUploaded SMALLINT NOT NULL DEFAULT 0)";

        String albumTable = "CREATE TABLE album (albumid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY "
                + "(START WITH 1, INCREMENT BY 1), artistid INTEGER NOT NULL, albumname VARCHAR(100) NOT NULL, "
                + "loweralbumname VARCHAR(100) NOT NULL, artlocation VARCHAR(350))";

        String artistTable = "CREATE TABLE artist (artistid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY "
                + "(START WITH 1, INCREMENT BY 1), artistname VARCHAR(100) NOT NULL, sortbyname VARCHAR(100) NOT NULL)";

        String playlistTable = "CREATE TABLE playlist (id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY"
                + "(START WITH 1, INCREMENT BY 1), name VARCHAR(100) NOT NULL)";

        String playlistTracksTable = "CREATE TABLE playlistTracks (ptid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY"
                + " (START WITH 1, INCREMENT BY 1),playlistid INTEGER NOT NULL, trackid INTEGER NOT NULL DEFAULT -1, trackorder INTEGER "
                + "NOT NULL, spotifyID INTEGER NOT NULL DEFAULT -1)";

        String spotifyTracksTable = "CREATE TABLE spotifyTracks (spotifyID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY "
                + "(START WITH 1, INCREMENT BY 1),playlistid INTEGER NOT NULL, spotifyLink VARCHAR(50) NOT NULL, trackname VARCHAR(100) NOT NULL, "
                + "artistname VARCHAR(100) NOT NULL )";

        String spotifyTracksPK = "ALTER TABLE spotifyTracks ADD CONSTRAINT \"SPOTIFY_PK\" PRIMARY KEY (spotifyID) ";


        String playlistPK = "ALTER TABLE playlist ADD CONSTRAINT \"PLAYLIST_PK\" PRIMARY KEY (id)";
        String tablePK = "ALTER TABLE track ADD CONSTRAINT \"TRACK_PK\" PRIMARY KEY (trackid)";
        String albumPK = "ALTER TABLE album ADD CONSTRAINT \"ALBUM_PK\" PRIMARY KEY (albumid)";
        String artistPK = "ALTER TABLE artist ADD CONSTRAINT \"PRIMARY_KEY\" PRIMARY KEY (artistid)";
        String playlistTracksPK = "ALTER TABLE playlisttracks ADD CONSTRAINT \"PT_PK\" PRIMARY KEY(ptid) ";
        String playlistTracksIndex = "CREATE INDEX playlistIndex ON playlistTracks(playlistid) ";

        String lyrics = "CREATE TABLE lyrics(trackid int, lyrics varchar(5000),lyricsUnavailable SMALLINT NOT NULL DEFAULT 0)";
        //String pathIndex = "CREATE INDEX pathindex ON track(path) ";
        //String albumNameIndex = "CREATE INDEX albumnameindex ON album(loweralbumname) ";
        //String artistNameIndex = "CREATE INDEX artistindex ON track(path) ";

        //On error, set skipPathCheck to false;


        if (!db.createTable(trackTable)) {
            Dbg.syserr("Failed to create track table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(albumTable)) {
            Dbg.syserr("Failed to create album table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(artistTable)) {
            Dbg.syserr("Failed to create artist table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(playlistTable)) {
            Dbg.syserr("Failed to create playlist table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(playlistTracksTable)) {
            Dbg.syserr("Failed to create playlist tracks table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(spotifyTracksTable)) {
            Dbg.syserr("Failed to create Spotify tracks table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(tablePK)) {
            Dbg.syserr("Failed to add PK to track table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(albumPK)) {
            Dbg.syserr("Failed to add PK to album table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(artistPK)) {
            Dbg.syserr("Failed to add PK to artist table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(playlistPK)) {
            Dbg.syserr("Failed to add PK to playlist table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(playlistTracksPK)) {
            Dbg.syserr("Failed to add PK to playlist tracks table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(playlistTracksIndex)) {
            Dbg.syserr("Failed to add index to playlist tracks table (playlistID column");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(spotifyTracksPK)) {
            Dbg.syserr("Failed to add PK to spotify tracks table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }

        if (!db.createTable(lyrics)) {
            Dbg.syserr("Failed to create lyrics table");
            //db.printLastError();
            //System.out.println("------------------------------------------");
            skipPathCheck = false;
            tablesCreatedSuccessfully = false;
        }
        creatingTables = false;
    }

    public synchronized int addNewFolder(String baseDirectory) {
        return addNewFolder(baseDirectory, true);
    }

    public synchronized int addNewFolder(String baseDirectory, boolean recusive) {
        String[] baseDirectories = {baseDirectory};
        return addNewFolder(baseDirectories, recusive);
    }

    public synchronized int addSongToLibrary(String fullPath) {
        String[] fullPaths = {fullPath};
        return addSongToLibrary(fullPaths);
    }

    /**
     * Convenience method. Sets recursive to true automatically
     * Adds any songs in the specified folder to the database
     * @param baseDirectory the path to the folder that contains the songs you wish to add
     */
    public synchronized int addNewFolder(String[] baseDirectories) {
        return addNewFolder(baseDirectories, true);
    }

    public synchronized int addNewFolder(String[] baseDirectories, boolean recursive) {
        if (creatingTables == true || (trackAdder != null && trackAdder.isAlive())) {
            new ErrorMessage("Importing already in progress. Please wait before importing further tracks");
            return -1;
        }

        if (!db.isConnected()) {
            Dbg.syserr("Could not connect to database!");
            return -1;
        }

        //Reset counts
        listOfFailedSongs = new ArrayList<String>();
        listOfIgnoredSongs = new ArrayList<String>();
        listOfSongsWithoutSCMS = new ArrayList<String>();
        addedSongs = 0;


        skipPathCheck = isDatabaseEmpty();
        if (debugging) {
            if (skipPathCheck) {
                Dbg.sysout("Skipping path check; Database is empty");
            } else {
                Dbg.sysout("Not skipping path check; Database not empty");
            }
        }
        trackAdder = new TrackAdder(baseDirectories, recursive);
        trackAdder.start();
        return addedSongs;

    }

    private boolean isDatabaseEmpty() {
        CavaResultSet rs;
        if ((rs = db.select("SELECT COUNT(*) FROM track")) != null) {
            if (rs.fetchRow()) {
                if (rs.fetchInt(1) > 0) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        if ((rs = db.select("SELECT COUNT(*) FROM artist")) != null) {
            if (rs.fetchRow()) {
                if (rs.fetchInt(1) > 0) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        if ((rs = db.select("SELECT COUNT(*) FROM album")) != null) {
            if (rs.fetchRow()) {
                if (rs.fetchInt(1) > 0) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }


        return true;
    }

    public synchronized int addSongToLibrary(String[] fullPaths) {
        if (creatingTables == true || (trackAdder != null && trackAdder.isAlive())) {
            new ErrorMessage("Importing already in progress. Please wait before importing further tracks");
            return -1;
        }

        if (!db.isConnected()) {
            Dbg.syserr("Could not connect to database!");
            return -1;
        }
        //Reset counts
        listOfFailedSongs = new ArrayList<String>();
        listOfIgnoredSongs = new ArrayList<String>();
        listOfSongsWithoutSCMS = new ArrayList<String>();
        addedSongs = 0;
        skipPathCheck = false;
        trackAdder = new TrackAdder(fullPaths);
        trackAdder.start();
        return addedSongs;
    }

    public void startSCMSGeneration() {
        if (Constants.MIRAJEAVAILABLE) {
            SCMSGenerator.generateSCMS();
        }
    }

    /**
     * Create the 'sortbyartist' field from the artist name. Converts to lower case, and
     * removes 'the' if necessary
     * @param artist the artist name to convert
     * @return the converted artist name
     */
    public static String createSortByArtist(String artist) {
        String sortByArtist = artist.toLowerCase();
        if (sortByArtist.length() > 5) {
            if (sortByArtist.substring(0, 4).equals("the ")) {
                sortByArtist = sortByArtist.substring(4);
            }
        }
        return sortByArtist;
    }

    /**
     * Find out if the given file is a valid music file by looking at its extension
     * @param path the path to the file
     * @return true if the extension is mp3 or flac, false if not
     */
    private boolean isValidMusicFile(String path) {
        String[] validExtensions = new String[2];
        validExtensions[0] = "mp3";
        validExtensions[1] = "flac";
        int indexOfLastDot = path.lastIndexOf('.');
        if (indexOfLastDot == -1) {
            return false;
        }
        for (String s : validExtensions) {
            if (s.equalsIgnoreCase(path.substring(indexOfLastDot + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints any songs that could not be added to the database
     */
    public void printFailedItems() {
        if (listOfFailedSongs.size() > 0) {
            ErrorMessage msg = new ErrorMessage("There were " + listOfFailedSongs.size() + " songs that could not be added to your library:");
            for (String s : listOfFailedSongs) {
                msg.addErrorMessage(s);
            }
        } else {
            Dbg.sysout("No failed items");
        }
    }

    /**
     * Prints any songs that were not added to the database beacuse they were already in it
     */
    public void printIgnoredItems() {
        if (listOfIgnoredSongs.size() > 0) {
            Dbg.sysout("There were " + listOfIgnoredSongs.size() + " ignored items. Printing...");
            for (String s : listOfIgnoredSongs) {
                Dbg.sysout(s);
            }
        } else {
            Dbg.sysout("No ignored items");
        }
    }

    /**
     * Prints any songs that SCMS data could not be generated for
     */
    public void printSCMSFailedItems() {
        if (listOfSongsWithoutSCMS.size() > 0) {
            ErrorMessage msg = new ErrorMessage("There were " + listOfSongsWithoutSCMS.size() + " that SCMS data could not be generated from:");
            for (String s : listOfSongsWithoutSCMS) {
                msg.addErrorMessage(s);
            }
        } else {
            Dbg.sysout("No items failed SCMS generation");
        }
    }

    /**
     * Print the count of added songs
     */
    public void printCountOfAddedItems() {
        Dbg.sysout("There were " + addedSongs + " song(s) added to the database");
    }

    public boolean tablesCreatedSuccefully() {
        db.printLastError();
        return tablesCreatedSuccessfully;
    }

    /**
     * Inserts prepared dummy data. Useful if you need to test but dont have access to any music files (i.e.)
     * in labs. I will probably expand this section to make it more useful.
     */
    public void addDummyData() {
        String artistData = "INSERT INTO artist (artistname,sortbyname) VALUES('The Killers','Killers')"
                + ",('Bloc Party','Bloc Party'),('Arcade Fire','Arcade Fire')";
        if (db.insert(artistData) != 3) {
            Dbg.syserr("Failed to insert data to artist table");
            Dbg.syserr("Failed to insert data to artist table");
            db.printLastError();
            Dbg.syserr("------------------------------------------");
        }

        String albumData = "INSERT INTO album (artistid,albumname) VALUES(1,'Hot Fuss'),(1,'Day & Age')"
                + ",(1,'Sam''s Town'),(2,'Silent Alarm'),(2,'A weekend in the city'),(3,'Neon Bible'),"
                + "(3,'Funeral')";
        if (db.insert(albumData) != 7) {
            Dbg.syserr("Failed to insert data to album table");
            db.printLastError();
            Dbg.syserr("------------------------------------------");
        }

        String trackData = "INSERT INTO track (trackname,albumid,artistid,path,bitrate,codec,length,trackno) "
                + "VALUES('Jenny Was A Friend Of Mine',1,1,' ',' ',' ',245,1),"
                + "('Mr. Brightside',1,1,'','','',222,2),"
                + "('Smile Like you Mean it',1,1,'','','',235,3),"
                + "('Somebody Told Me',1,1,'','','',197,4),"
                + "('Losing Touch',2,1,'','','',254,1),"
                + "('Human',2,1,'','','',245,2),"
                + "('A Dustland Fairytale',2,1,'','','',225,5),"
                + "('Sam''s Town',3,1,'','','',246,1),"
                + "('Like Eating Glass',4,2,'','','',262,1),"
                + "('Song For Clay',5,2,'','','',290,1),"
                + "('Black Mirror',6,3,'','','',253,1),"
                + "('Neighbourhood #1 Tunnels',7,3,'','','',288,1)";
        if (db.insert(trackData) != 12) {
            Dbg.syserr("Failed to insert data to track table");
            db.printLastError();
            Dbg.syserr("------------------------------------------");
            Dbg.syserr(trackData + "");
        }
    }

    /**
     * Print the usage instructions
     */
    public static void printUsageInstructions() {
        Dbg.syserr("Error in usage.\nRun java BuildDatabase create [-l dblocation] to create the database");
        Dbg.syserr("\nRun java BuildDatabase add [-l dblocation] [-f folder] [-r recursive] [-d]  to add a folder to the already created database");
        Dbg.syserr("\nRun java BuildDatabase both [-l dblocation] [-f folder] [-r recursive] [-d] to create the database and add the specified folder");
        Dbg.syserr("\n\nOptions");
        Dbg.syserr("\n-l dblocation: the location to store the database folder. Default is ../lib/database/");
        Dbg.syserr("\n-f folder: the folder to add. Default is ../lib/database/SampleMusic/");
        Dbg.syserr("\n-r recursive: If you want to recurse through folders, specify true. To limit the additions to that folder only, specify false. True by default");
        Dbg.syserr("\n-d: If you do not want to add 'real' data to the database, but add some prepared dummy data, use this flag");
    }

    private String getDefaultDBLocation() {
        String currentDir = this.getClass().getResource("./").toString();
        int lastSlash = currentDir.lastIndexOf('/');
        lastSlash = currentDir.lastIndexOf('/', lastSlash - 1);
        lastSlash = currentDir.lastIndexOf('/', lastSlash - 1);
        String dbLocation = currentDir.substring(5, lastSlash + 1) + "lib/database/";
        if (dbLocation.charAt(2) == ':') {
            dbLocation = dbLocation.substring(1);
        }
        return System.getProperty("user.home") + "/.cava";
    }

    public static void cancelTrackImporting() {
        if (trackAdder != null) {
            trackAdder.cancel();
        }
    }

    /**
     * Create the database from the command line
     * @param args run without any arguments to see help
     */
    public static void main(String[] args) {

        String defaultLocationForDB = null;
        String defaultLocationForTracks = "../lib/database/SampleMusic";
        boolean create = false;
        boolean recurse = true;
        boolean add = false;
        boolean dummy = false;
        if (args.length == 0) {
            BuildDatabase.printUsageInstructions();
            System.exit(0);
        }
        String option = args[0];
        if (option.equalsIgnoreCase("run-tests")) {
            runTests();
            return;
        }
        if (option.equalsIgnoreCase("create")) {
            create = true;
        } else if (option.equalsIgnoreCase("add")) {
            create = false;
            add = true;
        } else if (option.equalsIgnoreCase("both")) {
            create = true;
            add = true;
        } else {
            BuildDatabase.printUsageInstructions();
            System.exit(0);
        }
        for (int arg = 1; arg < args.length; arg++) {
            if (args[arg].equalsIgnoreCase("-l")) {
                if (args.length < arg + 2) {
                    BuildDatabase.printUsageInstructions();
                    System.exit(0);
                } else {
                    defaultLocationForDB = args[++arg];
                }
            } else if (args[arg].equalsIgnoreCase("-f")) {
                if (args.length < arg + 2) {
                    BuildDatabase.printUsageInstructions();
                    System.exit(0);
                } else {
                    defaultLocationForTracks = args[++arg];
                }
            } else if (args[arg].equalsIgnoreCase("-r")) {
                if (args.length < arg + 2) {
                    BuildDatabase.printUsageInstructions();
                    System.exit(0);
                } else {
                    String r = args[++arg];
                    if (r.equalsIgnoreCase("true")) {
                        recurse = true;
                    } else if (r.equalsIgnoreCase("false")) {
                        recurse = false;
                    } else {
                        BuildDatabase.printUsageInstructions();
                        System.exit(0);
                    }
                }
            } else if (args[arg].equalsIgnoreCase("-d")) {
                dummy = true;
            } else {
                BuildDatabase.printUsageInstructions();
                System.exit(0);
            }
        }

        Dbg.sysout("Location : " + defaultLocationForDB);
        Dbg.sysout("Folder   : " + defaultLocationForTracks);
        Dbg.sysout("Create   : " + create);
        Dbg.sysout("Recurse  : " + recurse);
        Dbg.sysout("Add      : " + add);
        Dbg.sysout("Dummy    : " + dummy);
        Long startTime = System.currentTimeMillis();
        BuildDatabase program = new BuildDatabase(defaultLocationForDB, create);

        if (dummy == true) {
            program.addDummyData();
        } else if (add == true) {
            program.addNewFolder(defaultLocationForTracks, recurse);
            program.printCountOfAddedItems();
            program.printFailedItems();
            program.printIgnoredItems();
        }
        Long endTime = System.currentTimeMillis();
        Long duration = endTime - startTime;
        int durationInseconds = (int) (duration / 1000);
        int minutes = (int) durationInseconds / 60;
        int seconds = durationInseconds % 60;
        Dbg.sysout("Build took: " + minutes + " minute(s) and " + seconds + " second(s)");
    }

    public static void runTests() {
        BuildDatabase program = new BuildDatabase(null, true);
        Dbg.sysout("Folder: D:/Music/Air. Songs added: " + program.addNewFolder("D:\\Music\\Air"));
        Dbg.sysout("Folder: D:/Music/Arctic Monkeys. Songs added: " + program.addNewFolder("D:\\Music\\Arctic Monkeys"));
        Dbg.sysout("File  : D:/Music/Arcade Fire/Funeral/09 Rebellion (Lies).mp3 . Songs added: " + program.addSongToLibrary("D:\\Music\\Arcade Fire\\Funeral\\09 Rebellion (Lies).mp3"));
        Dbg.sysout("File  : D:/non/existant/song.mp3 . Songs added: " + program.addSongToLibrary("D:\\non\\existant\\song.mp3"));
        Dbg.sysout("Folder: D:/Music/Arctic Monkeys. Songs added: " + program.addNewFolder("D:\\Music\\Arctic Monkeys"));
    }

    public static boolean createDBInHomeFolder() {
        BuildDatabase builder = new BuildDatabase(System.getProperty("user.home") + "/.cava/", true);
        return builder.tablesCreatedSuccefully();
    }

    private class TrackAdder extends Thread {

        private String[] folders;
        private String[] files;
        private boolean recursive;
        private boolean cancelImport;
        private Long browserLastUpdatedAt;

        public TrackAdder(String[] folders, boolean recursive) {
            this.folders = folders;
            this.recursive = recursive;
            cancelImport = false;
            files = null;
            browserLastUpdatedAt = 0L;
        }

        public TrackAdder(String[] songsToAdd) {
            this.folders = null;
            files = songsToAdd;
            cancelImport = false;
            browserLastUpdatedAt = 0L;
        }

        public void cancel() {
            cancelImport = true;
            return;
        }

        @Override
        public void run() {
        	Long startTime = System.currentTimeMillis();
            Progressbar.startWorking(Progressbar.ProgressType.Import);
            Progressbar.setProgressValue(0, Progressbar.ProgressType.Import);
            Progressbar.setCancelListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    cancelTrackImporting();
                    Progressbar.finishedWorking(Progressbar.ProgressType.Import);

                }
            }, Progressbar.ProgressType.Import);
            if (folders != null) {
                for (String folder : folders) {
                    if (cancelImport) {
                        break;
                    }
                    addFolder(folder, recursive);
                }
            } else {
                for (String file : files) {
                    if (cancelImport) {
                        break;
                    }
                    addSongToLibrary(file, true);
                }
            }
            //Make sure tables have been updated before finishing execution
            if (browser != null) {
                Dbg.sysout("Refreshing tables");
                browser.showImportChanges();
            } else {
                Dbg.syserr("Browser is null");
            }
            Progressbar.finishedWorking(Progressbar.ProgressType.Import);
            int duration = (int) Math.floor((System.currentTimeMillis() - startTime)/1000);
            int minutes = (int) Math.floor(duration)/60;
            int seconds = duration - (minutes*60);
            Dbg.sysout("Database build time: " + minutes + " mintes and " + seconds + " seconds");
            startSCMSGeneration();
        }

        private void addFolder(String baseDirectory, boolean recursive) {
            File dir = new File(baseDirectory);
            File[] files;
            files = dir.listFiles();
        	if(files != null && files.length > 0){
	            try {
	                for (File f : files) {
	                    if (cancelImport) {
	                        break;
	                    }
	                    String fullPath = f.getCanonicalPath();
	                    if (f.isFile() && isValidMusicFile(fullPath)) {
	                        addSongToLibrary(fullPath, false);
	                    } else if (f.isDirectory() && recursive == true) {
	                        addFolder(fullPath, recursive);
	                    }
	                }
	            } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
        	}
        }

        private boolean addSongToLibrary(String fullPath, boolean checkFileType) {
            //Update browser tables if necessary
            if (browser != null) {
                //Update every 5 seconds
                if ((System.currentTimeMillis() - browserLastUpdatedAt) > 1000 * 5) {
                    browserLastUpdatedAt = System.currentTimeMillis();
                    browser.showImportChanges();
                }
            }

            if (checkFileType) {
                if (!isValidMusicFile(fullPath)) {
                    return false;
                }
            }
            TagData tagData = tagReader.getTagData(fullPath);
            if (tagData == null) {
                listOfFailedSongs.add(fullPath);
                if (debugging) {
                    Dbg.syserr("Failed to read tag data for song(" + fullPath + ")");
                }
                return false;
            } else {
                if (!addSong(tagData)) {
                    listOfFailedSongs.add(fullPath);
                    if (debugging) {
                        Dbg.syserr("Failed to insert tag data for song(" + fullPath + ")");
                        db.printLastError();
                    }
                    return false;
                } else {
                    return true;
                }
            }
        }

        /**
         * Add the song to the database
         * @param tagData A tagData object specifying all of the required information that has been read
         * @return true if the song was added or ignored, false if it could not be added
         */
        private boolean addSong(TagData tagData) {
            //Check to see if a file with this path already exists
            //If so, ignore.

            //Dont trim the path yet.
            String path = db.escapeString(tagData.getPath(), -1);
            if (path.length() >= 350) {
                if (debugging) {
                    Dbg.syserr("Path name too long for storage: " + path);
                    return false;
                }
            }
            //When creating tables for the first time, skip the path check.
            if (!skipPathCheck) {
                CavaResultSet rs;
                if ((rs = db.select("SELECT COUNT(*) FROM track WHERE path = '" + path + "'")) != null) {
                    if (!rs.fetchRow()) {
                        return false;
                    }
                    int count = rs.fetchInt(1);
                    if (count == -1) {
                        return false;
                    } else if (count > 0) {
                        //song exists. Add to skipped songs and return true
                        listOfIgnoredSongs.add(path);
                        return true;
                    }
                } else {
                    return false;
                }
            }

            //Get artist ID
            String artist = db.escapeString(tagData.getArtistName(), 100);
            String sortByArtist = createSortByArtist(artist);
            int artistID = getArtistID(artist, sortByArtist);
            if (artistID == -1) {
                return false;
            }

            //Get album ID
            String album = db.escapeString(tagData.getAlbumName(), 100);
            int albumID = getAlbumID(album, album.toLowerCase(), artistID);
            if (albumID == -1) {
                return false;
            }

            //can now insert
            //String trackName = db.escapeString(tagData.getTrackName(),100);
            //String bitRate = db.escapeString(tagData.getBitRate(),12);
            //String codec = db.escapeString(tagData.getCodec(),10);
            String trackName = db.checkLength(tagData.getTrackName(), 100);
            String bitRate = db.checkLength(tagData.getBitRate(), 12);
            String codec = db.checkLength(tagData.getCodec(), 10);

            //Don't add SCMS data on insert;
            int scmsAvailable = 0;
            byte[] scms = new byte[1];

            if (!db.addTrack(trackName, trackName.toLowerCase(), albumID, artistID, tagData.getPath(), bitRate, codec, tagData.getTrackLength(), tagData.getTrackNo(), scms, scmsAvailable)) {
                return false;
            } else {
                db.insert("INSERT INTO lyrics (trackid) VALUES (IDENTITY_VAL_LOCAL())");
                //System.out.println("###############  PICKA :"+picka);
                addedSongs++;
                return true;
            }
        }

        /**
         * Get the ID of the artist by name. If the artist does not exist, the artist is added and then
         * the new ID is returned.
         * @param artist the name of the artist
         * @param sortByArtist the sortByArtist field
         * @return the ID of the artist, or -1 on failure
         */
        private int getArtistID(String artist, String sortByArtist) {
            //If there was no data in the db at start of operation, use locally stored tree to find existing IDs
            CavaResultSet rs;
            if (skipPathCheck) {
                Integer idFromTree = artistIDs.get(sortByArtist);
                if (idFromTree != null) {
                    return idFromTree;
                } else {
                    //artist doesn't exist insert and then get ID
                    if (db.insert("INSERT INTO artist (artistname,sortbyname) VALUES('" + artist + "','" + sortByArtist + "')") != -1) {
                        //Return last-inserted ID

                        if ((rs = db.select("SELECT IDENTITY_VAL_LOCAL() FROM artist")) != null) {
                            if (rs.fetchRow()) {
                                int returnedID = rs.fetchInt(1);
                                artistIDs.put(sortByArtist, returnedID);
                                return returnedID;
                            }
                        }
                    } else {
                        //insert failed
                        return -1;
                    }
                }
            } else {
                if ((rs = db.select("SELECT COUNT(*) FROM artist WHERE sortbyname = '" + sortByArtist + "'")) != null) {
                    if (!rs.fetchRow()) {
                        return -1;
                    }
                    if (rs.fetchInt(1) > 0) {
                        //artist exists, so find artistID and return
                        if ((rs = db.select("SELECT artistid FROM artist WHERE sortbyname = '" + sortByArtist + "'")) != null) {
                            if (!rs.fetchRow()) {
                                return -1;
                            }
                            return rs.fetchInt(1);
                        } else {
                            return -1;
                        }
                    } else {
                        //artist doesn't exist insert and then get ID
                        if (db.insert("INSERT INTO artist (artistname,sortbyname) VALUES('" + artist + "','" + sortByArtist + "')") != -1) {
                            //Return last-inserted ID
                            if ((rs = db.select("SELECT IDENTITY_VAL_LOCAL() FROM artist")) != null) {
                                if (rs.fetchRow()) {
                                    return rs.fetchInt(1);
                                }
                            }
                        } else {
                            //insert failed
                            return -1;
                        }
                    }
                }
            }
            return -1;
        }

        /**
         * Get the ID of the album by name. If the album does not exist, the album is added and then
         * the new ID is returned.
         * @param album the name of the artist
         * @param albumAsLowerCase the name of the album in lower case
         * @return the ID of the album, or -1 on failure
         */
        private int getAlbumID(String album, String albumAsLowerCase, int artistID) {
            CavaResultSet rs;
            //If there was no data in the db at start of operation, use locally stored tree to find existing IDs
            if (skipPathCheck) {
                Integer idFromTree = albumStore.get(albumAsLowerCase);
                //Integer idFromTree = albumStore.get(new AlbumStore(albumAsLowerCase,artistID));
                if (idFromTree != null) {
                    return idFromTree;
                } else {
                    //album doesn't exist insert and then get ID
                    if (db.insert("INSERT INTO album (artistid,albumname,loweralbumname) VALUES(" + artistID + ",'" + album + "','" + albumAsLowerCase + "')") != -1) {
                        //Return last-inserted ID
                        if ((rs = db.select("SELECT IDENTITY_VAL_LOCAL() FROM album")) != null) {
                            if (rs.fetchRow()) {
                                int returnedID = rs.fetchInt(1);
                                albumStore.put(albumAsLowerCase, returnedID);
                                //albumStore.put(new AlbumStore(albumAsLowerCase,artistID), returnedID);
                                return returnedID;
                            }
                        }
                    } else {
                        //insert failed
                        return -1;
                    }
                }
            } else {
                if ((rs = db.select("SELECT COUNT(*) FROM album WHERE loweralbumname = '" + albumAsLowerCase + "' AND artistid=" + artistID)) != null) {
                    if (!rs.fetchRow()) {
                        return -1;
                    }
                    if (rs.fetchInt(1) > 0) {
                        //album exists, so find albumID and return
                        if ((rs = db.select("SELECT albumid FROM album WHERE loweralbumname = '" + albumAsLowerCase + "'")) != null) {
                            if (!rs.fetchRow()) {
                                return -1;
                            }
                            return rs.fetchInt(1);
                        } else {
                            return -1;
                        }
                    } else {
                        //album doesn't exist insert and then get ID
                        if (db.insert("INSERT INTO album (artistid,albumname,loweralbumname) VALUES(" + artistID + ",'" + album + "','" + albumAsLowerCase + "')") != -1) {
                            //Return last-inserted ID
                            if ((rs = db.select("SELECT IDENTITY_VAL_LOCAL() FROM album")) != null) {
                                if (rs.fetchRow()) {
                                    return rs.fetchInt(1);
                                }
                            }
                        } else {
                            //insert failed
                            return -1;
                        }
                    }
                }
            }
            return -1;
        }
    }
}
