package cava.miraje;

import java.util.*;

public class AudioDecoder {
    // Load the C libraries
    static {
        System.loadLibrary("mirajeaudio");
        System.loadLibrary("miraje");
    }
    
    // BEGIN JNI DEFS
    
    /**
     * Initialises C decoder
     * 
     * @param rate
     *            Rate
     * @param seconds
     *            Seconds
     * @param winsize
     *            Window size
     * @param debug
     *            Debugging on/off
     * @return Pointer to decoder
     */
    private native long mirajeaudio_initialise(int rate, int seconds, int winsize, boolean debug);

    /**
     * Decodes audio file using C decoding library
     * 
     * @param ma
     *            Pointer to decoder
     * @param file
     *            Path to audio file
     * @param frames
     *            Single element array to receive frames
     * @param size
     *            Single element array to receive size
     * @param ret
     *            Single element array to receive return value
     * @return Array containing matrix of decoded data
     */
    private native float[] mirajeaudio_decode(long ma, String file, int[] frames, int[] size, int[] ret);

    /**
     * Destroys the C decoder
     * 
     * @param ma
     *            Pointer to decoder
     */
    private native void mirajeaudio_destroy(long ma);

    /**
     * Cancels C decoding
     * 
     * @param ma
     *            Pointer to decoder
     */
    private native void mirajeaudio_canceldecode(long ma);

    /**
     * Holds pointer to C decoding library
     */
    private long ma;
    // END JNI DEFS

    /**
     * Creates an Audio Decoder
     */
    public AudioDecoder(int rate, int seconds, int winsize) {
        ma = mirajeaudio_initialise(rate, seconds, winsize, Constants.DEBUG);
    }

    /**
     * Decodes a given audio file
     * 
     * @param file
     *            Path to audio file to be decoded
     * @return Matrix containing decoded data
     * @throws AudioDecoderErrorException
     *             Thrown when error occurs during decoding
     * @throws AudioDecoderCanceledException
     *             Thrown when decoding is cancelled
     */
    public Matrix decode(String file) throws AudioDecoderErrorException, AudioDecoderCanceledException {
        int[] frames = new int[1];
        int[] size = new int[1];
        int[] ret = new int[1];

        float[] data = mirajeaudio_decode(ma, file, frames, size, ret);
        // Error while decoding
        if(ret[0] == -1)
            throw new AudioDecoderErrorException();
        // Decoding was cancelled
        else if(ret[0] == -2)
            throw new AudioDecoderCanceledException();
        // No data
        else if((frames[0] <= 0) || (size[0] <= 0))
            throw new AudioDecoderErrorException();

        Dbg.println("MiraJe: Decoded frames=" + frames[0] + ",size=" + size[0] + "");

        // Sort the frames by total energy (frame selection)
        Frame[] frameselection = new Frame[frames[0]];

        for(int j=0; j < frames[0]; j++) {
            frameselection[j] = new Frame(0, j);

            for(int i=0; i < size[0]; i++) {
                frameselection[j].data += data[i*frames[0]+j];
            }
        }

        Arrays.sort(frameselection, new Comparator<Frame>() {
            public int compare(Frame arg0, Frame arg1) {
                if(arg0.data < arg1.data) {
                    return -1;
                } else if(arg0.data > arg1.data) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        // Save the high energy frames to the Matrix 
        int copyframes = frames[0] / 2;
        Matrix stft = new Matrix(size[0], copyframes);

        for(int j=0; j < copyframes; j++) {
            for(int i=0; i < size[0]; i++) {
                stft.d[i][j] = data[i*frames[0]+frameselection[copyframes+j].pos];
            }
        }

        return stft;
    }

    /**
     * Destroys the C decoder when the Audio Decoder is destroyed
     */
    protected void finalize() {
        mirajeaudio_destroy(ma);
        try {
            super.finalize();
        } catch (Throwable e) {}
    }

    /**
     * Cancels the decoding of an audio file
     */
    public void cancelDecode() {
        mirajeaudio_canceldecode(ma);
    }
}
