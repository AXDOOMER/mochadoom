package p;

import doom.DoomMain;
import doom.think_t;
import java.util.Arrays;
import rr.line_t;
import rr.sector_t;

public class SlideDoor<T, V> {

    // UNUSED
    // Separate into p_slidoor.c?
    // ABANDONED TO THE MISTS OF TIME!!!
    //
    // EV_SlidingDoor : slide a door horizontally
    // (animate midtexture, then set noblocking line)
    //
    public static final int MAXSLIDEDOORS = 5;
    // how many frames of animation
    public static final int SNUMFRAMES = 4;

    public static final int SDOORWAIT = 35 * 3;
    public static final int SWAITTICS = 4;

    final slidename_t[] slideFrameNames = {
        new slidename_t("GDOORF1", "GDOORF2", "GDOORF3", "GDOORF4", // front
        "GDOORB1", "GDOORB2", "GDOORB3", "GDOORB4"), // back

        new slidename_t(), new slidename_t(), new slidename_t(), new slidename_t()
    };

    final slideframe_t[] slideFrames;

    private final DoomMain<T, V> DOOM;

    public SlideDoor(DoomMain<T, V> DOOM) {
        slideFrames = new slideframe_t[MAXSLIDEDOORS];
        Arrays.setAll(slideFrames, i -> new slideframe_t());
        this.DOOM = DOOM;
    }

    void P_InitSlidingDoorFrames() {
        int i;
        int f1;
        int f2;
        int f3;
        int f4;

        // DOOM II ONLY...
        if (!DOOM.isCommercial()) {
            return;
        }

        for (i = 0; i < MAXSLIDEDOORS; i++) {
            if (slideFrameNames[i].frontFrame1 == null) {
                break;
            }

            f1 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].frontFrame1);
            f2 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].frontFrame2);
            f3 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].frontFrame3);
            f4 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].frontFrame4);

            slideFrames[i].frontFrames[0] = f1;
            slideFrames[i].frontFrames[1] = f2;
            slideFrames[i].frontFrames[2] = f3;
            slideFrames[i].frontFrames[3] = f4;

            f1 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].backFrame1);
            f2 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].backFrame2);
            f3 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].backFrame3);
            f4 = DOOM.textureManager.TextureNumForName(slideFrameNames[i].backFrame4);

            slideFrames[i].backFrames[0] = f1;
            slideFrames[i].backFrames[1] = f2;
            slideFrames[i].backFrames[2] = f3;
            slideFrames[i].backFrames[3] = f4;
        }
    }

    //
    // Return index into "slideFrames" array
    // for which door type to use
    //
    int P_FindSlidingDoorType(line_t line) {
        int i;
        int val;

        for (i = 0; i < MAXSLIDEDOORS; i++) {
            val = DOOM.levelLoader.sides[line.sidenum[0]].midtexture;
            if (val == slideFrames[i].frontFrames[0]) {
                return i;
            }
        }

        return -1;
    }

    public void
            EV_SlidingDoor(line_t line,
                    mobj_t thing) {
        sector_t sec;
        slidedoor_t door;

        // DOOM II ONLY...
        if (!DOOM.isCommercial()) {
            return;
        }

        System.err.println("EV_SlidingDoor");

        // Make sure door isn't already being animated
        sec = line.frontsector;
        door = null;
        if (sec.specialdata != null) {
            if (thing.player == null) {
                return;
            }

            door = (slidedoor_t) sec.specialdata;
            if (door.type == sdt_e.sdt_openAndClose) {
                if (door.status == sd_e.sd_waiting) {
                    door.status = sd_e.sd_closing;
                }
            } else {
                return;
            }
        }

        // Init sliding door vars
        if (door == null) {
            door = new slidedoor_t();
            DOOM.actions.AddThinker(door);
            sec.specialdata = door;

            door.type = sdt_e.sdt_openAndClose;
            door.status = sd_e.sd_opening;
            door.whichDoorIndex = P_FindSlidingDoorType(line);

            if (door.whichDoorIndex < 0) {
                DOOM.doomSystem.Error("EV_SlidingDoor: Can't use texture for sliding door!");
            }

            door.frontsector = sec;
            door.backsector = line.backsector;
            door.function = think_t.T_SlidingDoor;
            door.timer = SWAITTICS;
            door.frame = 0;
            door.line = line;
        }
    }
}