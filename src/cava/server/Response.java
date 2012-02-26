package cava.server;

import java.io.Serializable;

public class Response implements Serializable {
    /**
     * Unique ID for serializing
     */
    private static final long serialVersionUID = 9053717557714613042L;
    
    /**
     * Tracks contained in request
     */
    private final ServerTrack[] tracks;
    
    /**
     * Creates a Response object containing the specified tracks
     * @param tracks Tracks to send in request
     */
    public Response(ServerTrack[] tracks) {
        this.tracks = tracks;
    }
    
    /**
     * Get the tracks stored in the object
     * @return Tracks
     */
    public ServerTrack[] getTracks() {
        return tracks;
    }
}