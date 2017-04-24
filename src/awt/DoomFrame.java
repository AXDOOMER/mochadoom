package awt;

import doom.CommandVariable;
import doom.DoomMain;
import i.Strings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.util.logging.Level;
import javax.swing.JFrame;
import m.Settings;
import mochadoom.Engine;
import mochadoom.Loggers;

/**
 * Common code for Doom's video frames
 */
public class DoomFrame<V, EvHandler extends Enum<EvHandler> & EventBase<EvHandler>> extends JFrame implements DoomVideoInterface<V> {
    private static final long serialVersionUID = -4130528877723831825L;

    /**
     * Must be aware of "Doom" so we can pass it event messages inside a crude queue.
     */
    protected final DoomMain<?, V> DOOM;
    
    /**
     * Canvas or JPanel
     */
    protected final Component content;
    
    /**
     * Graphics to draw image on
     */
    protected volatile Graphics2D g2d;

    /**
     * Display, its configuration and resolution related stuff
     */
    protected final DisplayModePicker dmp;
    protected final GraphicsDevice device;
    protected final int defaultWidth, defaultHeight;
    protected DisplayMode oldDisplayMode;
    protected DisplayMode displayMode;

    /**
     * Default window size. It might change upon entering full screen, so don't consider it absolute.
     * Due to letter boxing and screen doubling, stretching etc. it might be different that the screen buffer
     * (typically, larger).
     */
    protected final Dimension dimension;

    /**
     * Normally only used in full screen mode
     */
    protected int X_OFF;
    protected int Y_OFF;
    
    protected final EventObserver<EvHandler> observer;
    
    /**
     * Very generic JFrame. Along that it only initializes various properties of Doom Frame.
     * @param DOOM
     * @param content - a pane or canvas
     * @param device - graphics device
     */
    protected DoomFrame(DoomMain<?, V> DOOM, Component content, GraphicsDevice device, EventObserver<EvHandler> observer) {
        if (!handleGeom()) {
            DOOM.doomSystem.Error("bad -geom parameter");
        }

        this.device = device;
        this.DOOM = DOOM;
        this.content = content;
        this.dmp = new DisplayModePicker(device);

        // Set those here. If fullscreen isn't used, then they won't change.
        // They are necessary for normal initialization, though.
        this.defaultWidth = DOOM.graphicSystem.getScreenWidth();
        this.defaultHeight = DOOM.graphicSystem.getScreenHeight();
        this.dimension = new Dimension(defaultWidth, defaultHeight);
        this.observer = observer;

        /**
         * AWT: create the canvas.
         * MAES: this method works even on "stubborn" Linux distros that fuck up the window size.
         */
        try {
            if (!(DOOM.CM.equals(Settings.fullscreen, Boolean.TRUE) && switchToFullScreen())) {
                updateSize();
            }
        } catch (Exception e) {
            Loggers.getLogger(DoomFrame.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            DOOM.doomSystem.Error("Error creating DOOM AWT frame. Exiting. Reason: %s", e.getMessage());
        }
    }

    @Override
    public void SetGamma(int level) {
        DOOM.graphicSystem.setUsegamma(level);
    }

    @Override
    public void FinishUpdate() {
        this.update(null);
    }
    
    /**
     * I_SetPalette
     *
     * Any bit-depth specific palette manipulation is performed by the VideoRenderer. It can range from simple
     * (paintjob) to complex (multiple BufferedImages with locked data bits...) ugh!
     *
     * @param palette index (normally between 0-14).
     */
    @Override
    public void SetPalette(int palette) {
        DOOM.graphicSystem.setPalette(palette);
    }

    @Override
    public boolean switchFullscreen() {
        Loggers.getLogger(DoomFrame.class.getName()).log(Level.WARNING, "FULLSCREEN SWITHED");
        // remove the frame from view
        dispose();
        // change all the properties
        final boolean ret = switchToFullScreen();
        // now show back the frame
        showFrame(this);
        return ret;
    }

    @Override
    public boolean isFullscreen() {
        return oldDisplayMode != null;
    }

    /**
     * FULLSCREEN SWITCH CODE TODO: it's not enough to do this without also switching the screen's resolution.
     * Unfortunately, Java only has a handful of options which depend on the OS, driver, display, JVM etc. and it's not
     * possible to switch to arbitrary resolutions.
     *
     * Therefore, a "best fit" strategy with centering is used.
     */
    private boolean switchToFullScreen() {
        final boolean isFullScreen;
        if (oldDisplayMode == null) {
            isFullScreen = device.isFullScreenSupported();
            if (!isFullScreen) {
                return false;
            }

            // In case we need to revert.
            oldDisplayMode = device.getDisplayMode();
            // TODO: what if bit depths are too small?
            displayMode = dmp.pickClosest(defaultWidth, defaultHeight);
        } else {
            isFullScreen = false;
            
            // We restore the original resolution
            displayMode = oldDisplayMode;
            oldDisplayMode = null;
        }
        setUndecorated(isFullScreen);

        // Full-screen mode
        device.setFullScreenWindow(isFullScreen ? this : null);
        if (device.isDisplayChangeSupported()) {
            device.setDisplayMode(displayMode);
        }
        
        validate();
        dimension.width = isFullScreen ? displayMode.getWidth() : defaultWidth;
        dimension.height = isFullScreen ? displayMode.getHeight() : defaultHeight;
        X_OFF = (dimension.width - defaultWidth) / 2;
        Y_OFF = (dimension.height - defaultHeight) / 2;
        updateSize();
        return isFullScreen;
    }

    private void updateSize() {
        setPreferredSize(isFullscreen() ? dimension : null);
        content.setPreferredSize(dimension);
        content.setBounds(0, 0, content.getWidth() - 1, content.getHeight() - 1);
        content.setBackground(Color.black);
        
        // uninitialize graphics, so it can be reset on the next repaint
        final Graphics2D localG2d = g2d;
        g2d = null;
        if (localG2d != null) {
            localG2d.dispose();
        }
    }

    //@Override
    //public void StartTic() {
        /*if (!this.isActive()) {
            return;
        }*/

        //events.processAllPending();
    //}
    
    @Override
    public void setTitle() {
        setTitle(Strings.MOCHA_DOOM_TITLE + " - " + DOOM.bppMode);
    }

    @Override
    public void setMouseLoose() {
        //events.setMouseLoose();
    }

    @Override
    public void setMouseCaptured() {
        //events.setMouseCaptured();
    }
    
    private final boolean showFPS = Engine.getCVM().bool(CommandVariable.SHOWFPS);
    private long lastTime = System.currentTimeMillis();
    private int frames = 0;

    /**
     * Modified update method: no context needs to passed.
     * Will render only internal screens. Common between AWT and Swing  
     */
    @Override
    public void paint(Graphics g) {
        if (!this.isDisplayable()) {
            return;
        }
        
        /**
         * Work on a local copy of the stack - global one can become null at any moment
         */
        Graphics2D localG2d;
        
        /**
         * Techdemo v1.3: Mac OSX fix, compatible with Windows and Linux.
         * Should probably run just once. Overhead is minimal
         * compared to actually DRAWING the stuff.
         */
        if ((localG2d = g2d) == null) {
            g2d = localG2d = (Graphics2D) content.getGraphics();
        }
        
        /**
         * If the game starts too fast, it is possible to raise an exception there
         * We don't want to bother player with "something bad happened"
         * but we wouldn't just be quiet either in case of "something really bad happened"
         * - Good Sign 2017/04/09
         */
        if (localG2d == null) {
            Loggers.getLogger(DoomFrame.class.getName()).log(Level.INFO, "Starting or switching fullscreen, have no Graphics2d yet, skipping paint");
        } else {
            final Image image = DOOM.graphicSystem.getScreenImage();
            localG2d.drawImage(image, X_OFF, Y_OFF, this);
            if (showFPS) {
                ++frames;
                final long now = System.currentTimeMillis();
                final long lambda = now - lastTime;
                if (lambda >= 100L) {
                    setTitle(String.format("%s - %s FPS: %.2f", Strings.MOCHA_DOOM_TITLE, DOOM.bppMode, frames * 1000.0/lambda));
                    frames = 0;
                    lastTime = now;
                }
            }
        }
    }
}
