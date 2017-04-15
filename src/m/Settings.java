package m;

import static doom.englsh.*;
import static g.Keys.*;
import java.util.Comparator;
import java.util.Optional;
import utils.QuoteType;
import v.graphics.Plotter;
import v.renderers.BppMode;
import v.tables.GreyscaleFilter;

/**
 * An enumeration with the most basic default Doom settings their default values, used if nothing else is available.
 * They are applied first thing, and then updated from the .cfg file.
 * 
 * The file now also contains settings on many features introduced by this new version of Mocha Doom
 *  - Good Sign 2017/04/11
 */
public enum Settings {
    automap_plotter_style(Plotter.Style.Thick),
    mouse_sensitivity(5),
    sfx_volume(8),
    music_volume(8),
    show_messages(true),
    alwaysrun(false), // Always run is OFF
    key_right((int) KEY_RIGHTARROW),
    key_left((int) KEY_LEFTARROW),
    key_up((int) 'w'),
    key_down((int) 's'),
    key_strafeleft((int) 'a'),
    key_straferight((int) 'd'),
    key_fire((int) KEY_CTRL),
    key_use((int) ' '),
    key_strafe((int) KEY_ALT),
    key_speed((int) KEY_SHIFT),
    use_mouse(true),
    mouseb_fire(0),
    mouseb_strafe(2), // AX: Fixed
    mouseb_forward(1), // AX: Value inverted with the one above
    use_joystick(false),
    joyb_fire(0),
    joyb_strafe(1),
    joyb_use(3),
    joyb_speed(2),
    screenblocks(10),
    detaillevel(0),
    snd_channels(32),
    usegamma(0),
    mb_used(2),
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
    color_depth(BppMode.Indexed),
    fullscreen(false),
    fix_gamma_ramp(true),
    fix_gamma_palette(true),
    fix_sky_palette(false),
    fix_medi_need(true),
    fix_ouch_face(true),
    line_of_sight(LOS.Vanilla),
    vestrobe(true),
    parallelism_truecolor_tint(Runtime.getRuntime().availableProcessors()),
    parallelism_patch_columns(3),
    scale_screen_tiles(true),
    scale_melt(true),
    greyscale_filter(GreyscaleFilter.Average);
    
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
