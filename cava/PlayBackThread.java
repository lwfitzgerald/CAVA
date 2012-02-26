package cava;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

/**
 * This Class actually plays the track in a separate thread.
 * @author Ben (original Xuggler author: aclarke)
 *
 */
public class PlayBackThread extends Thread {

	private boolean resume;
	private TrackDatabase db;
	private NowPlaying nowPlaying;
	private int pausedStreamID;
	private long pausePosition;
	private boolean isPlaying;
	private boolean pauseFlag;
	private SourceDataLine mLine;
	private FloatControl volumecontrol;
	private AudioPlayer audioPlayer;
	private boolean fileOpened;
	
	private double totalPTS;
	private Long lastUpdate;
	private boolean seek;
	private Object seekSync = new Object();
	private int lengthOfCurrentTrack;
	private double gain;

	public PlayBackThread(NowPlaying nowPlaying, TrackDatabase db, boolean resume, AudioPlayer audioPlayer) {
	    this.setPriority(MAX_PRIORITY);
		this.resume = resume;
		this.db = db;
		this.nowPlaying = nowPlaying;
		this.audioPlayer = audioPlayer;
		lastUpdate = 0L;
		this.fileOpened = true;
	}
	
	public synchronized PauseData pause(){
		this.pauseFlag = true;
		while(this.isPlaying){
			//Wait for playback to finish
			//System.out.println("Waiting in playback thread");
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new PauseData(pausePosition,pausedStreamID,totalPTS);
	}
	
	public void setPosition(double value){
	    
		//Pretend we're resuming so the pause position is checked.
	    synchronized(seekSync) {
    		seek = true;
    		this.pausePosition =  Math.round(totalPTS * value);
	    }
		while(true){
		    synchronized(seekSync) {
		        if(seek) {
		            return;
		        } else {
		            try {
		                Thread.sleep(1);
		            } catch (InterruptedException e) {
		                // TODO Auto-generated catch block
		                e.printStackTrace();
		            }
		        }
		    }
		}
	}
	
	public synchronized void setPauseData(PauseData pauseData){
		this.pausePosition = pauseData.getPausePosition();
		this.pausedStreamID = pauseData.getPausedStreamID();
	}
	
	public synchronized boolean isPlaying() {
		return this.isPlaying;
	}
	
	public synchronized boolean fileOpened(){
		return this.fileOpened;
	}

	@Override
	public void run(){
		String trackPath;
    	if(nowPlaying.getCurrentTrack().isClientTrack()){
    		trackPath = ((ClientTrack)nowPlaying.getCurrentTrack()).getPath(db);
    	}else{
    		fileOpened = false;
    		return;
    	}
        	
		// Create a Xuggler container object
		IContainer container = IContainer.make();

		// Open up the container
		try {
			if (container.open(new RandomAccessFile(trackPath, "r"), IContainer.Type.READ, null) < 0)
				throw new IllegalArgumentException("could not open file: " + trackPath);
		} catch (FileNotFoundException e) {
			fileOpened = false;
			return;
		}
		
		fileOpened = true;

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// and iterate through the streams to find the first audio stream
		int audioStreamId = -1;
		IStreamCoder audioCoder = null;
		for(int i = 0; i < numStreams; i++)
		{
			// Find the stream object
			IStream stream = container.getStream(i);
			// Get the pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
			{
				audioStreamId = i;
				audioCoder = coder;
				break;
			}
		}
		if (audioStreamId == -1)
			throw new RuntimeException("could not find audio stream in container: "+trackPath);

		/*
		 * Now we have found the audio stream in this file.  Let's open up our decoder so it can
		 * do work.
		 */
		if (audioCoder.open() < 0)
			throw new RuntimeException("could not open audio decoder for container: "+trackPath);

		/*
		 * And once we have that, we ask the Java Sound System to get itself ready.
		 */
		openJavaSound(audioCoder);

		IPacket packet = IPacket.make();
		
		/*
		 * Set the length of the current track
		 */
		lengthOfCurrentTrack = nowPlaying.getCurrentTrack().getLength();


		/*
		 * If we're resuming, seek to the correct position first.
		 */

		if(audioCoder.getCodec().getName().equals("flac")) {
		    totalPTS = container.getFileSize();
		} else {
		    totalPTS = container.getStream(audioStreamId).getDuration();
		}
		
		if(resume){
			if(audioCoder.getCodec().getName().equals("flac")) {
				if(container.seekKeyFrame(pausedStreamID, pausePosition, pausePosition, pausePosition, IContainer.SEEK_FLAG_BYTE) < 0){
					Dbg.syserr("Seek returned < 0");
				}
			}else{
				if(container.seekKeyFrame(pausedStreamID, pausePosition, 0) < 0){
					Dbg.syserr("Seek returned < 0");
				}
			}
			resume = false;
		}
		
		/*
		 * Now, we start walking through the container looking at each packet.
		 */
		while(container.readNextPacket(packet) >= 0)
		{
			//System.out.println(packet.getPts());
			if(pauseFlag){					
				//Read the next packet, because that's where we'll start from next time as this one finishes first.
				//Keep reading until we find an audioStreadPacket though!
				
				while(packet.getStreamIndex() != audioStreamId){
					container.readNextPacket(packet);
				}
				
				if(audioCoder.getCodec().getName().equals("flac")) {
				    pausePosition = packet.getPosition();
				} else {
				    pausePosition = packet.getPts();
				}
				pausedStreamID = packet.getStreamIndex();
				//System.out.println("Pausing at:    " + packet.getPts());
				mLine.flush();
				break;
			}
			
			synchronized(seekSync) {
    			if(seek){
    				if(container.seekKeyFrame(pausedStreamID, pausePosition, 0) < 0){
    					Dbg.syserr("Seek returned < 0");
    				}
    				seek = false;
    				mLine.flush();
    				continue;
    				
    			}
			}
			isPlaying = true;
			
			/*
			 * Now we have a packet, let's see if it belongs to our audio stream
			 */
			if (packet.getStreamIndex() == audioStreamId)
			{

				/*
				 * We allocate a set of samples with the same number of channels as the
				 * coder tells us is in this buffer.
				 * 
				 * We also pass in a buffer size (1024 in our example), although Xuggler
				 * will probably allocate more space than just the 1024 (it's not important why).
				 */
				IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

				/*
				 * A packet can actually contain multiple sets of samples (or frames of samples
				 * in audio-decoding speak).  So, we may need to call decode audio multiple
				 * times at different offsets in the packet's data.  We capture that here.
				 */
				int offset = 0;

				/*
				 * Keep going until we've processed all data
				 */
				while(offset < packet.getSize())
				{
					//Break out of playback if we're pausing or seeking
					if(pauseFlag || seek){
						break;
					}
					int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
					if (bytesDecoded < 0){
						Dbg.syserr("got error decoding audio in: " + trackPath + "Packet size: ("+packet.getSize()+")" + "Audio stream ID:" + audioStreamId + "packet stream ID:" + packet.getStreamIndex());
						break;
						//throw new RuntimeException("got error decoding audio in: " + trackPath + "Packet size: ("+packet.getSize()+")" + "Audio stream ID:" + audioStreamId + "packet stream ID:" + packet.getStreamIndex());
					}
					offset += bytesDecoded;
					/*
					 * Some decoder will consume data in a packet, but will not be able to construct
					 * a full set of samples yet.  Therefore you should always check if you
					 * got a complete set of samples from the decoder
					 */
					if (samples.isComplete())
					{
						playJavaSound(samples);
					}
				}
				
				
				//Update Seek bar if necessary:
	            if((System.currentTimeMillis() - lastUpdate) > 1000){
	                lastUpdate = System.currentTimeMillis();
	                if(audioCoder.getCodec().getName().equals("flac")) {
    	                if(packet.getPosition() > 0) {
    	                    audioPlayer.getSeekbar().setDuration((double) packet.getPosition() / container.getFileSize(),true,lengthOfCurrentTrack);
    	                }
	                } else {
	                    audioPlayer.getSeekbar().setDuration(packet.getPts() / totalPTS,true,lengthOfCurrentTrack);
	                }
	            }

			}
			else
			{
				/*
				 * This packet isn't part of our audio stream, so we just silently drop it.
				 */
				do {} while(false);
			}

		}
		/*
		 * Technically since we're exiting anyway, these will be cleaned up by 
		 * the garbage collector... but because we're nice people and want
		 * to be invited places for Christmas, we're going to show how to clean up.
		 */
		closeJavaSound();

		if (audioCoder != null)
		{
			audioCoder.close();
			audioCoder = null;
		}
		if (container !=null)
		{
			container.close();
			container = null;
		}

		isPlaying = false;
		//If we've reached the end of the loop and it wasn't because the track was paused, then play the next track
		if(!pauseFlag){
		    //System.out.println("Track finished");
		    audioPlayer.DoAudioAction(AudioAction.TrackFinished);
		}
		pauseFlag = false;
	}

	private void openJavaSound(IStreamCoder aAudioCoder)
	{
		AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
				(int)IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()),
				aAudioCoder.getChannels(),
				true, /* xuggler defaults to signed 16 bit samples */
				false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try
		{
			mLine = (SourceDataLine) AudioSystem.getLine(info);
			/**
			 * if that succeeded, try opening the line.
			 */
			mLine.open(audioFormat);
			/**
			 * And if that succeed, start the line.
			 */
			
			// Create the volume controller
			volumecontrol = (FloatControl) mLine.getControl(FloatControl.Type.MASTER_GAIN);
			float dB = (float)(Math.log(gain)/Math.log(10.0)*20.0);
        		volumecontrol.setValue(dB);
			
			mLine.start();
		}
		catch (LineUnavailableException e)
		{
			throw new RuntimeException("could not open audio line");
		}


	}

	private void playJavaSound(IAudioSamples aSamples)
	{
		/**
		 * We're just going to dump all the samples into the line.
		 */
		byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
		mLine.write(rawBytes, 0, aSamples.getSize());
	}

	private void closeJavaSound()
	{
		if (mLine != null)
		{
			/*
			 * Wait for the line to finish playing
			 */
			mLine.drain();
			/*
			 * Close the line.
			 */
			mLine.close();
			mLine=null;
		}
	}
	
	/**
	 * Sets the volume
	 * @param gain Gain (a double from 0 to 1 with 1 being full volume)
	 */
	public void setVolume(double gain) {
        	float dB = (float)(Math.log(gain)/Math.log(10.0)*20.0);
        	this.gain = gain;
        	volumecontrol.setValue(dB);
	}

}
