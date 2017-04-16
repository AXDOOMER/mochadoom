/*-----------------------------------------------------------------------------
//
// Copyright (C) 1993-1996 Id Software, Inc.
// Copyright (C) 2017 Good Sign
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// From m_misc.c
//-----------------------------------------------------------------------------*/

package m;

import static doom.englsh.*;
import static g.Keys.*;
import java.util.Comparator;
import java.util.Optional;
import utils.QuoteType;
import v.graphics.Plotter;
import v.renderers.BppMode;
import v.renderers.SceneRendererMode;
import v.tables.GreyscaleFilter;

/**
 * An enumeration with the most basic default Doom settings their default values, used if nothing else is available.
 * They are applied first thing, and then updated from the .cfg file.
 * 
 * The file now also contains settings on many features introduced by this new version of Mocha Doom
 *  - Good Sign 2017/04/11
 * 
 * TODO: find a trick to separate settings groups in the same file vanilla-compatibly
 */
public enum Settings {
    /**
     * Defaults
     */
    alwaysrun(false), // Always run is OFF
    chatmacro0(HUSTR_CHATMACRO0),
    chatmacro1(HUSTR_CHATMACRO1),
    chatmacro2(HUSTR_CHATMACRO2),
    chatmacro3(HUSTR_CHATMACRO3),
    chatmacro4(HUSTR_CHATMACRO4),
    chatmacro5(HUSTR_CHATMACRO5),
    chatmacro6(HUSTR_CHATMACRO6),
    chatmacro7(HUSTR_CHATMACRO7),
    chatmacro8(HUSTR_CHATMACRO8),
    chatmacro9(HUSTR_CHATMACRO9),
    detaillevel(0),
    fullscreen(false),
    joyb_fire(0),
    joyb_strafe(1),
    joyb_use(3),
    joyb_speed(2),
    key_down((int) 's'),
    key_fire((int) KEY_CTRL),
    key_left((int) KEY_LEFTARROW),
    key_right((int) KEY_RIGHTARROW),
    key_speed((int) KEY_SHIFT),
    key_strafe((int) KEY_ALT),
    key_strafeleft((int) 'a'),
    key_straferight((int) 'd'),
    key_up((int) 'w'),
    key_use((int) ' '),
    mb_used(2),
    mouse_sensitivity(5),
    mouseb_fire(0),
    mouseb_forward(1), // AX: Value inverted with the one above
    mouseb_strafe(2), // AX: Fixed
    music_volume(8),
    screenblocks(10),
    sfx_volume(8),
    show_messages(true),
    snd_channels(32),
    use_joystick(false),
    use_mouse(true),
    usegamma(0),

    /**
     * Mocha Doom
     */
    automap_plotter_style(Plotter.Style.Thin), // Thin is vanilla, Thick is scaled, Deep slightly rounded scaled
    color_depth(BppMode.Indexed), // Indexed: 256, HiColor: 32 768, TrueColor: 16 777 216
    fix_gamma_ramp(false), // Vanilla do not use pure black color because Gamma LUT calculated without it, doubling 128
    fix_gamma_palette(false), // In vanilla, switching gamma with F11 hides Berserk or Rad suit tint
    fix_sky_palette(false), // In vanilla, sky color does not change when under effect of Invulnerability powerup
    fix_medi_need(false), // In vanilla, message "Picked up a medikit that you REALLY need!" never appears due to bug
    fix_ouch_face(false), // In vanilla, ouch face displayed only when acuired 25+ health when damaged for 25+ health
    line_of_sight(LOS.Vanilla), // Deaf monsters when thing pos corellates somehow with map vertex, change desync demos
    vestrobe(false), // Strobe effect on automap cut off from vanilla
    scale_screen_tiles(true), // If you scale screen tiles, it looks like vanilla
    scale_melt(true), // If you scale melt and use DoomRandom generator (not truly random), it looks exacly like vanilla
    semi_translucent_fuzz(false), // only works in AlphaTrueColor mode. Also ignored with fuzz_mix = true
    fuzz_mix(false), // Maes unique features on Fuzz effect. Vanilla dont have that, so they are switched off by default
    
    parallelism_realcolor_tint(Runtime.getRuntime().availableProcessors()), // Used for real color tinting to speed up
    parallelism_patch_columns(3), // When drawing screen graphics patches, this speeds up column drawing
    greyscale_filter(GreyscaleFilter.Average), // Used for FUZZ effect or with -greypal comand line argument (for test)
    scene_renderer_mode(SceneRendererMode.Serial); // In vanilla, scene renderer is serial. Parallel can be faster
    
    public final static Comparator<Settings> NAME_COMPARATOR = Comparator.comparing(Enum::name, String::compareTo);

    <T extends Enum<T>> Settings(final T defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = defaultValue.getClass();
    }
    
    Settings(final String defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = String.class;
    }

    Settings(final char defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = Character.class;
    }

    Settings(final int defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = Integer.class;
    }

    Settings(final long defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = Long.class;
    }

    Settings(final double defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = Double.class;
    }

    Settings(final boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.valueType = Boolean.class;
    }

    public final Class<?> valueType;
    public final Object defaultValue;
        
    public Optional<QuoteType> quoteType() {
        if (valueType == String.class)
            return Optional.of(QuoteType.DOUBLE);
        else if (valueType == Character.class)
            return Optional.of(QuoteType.SINGLE);
        
        return Optional.empty();
    }
    
    public enum LOS {Vanilla, Boom}
}
