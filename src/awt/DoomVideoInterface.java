package awt;

import doom.CommandVariable;
import doom.DoomMain;
import i.Game;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.im.InputContext;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** 
 *  Methods specific to Doom-System video interfacing. 
 *  In essence, whatever you are using as a final system-specific way to display
 *  the screens, should be able to respond to these commands. In particular,
 *  screen update requests must be honored, and palette/gamma request changes
 *  must be intercepted before they are forwarded to the renderers (in case they
 *  are system-specific, rather than renderer-specific).
 *  
 *  The idea is that the final screen rendering module sees/handles as less as
 *  possible, and only gets a screen to render, no matter what depth it is.
 */

public interface DoomVideoInterface<V> {
    void dispose();
	void StartTic();
	void SetPalette(int palette);
	void FinishUpdate();
	void SetGamma(int gammalevel);
    void setTitle();
    void switchFullscreen();
    void setMouseLoose();
    void setMouseCaptured();
    InputContext getInputContext();
    
    /**
     * Get an instance of JFrame to draw anything. This will try to create compatible Canvas and
     * will bing all AWT listeners
     */
    static <V> DoomVideoInterface<V> createAWTInterface(final DoomMain<?, V> DOOM) {
        final GraphicsDevice device = getDevice();
        final Component content = new Canvas(device.getDefaultConfiguration());
        final MochaEvents listener = new MochaEvents(new DoomEventPoster(DOOM, content));
        final DoomFrame<V> frame = new DoomFrame<>(DOOM, content, device, listener);
        init(frame, content, listener);
        return frame;
    }
    
    /**
     * Get an instance of JFrame to draw anything. This will try to create compatible Canvas and
     * will bing all AWT listeners
     */
    static <V> DoomVideoInterface<V> createSwingInterface(final DoomMain<?, V> DOOM) {
        final GraphicsDevice device = getDevice();
        final JComponent content = new JPanel(true);
        final DoomEventPoster poster = new DoomEventPoster(DOOM, content);
        final MochaEvents listener = new MochaEvents(poster);
        final DoomFrame<V> frame = new DoomFrame<>(DOOM, content, device, listener);
        init(frame, content, listener);
        return frame;
    }

    static <V> void init(final DoomFrame<V> frame, final Component content, final DoomListener listener) {
        /**
         * This should fix Tab key
         *  - Good Sign 2017/04/21
         */
        frame.setFocusTraversalKeysEnabled(false);
        content.setFocusTraversalKeysEnabled(false);

        /**
         * Add eventHandler listeners to JFrame and its Canvas elememt
         */
        content.addKeyListener(listener);        
        content.addMouseListener(listener);
        content.addMouseMotionListener(listener);
        frame.addComponentListener(listener);
        frame.addWindowFocusListener(listener);
        frame.addWindowListener(listener);
        frame.turnOnFrame(frame, frame.content);
        frame.showFrame(frame);
        frame.SetGamma(0);
    }
    
    /**
     * just get default screen device, be it 0 or 100500
     *  - Good Sign 2017/04/09
     */
    static GraphicsDevice getDevice() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
    
    default void turnOnFrame(final JFrame frame, final Component content) {
        frame.add(content);
        frame.getContentPane().setPreferredSize(content.getPreferredSize());

        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle();
    }

    default void showFrame(final JFrame frame) {
        /**
         * Set it to be later then setResizable to avoid extra space on right and bottom
         *  - Good Sign 2017/04/09
         * 
         * JFrame's size is auto-set here.
         */
        frame.pack();
        frame.setVisible(true);
        // Gently tell the eventhandler to wake up and set itself.	  
        frame.requestFocus();
    }
    
    /**
     * Incomplete. Only checks for -geom format
     */
    default boolean handleGeom() {
        int x = 0;
        int y = 0;
        
        // warning: char format, different type arg
        int xsign = ' ';
        int ysign = ' ';
        /*
        String displayname;
        String d;
        int n;
        int pnum;
        
        boolean oktodraw;
        long attribmask;
        
        // Try setting the locale the US, otherwise there will be problems
        // with non-US keyboards.
        if (this.getInputContext() == null || !this.getInputContext().selectInputMethod(java.util.Locale.US)) {
            System.err.println("Could not set the input context to US! Keyboard input will be glitchy!");
        } else {
            System.err.println("Input context successfully set to US.");
        }
        
        // check for command-line display name
        displayname = Game.getCVM().get(CommandVariable.DISP, String.class, 0).orElse(null);
        
        // check for command-line geometry*/
        if (Game.getCVM().present(CommandVariable.GEOM)) {
            try {
                String eval = Game.getCVM().get(CommandVariable.GEOM, String.class, 0).get().trim();
                // warning: char format, different type arg 3,5
                //n = sscanf(myargv[pnum+1], "%c%d%c%d", &xsign, &x, &ysign, &y);
                // OK, so we have to read a string that may contain
                // ' '/'+'/'-' and a number. Twice.
                StringTokenizer tk = new StringTokenizer(eval, "-+ ");
                // Signs. Consider positive.
                xsign = 1;
                ysign = 1;
                for (int i = 0; i < eval.length(); i++) {
                    if (eval.charAt(i) == '-') {
                        // First '-' on trimmed string: negagive
                        if (i == 0) {
                            xsign = -1;
                        } else {
                            ysign = -1;
                        }
                    }
                }
                
                //this should parse two numbers.
                if (tk.countTokens() == 2) {
                    x = xsign * Integer.parseInt(tk.nextToken());
                    y = ysign * Integer.parseInt(tk.nextToken());
                }
                
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * An interface to combine all needed listeners plus provide method to stop answering other mouse events
     * when it is moving and a function to process all queued events
     *
     * @author Good Sign
     */
    public static interface DoomListener extends
            WindowListener,
            ComponentListener,
            KeyListener,
            MouseListener,
            MouseMotionListener,
            WindowFocusListener,
            KeyEventDispatcher
    {}
}
