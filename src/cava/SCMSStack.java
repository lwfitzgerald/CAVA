package cava;

import java.util.Stack;

/**
 * This SCMS stack ensures syncronization, and allows the stack to be regenerated if necessary
 * @author ben
 *
 */
public class SCMSStack{
	
	private volatile static SCMSStack scmsStack = null;
	private volatile Stack<ClientTrack> stack = null;
	private volatile TrackDatabase db;
	private volatile boolean refreshing;
	
	/**
	 * Private constructor to ensure singleton-usage
	 */
	private SCMSStack(){
		db = new TrackDatabase();
		refreshing = false;
	}
	
	/**
	 * Get the stack
	 */
	public static synchronized SCMSStack getInstance(){
		if(scmsStack==null){
			scmsStack = new SCMSStack();
		}
	    return scmsStack;
	}

	/**
	 * Get the next item from the stack
	 */
	public synchronized ClientTrack pop(){
		while(refreshing){
			try {
				Thread.sleep(10);
				Dbg.sysout("Waiting for lock to be relinquished");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return stack.pop();
		
	}
	
	/**
	 * Re-generate the stack by checking the database for tracks without SCMS data.
	 */
	public synchronized void refresh(){
		refreshing=true;
		stack = db.getTracksWithoutSCMSData();
		Dbg.sysout("SCMS Stack now contains: "+stack.size() + " items");
		refreshing=false;
	}
	
	/**
	 * Re-generate the stack by checking the database for tracks without SCMS data.
	 */
	public synchronized int getSize(){
		while(refreshing){
			try {
				Thread.sleep(10);
				Dbg.sysout("Waiting for lock to be relinquished");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return stack.size();
	}
	
	
}