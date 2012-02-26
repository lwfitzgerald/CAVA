package cava.miraje;

/**
 * Contains methods to print debug messages
 */
public class Dbg {
    /**
     * Only "println" the given string if debugging is enabled
     * 
     * @param l
     *            String to print using System.out.println
     */
    public static void println(String l) {
        if(Constants.DEBUG) {
            System.out.println(l);
        }
    }

    /**
     * Only "print" the given string if debugging is enabled
     * 
     * @param l
     *            String to print using System.out.print
     */
    public static void print(String l) {
        if(Constants.DEBUG) {
            System.out.print(l);
        }
    }
}