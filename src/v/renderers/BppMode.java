package v.renderers;

import doom.CVarManager;
import doom.CommandVariable;

public enum BppMode {
    HiColor(5),
    Indexed(5),
    TrueColor(8);
    
    /** Bits representing color levels. 5 for 32. */
    public final int lightBits;

    private BppMode(int lightBits) {
        this.lightBits = lightBits;
    }

    public static BppMode chooseBppMode(CVarManager CVM) {
        if (CVM.bool(CommandVariable.HICOLOR)) {
            return HiColor;
        } else if (CVM.bool(CommandVariable.TRUECOLOR)) {
            return TrueColor;
        } else {
            return Indexed;
        }
    }
}
