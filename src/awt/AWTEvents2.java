package awt;

import static g.Keys.*;
import i.DoomEventInterface;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.KeyEventDispatcher;
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

public class AWTEvents2 implements WindowListener,KeyEventDispatcher,KeyListener,MouseListener,MouseMotionListener,DoomEventInterface {

    // modifications of eventQueue must be thread safe!
    public LinkedList<DoomEvent> eventQueue = new LinkedList<DoomEvent>();

    public DoomMain DM;
    public Component canvas;
    Robot robby;

    public AWTEvents2(DoomMain DM, Component canvas) {
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

        relockMouse(mouseevent);
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
        //addEvent(mouseevent);
        mouseMoved(mouseevent);
        
    }

    ArrayList<Point[]> cancelMoves = new ArrayList<Point[]>();
    
    Point lastMousePos = new Point(0,0);
    
    @Override
    public void mouseMoved(MouseEvent mouseevent) {
        DoomEvent dEv = new DoomEvent(mouseevent);
        Point srcPos = lastMousePos;
        Point destPos = mouseevent.getLocationOnScreen();
        Point deltaPoint = new Point(destPos.x-srcPos.x, destPos.y-srcPos.y);
        dEv.deltaPoint = deltaPoint;
        addEvent(dEv);

        relockMouse(mouseevent);
    }
    
    public void relockMouse(MouseEvent mouseevent) {
        if (canvas.hasFocus()) {
            Point destPoint = new Point(canvas.getX()+canvas.getWidth()/2, canvas.getY()+canvas.getHeight()/2);

            canvas.removeMouseListener(this);
            robby.mouseMove(destPoint.x, destPoint.y);
            canvas.addMouseListener(this);
        }
        
        lastMousePos = MouseInfo.getPointerInfo().getLocation();
    }
    
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        return false;
    }

    @Override
    public void windowActivated(WindowEvent windowevent) {
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
        // TODO Auto-generated method stub
        
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

    
    public void keyPressed(KeyEvent e) {
        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (isAcceptableKey(new DoomEvent(e))) {
            addEvent(e);
        }
        
        
        e.consume();
    }

    public void keyReleased(KeyEvent e) {
        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (isAcceptableKey(new DoomEvent(e))) {
            addEvent(e);
        }
        
        e.consume();
    }

    public void keyTyped(KeyEvent e) {
    //  if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0){
        if (isAcceptableKey(new DoomEvent(e))) {
            addEvent(e);
        }
    }
    
    
    int AWTMouseToKey(DoomEvent ev) {
        int buttonIdx = ev.getButton() - MouseEvent.BUTTON1;
        return KEY_MOUSE1+buttonIdx;
    }
    
    // number of total 'button' inputs, include keyboard keys, plus virtual
    // keys (mousebuttons and joybuttons becomes keys)
    public static final int NUMKEYS         = 256;

    public static final int MOUSEBUTTONS    = 8;
    public static final int JOYBUTTONS      = 14;  // 10 bases + 4 hat
    
    //
    // mouse and joystick buttons are handled as 'virtual' keys
    //
    public static final int KEY_MOUSE1        = NUMKEYS;                  
    public static final int KEY_JOY1          = KEY_MOUSE1+MOUSEBUTTONS;  
    public static final int KEY_DBLMOUSE1     = KEY_JOY1+JOYBUTTONS;        // double clicks
    public static final int KEY_DBLJOY1       = KEY_DBLMOUSE1+MOUSEBUTTONS;
    public static final int KEY_2MOUSE1       = KEY_DBLJOY1+JOYBUTTONS;
    public static final int KEY_DBL2MOUSE1    = KEY_2MOUSE1+MOUSEBUTTONS;
    public static final int KEY_MOUSEWHEELUP  = KEY_DBL2MOUSE1+MOUSEBUTTONS;
    public static final int KEY_MOUSEWHEELDOWN= KEY_MOUSEWHEELUP+1;
    public static final int KEY_2MOUSEWHEELUP = KEY_MOUSEWHEELUP+2;
    public static final int KEY_2MOUSEWHEELDOWN= KEY_MOUSEWHEELUP+3;
    public static final int NUMINPUTS = KEY_MOUSEWHEELUP+4;
    
    int currentMouseButtons = 0;
    
    @Override
    public void GetEvent() {
        DoomEvent X_event;
        event_t event=new event_t();;
        // put event-grabbing stuff in here
        if (eventQueue.isEmpty()) return;   
        X_event=nextEvent();
        //System.out.println("Event type:"+X_event.getID());

        // Check for Event.??? shit
        if (!ignorebutton){
            switch (X_event.getID())
            {
            case Event.KEY_PRESS:
                event=new event_t(evtype_t.ev_keydown, xlatekey(/*(KeyEvent)*/X_event));
                DM.PostEvent(event);
                //System.err.println("k");
                break;
            case Event.KEY_RELEASE:
                event=new event_t(evtype_t.ev_keyup, xlatekey(/*(KeyEvent)*/X_event));
                DM.PostEvent(event);
                //System.err.println( "ku");
                break;
            }
        }

        switch (X_event.getID()){
        // ButtonPress
        case Event.MOUSE_DOWN:
            /*
            currentMouseButtons = (X_event.getButton() == MouseEvent.BUTTON1 ? 1 : 0)
            | ((X_event.getButton() == MouseEvent.BUTTON3) ? 2 : 0)
            | ((X_event.getButton() == MouseEvent.BUTTON2) ? 4 : 0);*/


            event.data1 = AWTMouseToKey(X_event);
            event.data2 = event.data3 = 0;
            event.type=evtype_t.ev_keydown;//ev_mouse;
            DM.PostEvent(event);
            break;
            // ButtonRelease
        case Event.MOUSE_UP:
            event.type = evtype_t.ev_keyup;// ev_mouse;
/*
            currentMouseButtons = currentMouseButtons
            & ~(X_event.getButton() == MouseEvent.BUTTON1 ? 1 : 0)
            & ~(X_event.getButton() == MouseEvent.BUTTON3 ? 2 : 0)
            & ~(X_event.getButton() == MouseEvent.BUTTON2 ? 4 : 0);*/

            event.data1 = AWTMouseToKey(X_event);
            event.data2 = event.data3 = 0;
            DM.PostEvent(event);
            break;
            // MotionNotify:
        case Event.MOUSE_MOVE:
        case Event.MOUSE_DRAG:
            event.type = evtype_t.ev_mouse;
            // Get buttons, as usual.
            event.data1 = currentMouseButtons;
            event.data2 = X_event.deltaPoint.x << 2;
            event.data3 = -X_event.deltaPoint.y << 2;

            if ((event.data2 | event.data3)!=0) {
                DM.PostEvent(event);
            }
            break;
        case Event.MOUSE_ENTER:
            System.err.println("ACCEPTING keyboard input");
            canvas.requestFocus();
            canvas.setCursor(hidden);
            ignorebutton=false;
            break;
        case Event.MOUSE_EXIT:
            System.err.println("IGNORING keyboard input");
            canvas.setCursor(normal);

            ignorebutton=true;
            break;
        case Event.WINDOW_EXPOSE:
            // No real single equivalent for "ConfigureNotify"
        case Event.WINDOW_MOVED:
        case Event.WINDOW_DESTROY:
            break;
        default:
            // NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
            break;

        }

    }

    
    boolean isAcceptableKey(DoomEvent e) {
        return e.getKeyCode()<=KeyEvent.VK_F12 || e.getKeyChar()=='#';
    }
    

    void addEvent(AWTEvent ev) {
        addEvent(new DoomEvent(ev));

        if (ev instanceof InputEvent) {
            ((InputEvent)ev).consume();
        }
    }

    void addEvent(DoomEvent ev) {
        synchronized (eventQueue) {
            eventQueue.addLast(ev);
        }
    }

    public DoomEvent nextEvent() {
        DoomEvent ev;
        synchronized (eventQueue) {
            ev = (!eventQueue.isEmpty())?(DoomEvent)eventQueue.removeFirst():null;
        }
        return ev;
    }
    
    public static class DoomEvent {
        int keyCode;
        char keyChar;
        int ID;
        int button;
        int x;
        int y;
        Point deltaPoint;
        
        public DoomEvent(AWTEvent evt) {
            this.ID = evt.getID();
            if (evt instanceof KeyEvent) {
                this.keyChar = ((KeyEvent)evt).getKeyChar();
                this.keyCode = ((KeyEvent)evt).getKeyCode();
            }
            if (evt instanceof MouseEvent) {
                this.button = ((MouseEvent)evt).getButton();
                this.x = ((MouseEvent)evt).getX();
                this.y = ((MouseEvent)evt).getY();
            }
        }

        public int getKeyCode() {
            return keyCode;
        }

        public char getKeyChar() {
            return keyChar;
        }

        public int getID() {
            return ID;
        }

        public int getButton() {
            return button;
        }
        
        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    
    /** FIXME: input must be made scancode dependent rather than VK_Dependent,
     *  else a lot of things just don't work. 
     * 
     * @param e
     * @return
     */

    public int xlatekey(DoomEvent e)
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
    
    // This stuff should NOT get through in keyboard events.
    protected final int UNACCEPTABLE_MODIFIERS=(int) (InputEvent.ALT_GRAPH_DOWN_MASK+
                                             InputEvent.META_DOWN_MASK+
                                             InputEvent.META_MASK+
                                             InputEvent.WINDOW_EVENT_MASK+
                                             InputEvent.WINDOW_FOCUS_EVENT_MASK);

    protected boolean ignorebutton;

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
      

      Cursor hidden;
      Cursor normal;

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
}
