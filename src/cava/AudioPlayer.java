/*******************************************************************************
 * Copyright (c) 2008, 2010 Xuggle Inc.  All rights reserved.
 *  
 * This file is part of Xuggle-Xuggler-Main.
 *
 * Xuggle-Xuggler-Main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xuggle-Xuggler-Main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Xuggle-Xuggler-Main.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package cava;

import cava.scrobbler.CavaScrobbler;
import cava.spotify.SpotifyPlayer;

/**
 * Class to handle playing audio
 * @author Ben (author of original Xuggler code: aclarke)
 *
 */
public class AudioPlayer {
    private boolean isPaused;
    private TrackDatabase db;
    private NowPlaying nowPlaying;
    private boolean isPlaying;
    private boolean resume;
    private Seekbar seekBar;
    private Volumebar volumeBar;
    private PlayBackThread playBackThread;
    private PauseData pauseData;
    private Browser browser;
    
    private CavaScrobbler scrobbler;
    private SpotifyPlayer spotifyplayer;
    
	private ImageButton playPauseButton;
	private ImageButton muteButton;
	private ErrorMessage errorMessage;

    /**
     * Create a new AudioPlayer. Uses the NowPlaying object to work
     * @param nowPlayingLabel a JLabel to store the current track
     */
    public AudioPlayer(NowPlaying nowPlaying, TrackDatabase db, CavaScrobbler scrobbler) {
        setUpAudioPlayer(nowPlaying, db, scrobbler);
    }

    public void setSeekBar(Seekbar seekBar) {
        this.seekBar = seekBar;
    }
    
    public void setVolumeBar(Volumebar volumeBar) {
	    this.volumeBar = volumeBar;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    private void setUpAudioPlayer(NowPlaying nowPlaying, TrackDatabase db, CavaScrobbler scrobbler) {
        this.db = db;
        this.scrobbler = scrobbler;
        this.spotifyplayer = new SpotifyPlayer(Preferences.getSpotifyUsername(), Preferences.getSpotifyPassword(), Preferences.getVolume(), nowPlaying, this);
        Preferences.setSpotifyPlayer(spotifyplayer);
        isPlaying = false;
        isPaused = false;
        this.nowPlaying = nowPlaying;
        playBackThread = null;
    }
    

    /**
     * Perform an operation on the audio stream - play,pause,resume,stop,skip forward or skip back.
     * This is necessary because of concurrency problems; we cannot have a pause and a skip happening at the same time
     * @param audioAction the action to perform.
     */
    public synchronized void DoAudioAction(AudioAction audioAction) {
        switch (audioAction) {
            case Play:
                if (nowPlaying.validPlayPosition()) {
                	nowPlaying.playBackStarted();
                	
                	if(isPlaying) {
                	    // Scrobble if need be!
                        if(Preferences.getLastfmEnabled()) {
                            scrobbler.playbackFinished();
                        }
                        
                        DoAudioAction(AudioAction.StopPlaybackThread);
                        if(nowPlaying.getCurrentTrack().isClientTrack()) {
                            spotifyplayer.stop();
                        }
                	}
                	
                    // Unset the pause flag
                    resume = false;
                    isPaused = false;
                    isPlaying = false;
                    
                    DoAudioAction(AudioAction.DoPlayBack);
                }
                return;
            case Pause:
                // Stop playback timer to avoid premature scrobbles
                if(Preferences.getLastfmEnabled()) {
                    scrobbler.playbackPaused();
                }
                
                if (playBackThread != null && isPlaying && !isPaused) {
                    pauseData = playBackThread.pause();
                }
                
                spotifyplayer.pause();
                isPaused = true;
                return;
            case Resume:
                if (isPaused && nowPlaying.validPlayPosition()) {
                    if(nowPlaying.getCurrentTrack().isClientTrack()) {
                        resume = true;
                        DoAudioAction(AudioAction.DoPlayBack);
                    } else {
                        spotifyplayer.resume();
                    }
                    isPaused = false;
                    
                    // Resume playback timer
                    if(Preferences.getLastfmEnabled()) {
                        scrobbler.playbackResumed();
                    }
                } else {
                	DoAudioAction(AudioAction.Play);
                }
                return;
            case StopPlaybackThread:
                if(playBackThread != null && isPlaying) {
                    playBackThread.pause();
                }
                return;
            case Stop:
                if (playBackThread != null && playBackThread.isPlaying()) {
                    DoAudioAction(AudioAction.StopPlaybackThread);
                }
                
                spotifyplayer.stop();
                
                // Scrobble if need be!
                if(Preferences.getLastfmEnabled()) {
                    scrobbler.playbackFinished();
                }
                
                nowPlaying.clearTracks();
                isPlaying = false;
                nowPlaying.updateText();
                ListenerFor[] tablesToUpdate = {ListenerFor.Playlist, ListenerFor.PlaylistTrack};
                browser.refreshTables(tablesToUpdate);
                seekBar.setDuration(0, false, 0);
                isPaused = false;
                playPauseButton.setPlayPauseButton(AudioAction.Resume);
                return;
            case SkipForward:
                DoAudioAction(AudioAction.StopPlaybackThread);
                
                nowPlaying.skipForward();
                if(nowPlaying.validPlayPosition()){
                	DoAudioAction(AudioAction.Play);
                }else{
                	//If that's not a valid play position, we've reached the end of the playlist
                	nowPlaying.playBackFinished();
                	spotifyplayer.stop();
                	seekBar.setDuration(0, false, 0);
                    ListenerFor[] tables = {ListenerFor.PlaylistTrack};
                    browser.refreshTables(tables);
                    isPaused = false;
                    isPlaying = false;
                    //Ensure play/pause is now set to resume
                    playPauseButton.setPlayPauseButton(AudioAction.Resume);
                }
                return;
            case SkipBack:
                DoAudioAction(AudioAction.StopPlaybackThread);
                nowPlaying.skipBack();
                DoAudioAction(AudioAction.Play);
                return;
            case TrackFinished:
                // Scrobble if need be!
                if(Preferences.getLastfmEnabled()) {
                    scrobbler.playbackFinished();
                }
                
                DoAudioAction(AudioAction.StopPlaybackThread);
                nowPlaying.skipForward();
                if(nowPlaying.validPlayPosition()){
                	DoAudioAction(AudioAction.Play);
                }else{
                	nowPlaying.playBackFinished();
                	seekBar.setDuration(0, false, 0);
                    ListenerFor[] tables = {ListenerFor.PlaylistTrack};
                    browser.refreshTables(tables);
                    isPaused = false;
                    isPlaying = false;
                    //Ensure play/pause is now set to resume
                    playPauseButton.setPlayPauseButton(AudioAction.Resume);
                }
                return;
            case DoPlayBack:
                Track currentTrack = nowPlaying.getCurrentTrack();
                if(currentTrack.isClientTrack()) {
                    if(((ClientTrack) currentTrack).getPath(db).toLowerCase().endsWith(".flac")) {
                        seekBar.setEnabled(false);
                    } else {
                        seekBar.setEnabled(true);
                    }
                    
                    playBackThread = new PlayBackThread(nowPlaying, db, resume, this);

                    if (resume) {
                        playBackThread.setPauseData(pauseData);
                    }

                    playBackThread.start();

                    while (!playBackThread.isPlaying()) {
                        //Wait for playback to start before returning
                        //Break if the file couldn't be opened and print a message;
                        if (!playBackThread.fileOpened()) {
                        	if(errorMessage==null){
                        		errorMessage = new ErrorMessage("Failed to open file: " + ((ClientTrack)nowPlaying.getCurrentTrack()).getPath(db));
                        	}else{
                        		errorMessage.addErrorMessage("Failed to open file: " + ((ClientTrack)nowPlaying.getCurrentTrack()).getPath(db));
                        	}
                            DoAudioAction(AudioAction.SkipForward);
                            break;
                        }
                    }

                    if (playBackThread.fileOpened()) {
                        //Ensure play/pause button shows play
                        playPauseButton.setPlayPauseButton(AudioAction.Pause);

                        isPlaying = true;
                        nowPlaying.updateText();

                        // Make sure the volume is set properly
                        DoAudioAction(AudioAction.VolumeBarChange); 

                        // Tell scrobbler about current track
                        if(Preferences.getLastfmEnabled()) {
                            scrobbler.playbackStarted(nowPlaying.getCurrentTrack());
                        }

                        //Refresh track and playlist track tables so that currently playing tracks are displayed
                        ListenerFor[] tables = {ListenerFor.PlaylistTrack,ListenerFor.Track};
                        browser.refreshTables(tables);
                    }
                    
                    // This is a quick fix - when the user had muted, then skipped tracks, the mute button would reset to appearing unmuted, even though we were still supposed to be mute
                    double volumecheck = volumeBar.getVolume();

                    if(volumecheck == 0) { 
                        muteButton.switchButtonState();
                    }
                } else {
                    seekBar.setEnabled(true);
                    
                    int trackLength = nowPlaying.getCurrentTrack().getLength();
                    if(trackLength==0){
                        if(errorMessage==null){
                            errorMessage = new ErrorMessage("Could not connect to Spotify to play track: "+nowPlaying.getCurrentTrack().getTrackName());
                        }else{
                            errorMessage.addErrorMessage("Could not connect to Spotify to play track: "+nowPlaying.getCurrentTrack().getTrackName());
                        }
                        DoAudioAction(AudioAction.SkipForward);
                        break;
                	}
                	
                    spotifyplayer.play();
                    
                    seekBar.setDuration(0, true, trackLength);
                    
                    // Ensure play/pause button shows play
                    playPauseButton.setPlayPauseButton(AudioAction.Pause);
                    
                    isPlaying = true;
                    nowPlaying.updateText();
                    
                    // Refresh track and playlist track tables so that currently playing tracks are displayed
                    ListenerFor[] tables = {ListenerFor.PlaylistTrack,ListenerFor.Track};
                    browser.refreshTables(tables);
                }
                
                return;
            case SpotifyPlaybackStarted:                
                // Make sure the volume is set properly
                DoAudioAction(AudioAction.VolumeBarChange);
                
                // Tell scrobbler about current track
                if(Preferences.getLastfmEnabled()) {
                    scrobbler.playbackStarted(nowPlaying.getCurrentTrack());
                }
                return;
            case VolumeBarChange:
                // Change the volume, obviously
                if(volumeBar != null) {
                    volumeBar.sliderMoved();

                    double volume = volumeBar.getVolume();
                    Boolean check = muteButton.getCurrentImage();

                    if(isPlaying) {
                        if(playBackThread != null) {
                            playBackThread.setVolume(volume);
                        }
                        
                        if(nowPlaying.getCurrentTrack().isServerTrack()) {
                            spotifyplayer.setVolume((float) volume);
                        }
                    }

                    Preferences.updateVolumePreference((float) volume);

                    if(volume == 0) { 
                        //System.out.println( "should be mute atm" );
                        if( check == false ) {
                            muteButton.switchButtonState();
                        }
                    }

                }
                return;
            case Mute:
                // Received signal to either mute or unmute
                volumeBar.mute();
                muteButton.switchButtonState();
                return;
            case SeekBarChange:
                if (isPlaying) {
                    double position = seekBar.getPosition();

                    if(nowPlaying.getCurrentTrack().isClientTrack()) {
                        //If we're not paused, changed the position immediately, if we are
                        //then change the position we'll resume to and resume
                        if(!isPaused){
                            playBackThread.setPosition(position);
                        }else{
                            pauseData.setPausePosition(position);
                            DoAudioAction(AudioAction.Resume);
                        }
                    } else {
                        spotifyplayer.setPosition(position);
                    }
                }
                return;
            case CurrentTrackRemoved:
                //Pause
                DoAudioAction(AudioAction.Pause);
                //Unset text and seekbar
                nowPlaying.updateText();
                seekBar.setDuration(0, false, 0);
                //try and play again
            	DoAudioAction(AudioAction.Play);
            	return;
            	
        }
    }
    
    public Seekbar getSeekbar() {
        return seekBar;
    }

    public Volumebar getVolumebar() {
        return volumeBar;
    }
    
    public boolean isPlaying(){
    	return this.isPlaying;
    }
    
    public boolean isPaused(){
    	return this.isPaused;
    }

	public void setPlayButton(ImageButton playButton) {
		this.playPauseButton = playButton;
	}
	
	public void setMuteButton(ImageButton mButton) {
		this.muteButton = mButton;
	}
}
