package v.scale;

/**
 * Interface for an object that conveys screen resolution/scaling information, meant to replace the static declarations
 * in Defines.
 *
 * @author admin
 *
 */
public interface VideoScale {

    //It is educational but futile to change this
    //scaling e.g. to 2. Drawing of status bar,
    //menues etc. is tied to the scale implied
    //by the graphics.
    public static double INV_ASPECT_RATIO = 0.625; // 0.75, ideally

    //
    // For resize of screen, at start of game.
    // It will not work dynamically, see visplanes.
    //
    public static final int BASE_WIDTH = 320;
    public static final int BASE_HEIGHT = (int) (INV_ASPECT_RATIO * 320); // 200

    int getScreenWidth();
    int getScreenHeight();
    int getScalingX();
    int getScalingY();

    /**
     * Safest global scaling for fixed stuff like menus, titlepic etc
     */
    int getSafeScaling();

    /**
     * Get floating point screen multiplier. Not recommended, as it causes visual glitches. Replace with safe scale,
     * whenever possible
     */
    float getScreenMul();

    /**
     * Future, should signal aware objects that they should refresh their resolution-dependent state, structures,
     * variables etc.
     *
     * @return
     */
    boolean changed();
}
