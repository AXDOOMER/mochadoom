package awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

public class DoomCanvas extends Canvas {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void paint(Graphics g){
        
        g.setColor(new Color(.3f, .4f, .5f, .6f));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
    }
}
