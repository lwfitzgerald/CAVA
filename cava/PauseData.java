package cava;

public class PauseData {
	private long pausePTS;
	private int pausedStreamID;
	private double totalPTS;
	
	public PauseData(long pausePTS, int pausedStreamID, double totalPTS) {
		this.pausePTS = pausePTS;
		this.pausedStreamID = pausedStreamID;
		this.totalPTS = totalPTS;
	}
	
	public long getPausePosition(){
		return this.pausePTS;
	}
	
	public int getPausedStreamID(){
		return this.pausedStreamID;
	}
	
	public void setPausePosition(double pausePosition){
		if(pausePosition < 0 || pausePosition > 1){
			Dbg.syserr("Invalid pause position");
			return;
		}
		pausePTS = Math.round(totalPTS * pausePosition);
	}
}
