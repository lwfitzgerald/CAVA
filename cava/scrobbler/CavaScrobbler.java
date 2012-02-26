package cava.scrobbler;

import java.io.*;

import cava.Dbg;
import cava.Track;
import cava.TrackDatabase;

import net.roarsoftware.lastfm.Authenticator;
import net.roarsoftware.lastfm.CallException;
import net.roarsoftware.lastfm.Caller;
import net.roarsoftware.lastfm.scrobble.ResponseStatus;
import net.roarsoftware.lastfm.scrobble.Scrobbler;
import net.roarsoftware.lastfm.scrobble.Source;
import net.roarsoftware.lastfm.scrobble.SubmissionData;
import net.roarsoftware.lastfm.cache.FileSystemCache;

/**
 * Scrobbles to Last.fm
 */
public class CavaScrobbler {
    /**
     * Last.fm username
     */
    private volatile String username;
    
    /**
     * Last.fm password
     */
    private volatile String password;
    
    /**
     * Track Database
     */
    private TrackDatabase db;
    
    /**
     * Scrobbling library object
     */
    private Scrobbler scrobbler;
    
    /**
     * Scrobble cache
     */
    private FileSystemCache cache;
    
    /**
     * If true, the last attempt to initialise failed due to incorrect user
     * details
     */
    private volatile boolean detailfail; 
    
    /**
     * Whether the scrobbler has been initialised yet
     */
    private volatile boolean initialised;
    
    /**
     * Whether the current track has been scrobbled yet
     */
    private volatile boolean trackscrobbled;
    
    /**
     * The track currently being played
     */
    private Track track;
    
    /**
     * Artist name of track currently being played
     */
    private String trackartist;
    
    /**
     * The time when the track currently being played started
     */
    private long starttime;
    
    /**
     * Whether playback is currently paused
     */
    private boolean paused;
    
    /**
     * The time when playback was paused
     */
    private long pausestart; // When the track was paused
    
    /**
     * How long playback of the current track has been paused (cumulative)
     */
    private long pausetime; // How long it was paused for
    
    /**
     * Set when we submit our first of 5 requests within a second
     */
    private volatile long timerstart;
    
    /**
     * Stores the number of requests in the last second
     */
    private volatile int withinsecond;
    
    /**
     * Sync for the playbackFinished method
     */
    private Object playbackFinishedSync = new Object();
    
    /**
     * Sync for the checkInitialisedmethod
     */
    private Object checkInitialisedSync = new Object();
    
    /**
     * Sync for the submitCached method
     */
    private Object submitCachedSync = new Object();
    
    /**
     * Last.fm API key
     */
    private static final String apikey = "f8e9c88e07faa964ae99a68028e6a7b6";
    
    /**
     * Last.fm API secret
     */
    private static final String secret = "b9a26d17a42a57ddad48328631870b74";
    
    /**
     * Creates a new CavaScrobbler
     * 
     * @param username
     *            Last.fm Username
     * @param password
     *            Last.fm Password
     * @param db
     *            Database to retrieve track information from
     */
    public CavaScrobbler(String username, String password, TrackDatabase db) {
        this.username = username;
        this.password = password;
        this.db = db;
        this.track = null;
        this.initialised = false;
        this.detailfail = false;
        this.timerstart = 0;
        this.withinsecond = 0;
        
        scrobbler = Scrobbler.newScrobbler("tst", "1.0", username);
        
        // Set the correct cache
        Caller.getInstance().setCache((cache = new FileSystemCache(new File(System.getProperty("user.home") + "/.cava"))));
    }
    
    /**
     * Connects to Last.fm and initialises a new session
     * 
     * @throws AuthException
     *             Thrown when authenticating using given username and password
     *             fails
     * @throws IOException
     *             Thrown when there is an IO error during initialisation of the
     *             session
     */
    private void initSession() throws AuthException, IOException {
        obeyRequestLimit();
        
        try {
            net.roarsoftware.lastfm.Session session = Authenticator.getMobileSession(username, password, apikey, secret);
            
            if(session == null || !scrobbler.handshake(session).ok()) {
                detailfail = true;
                throw new AuthException();
            } else {
                initialised = true;
            }
        } catch (CallException e) {
            throw new IOException();
        }
    }
    
    /**
     * Check if scrobbler is initialised and if not, initialises it
     * 
     * @return Whether initialisation was successful
     */
    private boolean checkInitialised() {
        synchronized(checkInitialisedSync) {
            if(detailfail) {
                return false;
            }
            
            if(!initialised) {
                try {
                    initSession();
                } catch (Exception e) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    /**
     * Sleep the thread if required to obey the five requests per second rule
     */
    private void obeyRequestLimit() {
        if(withinsecond == 0 || System.currentTimeMillis() - timerstart >= 1000) {
            // No requests in last second
            timerstart = System.currentTimeMillis();
            withinsecond = 1;
        } else {
            // There have been requests in the last second
            if(withinsecond == 5) {
                // Wait 1 second
                Dbg.sysout("CavaScrobbler: 5 Requests in last second, sleeping for 1 second");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}

                timerstart = System.currentTimeMillis();
                withinsecond = 1;
            } else {
                withinsecond++;
            }
        }
    }
    
    /**
     * Updates the username and password in use and reinitialises the session
     * 
     * @param username
     *            Last.fm username
     * @param password
     *            Last.fm password
     */
    public void updateDetails(String username, String password) {
        Dbg.sysout("CavaScrobbler: Updating details");
        this.username = username;
        this.password = password;
        this.detailfail = false;
        this.initialised = false;
    }
    
    /**
     * Tell the Scrobbler that the player has started playing a track (This sets
     * your Last.fm "Now Playing" but does not scrobble)
     * 
     * @param track
     *            Track currently being played
     */
    public void playbackStarted(Track track) {
        /* Set scrobble status and time. */
        this.track = track;
        trackscrobbled = false;
        trackartist = track.getArtistName(db);
        paused = false;
        starttime = System.currentTimeMillis();
        pausetime = 0;
        
        /* Set now playing. */
        Dbg.sysout("CavaScrobbler: Starting submit \"Now Playing\" thread");
        (new ThreadedOperation(Operation.submitNowPlaying, track, trackartist)).start();
    }
    
    /**
     * Call this when a track is paused
     */
    public void playbackPaused() {
        paused = true;
        pausestart = System.currentTimeMillis();
    }
    
    /**
     * Call this when a paused track is resumed
     */
    public void playbackResumed() {
        paused = false;
        pausetime += System.currentTimeMillis() - pausestart;
    }
    
    /**
     * Call this when a track is stopped or finishes
     */
    public void playbackFinished() {
        synchronized(playbackFinishedSync) {
            // If we're paused, set how long it's lasted before submitting
            if(paused) {
                playbackResumed();
            }
            
            if(scrobbleCheck()) {
                trackscrobbled = true;
                // If we need to, scrobble
                Dbg.sysout("CavaScrobbler: Starting submit thread");
                (new ThreadedOperation(Operation.submit, track, trackartist, starttime)).start();
            }
        }
    }
    
    /**
     * Returns whether or not the current track should be scrobbled
     * 
     * @return Whether the current track should be scrobbler
     */
    private boolean scrobbleCheck() {
        if(trackscrobbled) {
            return false;
        }
        
        if(track == null) {
            return false;
        }
        
        if(track.getLength() < 30) {
            return false;
        }
        
        /* Don't submit if track was played for less than 240 seconds or half it's length. */
        int playedfor = (int) (System.currentTimeMillis() - starttime - pausetime) / 1000;
        
        if(playedfor < 240 && playedfor < track.getLength() / 2) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Submits the currently playing track for "Now Playing"
     * 
     * @return Whether track was submitted as currently playing
     */
    private void submitNowPlaying(Track track, String trackartist) {
        submitNowPlaying(track, trackartist, 1);
    }
    
    /**
     * Submits the currently playing track for "Now Playing"
     * 
     * @param attempt
     *            Submission attempt number
     * @return Whether track was submitted as currently playing
     */
    private void submitNowPlaying(Track track, String trackartist, int attempt) {
        if(checkInitialised()) {
            obeyRequestLimit();
            
            try {
                ResponseStatus result = scrobbler.nowPlaying(
                    trackartist, track.getTrackName(), null,
                    track.getLength(), -1
                );
                
                if(attempt == 1 && result.getStatus() == ResponseStatus.BADSESSION) {
                    initialised = false;
                    submitNowPlaying(track, trackartist, 2);
                    
                    return;
                }
            }
            catch(IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Submits the currently playing track if appropriate
     * @return Whether track was submitted
     */
    private void submit(Track track, String trackartist, long starttime) {
        submit(track, trackartist, starttime, 1);
    }
    
    /**
     * Submits the currently playing track
     * 
     * @param attempt
     *            Submission attempt number
     * 
     * @return Whether track was submitted
     */
    private void submit(Track track, String trackartist, long starttime, int attempt) {
        if(checkInitialised()) {
            obeyRequestLimit();
            
            try {
                ResponseStatus result = scrobbler.submit(
                    trackartist, track.getTrackName(), null,
                    track.getLength(), -1, Source.USER, starttime / 1000
                );
                
                if(result.ok()) {
                    // Try to submit cached tracks
                    submitCached();
                    
                    return;
                }
                
                if(attempt == 1 && result.getStatus() == ResponseStatus.BADSESSION) {
                    // Try to initialise the session and resubmit
                    initialised = false;
                    submit(track, trackartist, starttime, 2);
                    
                    return;
                }
                
                cacheScrobble(track, trackartist, starttime);
            }
            catch(IOException e) {
                // Ignore
            }
        } else {
            cacheScrobble(track, trackartist, starttime);
        }
    }
    
    /**
     * Caches the scrobble of a track
     * 
     * @param track
     *            Track
     * @param trackartist
     *            Track artist
     * @param starttime
     *            Time at start of playback
     */
    private void cacheScrobble(Track track, String trackartist, long starttime) {
        cache.cacheScrobble(new SubmissionData(trackartist, track.getTrackName(), null, track.getLength(), -1, Source.USER, starttime / 1000));
    }
    
    /**
     * Submits cached scrobbles
     */
    private void submitCached() {
        synchronized(submitCachedSync) {
            obeyRequestLimit();
            
            try {
                cache.scrobble(scrobbler);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    private enum Operation { submit, submitNowPlaying }
    
    private class ThreadedOperation extends Thread {
        private final Operation op;
        private final Track track;
        private final String trackartist;
        
        private final long starttime;
        
        public ThreadedOperation(Operation op, Track track, String trackartist) {
            this.setPriority(MIN_PRIORITY);
            this.op = op;
            
            this.track = track;
            this.trackartist = trackartist;
            
            this.starttime = 0;
        }
        
        public ThreadedOperation(Operation op, Track track, String trackartist, long starttime) {
            this.setPriority(MIN_PRIORITY);
            this.op = op;
            
            this.track = track;
            this.trackartist = trackartist;
            
            this.starttime = starttime;
        }
        
        @Override
        public void run() {
            switch(op) {
                case submit:
                    submit(track, trackartist, starttime);
                    Dbg.sysout("CavaScrobbler: Submit thread finished");
                    break;
                case submitNowPlaying:
                    submitNowPlaying(track, trackartist);
                    Dbg.sysout("CavaScrobbler: Submit \"Now Playing\" thread finished");
            }
        }
    }
}
