package v.tables;

import doom.DoomMain;
import static p.MobjFlags.MF_TRANSLATION;
import static p.MobjFlags.MF_TRANSSHIFT;
import v.renderers.BppMode;

/**
 *   Combined colormap and light LUTs.
 *   Used for z-depth cuing per column/row,
 *   and other lighting effects (sector ambient, flash).
 *   
 * @author velktron
 *
 * @param <V> The data type of the SCREEN
 */

public class LightsAndColors<V> {
    
    private static volatile LCData LC_DATA = null;
    
    /** For HiColor, these are, effectively, a bunch of 555 RGB palettes,
     *  for TrueColor they are a bunch of 32-bit ARGB palettes etc.
     *  Only for indexed they represent index remappings.  
     */

    /** "peg" this to the one from RendererData */
    public V[] colormaps;

    /** lighttable_t** */
    public V[] walllights;

    /** Use in conjunction with player.fixedcolormap */
    public V fixedcolormap;
	
	/**
	 * Color tables for different players, translate a limited part to another
	 * (color ramps used for suit colors).
	 */
	public byte[][] translationtables;

    // bumped light from gun blasts
    public int extralight;

    public V[][] scalelight;
    public V[] scalelightfixed;
    public V[][] zlight;
    public V[] spritelights;
    private final DoomMain<?, V> DM;

    public LightsAndColors(final DoomMain<?, V> DM) {
        this.DM = DM;
    }

    public int lightBits() {
        return getLCData().LBITS;
    }

    public int lightBright() {
        return getLCData().LIGHTBRIGHT;
    }

    public int lightLevels() {
        return getLCData().LIGHTLEVELS;
    }

    public int lightScaleShift() {
        return getLCData().LIGHTSCALESHIFT;
    }

    public int lightSegShift() {
        return getLCData().LIGHTSEGSHIFT;
    }

    public int lightZShift() {
        return getLCData().LIGHTZSHIFT;
    }

    public int maxLightScale() {
        return getLCData().MAXLIGHTSCALE;
    }

    public int maxLightZ() {
        return getLCData().MAXLIGHTZ;
    }

    public int numColorMaps() {
        return getLCData().NUMCOLORMAPS;
    }

    public final byte[] getTranslationTable(long mobjflags) {
        return translationtables[(int) ((mobjflags & MF_TRANSLATION) >> MF_TRANSSHIFT)];
    }
    
    private LCData getLCData() {
        if (LC_DATA == null || LC_DATA.bpp != DM.bppMode) {
            final LCData localLCData;
            // Double Checked Locking
            synchronized (LCData.class) {
                localLCData = LC_DATA;
                if (localLCData == null || localLCData.bpp != DM.bppMode) {
                    LC_DATA = new LCData(DM.bppMode);
                }
            }
        }
        
        return LC_DATA;
    }

    private static class LCData {
        final BppMode bpp;
        
        /** Bits representing color levels. 5 for 32. */
        final int LBITS;
        
        /**
         * These two are tied by an inverse relationship. E.g. 256 levels, 0 shift
         * 128 levels, 1 shift ...etc... 16 levels, 4 shift (default). Or even less,
         * if you want.
         * 
         * By setting it to the max however you get smoother light and get rid of
         * lightsegshift globally, too. Of course, by increasing the number of light
         * levels, you also put more memory pressure, and due to their being only
         * 256 colors to begin with, visually, there won't be many differences.
         */


        final int LIGHTLEVELS;
        final int LIGHTSEGSHIFT;


        /** Number of diminishing brightness levels.
           There a 0-31, i.e. 32 LUT in the COLORMAP lump. 
           TODO: how can those be distinct from the light levels???
           */    

        final int NUMCOLORMAPS;


        // These are a bit more tricky to figure out though.

        /** Maximum index used for light levels of sprites. In practice,
         *  it's capped by the number of light levels???
         *  
         *  Normally set to 48 (32 +16???)
         */

        final int MAXLIGHTSCALE;

        /** Used to scale brightness of walls and sprites. Their "scale" is shifted by
         *  this amount, and this results in an index, which is capped by MAXLIGHTSCALE.
         *  Normally it's 12 for 32 levels, so 11 for 64, 10 for 128, ans 9 for 256.
         *  
         */
        final int LIGHTSCALESHIFT;

        /** This one seems arbitrary. Will auto-fit to 128 possible levels? */
        final int MAXLIGHTZ;


        final int LIGHTBRIGHT;

        /** Normally 20 for 32 colormaps, applied to distance.
         * Formula: 25-LBITS
         *  
         */
        final int LIGHTZSHIFT;

        LCData(final BppMode bpp) {
            this.bpp = bpp;
            LBITS = bpp == BppMode.TrueColor ? 8 : 5;
            LIGHTLEVELS = 1 << LBITS;
            MAXLIGHTZ = LIGHTLEVELS * 4;
            LIGHTBRIGHT = 2;
            LIGHTSEGSHIFT = 8 - LBITS;
            NUMCOLORMAPS = LIGHTLEVELS;
            MAXLIGHTSCALE = 3 * LIGHTLEVELS / 2;
            LIGHTSCALESHIFT = 17 - LBITS;
            LIGHTZSHIFT = 25 - LBITS;
        }
    }
}