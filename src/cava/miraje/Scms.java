package cava.miraje;

import java.io.*;

/**
 * Statistical Cluster Model Similarity class. A Gaussian representation of a
 * song. The distance between two models is computed with the symmetrized
 * Kullback Leibler Divergence.
 */
public class Scms {
    /**
     * Mean
     */
    private float[] mean;

    /**
     * Covariance
     */
    private float[] cov;

    /**
     * Inverse covariance
     */
    private float[] icov;

    /**
     * Dimension
     */
    private int dim;

    /**
     * Create an Scms object with the specified dimension
     * @param dimension Dimension
     */
    public Scms(int dimension) {
        dim = dimension;
        int symDim = (dim * dim + dim) / 2;

        mean = new float[dim];
        cov = new float[symDim];
        icov = new float[symDim];
    }

    /**
     * Computes a Scms model from the MFCC representation of a track.
     * 
     * @param mfcc
     *            Mfcc for track
     * @return Scms model from the mfcc representation
     * @throws ScmsImpossibleException Thrown if Scms generation fails
     */
    public static Scms getScms(Matrix mfcc) throws ScmsImpossibleException {
        DbgTimer t = new DbgTimer();

        // Mean
        Vector m = mfcc.mean();

        // Covariance
        Matrix c = mfcc.covariance(m);

        // Inverse Covariance
        Matrix ic;
        try {
            DbgTimer t2 = new DbgTimer();
            ic = c.inverse();
            Dbg.println("MiraJe: Matrix inversion took " + t2.stop() + "ms");
        } catch (MatrixSingularException e) {
            throw new ScmsImpossibleException();
        }

        // Store the Mean, Covariance, Inverse Covariance in an optimal format.
        int dim = m.rows;
        Scms s = new Scms(dim);
        int l = 0;
        for(int i=0; i < dim; i++) {
            s.mean[i] = m.d[i][0];
            for(int j=i; j < dim; j++) {
                s.cov[l] = c.d[i][j];
                s.icov[l] = ic.d[i][j];
                l++;
            }
        }

        Dbg.println("MiraJe: Scms created in: " + t.stop() + "ms");

        return s;
    }

    /**
     * Function to compute the spectral distance between two song models. This
     * is a fast implementation of the symmetrized Kullback Leibler Divergence.
     * 
     * @param s1
     *            Scms 1
     * @param s2
     *            Scms 2
     * @param c
     *            Scms config/cache
     * @return Spectral distance between the two song models
     */
    public static float distance(Scms s1, Scms s2, ScmsConfiguration c) {
        float val = 0;

        int i;
        int k;
        int idx = 0;
        int dim = c.dim;
        int covlen = c.covlen;
        float tmp1;

        float[] mdiff = c.mdiff, aicov = c.aicov;

        for(i=0; i < covlen; i++) {
            aicov[i] = s1.icov[i] + s2.icov[i];
        }

        for(i=0; i < dim; i++) {
            idx = i*dim - (i*i+i)/2;
            val += s1.cov[idx+i] * s2.icov[idx+i] + s2.cov[idx+i] * s1.icov[idx+i];

            for(k=i+1; k < dim; k++) {
                val += 2*s1.cov[idx+k] * s2.icov[idx+k] + 2*s2.cov[idx+k] * s1.icov[idx+k];
            }
        }

        for(i=0; i < dim; i++) {
            mdiff[i] = s1.mean[i] - s2.mean[i];
        }

        for(i=0; i < dim; i++) {
            idx = i - dim;
            tmp1 = 0;

            for(k=0; k <= i; k++) {
                idx += dim - k;
                tmp1 += aicov[idx] * mdiff[k];
            }
            for(k=i+1; k < dim; k++) {
                idx++;
                tmp1 += aicov[idx] * mdiff[k];
            }
            val += tmp1 * mdiff[i];
        }

        // Original code:
        //val = val/4 - dim/2;

        // If val is below 0, set it to 0
        val = Math.max(0.0f, val/4 - dim/2);

        return val;
    }

    /**
     * Manual serialization of a Scms object to a byte array
     * 
     * @return Scms as byte array
     */
    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream bw = new DataOutputStream(stream);

        try {
            bw.writeInt(dim);
            for(int i=0; i < mean.length; i++) {
                bw.writeFloat(mean[i]);
            }
            for(int i=0; i < cov.length; i++) {
                bw.writeFloat(cov[i]);
            }
            for(int i=0; i < icov.length; i++) {
                bw.writeFloat(icov[i]);
            }
        } catch (IOException e) {}

        return stream.toByteArray();
    }

    /**
     * Manual deserialisation of an Scms from a byte array
     * 
     * @param buf
     *            Byte array
     * @param s
     *            Scms object to store in
     */
    public static void fromBytes(byte[] buf, Scms s) {
        ByteArrayInputStream stream = new ByteArrayInputStream(buf);
        DataInputStream br = new DataInputStream(stream);

        try {
            s.dim = br.readInt();
            for(int i=0; i < s.mean.length; i++) {
                s.mean[i] = br.readFloat();
            }
            for(int i=0; i < s.cov.length; i++) {
                s.cov[i] = br.readFloat();
            }
            for(int i=0; i < s.icov.length; i++) {
                s.icov[i] = br.readFloat();
            }
        } catch (IOException e) {}
    }
}
