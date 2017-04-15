package v.graphics;

import java.lang.reflect.Array;
import static utils.GenericCopy.*;

public interface ColorTransform {
    
    default boolean initTransform(Wipers.WiperImpl<?, ?> wiper) {
        memcpy(wiper.wipeStartScr, 0, wiper.wipeEndScr, 0, Array.getLength(wiper.wipeEndScr));
        return false;
    }
    
    default boolean colorTransformB(Wipers.WiperImpl<byte[], ?> wiper) {
        byte[] w = wiper.wipeStartScr, e = wiper.wipeEndScr;
        boolean changed = false;
        for (int i = 0, newval; i < w.length; ++i) {
            if (w[i] != e[i]) {
                w[i] = w[i] > e[i]
                    ? (newval = w[i] - wiper.ticks) < e[i] ? e[i] : (byte) newval
                    : (newval = w[i] + wiper.ticks) > e[i] ? e[i] : (byte) newval;
                changed = true;
            }
        }
        return !changed;
    }

    default boolean colorTransformS(Wipers.WiperImpl<short[], ?> wiper) {
        short[] w = wiper.wipeStartScr, e = wiper.wipeEndScr;
        boolean changed = false;
        for (int i = 0, newval; i < w.length; ++i) {
            if (w[i] != e[i]) {
                w[i] = w[i] > e[i]
                    ? (newval = w[i] - wiper.ticks) < e[i] ? e[i] : (byte) newval
                    : (newval = w[i] + wiper.ticks) > e[i] ? e[i] : (byte) newval;
                changed = true;
            }
        }
        return !changed;
    }

    default boolean colorTransformI(Wipers.WiperImpl<int[], ?> wiper) {
        int[] w = wiper.wipeStartScr, e = wiper.wipeEndScr;
        boolean changed = false;
        for (int i = 0, newval; i < w.length; ++i) {
            if (w[i] != e[i]) {
                w[i] = w[i] > e[i]
                    ? (newval = w[i] - wiper.ticks) < e[i] ? e[i] : (byte) newval
                    : (newval = w[i] + wiper.ticks) > e[i] ? e[i] : (byte) newval;
                changed = true;
            }
        }
        return !changed;
    }
}
