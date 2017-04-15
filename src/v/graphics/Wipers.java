package v.graphics;

import f.Wiper;
import java.lang.reflect.Array;
import m.IRandom;
import utils.GenericCopy;

/**
 * SCREEN WIPE PACKAGE
 */
public class Wipers implements ColorTransform, Melt {
    private static final Wipers instance = new Wipers();
    
    /**
     * They are repeated thrice for a reason - they are overloads with different arguments
     * - Good Sign 2017/04/06
     * 
     * ASS-WIPING functions
     */
    public enum WipeFunc {
        doColorXFormB(instance::colorTransformB, byte[].class),
        doColorXFormS(instance::colorTransformS, short[].class),
        doColorXFormI(instance::colorTransformI, int[].class),
        
        initColorXForm(instance::initTransform),
        doColorXForm(doColorXFormB, doColorXFormS, doColorXFormI),
        exitColorXForm(w -> false),

        initScaledMelt(instance::initMeltScaled),
        doScaledMelt(instance::doMeltScaled),

        initMelt(instance::initMelt),
        doMelt(instance::doMelt),
        exitMelt(instance::exitMelt);
        
        private final Class supportFor;
        private final WF func;
        
        WipeFunc(WF func) {
            this.supportFor = null;
            this.func = func;
        }
        
        <V> WipeFunc(WF<V> func, Class<V> supportFor) {
            this.supportFor = supportFor;
            this.func = func;
        }
        
        @SuppressWarnings("unchecked")
        WipeFunc(final WipeFunc... wf) {
            this.supportFor = null;
            this.func = w -> {

                for (int i = 0; i < wf.length; ++i) {
                    if (w.bufferType == wf[i].supportFor) {
                        return wf[i].func.invoke(w);
                    }
                }

                throw new UnsupportedOperationException("Do not have support for: " + w.bufferType);
            };
        }
        
        interface WF<V> { public boolean invoke(WiperImpl<V, ?> wiper); }
    }
    
    public static <V, E extends Enum<E>> Wiper createWiper(IRandom rnd, Screens<V, E> screens, E ws, E we, E ms) {
        return new WiperImpl<>(rnd, screens, ws, we, ms);
    }
    
    protected final static class WiperImpl<V, E extends Enum<E>> implements Wiper {
        private final Relocation relocation = new Relocation(0, 0, 1);
        final IRandom random;
        final Screens<V, E> screens;
        final Class bufferType;
        final V wipeStartScr;
        final V wipeEndScr;
        final V wipeScr;
        final int screenWidth;
        final int screenHeight;
        final int dupx;
        final int dupy;
        final int scaled_16;
        final int scaled_8;
        int[] y;
        int ticks;

        /** when false, stop the wipe */
        volatile boolean go = false;

        private WiperImpl(IRandom RND, Screens<V, E> screens, E wipeStartScreen, E wipeEndScreen, E mainScreen) {
            this.random = RND;
            this.wipeStartScr = screens.getScreen(wipeStartScreen);
            this.wipeEndScr = screens.getScreen(wipeEndScreen);
            this.wipeScr = screens.getScreen(mainScreen);
            this.bufferType = this.wipeScr.getClass();
            this.screens = screens;
            this.screenWidth = screens.getScreenWidth();
            this.screenHeight = screens.getScreenHeight();
            this.dupx = screens.getScalingX();
            this.dupy = screens.getScalingY();
            this.scaled_16 = dupy << 4;
            this.scaled_8 = dupy << 3;
        }
        
        void startToScreen(int source, int destination) {
            screens.screenCopy(wipeStartScr, wipeScr, relocation.retarget(source, destination));
        }

        void endToScreen(int source, int destination) {
            screens.screenCopy(wipeEndScr, wipeScr, relocation.retarget(source, destination));
        }

        /**
         * Sets "from" screen and stores it in "screen 2"
         */
        @Override
        public boolean StartScreen(int x, int y, int width, int height) {
            GenericCopy.memcpy(wipeScr, 0, wipeStartScr, 0, Array.getLength(wipeStartScr));
            return false;
        }

        /**
         * Sets "to" screen and stores it to "screen 3"
         */
        @Override
        public boolean EndScreen(int x, int y, int width, int height) {
            // Set end screen to "screen 3" and copy visible screen to it.
            GenericCopy.memcpy(wipeScr, 0, wipeEndScr, 0, Array.getLength(wipeEndScr));
            // Restore starting screen.
            GenericCopy.memcpy(wipeStartScr, 0, wipeScr, 0, Array.getLength(wipeScr));
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean ScreenWipe(WipeType type, int x, int y, int width, int height, int ticks) {
            boolean rc;

            //System.out.println("Ticks do "+ticks);
            this.ticks = ticks;
            
            // initial stuff
            if (!go) {
                go = true;
                //wipe_scr = new byte[width*height]; // DEBUG
                // HOW'S THAT FOR A FUNCTION POINTER, BIATCH?!
                type.getInitFunc().func.invoke(this);
            }

            // do a piece of wipe-in
            rc = type.getDoFunc().func.invoke(this);
            // V.DrawBlock(x, y, 0, width, height, wipe_scr); // DEBUG

            // final stuff
            if (rc) {
                go = false;
                type.getExitFunc().func.invoke(this);
            }

            return !go;
        }
    }
    
    public interface WipeType {
        WipeFunc getInitFunc();
        WipeFunc getDoFunc();
        WipeFunc getExitFunc();
    }
    
    private Wipers() {}
}
