package cava.server;

/**
 * Provides a mapping of special group codes to their meanings
 * If a track's been moved to the available table, group = -1
 * If an attempt to move a track was made, but the spotify check wasn't possible, group = -2
 * @author Ben
 *
 */
public enum SCMSGroupCodes {
	madeAvailable,awaitingSpotifyCheck;
	
	public int getIntValue(){
		if(this==madeAvailable){
			return -1;
		}else{
			return -2;
		}
	}
	
	public static SCMSGroupCodes getEnumFromInt(int value){ 
		if(value==-1){
			return madeAvailable;
		}else{
			return awaitingSpotifyCheck;
		}
	}
}
