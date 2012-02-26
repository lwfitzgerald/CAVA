package cava;

import cava.miraje.Mir;
import cava.server.Client;
import cava.server.ServerTrack;

public class PlaylistGenerator {
    public static void generateSmart(ClientTrack[] selectedTracks, TrackDatabase db, PlaylistTableModel playlistTableModel) {
        (new DoOperation(Operation.generateSmart, selectedTracks, db, playlistTableModel)).start();
    }
    
    private static void generateSmartOperation(ClientTrack[] selectedTracks, TrackDatabase db, PlaylistTableModel playlistTableModel) {
        boolean someSCMSDataAvailable = false;
        for(int i=0; i < selectedTracks.length; i++) {
            if(selectedTracks[i].getScms(db) != null) {
                someSCMSDataAvailable = true;
            }else{
                selectedTracks[i] = null;
            }
        }
        if(someSCMSDataAvailable){
            int[] similarTracks = Mir.similarTracks(selectedTracks, db, 25);
            if(similarTracks==null || similarTracks.length==0){
            	new ErrorMessage("No other tracks with playlist data could be found.");
            }else{
	            playlistTableModel.createNewPlaylist(similarTracks,"New Smart Playlist");
	            playlistTableModel.fireTableDataChanged();
            }
        }else{
            new ErrorMessage("No data is available for any of the selected tracks");
        }
        
        // Unset SCMS data in tracks
        for(int i=0; i < selectedTracks.length; i++) {
            if(selectedTracks[i] != null) {
                selectedTracks[i].unsetScms();
            }
        }
    }
    
    public static void generateSpotify(ClientTrack[] selectedTracks, TrackDatabase db, PlaylistTableModel playlistTableModel) {
        (new DoOperation(Operation.generateSpotify, selectedTracks, db, playlistTableModel)).start();
    }
    
    private static void generateSpotifyOperation(ClientTrack[] selectedTracks, TrackDatabase db, PlaylistTableModel playlistTableModel) {
        Client client = new Client(5000, db);
        ServerTrack[] seeds = new ServerTrack[selectedTracks.length];
        
        boolean someSCMSDataAvailable = false;
        for(int i=0; i < selectedTracks.length; i++){
            if(selectedTracks[i].getScms(db) != null){
                someSCMSDataAvailable = true;
                seeds[i] = new ServerTrack(i,selectedTracks[i].getTrackName(),selectedTracks[i].getArtistName(db),selectedTracks[i].getScms());
            }else{
                seeds[i] = null;
            }
        }
        if(someSCMSDataAvailable){
            try{
                db.createNewSpotifyPlaylist(client.similarTracks(seeds));
            }catch(Exception e){
                new ErrorMessage("Error in connection to server. Could not generate Spotify playlist");
            }
            playlistTableModel.setPlaylists();
            playlistTableModel.fireTableDataChanged();
        }else{
            new ErrorMessage("No SCMS data is available for any of the selected tracks");
        }
        
        // Unset SCMS data in tracks
        for(int i=0; i < selectedTracks.length; i++) {
            if(selectedTracks[i] != null) {
                selectedTracks[i].unsetScms();
            }
        }
    }
    
    private static enum Operation { generateSmart, generateSpotify };
    
    private static class DoOperation extends Thread {
        private Operation operation;
        
        private ClientTrack[] selectedTracks;
        private TrackDatabase db;
        private PlaylistTableModel playlistTableModel;
        
        public DoOperation(Operation operation, ClientTrack[] selectedTracks, TrackDatabase db, PlaylistTableModel playlistTableModel) {
            this.setPriority(MIN_PRIORITY);
            this.operation = operation;
            
            this.selectedTracks = selectedTracks;
            this.db = db;
            this.playlistTableModel = playlistTableModel;
        }
        
        public void run() {
            if(operation == Operation.generateSmart) {
                generateSmartOperation(selectedTracks, db, playlistTableModel);
            } else {
                generateSpotifyOperation(selectedTracks, db, playlistTableModel);
            }
        }
    }
}
