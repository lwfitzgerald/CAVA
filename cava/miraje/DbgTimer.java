package cava.miraje;

/**
 * Used to time operations
 */
public class DbgTimer {
    private long start;

    /**
     * Creates a new DbgTimer object and records the start time
     */
    public DbgTimer() {
        start = System.currentTimeMillis();
    }
    
    /**
     * Returns the time in milliseconds elapsed since the timer was created
     * 
     * @return Time elapsed in milliseconds
     */
    public long stop() {
        return System.currentTimeMillis() - start; 
    }
    
    public void reset() {
        start = System.currentTimeMillis();
    }
}