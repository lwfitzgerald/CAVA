package cava.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RequestMaster extends Thread {
    private int port;
    private ServerDatabase db;
    private CavaServer server;
    private boolean quit;
    private ServerSocket ss;
    private Socket s;
    
    public RequestMaster(int port, ServerDatabase db, CavaServer server) {
        this.port = port;
        this.server = server;
        this.db = db;
    }
    
    @Override
    public void run() {
        quit = false;
        
        ss = null;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Failed to create server socket on port " + port);
            System.exit(1);
        }
        
        while(!quit) {
            try {
                // Accept connection and start a thread to handle the request
                s = ss.accept();
                (new RequestHandler(s, db, server)).start();
            } catch (IOException e) {
                if(!quit) {
                    System.err.println("Failed to receive socket connection");
                }
            }
        }
    }
    
    /**
     * Call to stop the Request Master
     */
    public void quit() {
        quit = true;
    	try {
		ss.close();
		} catch (IOException e) {
			// Ignore as quitting
		}
    }
}
