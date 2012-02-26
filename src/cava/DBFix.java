package cava;

public class DBFix {
	public static void main(String args[]){
		Database db = new Database(System.getProperty("user.home") + "/.cava/trackDB");
		
		if(!db.createTable("ALTER TABLE lyrics ADD COLUMN lyricsUnavailable SMALLINT NOT NULL DEFAULT 0")){
			Dbg.syserr("Failed add lyricsUnavailable column");
			db.printLastError();
			Dbg.syserr("------------------------------------------");
		}
	}
}
