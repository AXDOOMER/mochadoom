package awt;

import static awt.MochaDoomInputEvent.*;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

/**
 * An alternate events class, more suited for handling the complex situations that might arise during daily use, window
 * moving, etc. Use instead of the old AWTEvents, which is present but deprecated.
 *
 * @author vekltron
 */
class MochaEvents extends ConcurrentEvents {
    MochaEvents() {}

    /////////////////////    WINDOW STUFF //////////////////////
    @Override
    public void windowActivated(WindowEvent windowevent) {
        if (D) {
            System.err.println("Window activated");
        }
        eventQueue.add(EV_CONFIGURE_NOTIFY);
    }

    @Override public void windowClosed(WindowEvent windowevent) {}
    @Override public void windowClosing(WindowEvent windowevent) {}

    @Override
    public void windowDeactivated(WindowEvent windowevent) {
        // Clear the queue if focus is lost.
        eventQueue.clear();
    }

    @Override
    public void windowDeiconified(WindowEvent windowevent) {
        eventQueue.add(EV_CONFIGURE_NOTIFY);
    }

    @Override
    public void windowIconified(WindowEvent windowevent) {
        eventQueue.clear();
    }

    @Override
    public void windowOpened(WindowEvent windowevent) {
        eventQueue.add(EV_CREATE_NOTIFY);
    }
    
    ////////////LISTENERS //////////
    @Override
    public void keyPressed(KeyEvent e) {
        // //  updateLockingKeys();
        // if (! lockingKeyStates.containsKey(e.getKeyCode())) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            eventQueue.add(new MochaDoomInputEvent(KEY_PRESS, e));
        }

        e.consume();
        // }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // updateLockingKeys();
        // if (! lockingKeyStates.containsKey(e.getKeyCode())) {

        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            eventQueue.add(new MochaDoomInputEvent(KEY_RELEASE, e));
        }
        e.consume();
        // }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //updateLockingKeys();
        //if (! lockingKeyStates.containsKey(e.getKeyCode())) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            eventQueue.add(new MochaDoomInputEvent(KEY_TYPE, e));
        }

        e.consume();
        //}
    }

    //////////////////////////// MOUSE EVENTS   ////////////////////////////
    @Override
    public void mouseClicked(MouseEvent mouseevent) {
        //System.out.println("Mouse clicked");
        eventQueue.add(EV_MOUSE_CLICKED);
    }

    @Override
    public void mouseEntered(MouseEvent mouseevent) {
        //System.out.println("Mouse entered");
        eventQueue.add(EV_MOUSE_ENTERED);
    }

    @Override
    public void mouseExited(MouseEvent mouseevent) {
        //System.out.println("Mouse exited");
        eventQueue.add(EV_MOUSE_EXITED);
    }

    @Override
    public void mousePressed(MouseEvent mouseevent) {
        if (!we_are_moving) {// Don't let presses go through when moving.
            eventQueue.add(new MochaDoomInputEvent(BUTTON_PRESS, mouseevent));
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseevent) {
        if (!we_are_moving) { // Don't let presses go through when moving.
            eventQueue.add(new MochaDoomInputEvent(BUTTON_RELEASE, mouseevent));
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseevent) {
        eventQueue.add(new MochaDoomInputEvent(DRAG_NOTIFY, mouseevent));
    }

    @Override
    public void mouseMoved(MouseEvent mouseevent) {
        eventQueue.add(new MochaDoomInputEvent(MOTION_NOTIFY, mouseevent));
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // Do what, here? Pausing would be a good idea.
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        eventQueue.add(EV_WINDOW_MOVING);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        eventQueue.add(EV_CONFIGURE_NOTIFY);
    }

    @Override
    public void componentShown(ComponentEvent e) {
        eventQueue.add(EV_CREATE_NOTIFY);
    }

    @Override
    public void windowGainedFocus(WindowEvent arg0) {
        eventQueue.add(EV_FOCUS_GAINED);
    }

    @Override
    public void windowLostFocus(WindowEvent arg0) {
        eventQueue.add(EV_FOCUS_LOST);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        //if (e.getKeyCode() == KeyEvent.VK_TAB)
        //	eventQueue.add(new MochaDoomInputEvent(KeyRelease, e));
        return false;
    }
    
    ///////////////////////// KEYBOARD EVENTS ///////////////////////////////////
    // UNUSED used commented out because it doesn't appear to work
    // I think that there is no bulletproof method to poll the state of
    // the CAPS LOCK key, and forcing a call to getLockingKeyState()
    // results in an UnsupportedOperationException
    // For now, the best course of action seems to be intercepting toggling only,
    // rather than state. It also looks like a compicated thing to
    // run at every key update.
    /*private void updateLockingKeys() {
        Toolkit toolkit = canvas.getToolkit();
        for (Iterator<Map.Entry<Integer, Boolean>> it =
                 lockingKeyStates.entrySet().iterator();
             it.hasNext();
             ) {
            Map.Entry<Integer, Boolean> entry = it.next(); 
            Integer keyCode = entry.getKey();
            Boolean oldState = entry.getValue();
            try {
            	if (D) System.err.println("Trying");
                boolean newState = toolkit.getLockingKeyState(keyCode);
                if (! Boolean.valueOf(newState).equals(oldState)) {
                	if (D) System.out.println("New event");
                    int eventType =
                        newState ? LOCK_ON
                                 : LOCK_OFF;
                    eventQueue.add(new MochaDoomInputEvent(eventType,null));
                    entry.setValue(newState);
                }
            } catch (UnsupportedOperationException ex) {
                // Key not present
                it.remove();
            }
        }
    }*/
}
