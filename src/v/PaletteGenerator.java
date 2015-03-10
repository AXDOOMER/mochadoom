package v;

import java.awt.Color;

/**
 * Palette generation failsafe. Uses only data from the first palette, and
 * generates the rest by tinting according to the Doom wiki specs. Uses info
 * from: http://doom.wikia.com/wiki/PLAYPAL
 * 
 * Now extended to contain many extended color synthesizing tools, used
 * in super-indexed and truecolor software rendered displays.
 * 
 * @author Maes
 */
public class PaletteGenerator {
    public final static int playpal[] = { 0x00, 0x00, 0x00, 0x1F, 0x17, 0x0B,
            0x17, 0x0F, 0x07, 0x4B, 0x4B, 0x4B, 0xFF, 0xFF, 0xFF, 0x1B, 0x1B,
            0x1B, 0x13, 0x13, 0x13, 0x0B, 0x0B, 0x0B, 0x07, 0x07, 0x07, 0x2F,
            0x37, 0x1F, 0x23, 0x2B, 0x0F, 0x17, 0x1F, 0x07, 0x0F, 0x17, 0x00,
            0x4F, 0x3B, 0x2B, 0x47, 0x33, 0x23, 0x3F, 0x2B, 0x1B, 0xFF, 0xB7,
            0xB7, 0xF7, 0xAB, 0xAB, 0xF3, 0xA3, 0xA3, 0xEB, 0x97, 0x97, 0xE7,
            0x8F, 0x8F, 0xDF, 0x87, 0x87, 0xDB, 0x7B, 0x7B, 0xD3, 0x73, 0x73,
            0xCB, 0x6B, 0x6B, 0xC7, 0x63, 0x63, 0xBF, 0x5B, 0x5B, 0xBB, 0x57,
            0x57, 0xB3, 0x4F, 0x4F, 0xAF, 0x47, 0x47, 0xA7, 0x3F, 0x3F, 0xA3,
            0x3B, 0x3B, 0x9B, 0x33, 0x33, 0x97, 0x2F, 0x2F, 0x8F, 0x2B, 0x2B,
            0x8B, 0x23, 0x23, 0x83, 0x1F, 0x1F, 0x7F, 0x1B, 0x1B, 0x77, 0x17,
            0x17, 0x73, 0x13, 0x13, 0x6B, 0x0F, 0x0F, 0x67, 0x0B, 0x0B, 0x5F,
            0x07, 0x07, 0x5B, 0x07, 0x07, 0x53, 0x07, 0x07, 0x4F, 0x00, 0x00,
            0x47, 0x00, 0x00, 0x43, 0x00, 0x00, 0xFF, 0xEB, 0xDF, 0xFF, 0xE3,
            0xD3, 0xFF, 0xDB, 0xC7, 0xFF, 0xD3, 0xBB, 0xFF, 0xCF, 0xB3, 0xFF,
            0xC7, 0xA7, 0xFF, 0xBF, 0x9B, 0xFF, 0xBB, 0x93, 0xFF, 0xB3, 0x83,
            0xF7, 0xAB, 0x7B, 0xEF, 0xA3, 0x73, 0xE7, 0x9B, 0x6B, 0xDF, 0x93,
            0x63, 0xD7, 0x8B, 0x5B, 0xCF, 0x83, 0x53, 0xCB, 0x7F, 0x4F, 0xBF,
            0x7B, 0x4B, 0xB3, 0x73, 0x47, 0xAB, 0x6F, 0x43, 0xA3, 0x6B, 0x3F,
            0x9B, 0x63, 0x3B, 0x8F, 0x5F, 0x37, 0x87, 0x57, 0x33, 0x7F, 0x53,
            0x2F, 0x77, 0x4F, 0x2B, 0x6B, 0x47, 0x27, 0x5F, 0x43, 0x23, 0x53,
            0x3F, 0x1F, 0x4B, 0x37, 0x1B, 0x3F, 0x2F, 0x17, 0x33, 0x2B, 0x13,
            0x2B, 0x23, 0x0F, 0xEF, 0xEF, 0xEF, 0xE7, 0xE7, 0xE7, 0xDF, 0xDF,
            0xDF, 0xDB, 0xDB, 0xDB, 0xD3, 0xD3, 0xD3, 0xCB, 0xCB, 0xCB, 0xC7,
            0xC7, 0xC7, 0xBF, 0xBF, 0xBF, 0xB7, 0xB7, 0xB7, 0xB3, 0xB3, 0xB3,
            0xAB, 0xAB, 0xAB, 0xA7, 0xA7, 0xA7, 0x9F, 0x9F, 0x9F, 0x97, 0x97,
            0x97, 0x93, 0x93, 0x93, 0x8B, 0x8B, 0x8B, 0x83, 0x83, 0x83, 0x7F,
            0x7F, 0x7F, 0x77, 0x77, 0x77, 0x6F, 0x6F, 0x6F, 0x6B, 0x6B, 0x6B,
            0x63, 0x63, 0x63, 0x5B, 0x5B, 0x5B, 0x57, 0x57, 0x57, 0x4F, 0x4F,
            0x4F, 0x47, 0x47, 0x47, 0x43, 0x43, 0x43, 0x3B, 0x3B, 0x3B, 0x37,
            0x37, 0x37, 0x2F, 0x2F, 0x2F, 0x27, 0x27, 0x27, 0x23, 0x23, 0x23,
            0x77, 0xFF, 0x6F, 0x6F, 0xEF, 0x67, 0x67, 0xDF, 0x5F, 0x5F, 0xCF,
            0x57, 0x5B, 0xBF, 0x4F, 0x53, 0xAF, 0x47, 0x4B, 0x9F, 0x3F, 0x43,
            0x93, 0x37, 0x3F, 0x83, 0x2F, 0x37, 0x73, 0x2B, 0x2F, 0x63, 0x23,
            0x27, 0x53, 0x1B, 0x1F, 0x43, 0x17, 0x17, 0x33, 0x0F, 0x13, 0x23,
            0x0B, 0x0B, 0x17, 0x07, 0xBF, 0xA7, 0x8F, 0xB7, 0x9F, 0x87, 0xAF,
            0x97, 0x7F, 0xA7, 0x8F, 0x77, 0x9F, 0x87, 0x6F, 0x9B, 0x7F, 0x6B,
            0x93, 0x7B, 0x63, 0x8B, 0x73, 0x5B, 0x83, 0x6B, 0x57, 0x7B, 0x63,
            0x4F, 0x77, 0x5F, 0x4B, 0x6F, 0x57, 0x43, 0x67, 0x53, 0x3F, 0x5F,
            0x4B, 0x37, 0x57, 0x43, 0x33, 0x53, 0x3F, 0x2F, 0x9F, 0x83, 0x63,
            0x8F, 0x77, 0x53, 0x83, 0x6B, 0x4B, 0x77, 0x5F, 0x3F, 0x67, 0x53,
            0x33, 0x5B, 0x47, 0x2B, 0x4F, 0x3B, 0x23, 0x43, 0x33, 0x1B, 0x7B,
            0x7F, 0x63, 0x6F, 0x73, 0x57, 0x67, 0x6B, 0x4F, 0x5B, 0x63, 0x47,
            0x53, 0x57, 0x3B, 0x47, 0x4F, 0x33, 0x3F, 0x47, 0x2B, 0x37, 0x3F,
            0x27, 0xFF, 0xFF, 0x73, 0xEB, 0xDB, 0x57, 0xD7, 0xBB, 0x43, 0xC3,
            0x9B, 0x2F, 0xAF, 0x7B, 0x1F, 0x9B, 0x5B, 0x13, 0x87, 0x43, 0x07,
            0x73, 0x2B, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xDB, 0xDB, 0xFF, 0xBB,
            0xBB, 0xFF, 0x9B, 0x9B, 0xFF, 0x7B, 0x7B, 0xFF, 0x5F, 0x5F, 0xFF,
            0x3F, 0x3F, 0xFF, 0x1F, 0x1F, 0xFF, 0x00, 0x00, 0xEF, 0x00, 0x00,
            0xE3, 0x00, 0x00, 0xD7, 0x00, 0x00, 0xCB, 0x00, 0x00, 0xBF, 0x00,
            0x00, 0xB3, 0x00, 0x00, 0xA7, 0x00, 0x00, 0x9B, 0x00, 0x00, 0x8B,
            0x00, 0x00, 0x7F, 0x00, 0x00, 0x73, 0x00, 0x00, 0x67, 0x00, 0x00,
            0x5B, 0x00, 0x00, 0x4F, 0x00, 0x00, 0x43, 0x00, 0x00, 0xE7, 0xE7,
            0xFF, 0xC7, 0xC7, 0xFF, 0xAB, 0xAB, 0xFF, 0x8F, 0x8F, 0xFF, 0x73,
            0x73, 0xFF, 0x53, 0x53, 0xFF, 0x37, 0x37, 0xFF, 0x1B, 0x1B, 0xFF,
            0x00, 0x00, 0xFF, 0x00, 0x00, 0xE3, 0x00, 0x00, 0xCB, 0x00, 0x00,
            0xB3, 0x00, 0x00, 0x9B, 0x00, 0x00, 0x83, 0x00, 0x00, 0x6B, 0x00,
            0x00, 0x53, 0xFF, 0xFF, 0xFF, 0xFF, 0xEB, 0xDB, 0xFF, 0xD7, 0xBB,
            0xFF, 0xC7, 0x9B, 0xFF, 0xB3, 0x7B, 0xFF, 0xA3, 0x5B, 0xFF, 0x8F,
            0x3B, 0xFF, 0x7F, 0x1B, 0xF3, 0x73, 0x17, 0xEB, 0x6F, 0x0F, 0xDF,
            0x67, 0x0F, 0xD7, 0x5F, 0x0B, 0xCB, 0x57, 0x07, 0xC3, 0x4F, 0x00,
            0xB7, 0x47, 0x00, 0xAF, 0x43, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xD7, 0xFF, 0xFF, 0xB3, 0xFF, 0xFF, 0x8F, 0xFF, 0xFF, 0x6B, 0xFF,
            0xFF, 0x47, 0xFF, 0xFF, 0x23, 0xFF, 0xFF, 0x00, 0xA7, 0x3F, 0x00,
            0x9F, 0x37, 0x00, 0x93, 0x2F, 0x00, 0x87, 0x23, 0x00, 0x4F, 0x3B,
            0x27, 0x43, 0x2F, 0x1B, 0x37, 0x23, 0x13, 0x2F, 0x1B, 0x0B, 0x00,
            0x00, 0x53, 0x00, 0x00, 0x47, 0x00, 0x00, 0x3B, 0x00, 0x00, 0x2F,
            0x00, 0x00, 0x23, 0x00, 0x00, 0x17, 0x00, 0x00, 0x0B, 0x00, 0x00,
            0x00, 0xFF, 0x9F, 0x43, 0xFF, 0xE7, 0x4B, 0xFF, 0x7B, 0xFF, 0xFF,
            0x00, 0xFF, 0xCF, 0x00, 0xCF, 0x9F, 0x00, 0x9B, 0x6F, 0x00, 0x6B,
            0xA7, 0x6B, 0x6B };

    public final static int greypal[] = new int[768];


    public static byte[] generatePalette(int[] data, int colors,
            ColorTint[] tints) {

        final int palstride = colors * 3;
        final byte[] tmp = new byte[palstride * tints.length];
        final int[] rgb = new int[3];
        final int[] rgb2 = new int[3];

        for (int i = 0; i < colors; i++) {
            rgb[0] = data[3 * i];
            rgb[1] = data[1 + 3 * i];
            rgb[2] = data[2 + 3 * i];

            for (int t = 0; t < tints.length; t++) {
                tintRGB(tints[t], rgb, rgb2);
                tmp[palstride * t + 3 * i] = (byte) rgb2[0];
                tmp[palstride * t + 3 * i + 1] = (byte) rgb2[1];
                tmp[palstride * t + 3 * i + 2] = (byte) rgb2[2];
            }

        }

        return tmp;

    }

    /**
     * Tint a part of a RGB_888 colormap.
     * 
     * @param original
     *        The original reference colormap. DO NOT modify!
     * @param modified
     * @param colors
     *        Usually 256.
     * @param tint
     * @param gamma
     */

    public static void tintColormap(final int[] original, int[] modified,
            int colors, ColorTint tint,short[] gamma) {

        final int[] rgb = new int[4];
        final int[] rgb2 = new int[4];

        for (int i = 0; i < colors; i++) {
            final int rgba=original[i];
            rgb[0] = getRed(rgba);
            rgb[1] = getGreen(rgba);
            rgb[2] = getBlue(rgba);            

            tintRGB(tint, rgb, rgb2);
            
            // Apply gamma correction.
            rgb2[0]=gamma[rgb2[0]];
            rgb2[1]=gamma[rgb2[1]];
            rgb2[2]=gamma[rgb2[2]];
            
            modified[i] = getARGB(rgb2[0],rgb2[1],rgb2[2]);
        }

    }

    public static void tintColormap(final short[] original, short[] modified,
            int colors, ColorTint tint,short[] gamma) {

        final int[] rgb = new int[3];
        final int[] rgb2 = new int[3];

        for (int i = 0; i < colors; i++) {
            final int rgba = rgb555to888(original[i]);
            rgb[0] = getRed(rgba);
            rgb[1] = getGreen(rgba);
            rgb[2] = getBlue(rgba);

            tintRGB(tint, rgb, rgb2);
            
            // Apply gamma correction.
            rgb2[0]=gamma[rgb2[0]];
            rgb2[1]=gamma[rgb2[1]];
            rgb2[2]=gamma[rgb2[2]];
            modified[i] = rgb888to555(rgb2[0],rgb2[1],rgb2[2]);
        }

    }

    public static final void tintRGB(final ColorTint tint, final int[] rgb,
            int[] rgb2) {
        rgb2[0] = (int) (rgb[0] * (1 - tint.tint) + tint.r * tint.tint);
        rgb2[1] = (int) (rgb[1] * (1 - tint.tint) + tint.g * tint.tint);
        rgb2[2] = (int) (rgb[2] * (1 - tint.tint) + tint.b * tint.tint);
        if (rgb2[0] > 255)
            rgb2[0] = 255;
        if (rgb2[1] > 255)
            rgb2[1] = 255;
        if (rgb2[2] > 255)
            rgb2[2] = 255;

    }

    /**
     * ColorShiftPalette - lifted from dcolors.c Operates on RGB888 palettes in
     * separate bytes. at shift = 0, the colors are normal at shift = steps, the
     * colors are all the given rgb
     */

    public static final void ColorShiftPalette(byte[] inpal, byte[] outpal,
            int r, int g, int b, int shift, int steps) {
        int i;
        int dr, dg, db;
        int in_p, out_p;

        in_p = 0;
        out_p = 0;

        for (i = 0; i < 256; i++) {
            dr = r - inpal[in_p + 0];
            dg = g - inpal[in_p + 1];
            db = b - inpal[in_p + 2];

            outpal[out_p + 0] = (byte) (inpal[in_p + 0] + dr * shift / steps);
            outpal[out_p + 1] = (byte) (inpal[in_p + 1] + dg * shift / steps);
            outpal[out_p + 2] = (byte) (inpal[in_p + 2] + db * shift / steps);

            in_p += 3;
            out_p += 3;
        }
    }
  
    /** Get ARGB_8888 from RGB_555, with proper higher-bit
     *  replication.
     * 
     * @param rgb
     * @return
     */
    
    private static final int rgb555to888(short rgb) {
        int ri, gi, bi;
        int bits;

        // .... .... .... ....
        // 111 11 = 7C00
        // 11 111 = 03E0
        // 1F= 1 1111
        ri = (0x7C00 & rgb) >> 7;
        gi = (0x03E0 & rgb) >> 2;
        bi = (0x001F & rgb) << 3;

        // replicate 3 higher bits
        bits = (ri & 0xE0) >> 5;
        ri = ri + bits;

        bits = (gi & 0xE0) >> 5;
        gi = gi + bits;

        bits = (bi & 0xE0) >> 5;
        bi = bi + bits;

        // ARGB 8888 packed
        
        return 0xFF000000+(ri << 16) + (gi << 8) + (bi);
    }

    private static final short rgb4444to555(short rgb) {
        int ri, gi, bi;
        int bits;

        // .... .... .... ....
        // 1111

        ri = (0xF000 & rgb) >> 11;
        gi = (0x0F00 & rgb) >> 7;
        bi = (0x00F0 & rgb) >> 3;

        bits = (ri & 0x10) >> 4;
        ri = ri + bits;

        bits = (gi & 0x10) >> 4;
        gi = gi + bits;

        bits = (bi & 0x10) >> 4;
        bi = bi + bits;

        // RGBA 555 packed for NeXT

        return (short) ((ri << 10) + (gi << 5) + (bi));
    }

    /** Get RGB_555 from packed ARGB_8888.
     * 
     * @param argb
     * @return
     */
    
    public static final short rgb888to555(int rgb) {
        int ri, gi, bi;

        ri = (0xFF0000 & rgb) >> 19;
        gi = (0x00FF00 & rgb) >> 11;
        bi = (0x0000FF & rgb) >> 3;

        return (short) ((ri << 10) + (gi << 5) + (bi));
    }

    /** Get packed RGB_555 word from individual 8-bit RGB components. 
     * 
     *  WARNING: there's no sanity/overflow check for performance reasons.
     * 
     * @param r
     * @param g
     * @param b
     * @return
     */
    
    public static final short rgb888to555(int r,int g,int b) {

        return (short) (((r>>3) << 10) + ((g>>3) << 5) + (b>>3));
    }
    
    /** Get red from packed argb long word.
     * 
     * @param argb
     * @return
     */
    public final static int getRed(int argb) {
        return (0xFF0000 & argb) >> 16;
    }

    /** Get green from packed argb long word.
     * 
     * @param argb
     * @return
     */
    
    public final static int getGreen(int argb) {
        return (0x00FF00 & argb) >> 8;
    }

    /** Get blue from packed argb long word.
     * 
     * @param argb
     * @return
     */
    
    public final static int getBlue(int argb) {
        return (0x0000FF & argb);
    }

    public final static int getARGB(int r,int g, int b){
        return 0xFF000000+(r << 16) + (g << 8) + (b);
    }
    
    public static final short getRGB555(int red,int green,int blue){
        int ri,gi,bi;
        
        ri = (((red+4)>255?255:red+4))>>3;
        ri = ri > 31 ? 31 : ri;
        gi = (((green+4)>255?255:green+4))>>3;
        gi = gi > 31 ? 31 : gi;
        bi = (((blue+4)>255?255:blue+4))>>3;
        bi = bi > 31 ? 31 : bi;

        // RGB555 for HiColor
        return (short) ((ri<<10) + (gi<<5) + bi);
    }
    
    public static final short getRGB555(int rgb){
        return getRGB555(getRed(rgb),getGreen(rgb),getBlue(rgb));
    }
    
    /**RF_BuildLights lifted from dcolors.c
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

    public static final short[][] RF_BuildLights15 (int[] palette,int NUMLIGHTS)
    {
        int     l,c;
        int     red,green,blue;
        short[][] stuff=new short[NUMLIGHTS+1][256];

        for (l=0;l<NUMLIGHTS;l++)
        {
            for (c=0;c<256;c++)
            {
                red = getRed(palette[c]);
                green = getGreen(palette[c]);
                blue = getBlue(palette[c]);

                red = (red*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                green = (green*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                blue = (blue*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;

                // RGB555 for HiColor
                stuff[l][c] = getRGB555(red,green,blue);
            }
        }
        
        // Build special map for invulnerability
        BuildSpecials15(stuff[NUMLIGHTS],palette); 
        
        return stuff;
    }
    
    private static final void BuildSpecials15 (short[] stuff, int[] palette)
    {
        int     c,gray,best;
        int   red, green, blue;;

        for (c=0;c<256;c++)
        {
            red = getRed(palette[c]);
            green = getGreen(palette[c]);
            blue = getBlue(palette[c]);

            gray = (int) (255*(1.0-((float)red*0.299/256.0 + 
            						(float)green*0.587/256.0 +
            						(float)blue*0.114/256.0)));            
            
            // We are not done. Because of the grayscaling, the all-white cmap
            
            best=palette[BestColor(gray,gray,gray,palette,0,255)];
            stuff[c] = getRGB555(best);
            
        }
        

        // will lack tinting.
        
        
    }
    
    private static final void BuildSpecials24 (int[] stuff, int[] palette)
    {
        int     c,gray,best;
        int   red, green, blue;;

        for (c=0;c<256;c++)
        {
            red = getRed(palette[c]);
            green = getGreen(palette[c]);
            blue = getBlue(palette[c]);

            gray = (int) (255*(1.0-((float)red*0.299/256.0 + 
                                    (float)green*0.587/256.0 +
                                    (float)blue*0.114/256.0)));            
            
            // We are not done. Because of the grayscaling, the all-white cmap
            
            //best=palette[BestColor(gray,gray,gray,palette,0,255)];
            stuff[c] = new Color(gray,gray,gray).getRGB();
            
        }
        

        // will lack tinting.
        
        
    }
    
    public static final int BestColor (int r, int g, int b, int[] palette, int rangel, int rangeh)
	{
		int	i;
		long	dr, dg, db;
		long	bestdistortion, distortion;
		int	bestcolor;
		int	pal;

	//
	// let any color go to 0 as a last resort
	//
		bestdistortion = ( (long)r*r + (long)g*g + (long)b*b )*2;
		bestcolor = 0;

		for (i=rangel ; i<= rangeh ; i++)
		{
			dr = r - getRed(palette[i]);
			dg = g - getGreen(palette[i]);
			db = b - getBlue(palette[i]);
			distortion = dr*dr + dg*dg + db*db;
			if (distortion < bestdistortion)
			{
				if (distortion==0)
					return i;		// perfect match

				bestdistortion = distortion;
				bestcolor = i;
			}
		}

		return bestcolor;
	}
    
    /** Variation that produces true-color lightmaps
     * 
     * @author John Carmack
     * @param palette A packed ARGB 256-entry int palette, eventually tinted.
     * @param NUMLIGHTS Number of light levels to synth. Usually 32.
     */

    public static final int[][] RF_BuildLights24 (int[] palette,int NUMLIGHTS)
    {
        int     l,c;
        int     red,green,blue;
        int[][] stuff=new int[NUMLIGHTS+1][256];

        for (l=0;l<NUMLIGHTS;l++)
        {
            for (c=0;c<256;c++)
            {
                red = getRed(palette[c]);
                green = getGreen(palette[c]);
                blue = getBlue(palette[c]);

                red = (red*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                green = (green*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                blue = (blue*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;

                // Full-quality truecolor.
                stuff[l][c] = new Color(red,green,blue).getRGB();
            }
        }
        
        BuildSpecials24(stuff[NUMLIGHTS],palette); 
        
        return stuff;
    }
    
    public static final int[][] BuildLights24 (int[] palette,int NUMLIGHTS)
    {
        int     l,c;
        int     red,green,blue;
        int[][] stuff=new int[NUMLIGHTS+1][256];

        for (l=0;l<NUMLIGHTS;l++)
        {
            for (c=0;c<256;c++)
            {
                red = getRed(palette[c]);
                green = getGreen(palette[c]);
                blue = getBlue(palette[c]);

                red = (red*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                green = (green*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;
                blue = (blue*(NUMLIGHTS-l)+NUMLIGHTS/2)/NUMLIGHTS;

                // Full-quality truecolor.
                stuff[l][c] = new Color(red,green,blue).getRGB();
            }
        }
        
        BuildSpecials24(stuff[NUMLIGHTS],palette); 
        
        return stuff;
    }
    
    static {
        for (int i = 0; i < 256; i++) {
            greypal[3 * i] = i;
            greypal[3 * i + 1] = i;
            greypal[3 * i + 2] = i;
        }

    }
    
}
