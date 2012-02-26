package cava.spotify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.LineUnavailableException;

import cava.*;
import cava.server.ServerTrack;
import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.JotifyConnection;
import de.felixbruns.jotify.cache.FileCache;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.media.File;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackListener;

public class SpotifyPlayer implements PlaybackListener {
    private Jotify jotify;
    private String username;
    private String password;
    private volatile boolean loggedin;
    
    private NowPlaying nowPlaying;
    private AudioPlayer audioPlayer;
    
    private volatile boolean attempting;
    private volatile cava.Track attemptTrack;
    private volatile int attemptNo;
    
    private volatile boolean doPause;
    private volatile boolean doStop;
    
    private long lastSeekBarUpdate;
    
    private Object sync = new Object();
    
    public SpotifyPlayer(String username, String password, float startVolume, NowPlaying nowPlaying, AudioPlayer audioPlayer) {
        this.username = username;
        this.password = password;
        this.loggedin = false;
        this.nowPlaying = nowPlaying;
        this.audioPlayer = audioPlayer;
        this.attempting = false;
        this.attemptTrack = null;
        this.attemptNo = 1;
        this.doPause = false;
        this.doStop = false;
        this.lastSeekBarUpdate = 0;
    }
    
    private void login() throws ConnectionException, AuthenticationException {
        jotify.login(username, password);
        loggedin = true;
    }
    
    private boolean checkLoggedIn() {
        if(!loggedin) {
            try {
                if(jotify != null) {
                    jotify.close();
                    jotify = null;
                }
                
                jotify = new JotifyConnection(new FileCache(), 5, TimeUnit.SECONDS);
                jotify.volume(Preferences.getVolume());
                login();
                return true;
            } catch (ConnectionException e) {
                Dbg.syserr("Spotify connection error" + e.getMessage());
                return false;
            } catch (AuthenticationException e) {
                return false;
            }
        }
        
        return true;
    }
    
    public void updateDetails(String username, String password) {
        this.username = username;
        this.password = password;
        loggedin = false;
    }
    
    public void play() {
        // Cancel any queued pause / stop
        synchronized(sync) {
            doPause = false;
            doStop = false;
            
            if(!attempting) {
                attempting = true;
                
                // Call play method in a thread (or the loading will hang the player)
                (new PlayAttempt()).start();
            }
        }
    }
    
    private void playOperation() {
        // Stop current playback
        if(jotify != null) {
            jotify.stop();
        }
        
        // Check we're logged in
        if(checkLoggedIn()) {
            attemptTrack = nowPlaying.getCurrentTrack();
            ServerTrack track = (ServerTrack) attemptTrack;
    
            try {
                jotify.play(jotify.browse(new Track("spotify:track:" + track.getSpotifyLink())), File.BITRATE_320, this);
            } catch (TimeoutException e) {
                synchronized(sync) {
                    loggedin = false;
                    attempting = false;
                    Dbg.syserr("Spotify playback of \""
                            + ((ServerTrack) nowPlaying.getCurrentTrack()).getArtistName() + " - "
                            + nowPlaying.getCurrentTrack().getTrackName()
                            + "\" failed due to timeout");
                    if(attemptNo == 1) {
                        attemptNo++;
                        (new PlayAttempt()).start();
                    } else {
                        attemptNo = 1;
                        audioPlayer.DoAudioAction(AudioAction.SkipForward);
                    }
                }
            } catch (IOException e) {
                synchronized(sync) {
                    loggedin = false;
                    attempting = false;
                    Dbg.syserr("Spotify playback of \""
                            + ((ServerTrack) nowPlaying.getCurrentTrack()).getArtistName() + " - "
                            + nowPlaying.getCurrentTrack().getTrackName()
                            + "\" failed due to IO error");
                    if(attemptNo == 1) {
                        attemptNo++;
                        (new PlayAttempt()).start();
                    } else {
                        attemptNo = 1;
                        audioPlayer.DoAudioAction(AudioAction.SkipForward);
                    }
                }
            } catch (LineUnavailableException e) {}
        } else {
            synchronized(sync) {
                attempting = false;
                audioPlayer.DoAudioAction(AudioAction.SkipForward);
            }
        }
    }
    
    public void pause() {
        synchronized(sync) {
            if(!attempting) {
                if(jotify != null) {
                    jotify.pause();
                }
            } else {
                if(!doStop) {
                    doPause = true;
                }
            }
        }
    }
    
    public void resume() {
        synchronized(sync) {
            if(!attempting) {
                if(jotify != null) {
                    jotify.play();
                }
            } else {
                doPause = false;
            }
        }
    }
    
    public void stop() {
        synchronized(sync) {
            if(!attempting) {
                if(jotify != null) {
                    jotify.stop();
                }
            } else {
                doStop = true;
                doPause = false;
            }
        }
    }
    
    public void setPosition(double value) {
        try {
            if(jotify != null)
            {
                jotify.seek((int) (nowPlaying.getCurrentTrack().getLength() * value) * 1000);
            }
        } catch (IOException e) {}
    }
    
    public void setVolume(float volume) {
        if(jotify != null && !attempting) {
            jotify.volume(volume);
        }
    }
    
    @Override
    public void playbackFinished(Track track) {
        audioPlayer.DoAudioAction(AudioAction.TrackFinished);
    }

    @Override
    public void playbackPosition(Track track, int ms) {
        if(System.currentTimeMillis() - lastSeekBarUpdate > 1000) {
            // Update the seek bar
            audioPlayer.getSeekbar().setDuration(Math.min(1.0, (double) ms / track.getLength()), true, track.getLength() / 1000);
            lastSeekBarUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public void playbackResumed(Track track) {
        // Ignore
    }

    @Override
    public void playbackStarted(Track track) {
        synchronized(sync) {
            attemptNo = 1;
            
            if(attemptTrack != nowPlaying.getCurrentTrack()) {
                if(nowPlaying.getCurrentTrack().isServerTrack()) {
                    // We're not wanting to play the same track so play the new track
                    (new PlayAttempt()).start();
                } else {
                    // Not a server track so ignore
                    jotify.stop();
                    doStop = false;
                    doPause = false;
                    attempting = false;
                }
            } else if(doStop) {
                jotify.stop();
                doStop = false;
                attempting = false;
            } else if(doPause) {
                jotify.pause();
                doPause = false;
                attempting = false;
            } else {
                // We're still wanting to play this track so continue
                attempting = false;
                audioPlayer.DoAudioAction(AudioAction.SpotifyPlaybackStarted);
            }
        }
    }

    @Override
    public void playbackStopped(Track track) {
        audioPlayer.DoAudioAction(AudioAction.TrackFinished);
    }
    
    private class PlayAttempt extends Thread {
        @Override
        public void run() {
            playOperation();
        }
    }
}