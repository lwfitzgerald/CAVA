package cava.miraje;

/**
 * Utility class storing a cache and Configuration variables for the Scms
 * distance computation.
 */
public class ScmsConfiguration {
    /**
     * Dimension
     */
    public final int dim;
    
    /**
     * Covariance length
     */
    public final int covlen;
    
    /**
     * Mean difference
     */
    public final float[] mdiff;
    
    /**
     * Average inverse covariance
     */
    public final float[] aicov;

    /**
     * Create a ScmsConfiguration object with a given dimension to store a cache
     * and configuration variables for the Scms distance computation
     * 
     * @param dimension
     *            Dimension
     */
    public ScmsConfiguration(int dimension) {
        dim = dimension;
        covlen = (dim*dim + dim)/2;
        mdiff = new float[dim];
        aicov = new float[covlen];
    }
}
