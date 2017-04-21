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

import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;
import g.Signals;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is catching events thrown at him by someone, and sends them to underlying DOOM engine.
 * As a way to preserver vanilla until full understand the code, everything inside of underlying engine
 * is still considered black box and changes to it are minimal.
 * But I've tried to make high-level API on top level effective.
 * 
 * For example, we do not need to create some MochaDoomInputEvent for unique combination of AWTEvent and
 * its type - it can be easily switched by ID value from AWTEvent's ow information method. Also, we can
 * certainly know which ScanCodes we will get and what are their minimal and max values, because
 * of using Enum for them (my favorite type of data structure, huh!) 
 * And if we know that ScanCodes can only be something very limited, and every KeyEvent (an AWTEvent for keys)
 * will be translated into one of them, we only have to pre-create two copies of DOOM's event structure
 * for each entry of ScanCode Enum: one for press state, one for release.
 * 
 * Also note: Caps Lock key is handled in a very special way. We do not send him to underlying engine at all.
 * We only save state of it.
 * Also note: it seems there is no way to handle Num Lock itself (however, we can handle keys modified by it)
 * Also note: SysRq / Print Screen key only sends release state, so it has to send press to underlying engine
 * on release or it will be ignored.
 *  - Good Sign 2017/04/21
 * 
 * @author Good Sign
 * @author vekltron
 */
public class DoomEventPoster {
    protected static final int POINTER_WARP_COUNTDOWN = 1;
    private static final boolean D = false;
    
    /**
     * This event here is used as a static scratch copy. When sending out
     * messages, its contents are to be actually copied (struct-like).
     * This avoids the continuous object creation/destruction overhead,
     * And it also allows creating "sticky" status.
     * 
     * As we have now parallel processing of events, one event would eventually
     * overwrite another. So we have to keep thread-local copies of them.
     */
    private final ThreadLocal<event_t.mouseevent_t> mouse_event = new ThreadLocal<event_t.mouseevent_t>() {
        @Override
        protected event_t.mouseevent_t initialValue() {
            return new event_t.mouseevent_t(evtype_t.ev_mouse, 0, 0, 0);
        }
    };
    
    private final DoomMain DM;
    private final Component content;
    private final Point offset = new Point();
    private final Cursor hidden;
    private final Cursor normal;
    private final Robot robby;

    //////// CURRENT MOVEMENT AND INPUT STATUS //////////////
    private volatile int mousedx;
    private volatile int mousedy;
    private volatile int prevmousebuttons;
    private volatile int win_w2, win_h2;

    // Nasty hack for CAPS LOCK. Apparently, there's no RELIABLE way
    // to get the caps lock state programmatically, so we have to make 
    // do with simply toggling
    private volatile boolean capstoggle = false;
    private volatile boolean ignorebutton = false;

    public DoomEventPoster(DoomMain DM, Component content) {
        this.DM = DM;
        this.content = content;

        // AWT: create cursors.
        this.normal = content.getCursor();
        this.hidden = createInvisibleCursor();

        // Create AWT Robot for forcing mouse
        {
            Robot robot;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                Logger.getLogger(DoomEventPoster.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                System.err.println("AWT Robot could not be created, mouse input focus will be loose!");
                robot = null;
            }
            this.robby = robot;
        }
    }

    /**
     * NASTY hack to hide the cursor.
     * 
     * Create a 'hidden' cursor by using a transparent image
     * ...return the invisible cursor
     */
    private Cursor createInvisibleCursor() {
        Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);
        BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);
        Cursor hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(1, 1), "HiddenCursor");
        return hiddenCursor;
    }

	/**
     * Update relative position offset, and force mouse there.
	 */
    void reposition() {
        offset.x = content.getLocationOnScreen().x;
        offset.y = content.getLocationOnScreen().y;
        // Shamelessly ripped from Jake 2. Maybe it works better?
        Component c = this.content;
        //offset.x = 0;
        //offset.y = 0;
        win_w2 = c.getWidth() / 2;
        win_h2 = c.getHeight() / 2;

        if (robby != null) {
            robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
        }

        content.getInputContext().selectInputMethod(java.util.Locale.US);
        content.setCursor(hidden);
        if (D) {
            System.err.printf("Jake 2 method: offset MOVED to %d %d\n", offset.x, offset.y);
        }
    }
    
    public void sendEvent(Signals.ScanCode sc, int eventType) {
        final event_t.mouseevent_t mouseEvent = mouse_event.get();
        if (eventType == KeyEvent.KEY_PRESSED) {
            keyPress(sc, mouseEvent);
        } else if (eventType == KeyEvent.KEY_RELEASED) {
            keyRelease(sc, mouseEvent);
        }
    }
    
    public void sendEvent(AWTEvent X_event) {
        final event_t.mouseevent_t mouseEvent = mouse_event.get();
        // Keyboard events get priority vs mouse events.
        // In the case of combined input, however, we need
        if (!ignorebutton) {
            switch (X_event.getID()) {
                case KeyEvent.KEY_PRESSED: {
                    keyPress(Signals.getScanCode((KeyEvent) X_event), mouseEvent);
                    break;
                } case KeyEvent.KEY_RELEASED: {
                    keyRelease(Signals.getScanCode((KeyEvent) X_event), mouseEvent);
                    break;
                } case KeyEvent.KEY_TYPED: {
                    DM.PostEvent(Signals.getScanCode((KeyEvent) X_event).doomEventUp);

                    if (prevmousebuttons != 0) {
                        // Allow combined mouse/keyboard events.
                        mouseEvent.buttons = prevmousebuttons;
                        DM.PostEvent(mouseEvent);
                    }
                    break;
                }
            }
        }

        // Mouse events are also handled, but with secondary priority.
        ProcessMouse(X_event, mouseEvent);
        // Now for window events. This includes the mouse breaking the border.
        ProcessWindow(X_event); 
    }

    private void ProcessMouse(AWTEvent X_event, final event_t.mouseevent_t mouseEvent) {
        final MouseEvent MEV;
        final Point tmp;
        // Ignore ALL mouse events if we are moving the window.
        switch (X_event.getID()) {
            // ButtonPress
            case MouseEvent.MOUSE_PRESSED:
                MEV = (MouseEvent) X_event;
                mouseEvent.type = evtype_t.ev_mouse;
                mouseEvent.buttons = prevmousebuttons
                        = (MEV.getButton() == MouseEvent.BUTTON1 ? event_t.MOUSE_LEFT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? event_t.MOUSE_RIGHT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? event_t.MOUSE_MID : 0);
                mouseEvent.x = mouseEvent.y = 0;
                DM.PostEvent(mouseEvent);
                break;
            // ButtonRelease
            // This must send out an amended event.
            case MouseEvent.MOUSE_RELEASED:
                MEV = (MouseEvent) X_event;
                mouseEvent.type = evtype_t.ev_mouse;
                mouseEvent.buttons = prevmousebuttons
                        ^= (MEV.getButton() == MouseEvent.BUTTON1 ? event_t.MOUSE_LEFT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? event_t.MOUSE_RIGHT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? event_t.MOUSE_MID : 0);
                // A PURE mouse up event has no movement.
                mouseEvent.x = mouseEvent.y = 0;
                DM.PostEvent(mouseEvent);
                break;
            case MouseEvent.MOUSE_MOVED:
                MEV = (MouseEvent) X_event;
                tmp = MEV.getPoint();
                //this.AddPoint(tmp,center);
                mouseEvent.type = evtype_t.ev_mouse;
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);

                // A pure move has no buttons.
                mouseEvent.buttons = prevmousebuttons = 0;
                mouseEvent.x = (mousedx) << 2;
                mouseEvent.y = (mousedy) << 2;

                if ((mouseEvent.x | mouseEvent.y) != 0) {
                    DM.PostEvent(mouseEvent);
                }
                break;

            case MouseEvent.MOUSE_DRAGGED:
                MEV = (MouseEvent) X_event;
                tmp = MEV.getPoint();
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);
                mouseEvent.type = evtype_t.ev_mouse;

                // A drag means no change in button state.
                mouseEvent.buttons = prevmousebuttons;
                mouseEvent.x = (mousedx) << 2;
                mouseEvent.y = (mousedy) << 2;

                if ((mouseEvent.x | mouseEvent.y) != 0) {
                    DM.PostEvent(mouseEvent);
                }
                break;
        }
    }

    private void keyRelease(final Signals.ScanCode sc, final event_t.mouseevent_t mouseEvent) {
        if (sc == Signals.ScanCode.SC_PRTSCRN) {
            DM.PostEvent(sc.doomEventDown);
        }
        
        if ((sc != Signals.ScanCode.SC_CAPSLK) || ((sc == Signals.ScanCode.SC_CAPSLK) && capstoggle)) {
            DM.PostEvent(sc.doomEventUp);
        }
        
        capstoggle = false;
        
        if (prevmousebuttons != 0) {
            // Allow combined mouse/keyboard events.
            mouseEvent.buttons = prevmousebuttons;
            DM.PostEvent(mouseEvent);
        }
    }

    private void keyPress(final Signals.ScanCode sc, final event_t.mouseevent_t mouseEvent) {
        // Toggle, but don't it go through.
        if (sc == Signals.ScanCode.SC_CAPSLK) {
            capstoggle = true;
        }
        
        if (sc != Signals.ScanCode.SC_CAPSLK) {
            DM.PostEvent(sc.doomEventDown);
        }
        
        if (prevmousebuttons != 0) {
            // Allow combined mouse/keyboard events.
            mouseEvent.buttons = prevmousebuttons;
            DM.PostEvent(mouseEvent);
        }
    }
    
    protected volatile boolean we_are_moving = false;

    private void ProcessWindow(AWTEvent X_event) {
        switch (X_event.getID()) {
            case MouseEvent.MOUSE_CLICKED:
                // Marks the end of a move. A press + release during a move will
                // trigger a "click" event, which is handled specially.
                if (we_are_moving) {
                    we_are_moving = false;
                    reposition();
                    ignorebutton = false;
                }
                break;
            case WindowEvent.WINDOW_LOST_FOCUS:
            case MouseEvent.MOUSE_EXITED:
                // Forcibly clear events                 
                DM.PostEvent(event_t.CANCEL_MOUSE);
                DM.PostEvent(event_t.CANCEL_KEYS);
                content.setCursor(normal);
                ignorebutton = true;
                break;

            case ComponentEvent.COMPONENT_MOVED:
                // Don't try to reposition immediately during a move
                // event, wait for a mouse click.
                we_are_moving = true;
                ignorebutton = true;
                // Forcibly clear events                 
                DM.PostEvent(event_t.CANCEL_MOUSE);
                DM.PostEvent(event_t.CANCEL_KEYS);
                break;
            case MouseEvent.MOUSE_ENTERED:
            case WindowEvent.WINDOW_GAINED_FOCUS:
                we_are_moving = false;
                //reposition();
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_ACTIVATED:
            case WindowEvent.WINDOW_DEICONIFIED:
            case ComponentEvent.COMPONENT_RESIZED:
                // All events that have to do with the window being changed,
                // moved etc. should go here. The most often result
                // in focus being lost and position being changed, so we
                // need to take charge.
                DM.justfocused = true;
                content.requestFocus();
                reposition();
                ignorebutton = false;
                break;
            default:
                // NOT NEEDED in AWT if (doShm && X_event.type == X_shmeventtype) shmFinished = true;
                break;

        }
        
        // If the mouse moved, don't wait until it managed to get out of the 
        // window to bring it back.
        if (!we_are_moving && (mousedx != 0 || mousedy != 0)) {
            // move the mouse to the window center again
            if (robby != null) {
                robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
            }
        }

        mousedx = mousedy = 0; // don't spaz.
    }
}
