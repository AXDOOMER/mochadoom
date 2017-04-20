package awt;

import awt.DoomVideoInterface.DoomListener;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

class DoomKeyDispatcher implements KeyEventDispatcher {
    
    private final DoomListener eventHandler;
    private final Component doomComponent;

    DoomKeyDispatcher(DoomListener eventHandler, Component doomComponent) {
        this.eventHandler = eventHandler;
        this.doomComponent = doomComponent;
    }
    
    boolean press = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            // MAES: simulating a key type.
            if (press) {
                eventHandler.keyPressed(new KeyEvent(doomComponent,
                    e.getID(), System.nanoTime(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            } else {
                eventHandler.keyReleased(new KeyEvent(doomComponent,
                    e.getID(), System.nanoTime(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            }
            press = !press;
        }
        return false;
    }
    
}
