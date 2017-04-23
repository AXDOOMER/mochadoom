package awt;

import awt.DoomVideoInterface.DoomListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

/**
 * After extensive refactoring, most of the stuff gone out. Mostly, it only does add into queue everything he gets.
 * Another purpose is to block mouse presses on some move events. But it seems to be bugged and should be tested
 *  - Good Sign 2017/04/21
 * 
 * An alternate events class, more suited for handling the complex situations that might arise during daily use, window
 * moving, etc. Use instead of the old AWTEvents, which is present but deprecated.
 *
 * @author vekltron
 */
final class MochaEvents extends ConcurrentEvents implements DoomListener {
    private final DoomEventPoster poster;
    
    MochaEvents(DoomEventPoster poster) {
        super(poster::sendEvent);
        this.poster = poster;
    }

    @Override
    void setMouseCaptured() {
        poster.setMouseCaptured();
    }

    @Override
    void setMouseLoose() {
        poster.setMouseLoose();
    }
    
    ///////////////////// WINDOW STUFF //////////////////////
    @Override public void windowActivated(WindowEvent windowevent) { eventQueue.offer(windowevent); }
    @Override public void windowClosed(WindowEvent windowevent) {}
    @Override public void windowClosing(WindowEvent windowevent) {}
    @Override public void windowDeactivated(WindowEvent windowevent) { eventQueue.clear(); } // Clear the queue if focus is lost.
    @Override public void windowDeiconified(WindowEvent windowevent) { eventQueue.offer(windowevent); }
    @Override public void windowIconified(WindowEvent windowevent) { eventQueue.clear(); }
    @Override public void windowOpened(WindowEvent windowevent) { eventQueue.offer(windowevent); }
    
    //////////// KEYS LISTENERS //////////
    @Override public void keyPressed(KeyEvent e) { eventQueue.offer(e); e.consume(); }
    @Override public void keyReleased(KeyEvent e) { eventQueue.offer(e); e.consume(); }
    @Override public void keyTyped(KeyEvent e) { eventQueue.offer(e); e.consume(); }

    //////////////////////////// MOUSE EVENTS ////////////////////////////
    @Override public void mouseClicked(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mouseEntered(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mouseExited(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mousePressed(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mouseReleased(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mouseDragged(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void mouseMoved(MouseEvent mouseevent) { eventQueue.offer(mouseevent); }
    @Override public void componentHidden(ComponentEvent e) {} // Do what, here? Pausing would be a good idea.
    @Override public void componentMoved(ComponentEvent e) { eventQueue.offer(e);}
    @Override public void componentResized(ComponentEvent e) { eventQueue.offer(e); }
    @Override public void componentShown(ComponentEvent e) { eventQueue.offer(e); }
    @Override public void windowGainedFocus(WindowEvent e) { eventQueue.offer(e); }
    @Override public void windowLostFocus(WindowEvent e) { eventQueue.offer(e); }
    @Override public boolean dispatchKeyEvent(KeyEvent e) { return false; }
}
