package mochadoom;

import doom.CVarManager;
import doom.ConfigManager;
import doom.DoomMain;
import java.io.IOException;
import java.util.Arrays;

public class Engine {
    private static String[] myArgs = {};
    private static volatile CVarManager cvm = null;
    private static volatile ConfigManager cm = null;
    
    public static void main(final String[] argv) throws IOException {
        myArgs = argv;
        DoomMain local = new DoomMain<>();
    }
    
    public static CVarManager getCVM() {
        CVarManager local = Engine.cvm;
        if (local == null) {
            synchronized (CVarManager.class) {
                local = Engine.cvm;
                if (local == null) {
                    Engine.cvm = local = new CVarManager(Arrays.asList(myArgs));
                }
            }
        }
        return local;
    }
    
    public static ConfigManager getConfig() {
        ConfigManager local = Engine.cm;
        if (local == null) {
            synchronized (ConfigManager.class) {
                local = Engine.cm;
                if (local == null) {
                    Engine.cm = local = new ConfigManager();
                }
            }
        }
        return local;
    }
}