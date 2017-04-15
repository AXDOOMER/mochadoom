package v.renderers;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import v.scale.VideoScale;

class BufferedRenderer extends SoftwareIndexedVideoRenderer {
    private final WritableRaster[] rasters = new WritableRaster[SCREENS_COUNT];

    /**
     * This actually creates a raster with a fixed underlying array, but NOT the images themselves. So it's possible to
     * have "imageless" rasters (unless you specifically request to make them visible, of course).
     */
    BufferedRenderer(VideoScale vs, byte[] pal, byte[][] colormap) {
        super(vs, pal, colormap);
        for (DoomScreen s: DoomScreen.values()) {
            final int index = s.ordinal();
            // Only create non-visible data, pegged to the raster. Create visible images only on-demand.
            final DataBufferByte db = (DataBufferByte) newBuffer(s);
            // should be fully compatible with IndexColorModels from SoftwareIndexedVideoRenderer
            rasters[index] = Raster.createInterleavedRaster(db, width, height, width, 1, new int[]{0}, new Point(0, 0));
        }
        // Thou shalt not best nullt!!! Sets currentscreen
        forcePalette();
    }

    /**
     * Clear the screenbuffer so when the whole screen will be recreated palettes will too
     * These screens represent a complete range of palettes for a specific gamma and specific screen
     */
    @Override
    public final void forcePalette() {
        this.currentscreen = new BufferedImage(cmaps[usegamma][usepalette], rasters[DoomScreen.FG.ordinal()], true, null);
    }
}

//$Log: BufferedRenderer.java,v $
//Revision 1.18  2012/09/24 17:16:23  velktron
//Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
//Revision 1.17.2.3  2012/09/24 16:56:06  velktron
//New hierarchy, less code repetition.
//
