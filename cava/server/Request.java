package cava.server;

import java.io.Serializable;

/**
 * Object containing request data sent to CavaServer
 */
public class Request implements Serializable {
    /**
     * Unique ID for serializing
     */
    private static final long serialVersionUID = -5275275243492108887L;

    /**
     * Enum to describe type of request
     */
    public enum RequestType { SUBMIT, GETSIMILAR }
    
    /**
     * Type of request, RequestType.SUBMIT or RequestType.GETSIMILAR
     */
    private final RequestType requesttype;
    
    /**
     * Tracks contained in request
     */
    private final ServerTrack[] tracks;
    
    /**
     * Creates a Request object with specified type and data
     * @param requesttype Request type, RequestType.SUBMIT or RequestType.GETSIMILAR
     * @param tracks Tracks to send in request
     */
    public Request(RequestType requesttype, ServerTrack[] tracks) {
        this.requesttype = requesttype;
        this.tracks = tracks;
    }
    
    /**
     * Get the request type
     * @return Request type, RequestType.SUBMIT or RequestType.GETSIMILAR
     */
    public RequestType getType() {
        return requesttype;
    }
    
    /**
     * Get the tracks stored in the object
     * @return Tracks
     */
    public ServerTrack[] getTracks() {
        return tracks;
    }
}