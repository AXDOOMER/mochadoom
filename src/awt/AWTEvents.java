package awt;

import static g.Keys.*;
import i.DoomEventInterface;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;

import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;

/** A very basic event handling class, directly based on linuxdoom's.
 *  Sadly, it doesn't work so well cross-platform. Use MochaEvents instead.
 * 
 * @author velktron
 *
 */

@Deprecated
public class AWTEvents implements WindowListener,KeyEventDispatcher,KeyListener,MouseListener,MouseMotionListener,DoomEventInterface {

    // modifications of eventQueue must be thread safe!
    static LinkedList<AWTEvent> eventQueue = new LinkedList<AWTEvent>();

    //// STATUS STUFF ///////////
    public final DoomMain DM;
    public final Component canvas;
    Robot robby;
    Cursor hidden;
    Cursor normal;

    //////// CURRENT MOVEMENT AND INPUT STATUS //////////////

    protected static final int POINTER_WARP_COUNTDOWN = 1;
    protected int lastmousex;
    protected int lastmousey;
    protected Point lastmouse;
    protected int mousedx;
    protected int mousedy;
    protected boolean mousemoved;
    protected boolean ignorebutton;
    protected boolean grabMouse=true;
    protected int doPointerWarp=POINTER_WARP_COUNTDOWN;


    public AWTEvents(DoomMain DM, Component canvas) {
        this.DM = DM;
        this.canvas = canvas;

        // AWT: create cursors.
        this.normal=canvas.getCursor();
        this.hidden=this.createInvisibleCursor();

        // Create AWT Robot for forcing mouse
        try {
            robby=new Robot();
        } catch (Exception e){
            System.out.println("AWT Robot could not be created, mouse input focus will be loose!");
        }
        
    }

    //////////// LISTENERS //////////

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {

        return false;
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {  
            addEvent(e);            
        }

        e.consume();
    }

    public void keyReleased(KeyEvent e) {

        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {
            addEvent(e);
        }
        e.consume();
    }

    public void keyTyped(KeyEvent e) {
        if (e.getKeyCode()<=KeyEvent.VK_F12) {
            addEvent(e);
        }

        e.consume();        
    }

    ///////////////////// MOUSE EVENTS   ///////////////////////

    @Override
    public void mouseClicked(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mouseEntered(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mouseExited(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mousePressed(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mouseReleased(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mouseDragged(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }

    @Override
    public void mouseMoved(MouseEvent mouseevent) {
        addEvent(mouseevent);

    }


    ///////////////////// QUEUE HANDLING ///////////////////////

    static void addEvent(AWTEvent ev) {
        synchronized (eventQueue) {
            eventQueue.addLast(ev);
        }
    }

    public static AWTEvent nextEvent() {
        AWTEvent ev;
        synchronized (eventQueue) {
            ev = (!eventQueue.isEmpty())?(AWTEvent)eventQueue.removeFirst():null;
        }
        return ev;
    }

    // This event here is used as a static scratch copy. When sending out
    // messages, its contents are to be actually copied (struct-like).
    // This avoids the continuous object creation/destruction overhead,
    // And it also allows creating "sticky" status.

    final event_t event=new event_t();
    // Special FORCED and PAINFUL key and mouse cancel event.
    final event_t cancelkey=new event_t(evtype_t.ev_clear,0xFF,0,0);
    final event_t cancelmouse=new event_t(evtype_t.ev_mouse,0,0,0);
    int prevmousebuttons;
    
    @Override
    public void GetEvent() {
        AWTEvent X_event;
        MouseEvent MEV;
        Point tmp;

        // put event-grabbing stuff in here
        if (eventQueue.isEmpty()) return;   
        X_event=nextEvent();
        //System.out.println("Event type:"+X_event.getID());

        // Keyboard events get priority vs mouse events.
        // In the case of combined input, however, we need
        if (!ignorebutton){
            switch (X_event.getID())
            {
            case Event.KEY_PRESS: {

                event.type=evtype_t.ev_keydown;
                event.data1=xlatekey((KeyEvent)X_event);
                DM.PostEvent(event);
                if (prevmousebuttons!=0){
                    
                // Allow combined mouse/keyboard events.
                event.data1=prevmousebuttons;
                event.type=evtype_t.ev_mouse;
                DM.PostEvent(event);
                }
                //System.err.println("k");
                break;
            }
            case Event.KEY_RELEASE:
                event.type=evtype_t.ev_keyup;
                event.data1=xlatekey((KeyEvent)X_event);
                DM.PostEvent(event);
                
                if (prevmousebuttons!=0){
                    
                    // Allow combined mouse/keyboard events.
                    event.data1=prevmousebuttons;
                    event.type=evtype_t.ev_mouse;
                    DM.PostEvent(event);
                    }
                //System.err.println( "ku");
                break;
            }
        }

        // Mouse events are also handled, but with secondary priority.
        switch (X_event.getID()){
        // ButtonPress
        case Event.MOUSE_DOWN:
            MEV=(MouseEvent)X_event;
            event.type=evtype_t.ev_mouse;
            event.data1 = prevmousebuttons=
                (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
                (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
                (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
            event.data2 = event.data3 = 0;

            DM.PostEvent(event);
            //System.err.println( "b");
            break;

            // ButtonRelease
            // This must send out an amended event.

        case Event.MOUSE_UP:
            MEV=(MouseEvent)X_event;
            event.type = evtype_t.ev_mouse;
            event.data1 =prevmousebuttons^= 
                (MEV.getButton() == MouseEvent.BUTTON1 ? 1: 0) |
                (MEV.getButton() == MouseEvent.BUTTON2 ? 2: 0)|
                (MEV.getButton() == MouseEvent.BUTTON3 ? 4: 0);
            // A PURE mouse up event has no movement.
            event.data2 = event.data3 = 0;
            DM.PostEvent(event);
            //System.err.println("bu");
            break;
            // MotionNotify:
        case Event.WINDOW_MOVED:
        	// Moving the window does change the absolute reference point, while events only
        	// give a RELATIVE one.
        	offset.x=(int) (canvas.getLocationOnScreen().x);
        	offset.y=(int) (canvas.getLocationOnScreen().y);
        	//System.out.printf("Center MOVED to %d %d\n", center.x, center.y);
        case Event.MOUSE_MOVE:
            MEV=(MouseEvent)X_event;
            tmp=MEV.getPoint();
            //this.AddPoint(tmp,center);
            event.type = evtype_t.ev_mouse;
            this.mousedx=tmp.x-this.lastmousex;
            this.mousedy=this.lastmousey-tmp.y;
            this.lastmousex=tmp.x;
            this.lastmousey=tmp.y;                 
            MEV=(MouseEvent)X_event;
            // A pure move has no buttons.
            event.data1=prevmousebuttons=0;
            event.data2 = (mousedx) << 3;
            event.data3 = (mousedy) << 3;

           // System.out.printf("Mouse MOVED to %d %d\n", lastmousex, lastmousey);
            //System.out.println("Mouse moved without buttons: "+event.data1);
            if ((event.data2 | event.data3)!=0)
            {

                DM.PostEvent(event);
                //System.err.println( "m");
                mousemoved = true;
            } else
            {
                mousemoved = false;
            }
            break;


        case Event.MOUSE_DRAG:
            MEV=(MouseEvent)X_event;
            tmp=MEV.getPoint();
            //this.AddPoint(tmp,center);
            this.mousedx=tmp.x-this.lastmousex;
            this.mousedy=this.lastmousey-tmp.y;
            this.lastmousex=tmp.x;
            this.lastmousey=tmp.y;
           // System.out.printf("Mouse MOVED to %d %d\n", lastmousex, lastmousey);

            event.type = evtype_t.ev_mouse;
            // Get buttons, as usual.
            // Well, NOT as usual: in case of a drag, the usual 
            // mousebutton flags don't work. A "drag" means that at
            // least the 1st mouse button is held down, while the other
            // two might or might not be so.
            event.data1 = prevmousebuttons;
                //(MEV.getModifiers() == MouseEvent.BUTTON1_DOWN_MASK ? 1: 0)|
                //(MEV.getModifiers() == MouseEvent.BUTTON2_DOWN_MASK ? 2: 0)|
                //(MEV.getModifiers() == MouseEvent.BUTTON3_DOWN_MASK ? 4: 0);
            event.data2 = (mousedx) << 3;
            event.data3 = (mousedy) << 3;

            //System.out.printf("Mouse DRAGGED to %d %d\n", mousedx, mousedy);
            //System.out.println("Mouse moved with buttons pressed: "+event.data1);
            if ((event.data2 | event.data3)!=0)
            {
                DM.PostEvent(event);
                //System.err.println( "m");
                mousemoved = false;
            } else
            {
                mousemoved = true;
            }
            break;
        case Event.MOUSE_ENTER:
            //System.err.println("ACCEPTING keyboard input");
            canvas.requestFocus();
            canvas.setCursor(hidden);
        	offset.x=(int) (canvas.getLocationOnScreen().x);
        	offset.y=(int) (canvas.getLocationOnScreen().y);
        	System.out.printf("Offset MOVED to %d %d\n", offset.x, offset.y);
            this.grabMouse();
            ignorebutton=false;
            break;
        case Event.MOUSE_EXIT:
            // Forcibly clear events                 

            DM.PostEvent(cancelmouse);
            DM.PostEvent(cancelkey);
            reposition();
        	System.out.printf("FORCED and PAINFUL event clearing!\n");
            canvas.setCursor(normal);             
            ignorebutton=true;
            break;
        case Event.WINDOW_EXPOSE:
        	offset.x=(int) (canvas.getLocationOnScreen().x);
        	offset.y=(int) (canvas.getLocationOnScreen().y);
        	System.out.printf("Center MOVED to %d %d\n", offset.x, offset.y);
            // No real single equivalent for "ConfigureNotify"

        case Event.WINDOW_DESTROY:
            break;
        default:
            // NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
            break;

        }

    }

	/** Update relative position offset, and force mouse there.
	 * 
	 */
	void reposition() {
		lastmousex=canvas.getWidth()/2;
		lastmousey=canvas.getHeight()/2;
		offset.x=(int) (canvas.getLocationOnScreen().x);
		offset.y=(int) (canvas.getLocationOnScreen().y);
	}
    
    Point offset=new Point();

    /** Returns true if flags are included in arg.
     * Synonymous with (flags & arg)!=0
     * 
     * @param flags
     * @param arg
     * @return
     */
    public static final boolean flags(int flags, int arg){
        return ((flags & arg)!=0);
    }


    /////////////////////    WINDOW STUFF //////////////////////


    @Override
    public void windowActivated(WindowEvent windowevent) {
        System.out.println("Window activated");
        canvas.getInputContext().selectInputMethod(java.util.Locale.US);

    }

    @Override
    public void windowClosed(WindowEvent windowevent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowClosing(WindowEvent windowevent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowDeactivated(WindowEvent windowevent) {
        // Clear the queue if focus is lost.
        System.out.println("Eventqueue flushed!");
        eventQueue.clear();

    }

    @Override
    public void windowDeiconified(WindowEvent windowevent) {
        canvas.getInputContext().selectInputMethod(java.util.Locale.US);

    }

    @Override
    public void windowIconified(WindowEvent windowevent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowOpened(WindowEvent windowevent) {
        canvas.getInputContext().selectInputMethod(java.util.Locale.US);

    }

    /**
     * NASTY hack to hide the cursor.
     * 
     * Create a 'hidden' cursor by using a transparent image
     * ...return the invisible cursor
     */

    protected Cursor createInvisibleCursor()

    {
        Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);
        BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);
        Cursor hiddenCursor = Toolkit.getDefaultToolkit( ).createCustomCursor(transparentImage, new Point(1, 1),    "HiddenCursor");
        return hiddenCursor;
    }

    public void grabMouse() {
        // Warp the pointer back to the middle of the window
        //  or it will wander off - that is, the game will
        //  loose input focus within X11.
        if (grabMouse)
        {
            if (doPointerWarp--<=0)
            {
                // Don't warp back if we deliberately alt-tabbed away.
                Point p=canvas.getMousePosition();
                if (p!=null){
                	
                	
                   robby.mouseMove(	offset.x+canvas.getWidth()/2,
                		   offset.y+canvas.getHeight()/2);
                    lastmousex=/*center.x+*/canvas.getWidth()/2;
                    lastmousey=/*center.y+*/canvas.getHeight()/2;
                    //System.out.printf("Mouse FORCED back to %d %d\n", lastmousex, lastmousey);
                }
                doPointerWarp = POINTER_WARP_COUNTDOWN;
            } 

        }

        mousemoved = false;

    }

    /** FIXME: input must be made scancode dependent rather than VK_Dependent,
     *  else a lot of things just don't work. 
     * 
     * @param e
     * @return
     */

    public int xlatekey(KeyEvent e)
    {

        int rc;

        switch(rc =     e.getKeyCode())

        //Event.XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))

        {
        case KeyEvent.VK_LEFT:    rc = KEY_LEFTARROW; break;
        case KeyEvent.VK_RIGHT:   rc = KEY_RIGHTARROW;    break;
        case KeyEvent.VK_DOWN:    rc = KEY_DOWNARROW; break;
        case KeyEvent.VK_UP:  rc = KEY_UPARROW;   break;
        case KeyEvent.VK_ESCAPE:  rc = KEY_ESCAPE;    break;
        case KeyEvent.VK_ENTER:   rc = KEY_ENTER;     break;
        case KeyEvent.VK_CONTROL: rc= KEY_CTRL; break;
        case KeyEvent.VK_ALT: rc=KEY_ALT; break;
        case KeyEvent.VK_SHIFT: rc=KEY_SHIFT; break;
        // Added handling of pgup/pgdown/home etc.
        case KeyEvent.VK_PAGE_DOWN: rc= KEY_PGDN;	break;
        case KeyEvent.VK_PAGE_UP: rc= KEY_PGUP;	break;
        case KeyEvent.VK_HOME: rc= KEY_HOME;	break;
        case KeyEvent.VK_F1:  rc = KEY_F1;        break;
        case KeyEvent.VK_F2:  rc = KEY_F2;        break;
        case KeyEvent.VK_F3:  rc = KEY_F3;        break;
        case KeyEvent.VK_F4:  rc = KEY_F4;        break;
        case KeyEvent.VK_F5:  rc = KEY_F5;        break;
        case KeyEvent.VK_F6:  rc = KEY_F6;        break;
        case KeyEvent.VK_F7:  rc = KEY_F7;        break;
        case KeyEvent.VK_F8:  rc = KEY_F8;        break;
        case KeyEvent.VK_F9:  rc = KEY_F9;        break;
        case KeyEvent.VK_F10: rc = KEY_F10;       break;
        case KeyEvent.VK_F11: rc = KEY_F11;       break;
        case KeyEvent.VK_F12: rc = KEY_F12;       break;
        case KeyEvent.VK_BACK_SPACE:
        case KeyEvent.VK_DELETE:  rc = KEY_BACKSPACE; break;
        case KeyEvent.VK_PAUSE:   rc = KEY_PAUSE;     break;
        case KeyEvent.KEY_RELEASED:
        case KeyEvent.VK_TAB: rc = KEY_TAB;       break;

        case KeyEvent.KEY_PRESSED:
            switch(e.getKeyCode()){

            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS: 
                rc = KEY_EQUALS;    
                break;

            case (13):
            case KeyEvent.VK_SUBTRACT: 
            case KeyEvent.VK_MINUS:   
                rc = KEY_MINUS;     
                break;

            case KeyEvent.VK_SHIFT:
                rc = KEY_SHIFT;
                break;

            case KeyEvent.VK_CONTROL:
                rc = KEY_CTRL;
                break;                           

            case KeyEvent.VK_ALT:
                rc = KEY_ALT;
                break;

            case KeyEvent.VK_SPACE:
                rc = ' ';
                break;

            }

        default:

            /*if (rc >= KeyEvent.VK_SPACE && rc <= KeyEvent.VK_DEAD_TILDE)
            {
            rc = (int) (rc - KeyEvent.FOCUS_EVENT_MASK + ' ');
            break;
            } */
            if (rc >= KeyEvent.VK_A && rc <= KeyEvent.VK_Z){
                rc = rc-KeyEvent.VK_A +'a';
                break;
            }
            // Unknown. Probably fucking up with the keyboard locale. Switch to be sure.
            // Sorry for this horrible hack, but Java can't read locale-free keycodes -_-
            // this.getInputContext().selectInputMethod(java.util.Locale.US);
            //if (rc>KEY_F12) rc=KEY_RSHIFT;
            break;
        }

        //System.out.println("Typed "+e.getKeyCode()+" char "+e.getKeyChar()+" mapped to "+Integer.toHexString(rc));

        // Sanitize. Nothing beyond F12 must pass through, else you will
        // get the "all cheats" bug.
        return rc;//Math.min(rc,KEY_F12);

    }

    protected void AddPoint(Point A, Point B){
    	A.x+=B.x;
    	A.y+=B.y;
    }
    
}