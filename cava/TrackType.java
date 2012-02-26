package cava;

public enum TrackType{
	Local,Spotify;

	public String getPlaylistTracksField(){
		if(this==Local){
			return "trackid";
		}else{
			return "spotifyid";
		}
	}
}