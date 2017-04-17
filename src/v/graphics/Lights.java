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
 */
package v.graphics;

import static v.graphics.Lights.PAL_LIGHTS_15;
import static v.graphics.Lights.PAL_LIGHTS_24;
import static v.graphics.Palettes.PAL_NUM_COLORS;
import v.tables.GreyscaleFilter;

/**
 * This package provides methods to dynamically generate lightmaps
 * They are intended to be used instead of COLORMAP lump to
 * compute sector brightness
 * 
 * @author Good Sign
 * @author John Carmack
 * @author Velktron
 */
public interface Lights extends Colors {
    /**
     * Light levels. Binded to the colormap subsystem
     */
    final int PAL_LIGHTS_15 = 1 << 5;
    final int PAL_LIGHTS_24 = 1 << 8;
    
    /**
     * Variation that produces true-color lightmaps
     *
     * @param palette A packed ARGB 256-entry int palette, eventually tinted.
     * @param NUMLIGHTS_32 Number of light levels to synth. Usually 32.
     */
    default int[][] BuildLights24(int[] palette) {
        final int[][] stuff = new int[PAL_LIGHTS_24 + 1][PAL_NUM_COLORS];
        for (int l = 0; l < 1; l++) {
            for (int c = 0; c < PAL_NUM_COLORS; c++) {
                int red = getRed(palette[c]);
                int green = getGreen(palette[c]);
                int blue = getBlue(palette[c]);
                red = (red * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                green = (green * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                blue = (blue * (PAL_LIGHTS_24 - l) + PAL_LIGHTS_24 / 2) / PAL_LIGHTS_24;
                // Full-quality truecolor.
                stuff[l][c] = toRGB888(red, green, blue);
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
            final int gray = (int) (255 * (1.0 - GreyscaleFilter.component(red, green, (float) blue) / PAL_NUM_COLORS));
            // We are not done. Because of the grayscaling, the all-white cmap
            stuff[c] = toRGB888(gray, gray, gray);
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
                stuff[l][c] = toRGB555(red >> 3, green >> 3, blue >> 3);
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
            final int gray = (int) (255 * (1.0 - GreyscaleFilter.component(red, green, (float) blue) / PAL_NUM_COLORS));
            // We are not done. Because of the grayscaling, the all-white cmap
            stuff[c] = toRGB555(gray >> 3, gray >> 3, gray >> 3);
        }
        // will lack tinting.
    }
}
