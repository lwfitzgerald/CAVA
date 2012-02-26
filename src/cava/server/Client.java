package cava.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import cava.Preferences;
import cava.TrackDatabase;

public class Client {
    private final int port;
    private final TrackDatabase db;
    private static SCMSUploader scmsUploader;
    
    public Client(int port, TrackDatabase db) {
        this.port = port;
        this.db = db;
    }
    
    public boolean submitTracks(ServerTrack[] tracks) {
        try {
            Socket s = new Socket("inflatablegoldfish.com", port);
            OutputStream stream = s.getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(stream);
            
            // Create the request object
            Request request = new Request(Request.RequestType.SUBMIT, tracks);
            
            // Send the request object
            out.writeObject(request);
            out.flush();
            out.close();
        } catch (Exception e) {
            System.err.println("Error communicating with remote server");
            return false;
        }
        
        return true;
    }
    
    public void submitTracksFromDB() {
    	if(Preferences.getScmsPreference() && (scmsUploader==null || !scmsUploader.isAlive())){
    		(new SCMSUploader()).start();
    	}
    }
    
    public static void cancelUploading(){
    	if(scmsUploader!=null){
    		scmsUploader.cancel();
    	}    	
    }
    
    public ServerTrack[] similarTracks(ServerTrack[] seeds) {
        Response response = null;
        
        try {
            Socket s = new Socket("inflatablegoldfish.com", port);
            OutputStream ostream = s.getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(ostream);
            
            // Create the request object
            Request request = new Request(Request.RequestType.GETSIMILAR, seeds);
            
            // Send the request object
            out.writeObject(request);
            out.flush();
            
            InputStream istream = s.getInputStream();
            ObjectInputStream in = new ObjectInputStream(istream);
            
            // Read the response from the server
            response = (Response) in.readObject();
            in.close();
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Error communicating with remote server");
            return null;
        }
        
        return response.getTracks();
    }
    
    private class SCMSUploader extends Thread{
    	
    	private boolean cancel;
    	
    	public void cancel(){
    		cancel = true;
    	}
    	
    	@Override
    	public void run() {
            ServerTrack[] tracks;
            cancel = false;
            while(!cancel && ((tracks = db.getTracksToUpload()) != null)) {
                if(submitTracks(tracks)) {
                    db.markAsUploaded(tracks);
                } else {
                    return;
                }
            }
    	}
    }
}
