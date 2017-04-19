/*
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package awt;

import static awt.MochaDoomInputEvent.*;
import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;
import static g.Keys.KEY_ALT;
import static g.Keys.KEY_BACKSPACE;
import static g.Keys.KEY_BRCLOSE;
import static g.Keys.KEY_BROPEN;
import static g.Keys.KEY_BSLASH;
import static g.Keys.KEY_CAPSLOCK;
import static g.Keys.KEY_COMMA;
import static g.Keys.KEY_CTRL;
import static g.Keys.KEY_DOWNARROW;
import static g.Keys.KEY_END;
import static g.Keys.KEY_ENTER;
import static g.Keys.KEY_ESCAPE;
import static g.Keys.KEY_F1;
import static g.Keys.KEY_F10;
import static g.Keys.KEY_F11;
import static g.Keys.KEY_F12;
import static g.Keys.KEY_F2;
import static g.Keys.KEY_F3;
import static g.Keys.KEY_F4;
import static g.Keys.KEY_F5;
import static g.Keys.KEY_F6;
import static g.Keys.KEY_F7;
import static g.Keys.KEY_F8;
import static g.Keys.KEY_F9;
import static g.Keys.KEY_HOME;
import static g.Keys.KEY_LEFTARROW;
import static g.Keys.KEY_MULTPLY;
import static g.Keys.KEY_NUMLOCK;
import static g.Keys.KEY_PAUSE;
import static g.Keys.KEY_PERIOD;
import static g.Keys.KEY_PGDN;
import static g.Keys.KEY_PGUP;
import static g.Keys.KEY_PRNTSCRN;
import static g.Keys.KEY_QUOTE;
import static g.Keys.KEY_RIGHTARROW;
import static g.Keys.KEY_SCROLLLOCK;
import static g.Keys.KEY_SEMICOLON;
import static g.Keys.KEY_SHIFT;
import static g.Keys.KEY_TAB;
import static g.Keys.KEY_UPARROW;
import i.Game;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.StreamSupport;
import m.Settings;

/**
 * ///////////////////// QUEUE HANDLING ///////////////////////
 * @author vekltron
 */
public class DoomEventPoster {
    protected static final int POINTER_WARP_COUNTDOWN = 1;
    private static final boolean D = false;
    private static final int INPUT_THREADS = Game.getConfig().getValue(Settings.parallelism_input, Integer.class);
    private static final ForkJoinPool pool = INPUT_THREADS > 0 ? new ForkJoinPool(INPUT_THREADS) : null;

    private final DoomMain DM;
    private final Component canvas;
    private final MochaEvents me;

    //////// CURRENT MOVEMENT AND INPUT STATUS //////////////
    protected int lastmousex;
    protected int lastmousey;
    protected Point lastmouse;
    protected int mousedx;
    protected int mousedy;
    protected boolean mousemoved;
    protected boolean grabMouse = true;
    protected int doPointerWarp = POINTER_WARP_COUNTDOWN;

    // This event here is used as a static scratch copy. When sending out
    // messages, its contents are to be actually copied (struct-like).
    // This avoids the continuous object creation/destruction overhead,
    // And it also allows creating "sticky" status.
    private final event_t event = new event_t();
    // Special FORCED and PAINFUL key and mouse cancel event.
    private final event_t cancelkey = new event_t(evtype_t.ev_clear, 0xFF, 0, 0);
    private final event_t cancelmouse = new event_t(evtype_t.ev_mouse, 0, 0, 0);

    private int prevmousebuttons;

    // Nasty hack for CAPS LOCK. Apparently, there's no RELIABLE way
    // to get the caps lock state programmatically, so we have to make 
    // do with simply toggling
    private boolean capstoggle = false;

    private int win_w2, win_h2;
    private int move = 0;
    Robot robby;
    Cursor hidden;
    Cursor normal;
    Point offset = new Point();

    public DoomEventPoster(DoomMain DM, Canvas canvas, MochaEvents me) {
        this.DM = DM;
        this.canvas = canvas;

        // AWT: create cursors.
        this.normal = canvas.getCursor();
        this.hidden = this.createInvisibleCursor();

        // Create AWT Robot for forcing mouse
        try {
            robby=new Robot();
        } catch (Exception e){
           System.err.println("AWT Robot could not be created, mouse input focus will be loose!");
        }

        this.me = me;
    }

    /**
     * NASTY hack to hide the cursor.
     * 
     * Create a 'hidden' cursor by using a transparent image
     * ...return the invisible cursor
     */

    protected Cursor createInvisibleCursor() {
        Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);
        BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);
        Cursor hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(1, 1), "HiddenCursor");
        return hiddenCursor;
    }

	/**
     * Update relative position offset, and force mouse there.
	 */
    void reposition() {
        offset.x = (int) (canvas.getLocationOnScreen().x);
        offset.y = (int) (canvas.getLocationOnScreen().y);
        // Shamelessly ripped from Jake 2. Maybe it works better?
        Component c = this.canvas;
        //offset.x = 0;
        //offset.y = 0;
        win_w2 = c.getWidth() / 2;
        win_h2 = c.getHeight() / 2;

        robby.mouseMove(offset.x + win_w2, offset.y + win_h2);

        canvas.getInputContext().selectInputMethod(java.util.Locale.US);
        canvas.setCursor(hidden);
        if (D) {
            System.err.printf("Jake 2 method: offset MOVED to %d %d\n", offset.x, offset.y);
        }
    }
    
    public void ProcessEvents() {
        if (INPUT_THREADS > 0) {
            pool.submit(() -> StreamSupport.stream(me, true).forEach(this::GetEvent));
        } else {
            StreamSupport.stream(me, false).forEach(this::GetEvent);
        }
    }

    public void GetEvent(MochaDoomInputEvent X_event) {
        MouseEvent MEV;
        Point tmp;
        // Unlike most keys, caps lock etc. can be polled, so no need to worry
        // about them getting stuck.  So they are re-polled after all other
        // key states have beeen cleared.
        /*  if (DM.shouldPollLockingKeys()) {
            for (Map.Entry<Integer, Boolean> e: lockingKeyStates.entrySet()) {
                e.setValue(null);
            }
            updateLockingKeys();
        } */
        // put event-grabbing stuff in here
        //System.out.println("Event type:"+X_event.getID());

        // Keyboard events get priority vs mouse events.
        // In the case of combined input, however, we need
        if (!me.ignorebutton) {
            switch (X_event.type) {
                case KEY_PRESS: {
                    event.type = evtype_t.ev_keydown;
                    event.data1 = xlatekey((KeyEvent) X_event.ev, -1);

                    // Toggle, but don't it go through.
                    if (event.data1 == KEY_CAPSLOCK) {
                        capstoggle = true;
                    }

                    if (event.data1 != KEY_CAPSLOCK) {
                        DM.PostEvent(event);
                    }

                    if (prevmousebuttons != 0) {

                        // Allow combined mouse/keyboard events.
                        event.data1 = prevmousebuttons;
                        event.type = evtype_t.ev_mouse;
                        DM.PostEvent(event);
                    }
                    //System.err.println("k");
                    break;
                }

                case KEY_RELEASE:
                    event.type = evtype_t.ev_keyup;
                    event.data1 = xlatekey((KeyEvent) X_event.ev, -1);

                    if ((event.data1 != KEY_CAPSLOCK)
                            || ((event.data1 == KEY_CAPSLOCK) && capstoggle)) {
                        DM.PostEvent(event);
                    }

                    capstoggle = false;

                    if (prevmousebuttons != 0) {

                        // Allow combined mouse/keyboard events.
                        event.data1 = prevmousebuttons;
                        event.type = evtype_t.ev_mouse;
                        DM.PostEvent(event);
                    }
                    //System.err.println( "ku");
                    break;

                case KEY_TYPE:
                    event.type = evtype_t.ev_keyup;
                    event.data1 = xlatekey((KeyEvent) X_event.ev, -1);
                    DM.PostEvent(event);

                    if (prevmousebuttons != 0) {

                        // Allow combined mouse/keyboard events.
                        event.data1 = prevmousebuttons;
                        event.type = evtype_t.ev_mouse;
                        DM.PostEvent(event);
                    }
                    //System.err.println( "ku");
                    break;
            }
        }

        // Ignore ALL mouse events if we are moving the window.
        // Mouse events are also handled, but with secondary priority.
        switch (X_event.type) {
            // ButtonPress
            case BUTTON_PRESS:
                MEV = (MouseEvent) X_event.ev;
                event.type = evtype_t.ev_mouse;
                event.data1 = prevmousebuttons
                        = (MEV.getButton() == MouseEvent.BUTTON1 ? 1 : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? 2 : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? 4 : 0);
                event.data2 = event.data3 = 0;

                DM.PostEvent(event);
                //System.err.println( "b");
                break;

            // ButtonRelease
            // This must send out an amended event.
            case BUTTON_RELEASE:
                MEV = (MouseEvent) X_event.ev;
                event.type = evtype_t.ev_mouse;
                event.data1 = prevmousebuttons
                        ^= (MEV.getButton() == MouseEvent.BUTTON1 ? 1 : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? 2 : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? 4 : 0);
                // A PURE mouse up event has no movement.
                event.data2 = event.data3 = 0;
                DM.PostEvent(event);
                //System.err.println("bu");
                break;
            case MOTION_NOTIFY:
                MEV = (MouseEvent) X_event.ev;
                tmp = MEV.getPoint();
                //this.AddPoint(tmp,center);
                event.type = evtype_t.ev_mouse;
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);

                // A pure move has no buttons.
                event.data1 = prevmousebuttons = 0;
                event.data2 = (mousedx) << 2;
                event.data3 = (mousedy) << 2;

                // System.out.printf("Mouse MOVED to %d %d\n", lastmousex, lastmousey);
                //System.out.println("Mouse moved without buttons: "+event.data1);
                if ((event.data2 | event.data3) != 0) {

                    DM.PostEvent(event);
                    //System.err.println( "m");
                    mousemoved = true;
                } else {
                    mousemoved = false;
                }
                break;

            case DRAG_NOTIFY:
                MEV = (MouseEvent) X_event.ev;
                tmp = MEV.getPoint();
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);
                event.type = evtype_t.ev_mouse;

                // A drag means no change in button state.
                event.data1 = prevmousebuttons;
                event.data2 = (mousedx) << 2;
                event.data3 = (mousedy) << 2;

                if ((event.data2 | event.data3) != 0) {
                    DM.PostEvent(event);
                    //System.err.println( "m");
                    mousemoved = true;
                } else {
                    mousemoved = false;
                }
                break;
        }

        ProcessMouse(X_event); 
    }

    private void ProcessMouse(MochaDoomInputEvent X_event) {
        // Now for window events. This includes the mouse breaking the border.
        switch (X_event.type) {
            case MOUSE_CLICKED:
                // Marks the end of a move. A press + release during a move will
                // trigger a "click" event, which is handled specially.
                if (me.we_are_moving) {
                    me.we_are_moving = false;
                    reposition();
                    me.ignorebutton = false;
                }
                break;
            case FOCUS_LOST:
            case MOUSE_EXITED:
                // Forcibly clear events                 
                DM.PostEvent(cancelmouse);
                DM.PostEvent(cancelkey);
                canvas.setCursor(normal);
                me.ignorebutton = true;
                break;

            case WINDOW_MOVING:
                // Don't try to reposition immediately during a move
                // event, wait for a mouse click.
                me.we_are_moving = true;
                me.ignorebutton = true;
                // Forcibly clear events                 
                DM.PostEvent(cancelmouse);
                DM.PostEvent(cancelkey);
                move++;
                break;
            case MOUSE_ENTERED:
            case FOCUS_GAINED:
                me.we_are_moving = false;
                //reposition();
            case CONFIGURE_NOTIFY:
            case CREATE_NOTIFY:
                // All events that have to do with the window being changed,
                // moved etc. should go here. The most often result
                // in focus being lost and position being changed, so we
                // need to take charge.
                DM.justfocused = true;
                canvas.requestFocus();
                reposition();
                me.ignorebutton = false;
                break;
            default:
                // NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
                break;

        }
        
        // If the mouse moved, don't wait until it managed to get out of the 
        // window to bring it back.
        if (!me.we_are_moving && (mousedx != 0 || mousedy != 0)) {
            // move the mouse to the window center again
            robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
        }

        mousedx = mousedy = 0; // don't spaz.
    }
    
    
    /**
     * FIXME: input must be made scancode dependent rather than VK_Dependent,
     * else a lot of things just don't work. 
     * 
     * @param e
     * @param rc 
     * @return
     */
    public int xlatekey(KeyEvent e, int rc)
    {

        if (e != null) rc = e.getKeyCode();
        switch(rc)

        //Event.XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))

        {
        case KeyEvent.VK_PRINTSCREEN:   rc = KEY_PRNTSCRN; 
        case KeyEvent.VK_COMMA:   rc = KEY_COMMA; break;
        case KeyEvent.VK_PERIOD:   rc = KEY_PERIOD; break;
        case KeyEvent.VK_QUOTE:   rc = KEY_QUOTE; break;
        case KeyEvent.VK_SEMICOLON:   rc = KEY_SEMICOLON; break;
        case KeyEvent.VK_OPEN_BRACKET:   rc = KEY_BROPEN; break;
        case KeyEvent.VK_CLOSE_BRACKET:   rc = KEY_BRCLOSE; break;
        case KeyEvent.VK_BACK_SLASH:   rc = KEY_BSLASH; break;
        case KeyEvent.VK_MULTIPLY:   rc = KEY_MULTPLY; break;
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
        case KeyEvent.VK_END: rc= KEY_END;	break;
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

        case KeyEvent.VK_TAB: rc = KEY_TAB;       break;
        case KeyEvent.VK_CAPS_LOCK: rc = KEY_CAPSLOCK; break;
        case KeyEvent.VK_NUM_LOCK: rc = KEY_NUMLOCK; break;
        case KeyEvent.VK_SCROLL_LOCK: rc = KEY_SCROLLLOCK; break;
        /*
        case KeyEvent.KEY_RELEASED:
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
                rc = KEY_RSHIFT;
                break;

            case KeyEvent.VK_CONTROL:
                rc = KEY_RCTRL;
                break;                           

            case KeyEvent.VK_ALT:
                rc = KEY_RALT;
                break;

            case KeyEvent.VK_SPACE:
                rc = ' ';
                break;

            }
            */

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
            break;
        }

        //System.out.println("Typed "+e.getKeyCode()+" char "+e.getKeyChar()+" mapped to "+Integer.toHexString(rc));
        return rc;//Math.min(rc,KEY_F12);
    }
}
