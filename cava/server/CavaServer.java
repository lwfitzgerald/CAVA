package cava.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CavaServer {
    private final ServerDatabase db;
    private boolean process;
    private RequestMaster reqMaster;
    private Processor processor;

    public CavaServer(int port) {
        this.db = new ServerDatabase();
        this.process = (db.getFirstNewTrack()==null && db.getTracksAwaitingSpotifyCheck()==null) ? false : true;
        
        reqMaster = new RequestMaster(port, db, this);
        processor = new Processor(db, this);
        
        // Start the threads
        reqMaster.start();
        processor.start();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String input;
        try {
			while((input=stdin.readLine()) != null){
				if(input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")){
					killThreads();
					return;
				}
			}
			System.out.println("Finished reading input");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void killThreads(){
    	reqMaster.quit();
    	processor.quit();
    }
    
    
    public boolean getProcess() {
        return process;
    }
    
    public synchronized void setProcess(boolean doprocessing) {
        process = doprocessing;
    }
    
    public static void main(String[] args) {
        new CavaServer(5000);
    }
}
