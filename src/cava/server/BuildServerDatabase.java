package cava.server;

import cava.*;

public class BuildServerDatabase {
	private Database db;
	private String databaseName = "CAVAserverDB";
	private boolean tablesCreatedSuccessfully;
	
	public BuildServerDatabase() {
		System.setProperty("derby.stream.error.file", System.getProperty("user.home") + "/.cava/derbyServer.log");
		String path = System.getProperty("user.home") + "/.cava";
		db = new Database(path + "/" + databaseName,true);
    	if(!db.isConnected()){
    		db.printLastError();
    	}else{
			tablesCreatedSuccessfully = true;
			BuildTables();
    	}
	}
	
	private void BuildTables(){
		
		if(!db.isConnected()){
			cava.Dbg.syserr("Could not connect to database!");
			return;
		}
		
		String artistTable = "CREATE TABLE artist (artistID INTEGER NOT NULL GENERATED ALWAYS " +
				"AS IDENTITY (START WITH 1, INCREMENT BY 1), artistname VARCHAR (100) NOT NULL)";
		
		String tracksAvailableTable = "CREATE TABLE tracksAvailable (trackID INTEGER NOT NULL GENERATED ALWAYS " +
		"AS IDENTITY (START WITH 1, INCREMENT BY 1), artistID INTEGER NOT NULL,trackname VARCHAR(100) NOT NULL, spotifylink VARCHAR(50), " +
		"scms BLOB(2147483647),failed INTEGER NOT NULL default 0 )";
		
		String tracksUploadedTable = "CREATE TABLE tracksUploaded (trackID INTEGER NOT NULL GENERATED ALWAYS " +
		"AS IDENTITY (START WITH 1, INCREMENT BY 1),trackname VARCHAR(100) NOT NULL,artistname VARCHAR(100) NOT NULL, " +
		"scms BLOB(2147483647), scmsGroup INTEGER NOT NULL)";
		
		String tempTable = "CREATE TABLE temp (id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
				"trackname VARCHAR(100) NOT NULL, artistname VARCHAR(100) NOT NULL, scms BLOB(2147483647) )";
		
		String aliasesTable = "CREATE TABLE aliases (artistname VARCHAR(100) NOT NULL, trackname VARCHAR(100) NOT NULL, tracksUploadedID INTEGER NOT NULL)";
		
		String artistPK = "ALTER TABLE artist ADD CONSTRAINT \"artistPK\" PRIMARY KEY (artistid)";
		String tracksAvailablePK = "ALTER TABLE tracksAvailable ADD CONSTRAINT \"tracksAvailablePK\" PRIMARY KEY (trackID)";
		String tracksUploadedPK = "ALTER TABLE tracksUploaded ADD CONSTRAINT \"tracksUploadedPK\" PRIMARY KEY (trackID)";
		String tempTablePK = "ALTER TABLE temp ADD CONSTRAINT \"tempTablePK\" PRIMARY KEY (id)";
		
		//Indexes on trackname and artist name for the aliases table and tracksUploaded. Also scmsgroup on tracksUploaded
		String aliasArtistIndex = "CREATE INDEX aliasArtistIndex ON aliases(artistname) ";
		String aliasTrackIndex = "CREATE INDEX aliasTrackIndex ON aliases(trackname) ";
		String uploadsArtistIndex = "CREATE INDEX uploadsArtistIndex ON aliases(artistname) ";
		String uploadsTrackIndex = "CREATE INDEX uploadsTrackIndex ON aliases(trackname) ";
		String scmsGroupIndex = "CREATE INDEX scmsGroupIndex ON tracksUploaded(scmsGroup) ";
		
		
		createTable(artistTable,"Could not create artist table");
		createTable(tracksAvailableTable,"Could not create table of tracks avalable for playlist generation");
		createTable(tracksUploadedTable,"Could not create table of newly uploaded tracks");
		createTable(aliasesTable,"Could not create table of aliases");
		createTable(tempTable,"Could not create temporaty table for tracks");
		
		createTable(artistPK,"Could not add primary key to artist table");
		createTable(tracksAvailablePK,"Could not add primary key to tracks available table");
		createTable(tracksUploadedPK,"Could not add primary key to tracks available table");
		createTable(tempTablePK,"Could not add primary key to temp table");
		
		createTable(aliasArtistIndex,"Could not add artist index to aliases table");
		createTable(aliasTrackIndex,"Could not add track index to aliases table");
		createTable(uploadsArtistIndex,"Could not add artist index to uploads table");
		createTable(uploadsTrackIndex,"Could not add track index to uploads table");
		createTable(scmsGroupIndex,"Could not add scms group index to tracks uploaded table");
		
		
		if(tablesCreatedSuccessfully){
			System.out.println("Tables Created Successfully");
		}
		
	}
	
	private void createTable(String tableSQL, String errorMsg){
		if(!db.createTable(tableSQL)){
			System.err.println(errorMsg);
			db.printLastError();
			System.err.println("------------------------------------------");
			tablesCreatedSuccessfully = false;
		}
	}
	
	
	public static void main(String[] args) {
		new BuildServerDatabase();
	}

}
