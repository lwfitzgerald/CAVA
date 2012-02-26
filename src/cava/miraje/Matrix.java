package cava.miraje;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Represents a matrix
 */
@SuppressWarnings("serial")
public class Matrix implements Serializable {
    /**
     * Matrix data
     */
    public float[][] d;

    /**
     * Number of columns
     */
    public int columns;

    /**
     * Number of rows
     */
    public int rows;

    // BEGIN JNI DEFS
    /**
     * Inverse a matrix held in a single dimension array using the C library
     * 
     * @param floatmatrix
     *            Matrix held in single dimension array
     * @param rows
     *            Number of rows
     * @param cols
     *            Number of columns
     * @return Integer representing success or failure
     */
    private native int miraje_inversematrix(float[] floatmatrix, int rows, int cols);

    /**
     * Multiply
     * 
     * @param matrix1
     *            Matrix 1
     * @param matrix2
     *            Matrix 2
     * @param rows1
     *            Number of rows in Matrix 1
     * @param cols1
     *            Number of columns in Matrix 1
     * @param cols2
     *            Number of columns in Matrix 2
     * @return Resulting matrix from multiplication
     */
    private native float[] miraje_multiplymatrix(float[] matrix1, float[] matrix2, int rows1, int cols1, int cols2);
    // END JNI DEFS

    /**
     * Creates an n x m matrix
     * 
     * @param rows
     *            Number of rows
     * @param columns
     *            Number of columns
     */
    public Matrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        d = new float[rows][columns];
    }

    /**
     * Multiplies two matrices together
     * 
     * @param m2
     *            Matrix to multiply with
     * @return The resulting matrix from the multiplication
     * @throws MatrixDimensionMismatchException Thrown when dimensions of matrices mismatch
     */
    public Matrix multiply(Matrix m2) throws MatrixDimensionMismatchException {
        if(columns != m2.rows) {
            throw new MatrixDimensionMismatchException();
        }

        Matrix m3 = new Matrix(this.rows, m2.columns);

        if(Constants.USECLIB) {
            m3.updateFromSingleArray(miraje_multiplymatrix(this.toSingleArray(), m2.toSingleArray(), this.rows, this.columns, m2.columns));
        } else {
            for(int i=0; i < this.rows; i++) {
                for(int j=0; j < m2.columns; j++) {
                    for(int k=0; k < this.columns; k++) {
                        m3.d[i][j] += this.d[i][k] * m2.d[k][j];
                    }
                }
            }
        }

        return m3;
    }

    /**
     * Get the mean vector of the matrix
     * @return Mean vector
     */
    public Vector mean() {
        Vector mean = new Vector(rows);
        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                mean.d[i][0] += d[i][j] / columns;
            }
        }

        return mean;
    }

    public void print() {
        print(rows, columns);
    }

    /**
     * Prints out a text representation of the columns and rows of the matrix
     * specified
     * 
     * @param rows
     *            Number of rows
     * @param columns
     *            Number of columns
     */
    public void print(int rows, int columns) {
        System.out.println("Rows: " + this.rows + " Columns: " + this.columns);
        System.out.println("[");

        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                System.out.print(d[i][j] + " ");
            }
            System.out.println(";");
        }
        System.out.println("]");
    }

    /**
     * Prints out a text representation of the matrix with the columns and rows
     * swapped
     */
    public void printTurn() {
        printTurn(rows, columns);
    }

    /**
     * Prints out a text representation of the columns and rows of the matrix
     * specified with the columns and rows swapped
     * 
     * @param rows
     *            Number of rows
     * @param columns
     *            Number of columns
     */
    public void printTurn(int rows, int columns) {
        System.out.println("Rows: " + this.rows + " Columns: " + this.columns);
        System.out.println("[");

        for(int i=0; i < columns; i++) {
            for(int j=0; j < rows; j++) {
                System.out.print(d[j][i] + " ");
            }
            System.out.println(";");
        }
        System.out.println("]");
    }

    /**
     * Returns the covariance of the matrix using the given vector
     * 
     * @param mean
     *            Vector
     * @return Covariance of matrix using the given vector
     */
    public Matrix covariance(Vector mean) {
        Matrix cache = new Matrix(rows, columns);
        float factor = 1.0f / (float) (columns - 1);
        for(int j=0; j < rows; j++) {
            for(int i=0; i < columns; i++) {
                cache.d[j][i] = (d[j][i] - mean.d[j][0]);
            }
        }

        Matrix cov = new Matrix(mean.rows, mean.rows);
        for(int i=0; i < cov.rows; i++) {
            for(int j=0; j <= i; j++) {
                float sum = 0.0f;
                for(int k=0; k < columns; k++) {
                    sum += cache.d[i][k] * cache.d[j][k];
                }
                sum *= factor;
                cov.d[i][j] = sum;
                if(i == j) {
                    continue;
                }
                cov.d[j][i] = sum;
            }
        }

        return cov;
    }

    /**
     * Writes the matrix object to a file
     * 
     * @param file
     *            Path of the file to write to
     * @throws IOException
     */
    public void write(String file) throws IOException {
        FileOutputStream filestream = new FileOutputStream(file);
        DataOutputStream writer = new DataOutputStream(filestream);
        writer.writeInt(rows);
        writer.writeInt(columns);

        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                writer.writeFloat(d[i][j]);
            }
        }

        writer.flush();
        writer.close();
    }

    /**
     * Loads a Matrix object from a given FileInputStream
     * 
     * @param filestream
     *            FileInputStream of file to load matrix from
     * @return Matrix object loaded from file
     */
    public static Matrix load(InputStream filestream) throws IOException {
        DataInputStream reader = new DataInputStream(filestream);
        int rows = reader.readInt();
        int columns = reader.readInt();

        Matrix m = new Matrix(rows, columns);

        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                m.d[i][j] = reader.readFloat();
            }
        }

        reader.close();
        return m;
    }

    private float[] toSingleArray() {
        float[] floatmatrix = new float[rows * columns];

        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                floatmatrix[i * columns + j] = d[i][j];
            }
        }

        return floatmatrix;
    }

    private void updateFromSingleArray(float[] floatmatrix) {
        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                d[i][j] = floatmatrix[i * columns + j];
            }
        }
    }

    /**
     * Uses the Gauss-Jordan method to invert the matrix with decimal precision
     * 
     * @return The inverted matrix
     * @throws MatrixSingularException
     */
    public Matrix inverse() throws MatrixSingularException {
        Matrix inv = new Matrix(rows, columns);

        if (Constants.USECLIB) {
            float[] floatmatrix = toSingleArray();
            miraje_inversematrix(floatmatrix, rows, columns);
            inv.updateFromSingleArray(floatmatrix);
        } else {
            BigDecimal[][] e = new BigDecimal[rows + 1][columns + 1];

            // Create a BigDecimal object at every entry
            for(int i=0; i <= rows; i++) {
                for(int j=0; j <= columns; j++) {
                    // When on the diagonal set to 1, otherwise 0
                    e[i][j] = (i == j && i != 0) ? BigDecimal.ONE : BigDecimal.ZERO;
                }
            }

            BigDecimal[][] m = new BigDecimal[rows + 1][columns + 1];
            for(int i=0; i <= rows; i++) {
                for(int j=0; j <= columns; j++) {
                    m[i][j] = (i == 0 || j == 0) ? BigDecimal.ZERO : new BigDecimal(d[i - 1][j - 1], MathContext.DECIMAL128);
                }
            }

            gaussJordan(m, rows, e, rows);

            for(int i=1; i <= rows; i++) {
                for(int j=1; j <= columns; j++) {
                    inv.d[i-1][j-1] = m[i][j].floatValue();
                }
            }
        }

        return inv;
    }

    /**
     * Gauss-Jordan
     * 
     * @param a
     *            Decimal precision matrix
     * @param n
     *            Size of matrix a
     * @param b
     *            Decimal precision matrix
     * @param m
     *            Size of matrix b
     * @throws MatrixSingularException
     */
    private void gaussJordan(BigDecimal[][] a, int n, BigDecimal[][] b, int m) throws MatrixSingularException {
        int[] indxc = new int[n+1];
        int[] indxr = new int[n+1];
        int[] ipiv = new int[n+1];
        int i, icol = 0, irow = 0, j, k, l, ll;
        BigDecimal big, dum, pivinv, temp;

        for(j=1; j <= n; j++)
            ipiv[j] = 0;

        for(i=1; i <= n; i++) {
            big = BigDecimal.ZERO;
            for(j=1; j <= n; j++) {
                if(ipiv[j] != 1) {
                    for(k=1; k <= n; k++) {
                        if(ipiv[k] == 0) {
                            if(a[j][k].abs().compareTo(big) != -1) {
                                big = a[j][k].abs();
                                irow = j;
                                icol = k;
                            }
                        } else if(ipiv[k] > 1) {
                            Dbg.println("MiraJe - Gauss/Jordan Singular Matrix (1)");
                            throw new MatrixSingularException();
                        }
                    }
                }
            }

            ipiv[icol]++;
            if(irow != icol) {
                for(l=1; l <= n; l++) {
                    temp = a[irow][l];
                    a[irow][l] = a[icol][l];
                    a[icol][l] = temp;
                }
                for(l=1; l <= m; l++) {
                    temp = b[irow][l];
                    b[irow][l] = b[icol][l];
                    b[icol][l] = temp;
                }
            }

            indxr[i] = irow;
            indxc[i] = icol;
            if(a[icol][icol].compareTo(BigDecimal.ZERO) == 0) {
                Dbg.println("MiraJe - Gauss/Jordan Singular Matrix (2)");
                throw new MatrixSingularException();
            }

            pivinv = BigDecimal.ONE.divide(a[icol][icol], MathContext.DECIMAL128);
            a[icol][icol] = BigDecimal.ONE;

            for(l=1; l <= n; l++) {
                a[icol][l] = a[icol][l].multiply(pivinv, MathContext.DECIMAL128);
            }
            for(l=1; l <= m; l++) {
                b[icol][l] = b[icol][l].multiply(pivinv, MathContext.DECIMAL128);
            }

            for(ll=1; ll <= n; ll++) {
                if(ll != icol) {
                    dum = a[ll][icol];
                    a[ll][icol] = BigDecimal.ZERO;
                    for(l=1; l <= n; l++) {
                        a[ll][l] = a[ll][l].subtract(a[icol][l].multiply(dum, MathContext.DECIMAL128), MathContext.DECIMAL128);
                    }
                    for(l=1; l <= m; l++) {
                        b[ll][l] = b[ll][l].subtract(b[icol][l].multiply(dum, MathContext.DECIMAL128), MathContext.DECIMAL128);
                    }
                }
            }
        }

        for(l=n; l >= 1; l--) {
            if(indxr[l] != indxc[l]) {
                for(k=1; k <= n; k++) {
                    temp = a[k][indxr[l]];
                    a[k][indxr[l]] = a[k][indxc[l]];
                    a[k][indxc[l]] = temp;
                }
            }
        }
    }
}
