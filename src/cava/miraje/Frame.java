package cava.miraje;

/**
 * Holds the data and position of a frame
 */
public class Frame {
    /**
     * Data
     */
    public float data;
    
    /**
     * Position
     */
    public final int pos;

    /**
     * Create the holder for the data and position of a frame
     * 
     * @param data
     *            Data
     * @param pos
     *            Position
     */
    public Frame(float data, int pos) {
        this.data = data;
        this.pos = pos;
    }
}