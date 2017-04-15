package v.graphics;

public interface Lines {
    /**
     * Bresenham's line algorithm modified to use custom Plotter
     * 
     * @param plotter
     * @param x2
     * @param y2 
     */
    default void drawLine(Plotter plotter, int x1, int x2) { drawLine(plotter, x1, x2, 1, 1); }
    default void drawLine(Plotter plotter, int x2, int y2, int dupX, int dupY) {
        // delta of exact value and rounded value of the dependant variable
        int d = 0, dy, dx, ix, iy;
        
        {
            final int x = plotter.getX(), y = plotter.getY();

            dy = Math.abs(y2 - y);
            dx = Math.abs(x2 - x);

            ix = x < x2 ? 1 : -1; // increment direction
            iy = y < y2 ? 1 : -1;
        }
        
        int dy2 = (dy << 1); // slope scaling factors to avoid floating
        int dx2 = (dx << 1); // point
 
        if (dy <= dx) {
            for (;;) {
                plotter.plot();
                if (plotter.getX() == x2)
                    break;
                d += dy2;
                if (d > dx) {
                    plotter.shift(ix, iy);
                    d -= dx2;
                } else plotter.shiftX(ix);
            }
        } else {
            for (;;) {
                plotter.plot();
                if (plotter.getY() == y2)
                    break;
                d += dx2;
                if (d > dy) {
                    plotter.shift(ix, iy);
                    d -= dy2;
                } else plotter.shiftY(iy);
            }
        }
    }
    
}
