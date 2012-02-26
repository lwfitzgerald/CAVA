package cava;

public class Dbg {
    public static void sysout(String string) {
        if(Constants.DEBUG) {
            System.out.println(string);
        }
    }
    
    public static void syserr(String string) {
        if(Constants.DEBUG) {
            System.err.println(string);
        }
    }
}