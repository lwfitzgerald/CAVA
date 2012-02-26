package cava.server;

import cava.Database;
import cava.TrackDatabase;

public class UploadSCMS {
	
	
	public static void main(String args[]){
		if(args.length==1){
			//unset upload flag
			Database db = new Database(System.getProperty("user.home") + "/.cava/trackDB");
			db.update("UPDATE track SET scmsUploaded=0");
		}
		
		Client client =new Client(5000, new TrackDatabase());
		client.submitTracksFromDB();
		
	}
}
