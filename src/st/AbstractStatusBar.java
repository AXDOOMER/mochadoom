package st;

import doom.DoomMain;

public abstract class AbstractStatusBar implements IDoomStatusBar {
    protected final DoomMain<?, ?> DOOM;

    public AbstractStatusBar(DoomMain<?, ?> DOOM) {
        this.DOOM = DOOM;
    }
}
