package awt;

import doom.CommandVariable;
import doom.DoomMain;
import i.Game;
import i.Strings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Point;
import javax.swing.JFrame;
import m.Settings;

/**
 * Common code for Doom's video frames
 */
public class DoomFrame<V> extends JFrame implements DoomVideoInterface<V> {
    private static final long serialVersionUID = -4130528877723831825L;

    // Must be aware of "Doom" so we can pass it event messages inside a crude queue.
    public final DoomMain<?, V> DOOM;
    protected volatile Graphics2D g2d;

    /**
     * Normally only used in fullscreen mode
     * This might differ from the raster's width & height attribute for number of reasons
     */
    protected Dimension size;
    
    /**
     * Separate event handler a la _D_.
     * However I won't make it fully "eternity like" yet
     * also because it works quite flakey on Linux.
     */
    protected final ConcurrentEvents events;
    protected final DisplayModePicker dmp;
    protected final GraphicsDevice device;
    protected DisplayMode oldDisplayMode;
    protected DisplayMode displayMode;
    protected Component content;
    protected Point center;
    protected int X_OFF;
    protected int Y_OFF;
    
    /**
     * Very generic JFrame. Along that it only initializes various properties of Doom Frame.
     * @param DOOM
     * @param content - a pane or canvas
     * @param device - graphics device
     */
    protected DoomFrame(DoomMain<?, V> DOOM, Component content, GraphicsDevice device, ConcurrentEvents events) {
        if (!handleGeom()) {
            DOOM.doomSystem.Error("bad -geom parameter");
        }

        this.device = device;
        this.DOOM = DOOM;
        this.content = content;
        this.events = events;
        this.dmp = new DisplayModePicker(device);

        // Set those here. If fullscreen isn't used, then they won't change.
        // They are necessary for normal initialization, though.
        setDefaultDimension(DOOM.graphicSystem.getScreenWidth(), DOOM.graphicSystem.getScreenHeight());

        /**
         * AWT: create the canvas.
         * MAES: this method works even on "stubborn" Linux distros that fuck up the window size.
         */
        try {
            setCanvasSize(size);
            if (DOOM.CM.equals(Settings.fullscreen, Boolean.TRUE)) {
                switchToFullScreen(DOOM.graphicSystem.getScreenWidth(), DOOM.graphicSystem.getScreenHeight());
            }
        } catch (Exception e) {
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
     * Default window size and center spot. These might change upon entering full screen, so don't consider them
     * absolute. Due to letterboxing and screen doubling, stretching etc. they might be different that the screen buffer
     * (typically, larger).
     */
    private void setDefaultDimension(int width, int height) {
        this.size = new Dimension(width, height);
        this.center = new Point(X_OFF + size.width / 2, Y_OFF + size.height / 2);
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

    private void setCanvasSize(Dimension size) {
        content.setPreferredSize(size);
        content.setBounds(0, 0, content.getWidth() - 1, content.getHeight() - 1);
        content.setBackground(Color.black);
    }

    @Override
    public void switchFullscreen() {
        // remove the frame from view
        g2d.dispose();
        dispose();
        // uninitialize graphics, so it can be reset on the next repaint
        g2d = null;
        // change all the properties
        switchToFullScreen(DOOM.graphicSystem.getScreenWidth(), DOOM.graphicSystem.getScreenHeight());
        // now show back the frame
        showFrame(this);
    }

    /**
     * FULLSCREEN SWITCH CODE TODO: it's not enough to do this without also switching the screen's resolution.
     * Unfortunately, Java only has a handful of options which depend on the OS, driver, display, JVM etc. and it's not
     * possible to switch to arbitrary resolutions.
     *
     * Therefore, a "best fit" strategy with centering is used.
     */
    private void switchToFullScreen(final int width, final int height) {
        // In case we need to revert.
        oldDisplayMode = device.getDisplayMode();
        

        // TODO: what if bit depths are too small?
        displayMode = dmp.pickClosest(width, height);
        setModeOffset(dmp, width, height);
        switchToMode();
    }

    private void switchToMode() {
        boolean isFullScreen = device.isFullScreenSupported();
        setUndecorated(isFullScreen);
        setResizable(!isFullScreen);

        device.getDisplayModes();
        if (isFullScreen) {
            // Full-screen mode
            device.setFullScreenWindow(this);
            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(displayMode);
            }
            validate();
            final Dimension newsize = new Dimension(displayMode.getWidth(), displayMode.getHeight());
            this.setDefaultDimension(displayMode.getWidth(), displayMode.getHeight());
            setCanvasSize(newsize);

        } else {
            // Windowed mode
            pack();
            setVisible(true);
        }
    }

    private void setModeOffset(DisplayModePicker dmp, final int width, final int height) {
        int[] xy = dmp.getCentering(width, height, displayMode);
        this.X_OFF = xy[0];
        this.Y_OFF = xy[1];
    }

    @Override
    public void StartTic() {
        if (!this.isActive()) {
            return;
        }

        //  System.out.println("Getting events...");
        events.processAllPending();
        //eventhandler.grabMouse();
    }
    
    @Override
    public void setTitle() {
        setTitle(Strings.MOCHA_DOOM_TITLE + " - " + DOOM.bppMode);
    }
    
    private final boolean showFPS = Game.getCVM().bool(CommandVariable.SHOWFPS);
    private long lastTime = System.currentTimeMillis();
    private int frames = 0;

    /**
     * Modified update method: no context needs to passed.
     * Will render only internal screens. Common between AWT and Swing  
     */
    @Override
    public void paint(Graphics g) {
        /**
         * Techdemo v1.3: Mac OSX fix, compatible with Windows and Linux.
         * Should probably run just once. Overhead is minimal
         * compared to actually DRAWING the stuff.
         */
        if (g2d == null) {
            g2d = (Graphics2D) content.getGraphics();
        }
        
        /**
         * If the game starts too fast, it is possible to raise an exception there
         * We don't want to bother player with "something bad happened"
         * but we wouldn't just be quiet either in case of "something really bad happened"
         * - Good Sign 2017/04/09
         */
        if (g2d == null) {
            System.out.println("Starting too fast, haven't got Graphics2D yet, skipping paint");
        } else {
            final Image image = DOOM.graphicSystem.getScreenImage();
            g2d.drawImage(image, X_OFF, Y_OFF, this);
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
