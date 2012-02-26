package cava.miraje;

import java.io.*;

/**
 * Used for generating Mfcc's from decoded data
 */
public class Mfcc {
    Matrix filterWeights;
    Matrix dct;
    int[][] fwFT;
    
    /**
     * Create a new Mfcc with
     * 
     * @param winsize
     *            Window size
     * @param srate
     *            Sample rate
     * @param filters
     *            Mel coefficients
     * @param cc
     *            Mfcc coefficients
     */
    public Mfcc(int winsize, int srate, int filters, int cc) {
        try {
            // Load the DCT
            dct = Matrix.load(getFilterStream("dct.filter"));

            // Load the MFCC filters from the filter File.
            filterWeights = Matrix.load(getFilterStream("filterweights.filter"));
        } catch (IOException e) {
            System.err.println("Filters not found");
            System.exit(1);
        }

        fwFT = new int[filterWeights.rows][2];
        for(int i=0; i < filterWeights.rows; i++) {
            float last = 0;
            for(int j=0; j < filterWeights.columns; j++) {
                if((filterWeights.d[i][j] != 0) && (last == 0)) {
                    fwFT[i][0] = j;
                } else if((filterWeights.d[i][j] == 0) && (last != 0)) {
                    fwFT[i][1] = j;
                }
                last = filterWeights.d[i][j];
            }
            if(last != 0) {
                fwFT[i][1] = filterWeights.columns;
            }
        }
    }

    /**
     * Generate the Mfcc for a decoded audio file
     * 
     * @param m
     *            Matrix containing decoded audio file
     * @return Mfcc for audio file
     * @throws MfccFailedException
     *             Thrown if generating the Mfcc fails
     */
    public Matrix apply(Matrix m) throws MfccFailedException {
        DbgTimer t = new DbgTimer();

        Matrix mel = new Matrix(filterWeights.rows, m.columns);

        for(int i=0; i < m.columns; i++) {
            for(int k=0; k < filterWeights.rows; k++) {

                // The filter weights matrix is mostly 0.
                // So only multiply non-zero elements!
                for(int j=fwFT[k][0]; j < fwFT[k][1]; j++) {
                    mel.d[k][i] += filterWeights.d[k][j] * m.d[j][i];
                }

                mel.d[k][i] = (mel.d[k][i] < 1.0f ?
                        0 : (float)(10.0 * Math.log10(mel.d[k][i])));
            }
        }

        try {
            Matrix mfcc = dct.multiply(mel);

            Dbg.println("MiraJe: Mfcc execution took " + t.stop() + "ms");

            return mfcc;

        } catch (MatrixDimensionMismatchException e) {
            throw new MfccFailedException();
        }
    }

    /**
     * Load a filter file
     * 
     * @param filter
     *            Path to filter file
     * @return InputStream for filter file
     */
    private InputStream getFilterStream(String filter) {
        return this.getClass().getResourceAsStream("filters/" + filter);
    }
}
