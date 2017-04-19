package awt;

import doom.CommandVariable;
import doom.DoomMain;
import i.Game;
import i.Strings;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.VolatileImage;
import javax.swing.JFrame;
import m.Settings;

/**
 * Common code for Doom's video frames
 */
class DoomFrame<V> extends JFrame implements DoomVideoInterface<V> {

    protected static final boolean D = false;
    // This stuff should NOT get through in keyboard events.
    protected static final int UNACCEPTABLE_MODIFIERS =
        (int) (
              InputEvent.ALT_GRAPH_DOWN_MASK
            + InputEvent.META_DOWN_MASK
            + InputEvent.META_MASK
            + InputEvent.WINDOW_EVENT_MASK
            + InputEvent.WINDOW_FOCUS_EVENT_MASK
        );
    
    private static final long serialVersionUID = -4130528877723831825L;

    // Must be aware of "Doom" so we can pass it event messages inside a crude queue.
    public final DoomMain<?, V> DOOM;
    
    /**
     * Dimensions of the screen buffers. The display however, might differ due to e.g. letterboxing
     */
    protected int multiply = 1;

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
    protected final MochaEvents eventhandler;
    protected final DoomEventPoster eventposter;
    protected DisplayMode oldDisplayMode;
    protected DisplayMode currentDisplayMode;
    protected GraphicsDevice device;
    protected Robot robby;
    protected Canvas drawhere;
    protected Point center;
    protected int X_OFF;
    protected int Y_OFF;
    
    DoomFrame(DoomMain<?, V> DOOM) {
        /**
         * just get default screen device, be it 0 or 100500
         *  - Good Sign 2017/04/09
         */
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        this.DOOM = DOOM;
        this.eventhandler = new MochaEvents();
        this.drawhere = new Canvas();

        // Set those here. If fullscreen isn't used, then they won't change.
        // They are necessary for normal initialization, though.
        setDefaultDimension(DOOM.graphicSystem.getScreenWidth(), DOOM.graphicSystem.getScreenHeight());
        this.eventposter = new DoomEventPoster(DOOM, drawhere, eventhandler);
    }

    @Override
    public void SetGamma(int level) {
        if (D) {
            System.err.println("Setting gamma " + level);
        }
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
        this.size = new Dimension(width * multiply, height * multiply);
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

    /**
     * Call this before attempting to draw anything. This will create the window, the canvas and everything. Unlike a
     * simple JFrame, this is not automatic because of the order Doom does things.
     */
    DoomFrame<V> InitGraphics() {
        if (!handleGeom()) {
            DOOM.doomSystem.Error("bad -geom parameter");
        }

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
            DOOM.doomSystem.Error("Error creating AWTDoom frame. Exiting. Reason: %s", e.getMessage());
        }
        
        addListeners(this, drawhere, eventhandler);
        turnOnFrame(this, drawhere, eventhandler);
        setTitle(Strings.MOCHA_DOOM_TITLE + " - " + DOOM.bppMode);
        eventhandler.addEvent(MochaDoomInputEvent.GET_YOUR_ASS_OFF);
        return this;
    }

    private void setCanvasSize(Dimension size) {
        drawhere.setPreferredSize(size);
        drawhere.setBounds(0, 0, drawhere.getWidth() - 1, drawhere.getHeight() - 1);
        drawhere.setBackground(Color.black);
    }

    /**
     * FULLSCREEN SWITCH CODE TODO: it's not enough to do this without also switching the screen's resolution.
     * Unfortunately, Java only has a handful of options which depend on the OS, driver, display, JVM etc. and it's not
     * possible to switch to arbitrary resolutions.
     *
     * Therefore, a "best fit" strategy with centering is used.
     *
     */
    private void switchToFullScreen(final int width, final int height) {
        boolean isFullScreen = device.isFullScreenSupported();
        setUndecorated(isFullScreen);
        setResizable(!isFullScreen);

        // In case we need to revert.
        oldDisplayMode = device.getDisplayMode();

        DisplayModePicker dmp = new DisplayModePicker(device);

        // TODO: what if bit depths are too small?
        DisplayMode dm = dmp.pickClosest(width, height);

        int[] xy = dmp.getCentering(width, height, dm);
        this.X_OFF = xy[0];
        this.Y_OFF = xy[1];

        device.getDisplayModes();

        if (isFullScreen) {
            // Full-screen mode
            device.setFullScreenWindow(this);
            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(dm);
            }
            validate();

            Dimension newsize = new Dimension(dm.getWidth(), dm.getHeight());
            this.setDefaultDimension(dm.getWidth(), dm.getHeight());
            setCanvasSize(newsize);

        } else {
            // Windowed mode
            pack();
            setVisible(true);
        }

    }

    @Override
    public void StartTic() {
        if (!this.isActive()) {
            return;
        }

        //  System.out.println("Getting events...");
        eventposter.ProcessEvents();
        //eventhandler.grabMouse();
    }
    
    @Override
    public VolatileImage obtainVolatileImage(int width, int height) {
        return device.getDefaultConfiguration().createCompatibleVolatileImage(width, height);
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
            g2d = (Graphics2D) drawhere.getGraphics();
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
