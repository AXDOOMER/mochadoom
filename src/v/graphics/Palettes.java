/*
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
 * 
 * Refactored and included as the module of new software 2d graphics API
 * - Good Sign 2017/04/14
 *
 * Now extended to contain many extended color synthesizing tools, used
 * in super-indexed and truecolor software rendered displays.
 * 
 * @author Maes
 */
package v.graphics;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import v.tables.ColorTint;
import static v.tables.GammaTables.LUT_GAMMA;

/**
 * Palettes & colormaps library
 * 
 * @author Good Sign
 */
public interface Palettes {

    final int PAL_NUM_STRIDES = 3;

    /**
     * Maximum number of colors in palette
     */
    static int PAL_NUM_COLORS = 256;
    
    /**
     * Maximum number of palettes
     * PLAYPAL length / (PAL_NUM_COLORS * PAL_NUM_STRIDES)
     * 
     * TODO: think some way of support for future Hexen, Heretic, Strife palettes
     */
    static int NUM_PALETTES = 14;

    /**
     * Light levels. Binded to the colormap subsystem
     */
    final int PAL_LIGHTS_15 = 1 << 5;
    final int PAL_LIGHTS_24 = 1 << 8;
    
    /**
     * An index of of the lighted palette in colormap used for FUZZ effect and partial invisibility
     */
    final int COLORMAP_BLURRY = 6;

    /**
     * An index of of the most lighted palette in colormap
     */
    final int COLORMAP_BULLBRIGHT = 1;

    /**
     * An index of of palette0 in colormap which is not altered
     */
    final int COLORMAP_FIXED = 0;

    /**
     * Methods to be used by implementor
     */
    
    /** 
     * Perform any action necessary so that palettes get modified according to specified gamma.
     * Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     * 
     * @param gammalevel
     */
    void setUsegamma(int gammalevel);
    
    /**
     * Getter for gamma level
     * 
     * @return 
     */
    int getUsegamma();
    
    /** 
     * Perform any action necessary so that the screen output uses the specified palette
     * Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     * 
     * @param palette
     */
    void setPalette(int palette);
    
    /**
     * Getter for palette
     * 
     * @return 
     */
    int getPalette();
    
    /** 
     * Get the value corresponding to a base color (0-255).
     * Depending on the implementation this might be indexed,
     * RGB etc. Use whenever you need "absolute" colors.
     * 
     * @return int
     */
    int getBaseColor(byte color);

    default int getBaseColor(int color) { return getBaseColor((byte) color); }
    
    /**
     * Get red from packed argb long word.
     *
     * @param argb
     * @return
     */
    static int getRed(int argb) {
        return (0xFF0000 & argb) >> 16;
    }

    /**
     * Get red from packed rgb555
     *
     * @param rgb
     * @return
     */
    static int getRed5(int rgb) {
        return (rgb >> 10) & 0x1F;
    }

    /**
     * Get green from packed argb long word.
     *
     * @param argb
     * @return
     */
    static int getGreen(int argb) {
        return (0xFF00 & argb) >> 8;
    }

    /**
     * Get green from packed rgb555
     *
     * @param rgb
     * @return
     */
    static int getGreen5(int rgb) {
        return (rgb >> 5) & 0x1F;
    }

    /**
     * Get blue from packed argb long word.
     *
     * @param argb
     * @return
     */
    static int getBlue(int argb) {
        return 0xFF & argb;
    }
    
    /**
     * Get blue from packed rgb555
     *
     * @param rgb
     * @return
     */
    static int getBlue5(int rgb) {
        return rgb & 0x1F;
    }

    /**
     * Get all three colors into an array
     */
    static int[] getRGB888(int rgb, int[] container) {
        container[0] = getRed(rgb);
        container[1] = getGreen(rgb);
        container[2] = getBlue(rgb);
        return container;
    }
    
    /**
     * Get all three colors into an array
     */
    static int[] getRGB555(int rgb, int[] container) {
        container[0] = getRed5(rgb);
        container[1] = getGreen5(rgb);
        container[2] = getBlue5(rgb);
        return container;
    }
    
    /**
     * Compose rgb888 color (opaque if rgba)
     */
    static int toRGB888(int r, int g, int b) {
        return 0xFF000000 + ((r & 0xFF) << 16) + ((g & 0xFF) << 8) + (b & 0xFF);
    }
    
    /**
     * Compose rgb888 color
     */
    static short toRGB555(int r, int g, int b) {
        return (short) (((r & 0x1F) << 10) + ((g & 0x1F) << 5) + (b & 0x1F));
    }
    /**
     * Finds a color in the palette's range from rangel to rangeh closest to specified r, g, b
     * by distortion, the lesst distorted color is the result. Used for rgb555 invulnerability colormap
     */
    static int BestColor(int r, int g, int b, int[] palette, int rangel, int rangeh) {
        /**
         * let any color go to 0 as a last resort
         */
        long bestdistortion = ((long) r * r + (long) g * g + (long) b * b) * 2;
        int bestcolor = 0;
        for (int i = rangel; i <= rangeh; i++) {
            final long dr = r - getRed(palette[i]);
            final long dg = g - getGreen(palette[i]);
            final long db = b - getBlue(palette[i]);
            final long distortion = dr * dr + dg * dg + db * db;
            if (distortion < bestdistortion) {
                if (distortion == 0) {
                    return i; // perfect match
                }
                bestdistortion = distortion;
                bestcolor = i;
            }
        }
        return bestcolor;
    }
    
    static int CompareColors555(short rgb1, short rgb2) {
        return ColorDistance555(rgb1, rgb2) > 0 ? 1 : 0;
    }
    
    static long ColorDistance555(short rgb1, short rgb2) {
        final int r1 = getRed5(rgb1),
                g1 = getGreen5(rgb1),
                b1 = getBlue5(rgb1),
                r2 = getRed5(rgb2),
                g2 = getGreen5(rgb2),
                b2 = getBlue5(rgb2);
        
        final long dr = r1 - r2, dg = g1 - g2, db = b1 - b2;
        return dr * dr + dg * dg + db * db;
    }

    static int CompareColors(int rgb1, int rgb2) {
        return ColorDistance(rgb1, rgb2) > 0 ? 1 : 0;
    }
    
    static long ColorDistance(int rgb1, int rgb2) {
        final int r1 = getRed(rgb1),
                g1 = getGreen(rgb1),
                b1 = getBlue(rgb1),
                r2 = getRed(rgb2),
                g2 = getGreen(rgb2),
                b2 = getBlue(rgb2);
        
        final long dr = r1 - r2, dg = g1 - g2, db = b1 - b2;
        return dr * dr + dg * dg + db * db;
    }

    static int CompareColorsHSV(int rgb1, int rgb2) {
        return ColorDistance(rgb1, rgb2) > 0 ? 1 : 0;
    }
    
    static long ColorDistanceHSV(int rgb1, int rgb2) {
        final int r1 = (int) (0.21 * getRed(rgb1)),
                g1 = (int) (0.72 * getGreen(rgb1)),
                b1 = (int) (0.07 * getBlue(rgb1)),
                r2 = (int) (0.21 * getRed(rgb2)),
                g2 = (int) (0.72 * getGreen(rgb2)),
                b2 = (int) (0.07 * getBlue(rgb2));
        
        final long dr = r1 - r2, dg = g1 - g2, db = b1 - b2;
        return dr * dr + dg * dg + db * db;
    }

    static short rgb4444to555(short rgb) {
        // .... .... .... ....
        // 1111
        int ri = (0xF000 & rgb) >> 11;
        int gi = (0xF00 & rgb) >> 7;
        int bi = (0xF0 & rgb) >> 3;
        int bits = (ri & 16) >> 4;
        ri += bits;
        bits = (gi & 16) >> 4;
        gi += bits;
        bits = (bi & 16) >> 4;
        bi += bits;
        // RGBA 555 packed for NeXT
        return toRGB555(ri, gi, bi);
    }

    /**
     * Get ARGB_8888 from RGB_555, with proper higher-bit
     * replication.
     *
     * @param rgb
     * @return
     */
    static int rgb555to888(short rgb) {
        // .... .... .... ....
        // 111 11 = 7C00
        // 11 111 = 03E0
        // 1F= 1 1111
        int ri = (0x7C00 & rgb) >> 7;
        int gi = (0x3E0 & rgb) >> 2;
        int bi = (0x1F & rgb) << 3;
        // replicate 3 higher bits
        int bits = (ri & 224) >> 5;
        ri += bits;
        bits = (gi & 224) >> 5;
        gi += bits;
        bits = (bi & 224) >> 5;
        bi += bits;
        // ARGB 8888 packed
        return toRGB888(ri, gi, bi);
    }

    /**
     * Get RGB_555 from packed ARGB_8888.
     *
     * @param argb
     * @return
     */
    static short rgb888to555(int rgb) {
        int ri = (0xFF010000 & rgb) >> 19;
        int gi = (0xFF00 & rgb) >> 11;
        int bi = (0xFF & rgb) >> 3;
        return toRGB555(ri, gi, bi);
    }

    /**
     * Get packed RGB_555 word from individual 8-bit RGB components.
     *
     *  WARNING: there's no sanity/overflow check for performance reasons.
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    static short rgb888to555(int r, int g, int b) {
        return toRGB555(r >> 3, g >> 3, b >> 3);
    }

    default short getRGB555_Lights(int red, int green, int blue) {
        int ri = (((red + 4) > 255 ? 255 : red + 4)) >> 3;
        ri = ri > 31 ? 31 : ri;
        int gi = (((green + 4) > 255 ? 255 : green + 4)) >> 3;
        gi = gi > 31 ? 31 : gi;
        int bi = (((blue + 4) > 255 ? 255 : blue + 4)) >> 3;
        bi = bi > 31 ? 31 : bi;
        // RGB555 for HiColor
        return (short) ((ri << 10) + (gi << 5) + bi);
    }

    default short getRGB555_Lights(int rgb) {
        return getRGB555_Lights(getRed(rgb), getGreen(rgb), getBlue(rgb));
    }

    default void tintRGB(final ColorTint tint, final int[] rgb, int[] rgb2) {
        rgb2[0] = tint.tintRed8(rgb[0]);
        rgb2[1] = tint.tintGreen8(rgb[1]);
        rgb2[2] = tint.tintBlue8(rgb[2]);
    }

    default void tintRGB555(final ColorTint tint, final int[] rgb, int[] rgb2) {
        rgb2[0] = tint.tintRed5(rgb[0]);
        rgb2[1] = tint.tintGreen5(rgb[1]);
        rgb2[2] = tint.tintBlue5(rgb[2]);
    }

    /**
     * ColorShiftPalette - lifted from dcolors.c Operates on RGB888 palettes in
     * separate bytes. at shift = 0, the colors are normal at shift = steps, the
     * colors are all the given rgb
     */
    default void ColorShiftPalette(byte[] inpal, byte[] outpal, int r, int g, int b, int shift, int steps) {
        int in_p = 0;
        int out_p = 0;
        for (int i = 0; i < PAL_NUM_COLORS; i++) {
            final int dr = r - inpal[in_p + 0];
            final int dg = g - inpal[in_p + 1];
            final int db = b - inpal[in_p + 2];
            outpal[out_p + 0] = (byte) (inpal[in_p + 0] + dr * shift / steps);
            outpal[out_p + 1] = (byte) (inpal[in_p + 1] + dg * shift / steps);
            outpal[out_p + 2] = (byte) (inpal[in_p + 2] + db * shift / steps);
            in_p += 3;
            out_p += 3;
        }
    }

    /**
     * Variation that produces true-color lightmaps
     *
     * @author John Carmack
     * @param palette A packed ARGB 256-entry int palette, eventually tinted.
     * @param NUMLIGHTS_32 Number of light levels to synth. Usually 32.
     */
    default int[][] BuildLights24(int[] palette) {
        final int[][] stuff = new int[PAL_LIGHTS_24 + 1][PAL_NUM_COLORS];
        for (int l = 0; l < PAL_LIGHTS_24; l++) {
            for (int c = 0; c < PAL_NUM_COLORS; c++) {
                int red = getRed(palette[c]);
                int green = getGreen(palette[c]);
                int blue = getBlue(palette[c]);
                red = (red * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                green = (green * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                blue = (blue * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                // Full-quality truecolor.
                stuff[l][c] = new Color(red, green, blue).getRGB();
            }
        }
        BuildSpecials24(stuff[PAL_LIGHTS_24], palette);
        return stuff;
    }

    /**
     * Truecolor invulnerability specials
     * 
     * @param stuff
     * @param palette 
     */
    default void BuildSpecials24(int[] stuff, int[] palette) {
        for (int c = 0; c < PAL_NUM_COLORS; c++) {
            final int red = getRed(palette[c]);
            final int green = getGreen(palette[c]);
            final int blue = getBlue(palette[c]);
            final int gray = (int) (255 * (1.0 - (red * 0.299 / PAL_NUM_COLORS + green * 0.587 / PAL_NUM_COLORS + blue * 0.114 / PAL_NUM_COLORS)));
            // We are not done. Because of the grayscaling, the all-white cmap
            stuff[c] = new Color(gray, gray, gray).getRGB();
        }
        // will lack tinting.
    }

    /**
     * RF_BuildLights lifted from dcolors.c
     *
     * Used to compute extended-color colormaps even in absence of the
     * COLORS15 lump. Must be recomputed if gamma levels change, since
     * they actually modify the RGB envelopes.
     *
     * @author John Carmack
     * @author Velktron
     * @param palette A packed ARGB 256-entry int palette, eventually tinted.
     * @param NUMLIGHTS Number of light levels to synth. Usually 32.
     */
    default short[][] BuildLights15(int[] palette) {
        final short[][] stuff = new short[PAL_LIGHTS_15 + 1][PAL_NUM_COLORS];
        for (int l = 0; l < PAL_LIGHTS_15; l++) {
            for (int c = 0; c < PAL_NUM_COLORS; c++) {
                int red = getRed(palette[c]);
                int green = getGreen(palette[c]);
                int blue = getBlue(palette[c]);
                red = (red * (PAL_LIGHTS_15 - l) + PAL_LIGHTS_15 / 2) / PAL_LIGHTS_15;
                green = (green * (PAL_LIGHTS_15 - l) + PAL_LIGHTS_15 / 2) / PAL_LIGHTS_15;
                blue = (blue * (PAL_LIGHTS_15 - l) + PAL_LIGHTS_15 / 2) / PAL_LIGHTS_15;
                // RGB555 for HiColor
                stuff[l][c] = getRGB555_Lights(red, green, blue);
            }
        }
        
        // Build special map for invulnerability
        BuildSpecials15(stuff[PAL_LIGHTS_15], palette);
        return stuff;
    }

    /**
     * Invlulnerability map
     * 
     * @param stuff
     * @param palette 
     */
    default void BuildSpecials15(short[] stuff, int[] palette) {
        for (int c = 0; c < PAL_NUM_COLORS; c++) {
            final int red = getRed(palette[c]);
            final int green = getGreen(palette[c]);
            final int blue = getBlue(palette[c]);
            final int gray = (int) (255 * (1.0 - (red * 0.299 / PAL_NUM_COLORS + green * 0.587 / PAL_NUM_COLORS + blue * 0.114 / PAL_NUM_COLORS)));
            // We are not done. Because of the grayscaling, the all-white cmap
            stuff[c] = getRGB555_Lights(palette[BestColor(gray, gray, gray, palette, 0, 255)]);
        }
        // will lack tinting.
    }

    /**
     * Given raw palette data, returns an array with proper TrueColor data
     * @param byte[] pal proper palette
     * @return int[] 32 bit Truecolor ARGB colormap
     */
    default int[] cmapTrueColor(byte[] pal) {
        final int cmaps[] = new int[PAL_NUM_COLORS];
        
        // Initial palette can be neutral or based upon "gamma 0",
        // which is actually a bit biased and distorted
        for (int x = 0, xXstride = 0; x < PAL_NUM_COLORS; ++x, xXstride += PAL_NUM_STRIDES) {
            int r = /*LUT_GAMMA[0][*/pal[xXstride]/*]*/ & 0xFF; // R
            int g = /*LUT_GAMMA[0][*/pal[1 + xXstride]/*]*/ & 0xFF; // G
            int b = /*LUT_GAMMA[0][*/pal[2 + xXstride]/*]*/ & 0xFF; // B
            int color = 0xFF000000 | r << 16 | g << 8 | b;
            cmaps[x] = color;
        }
        
        return cmaps;
    }
    
    /**
     * Given raw palette data, returns an array with proper HiColor data
     * @param byte[] pal proper palette
     * @return short[] 16 bit HiColor RGB colormap
     */
    default short[] cmapHiColor(byte[] pal) {
        final short[] cmap = new short[PAL_NUM_COLORS];

        // Apply gammas a-posteriori, not a-priori.
        // Initial palette can be neutral or based upon "gamma 0",
        // which is actually a bit biased and distorted
        for (int x = 0, xXstride = 0; x < PAL_NUM_COLORS; ++x, xXstride += PAL_NUM_STRIDES) {
            int r = (/*LUT_GAMMA[0][*/pal[xXstride]/*]*/ & 0xFF) >> 3; // R
            int g = (/*LUT_GAMMA[0][*/pal[1 + xXstride]/*]*/ & 0xFF) >> 3; // G
            int b = (/*LUT_GAMMA[0][*/pal[2 + xXstride]/*]*/ & 0xFF) >> 3; // B
            int color = r << 10 | g << 5 | b;
            cmap[x] = (short) color;
        }
 
        return cmap;
    }

    /**
     * Given an array of certain length and raw palette data fills array
     * with IndexColorModel's for each palette. Gammas are applied a-priori
     * @param IndexColorModel[][] cmaps preallocated array, as it is often reconstructed for gamma, do not reallocate it
     * @param byte[] pal proper palette
     * @return the same araay as input, but all values set to new IndexColorModels
     */    
    default IndexColorModel[][] cmapIndexed(IndexColorModel cmaps[][], byte[] pal) {
        final int colorsXstride = PAL_NUM_COLORS * PAL_NUM_STRIDES;
        
        // Now we have our palettes.
        for (int i = 0; i < cmaps[0].length; ++i) {
            //new IndexColorModel(8, PAL_NUM_COLORS, pal, i * colorsXstride, false);
            cmaps[0][i] = createIndexColorModel(pal, i * colorsXstride);
        }

        // Wire the others according to the gamma table.
        final byte[] tmpcmap = new byte[colorsXstride];

        // For each gamma value...
        for (int j = 1; j < LUT_GAMMA.length; j++) {
            // For each palette
            for (int i = 0; i < NUM_PALETTES; i++) {
                for (int k = 0; k < PAL_NUM_COLORS; ++k) {
                    final int iXcolorsXstride_plus_StrideXk = i * colorsXstride + PAL_NUM_STRIDES * k;
                    tmpcmap[3 * k/**/] = (byte) LUT_GAMMA[j][0xFF & pal[/**/iXcolorsXstride_plus_StrideXk]]; // R
                    tmpcmap[3 * k + 1] = (byte) LUT_GAMMA[j][0xFF & pal[1 + iXcolorsXstride_plus_StrideXk]]; // G
                    tmpcmap[3 * k + 2] = (byte) LUT_GAMMA[j][0xFF & pal[2 + iXcolorsXstride_plus_StrideXk]]; // B
                }

                //new IndexColorModel(8, PAL_NUM_COLORS, tmpcmap, 0, false);
                cmaps[j][i] = createIndexColorModel(tmpcmap, 0);
            }
        }

        return cmaps;
    }

    /**
     * @param byte[] cmap a colormap from which to make color model
     * @param int start position in colormap from which to take PAL_NUM_COLORS
     * @return IndexColorModel
     */
    default IndexColorModel createIndexColorModel(byte cmap[], int start) {
        return new IndexColorModel(8, PAL_NUM_COLORS, cmap, start, false);
    }
}
