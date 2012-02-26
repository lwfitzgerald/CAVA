package cava.miraje;

/**
 * Represents a vector
 */
@SuppressWarnings("serial")
public class Vector extends Matrix {

    /**
     * Creates a vector by extending the matrix class
     * 
     * @param rows
     *            Number of rows
     */
    public Vector(int rows) {
        super(rows, 1);
    }
}