package awt;

import doom.CommandVariable;
import doom.DoomMain;
import doom.event_t;
import i.Game;
import i.InputListener;
import java.awt.Canvas;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.im.InputContext;
import java.awt.image.VolatileImage;
import java.util.StringTokenizer;
import javax.swing.JFrame;

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
    InputContext getInputContext();
    VolatileImage obtainVolatileImage(int width, int height);
    
    static <V> DoomVideoInterface<V> createAWTInterface(final DoomMain<?, V> DOOM) {
        return new DoomFrame<>(DOOM).InitGraphics();
    }
    
    /**
     * Add eventHandler listeners to JFrame and its Canvas elememt
     */
    default void addListeners(final JFrame frame, final Canvas canvas, final MochaEvents eventhandler) {
        canvas.addKeyListener(eventhandler);
        canvas.addMouseListener(eventhandler);
        canvas.addMouseMotionListener(eventhandler);
        frame.addComponentListener(eventhandler);
        frame.addWindowFocusListener(eventhandler);
        frame.addWindowListener(eventhandler);
    }
    
    default void turnOnFrame(final JFrame frame, final Canvas drawhere, final MochaEvents eventhandler) {
        /**
         * AWT: tab is a special case :-/
         * We need to "peg" it to the JFrame, rather than the canvas,
         * and the handler itself cannot auto-assign it.
         */
        final KeyEventDispatcher dispatcher = new DoomKeyDispatcher(eventhandler, drawhere);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);

        frame.add(drawhere);
        // this.add(gelatine);
        frame.getContentPane().setPreferredSize(drawhere.getPreferredSize());

        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        /**
         * Set it to be later then setResizable to avoid extra space on right and bottom
         *  - Good Sign 2017/04/09
         * 
         * JFrame's size is auto-set here.
         */
        frame.pack();
        
        // Gently tell the eventhandler to wake up and set itself.	  
        frame.requestFocus();
        SetGamma(0);
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
    
    default String processEvents() {
        final StringBuilder tmp = new StringBuilder();
        event_t event;
        while ((event = InputListener.nextEvent()) != null) {
            tmp.append(event.type.ordinal()).append("\n");
        }
        return tmp.toString();
    }
}
