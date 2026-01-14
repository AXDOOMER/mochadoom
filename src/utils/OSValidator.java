package utils;

/** Half-assed way of finding the OS we're running under, shamelessly 
 * ripped from:
 * 
 *  http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
 * .
 * This is required, as some things in AWT don't work exactly consistently cross-OS
 * (AWT frame size is the first thing that goes wrong, but also mouse grabbing
 * behavior).
 * 
 * TODO: replace with Apache Commons library?
 *  
 * @author velktron
 *
 */
public class OSValidator {

    public static boolean isWindows() {

        String os = System.getProperty("os.name").toLowerCase();
        //windows
        return (os.contains("win"));

    }

    public static boolean isMac() {

        String os = System.getProperty("os.name").toLowerCase();
        //Mac
        return (os.contains("mac"));

    }

    public static boolean isUnix() {

        String os = System.getProperty("os.name").toLowerCase();
        //linux or unix
        return (os.contains("nix") || os.contains("nux"));

    }

    public static boolean isUnknown() {
        return (!isWindows() && !isUnix() && !isMac());
    }
}
