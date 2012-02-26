package cava;

public enum AudioAction {
	Play,Pause,SkipForward,SkipBack,TrackFinished,Stop,DoPlayBack,Resume,SeekBarChange,VolumeBarChange,Mute,CurrentTrackRemoved,StopPlaybackThread,SpotifyPlaybackStarted;
	
	public AudioAction SwapPlayPause(){
		if(this==Pause){
			return Resume;
		}else if(this==Resume){
			return Pause;
		}else{
			return this;
		}
	}
}
