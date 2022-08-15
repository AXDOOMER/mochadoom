package m;

import doom.DoomMain;

public abstract class AbstractDoomMenu<T, V> implements IDoomMenu {

    ////////////////////// CONTEXT ///////////////////
    
    final DoomMain<T, V> DOOM;

    public AbstractDoomMenu(DoomMain<T, V> DOOM) {
        this.DOOM = DOOM;
    }
}