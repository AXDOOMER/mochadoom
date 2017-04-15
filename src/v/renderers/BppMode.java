package v.renderers;

import doom.CVarManager;
import doom.CommandVariable;

public enum BppMode {
    HiColor,
    Indexed,
    TrueColor;

    public static BppMode chooseBppMode(CVarManager CVM) {
        if (CVM.bool(CommandVariable.HICOLOR)) {
            return BppMode.HiColor;
        } else if (CVM.bool(CommandVariable.TRUECOLOR)) {
            return BppMode.TrueColor;
        } else {
            return BppMode.Indexed;
        }
    }
}
