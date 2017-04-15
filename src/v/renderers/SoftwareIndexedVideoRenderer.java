package v.renderers;

import java.awt.image.IndexColorModel;
import m.MenuMisc;
import v.graphics.Palettes;
import v.scale.VideoScale;
import v.tables.BlurryTable;
import v.tables.GammaTables;

abstract class SoftwareIndexedVideoRenderer extends SoftwareGraphicsSystem<byte[], byte[]> {

    /**
     * Indexed renderers keep separate color models for each colormap (intended as gamma levels) and palette levels
     */
    protected final IndexColorModel[][] cmaps = new IndexColorModel[GammaTables.LUT_GAMMA.length][Palettes.NUM_PALETTES];
    protected final BlurryTable blurryTable;

    SoftwareIndexedVideoRenderer(VideoScale vs, byte[] playpal, byte[][] colormap) {
        super(vs, byte[].class, playpal, colormap);
        /**
         * create gamma levels
         * Now we can reuse existing array of cmaps, not allocating more memory
         * each time we change gamma or pick item
         */
        cmapIndexed(cmaps, playpal);
        blurryTable = new BlurryTable(colormap);
    }

    @Override public int getBaseColor(byte color) { return color; }
    @Override public byte[] convertPalettedBlock(byte... src) { return src; }

    @Override
    public BlurryTable getBlurryTable() {
        return blurryTable;
    }

    @Override
    public boolean writeScreenShot(String name, DoomScreen screen) {
        // munge planar buffer to linear
        //DOOM.videoInterface.ReadScreen(screens[screen.ordinal()]);
        MenuMisc.WritePNGfile(name, screens.get(screen), width, height, cmaps[usegamma][usepalette]);
        return true;
    }
}
