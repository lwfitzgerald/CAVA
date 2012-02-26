package cava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EmptyStackException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cava.miraje.Mir;
import cava.miraje.MirAnalysisImpossibleException;
import cava.miraje.Scms;
import cava.server.Client;

public class SCMSGenerator extends Thread{

	private static SCMSGenerator scmsGenerator = null;
	private SCMSStack stack = null;
	private Long startTime;
	private int maxProcs = 8;
	private int numProcs;
	private ExecutorService executorService;
	private SCMSAnalyser[] analysers = null;
	private volatile boolean shuttingDown = false;
	private int numTracksAnalysed;
	private TrackDatabase db;
	private int totalTracks;
	private boolean showingProgress = false;

	private SCMSGenerator(){
		db = new TrackDatabase();
		startTime = System.currentTimeMillis();
		int processorsAvailable = Runtime.getRuntime().availableProcessors();
		numProcs = (processorsAvailable < 1) ? 1 : Math.min(maxProcs, processorsAvailable);
		numTracksAnalysed = 0;
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * Generate SCMS data for tracks that don't have it
	 * @return true if some tracks are now having SCMS data generated.
	 */
	public static synchronized boolean generateSCMS(){
		if(Constants.MIRAJEAVAILABLE){
			try{
				new Mir();
			}catch(UnsatisfiedLinkError e){
				return false;
			}
			if(scmsGenerator==null){
				scmsGenerator = new SCMSGenerator();
			}else if(scmsGenerator.shuttingDown){
				while(scmsGenerator!=null){
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				scmsGenerator = new SCMSGenerator();
			}
			int numTracks = scmsGenerator.startGeneration();
			if(numTracks > 0) {
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	public static void stopGeneration(){
		if(scmsGenerator!=null){
			scmsGenerator.terminate();
			while(scmsGenerator!=null){
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void terminate(){
		if(shuttingDown){
			return;
		}
		shuttingDown = true;
		if(analysers !=null){
			//Send termination signal
			for(int i=0;i<numProcs;i++){
				analysers[i].terminateAnalyser();
			}
			//Wait for all to finish
			for(int i=0;i<numProcs;i++){
				while(analysers[i].finished()!=true){
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
			executorService.shutdown();
			while(!executorService.isTerminated()){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			Long totalSeconds = (System.currentTimeMillis() - startTime)/1000;
			String totalTime;
			if(totalSeconds > 60){
				Long totalMinutes = totalSeconds/60;
				totalSeconds = totalSeconds - totalMinutes *60;
				totalTime = ""+totalMinutes+" minute(s) and + "+totalSeconds+" second(s)";
			}else{
				totalTime = ""+totalSeconds+" second(s)";
			}
			Dbg.sysout("SCMS generation for "+numTracksAnalysed+ " tracks using "+numProcs+" threads took "+totalTime);
		}
		
		if(showingProgress){
			Progressbar.finishedWorking(Progressbar.ProgressType.SCMS);
		}

		//If some tracks have now had SCMS data generated, upload the data
		if(numTracksAnalysed > 0){
			Client client =new Client(5000, new TrackDatabase());
			client.submitTracksFromDB();
		}

		analysers = null;
		stack=null;
		shuttingDown = false;
		scmsGenerator = null;
	}

	private int startGeneration(){
		if(stack==null){
			stack = SCMSStack.getInstance();
			stack.refresh();
			totalTracks = stack.getSize();
			if(totalTracks > 0){
				scmsGenerator.start();
			}else{
				scmsGenerator.terminate();
			}
		}else{
			stack.refresh();
			totalTracks = stack.getSize();
		}
		return totalTracks;
	}

	@Override
	public void run() {
		//Create the thread-pool
		executorService = Executors.newFixedThreadPool(numProcs);
		analysers = new SCMSAnalyser[numProcs];
		for(int i=0;i<numProcs;i++){
			analysers[i] = new SCMSAnalyser(i+1);
			executorService.execute(analysers[i]);
		}
		if(totalTracks > 2){
			showingProgress = true;
			Progressbar.startWorking(Progressbar.ProgressType.SCMS);
			Progressbar.setCancelListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Progressbar.finishedWorking(Progressbar.ProgressType.SCMS);
					stopGeneration();

				}

			}, Progressbar.ProgressType.SCMS);
		}
		
		while(!shuttingDown && stack.getSize() > 0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		terminate();

	}

	public static void main(String[] args) {
		SCMSGenerator.generateSCMS();
	}

	private class SCMSAnalyser extends Thread {

		private int threadNo;
		private boolean stop;
		public boolean finished;

		private Mir mir;

		SCMSAnalyser(int threadNo){
			if(Constants.DEBUG){
				//System.out.println("Starting up thread number: " + threadNo);
			}

			this.threadNo = threadNo;
			stop = false;
			finished = false;
			mir = new Mir();
			this.setPriority(Thread.MIN_PRIORITY);

		}

		public void terminateAnalyser() {
			if(Constants.DEBUG){
				//System.out.println("Shutting down thread number: " + threadNo);
			}
			this.stop = true;
		}

		public boolean finished(){
			return finished;
		}

		@Override
		public void run() {
			while(!stop){
				if(this.threadNo==1 && showingProgress){
					Progressbar.setProgressValue((int) Math.round((((double)numTracksAnalysed)/((double) totalTracks))*100), Progressbar.ProgressType.SCMS);
				}
				try{
					ClientTrack t = stack.pop();
					try {						
						Scms scms = mir.analyse(t.getPath(db));
						db.insertSCMS(t.getTrackID(), scms.toBytes());
						numTracksAnalysed++;
						//System.out.println("Finished anaylsis in SCMSGenerator.java for trackID:" + t.getTrackID());
					} catch (MirAnalysisImpossibleException e) {
						Dbg.syserr("Could not generate SCMS data for: " + t.getPath(db));
					}

				}catch (EmptyStackException e){
					Dbg.syserr("Stack empty. Breaking out");
					break;
				} catch(Exception e){
					e.printStackTrace();
				}
			}
			finished = true;
		}

	}	

}
