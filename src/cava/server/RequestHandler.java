package cava.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import cava.miraje.Mir;
import cava.server.Request.RequestType;

public class RequestHandler extends Thread {
    private Socket socket;
    private ServerDatabase db;
    private CavaServer server;
    private Request request;
    
    public RequestHandler(Socket s, ServerDatabase db, CavaServer server) {
        this.socket = s;
        this.db = db;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            InputStream stream = socket.getInputStream();
            ObjectInputStream in = new ObjectInputStream(stream);
            
            request = (Request) in.readObject();
            
            if(request.getType() == RequestType.SUBMIT) {
                handleSubmit();
            } else {
                handleGetSimilar();
            }
            socket.close();
        } catch (Exception e) {
            System.err.println("Failed to read request from stream");
            e.printStackTrace();
        }
    }
    
    private void handleSubmit() {
	System.out.println("Receiving tracks");
        db.addTracksToTempTable(request.getTracks());
        
        // Tell the processing thread to start processing!
        server.setProcess(true);
    }
    
    private void handleGetSimilar() {
        Response response = new Response(Mir.serverSimilarTracks(request.getTracks(), db, 25, 0.05f));
        
        try {
            OutputStream stream = socket.getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(stream);
            
            out.writeObject(response);
            out.flush();
            out.close();
        } catch (Exception e) {
            System.err.println("Failed to write response to stream");
        }
    }
}
