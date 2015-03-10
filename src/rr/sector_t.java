package rr;

import static data.Limits.MAXINT;
import static data.Limits.MAX_ADJOINING_SECTORS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FRACBITS;
import static p.DoorDefines.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import m.IRandom;
import doom.think_t;
import doom.thinker_t;
import p.Resettable;
import p.ThinkerList;
import p.fireflicker_t;
import p.glow_t;
import p.lightflash_t;
import p.mobj_t;
import p.strobe_t;
import p.vldoor_e;
import p.vldoor_t;
import s.degenmobj_t;
import w.DoomIO;
import w.IPackableDoomObject;
import w.IReadableDoomObject;

/**
 * The SECTORS record, at runtime. Stores things/mobjs. Can be
 * archived/unarchived during savegames.
 * 
 * @author Maes
 */
public class sector_t
        implements IReadableDoomObject, IPackableDoomObject, Resettable {

    public ThinkerList TL;

    public IRandom RND;

    public sector_t() {
        blockbox = new int[4];
        id = -1;
    }

    /** (fixed_t) */
    public int floorheight, ceilingheight;

    public short floorpic;

    public short ceilingpic;

    public short lightlevel;

    public short special;

    public short tag;

    /** 0 = untraversed, 1,2 = sndlines -1 */
    public int soundtraversed;

    /** thing that made a sound (or null) (MAES: single pointer) */
    public mobj_t soundtarget;

    /** mapblock bounding box for height changes */
    public int[] blockbox;

    /**
     * origin for any sounds played by the sector. Used to be degenmobj_t, but
     * that's really a futile distinction.
     */
    public degenmobj_t soundorg;

    /** if == validcount, already checked */
    public int validcount;

    /** list of mobjs in sector (MAES: it's used as a linked list) */
    public mobj_t thinglist;

    /**
     * thinker_t for reversable actions. This actually was a void*, and in
     * practice it could store doors, plats, floors and ceiling objects.
     */
    public SectorAction specialdata;

    public int linecount;

    // struct line_s** lines; // [linecount] size
    // MAES: make this line_t[] for now?
    public line_t[] lines;

    /** Use for internal identification */
    public int id;

    /** killough 1/30/98: improves searches for tags. */
    public int nexttag,firsttag;  

    public String toString() {
        String str =
            String.format("Sector: %d %x %x %d %d %d %d %d", id, floorheight,
                ceilingheight, floorpic, ceilingpic, lightlevel, special, // needed?
                tag); // needed?

        return str;

    }

    //
    // Find minimum light from an adjacent sector
    //
    public int FindMinSurroundingLight(int max) {
        int i;
        int min;
        line_t line;
        sector_t check;

        min = max;
        for (i = 0; i < this.linecount; i++) {
            line = this.lines[i];
            check = line.getNextSector(this);

            if (check == null)
                continue;

            if (check.lightlevel < min)
                min = check.lightlevel;
        }
        return min;
    }

    //
    // P_FindLowestFloorSurrounding()
    // FIND LOWEST FLOOR HEIGHT IN SURROUNDING SECTORS
    //
    public int FindLowestFloorSurrounding() {
        int i;
        line_t check;
        sector_t other;
        int floor = this.floorheight;

        for (i = 0; i < this.linecount; i++) {
            check = this.lines[i];
            other = check.getNextSector(this);

            if (other == null)
                continue;

            if (other.floorheight < floor)
                floor = other.floorheight;
        }
        return floor;
    }

    /**
     * P_FindHighestFloorSurrounding() FIND HIGHEST FLOOR HEIGHT IN SURROUNDING
     * SECTORS Compatibility problem: apparently this is hardcoded for vanilla
     * compatibility (instead of Integer.MIN_VALUE), but it will cause some
     * "semi-Boom" maps not to work, since it won't be able to lower stuff below
     * -500 units. The correct fix here would be to allow for -compatlevel style
     * options. Maybe later.
     * 
     * @param sec
     */

    public int FindHighestFloorSurrounding() {
        int i;
        line_t check;
        sector_t other;

        int floor = -500 * FRACUNIT;

        for (i = 0; i < this.linecount; i++) {
            check = this.lines[i];
            other = check.getNextSector(this);

            // The compiler nagged about this being unreachable, with
            // some older 1.6 JDKs, but that's obviously not true.
            if (other == null)
                continue;

            if (other.floorheight > floor)
                floor = other.floorheight;
        }
        return floor;
    }

    /**
     * P_FindNextHighestFloor FIND NEXT HIGHEST FLOOR IN SURROUNDING SECTORS
     * Note: this should be doable w/o a fixed array.
     * 
     * @param sec
     * @param currentheight
     * @return fixed
     */

    public int FindNextHighestFloor(int currentheight) {
        int i;
        int h;
        int min;
        line_t check;
        sector_t other;
        int height = currentheight;

        int heightlist[] = new int[MAX_ADJOINING_SECTORS];

        for (i = 0, h = 0; i < this.linecount; i++) {
            check = this.lines[i];
            other = check.getNextSector(this);

            if (other == null)
                continue;

            if (other.floorheight > height)
                heightlist[h++] = other.floorheight;

            // Check for overflow. Exit.
            if (h >= MAX_ADJOINING_SECTORS) {
                System.err
                        .print("Sector with more than 20 adjoining sectors\n");
                break;
            }
        }

        // Find lowest height in list
        if (h == 0)
            return currentheight;

        min = heightlist[0];

        // Range checking?
        for (i = 1; i < h; i++)
            if (heightlist[i] < min)
                min = heightlist[i];

        return min;
    }

    //
    // FIND LOWEST CEILING IN THE SURROUNDING SECTORS
    //
    public int FindLowestCeilingSurrounding() {
        int i;
        line_t check;
        sector_t other;
        int height = MAXINT;

        for (i = 0; i < this.linecount; i++) {
            check = this.lines[i];
            other = check.getNextSector(this);

            if (other == null)
                continue;

            if (other.ceilingheight < height)
                height = other.ceilingheight;
        }
        return height;
    }

    //
    // FIND HIGHEST CEILING IN THE SURROUNDING SECTORS
    //
    public int FindHighestCeilingSurrounding() {
        int i;
        line_t check;
        sector_t other;
        int height = 0;

        for (i = 0; i < this.linecount; i++) {
            check = this.lines[i];
            other = check.getNextSector(this);

            if (other == null)
                continue;

            if (other.ceilingheight > height)
                height = other.ceilingheight;
        }
        return height;
    }

    //
    // P_SpawnFireFlicker
    //
    public void SpawnFireFlicker() {
        fireflicker_t flick;

        // Note that we are resetting sector attributes.
        // Nothing special about it during gameplay.
        this.special = 0;

        flick = new fireflicker_t(RND);
        flick.function = think_t.T_FireFlicker;
        TL.AddThinker(flick);
        flick.sector = this;
        flick.maxlight = this.lightlevel;
        flick.minlight = this.FindMinSurroundingLight(this.lightlevel) + 16;
        flick.count = 4;
    }

    /**
     * Spawn a door that opens after 5 minutes
     */

    public void SpawnDoorRaiseIn5Mins(int secnum) {
        vldoor_t door;

        door = new vldoor_t();

        this.specialdata = door;
        this.special = 0;
        door.function = think_t.T_VerticalDoor;
        TL.AddThinker(door);
        door.sector = this;
        door.direction = 2;
        door.type = vldoor_e.raiseIn5Mins;
        door.speed = VDOORSPEED;
        door.topheight = this.FindLowestCeilingSurrounding();
        door.topheight -= 4 * FRACUNIT;
        door.topwait = VDOORWAIT;
        door.topcountdown = 5 * 60 * 35;
    }

    //
    // Spawn a door that closes after 30 seconds
    //
    public void SpawnDoorCloseIn30() {
        vldoor_t door;

        door = new vldoor_t();

        this.specialdata = door;
        this.special = 0;

        door.function = think_t.T_VerticalDoor;
        TL.AddThinker(door);
        door.sector = this;
        door.direction = 0;
        door.type = vldoor_e.normal;
        door.speed = VDOORSPEED;
        door.topcountdown = 30 * 35;
    }

    //
    // P_SpawnStrobeFlash
    // After the map has been loaded, scan each sector
    // for specials that spawn thinkers
    //
    public void SpawnStrobeFlash(int fastOrSlow, int inSync) {
        strobe_t flash;

        flash = new strobe_t();
        flash.sector = this;
        flash.darktime = fastOrSlow;
        flash.brighttime = STROBEBRIGHT;
        flash.function = think_t.T_StrobeFlash;
        TL.AddThinker(flash);
        flash.maxlight = this.lightlevel;
        flash.minlight = this.FindMinSurroundingLight(this.lightlevel);

        if (flash.minlight == flash.maxlight)
            flash.minlight = 0;

        // nothing special about it during gameplay
        this.special = 0;

        if (inSync == 0)
            flash.count = (RND.P_Random() & 7) + 1;
        else
            flash.count = 1;
    }

    /**
     * P_SpawnLightFlash After the map has been loaded, scan each sector for
     * specials that spawn thinkers
     */

    public void SpawnLightFlash() {
        lightflash_t flash;

        // nothing special about it during gameplay
        special = 0;

        flash = new lightflash_t(RND);
        flash.function = think_t.T_LightFlash;
        TL.AddThinker((thinker_t) flash);
        flash.sector = this;
        flash.maxlight = lightlevel;

        flash.minlight = FindMinSurroundingLight(lightlevel);
        flash.maxtime = 64;
        flash.mintime = 7;
        flash.count = (RND.P_Random() & flash.maxtime) + 1;
    }

    public void SpawnGlowingLight() {
        glow_t g;

        g = new glow_t();
        g.sector = this;
        g.minlight = FindMinSurroundingLight(this.lightlevel);
        g.maxlight = lightlevel;
        g.function = think_t.T_Glow;
        TL.AddThinker(g);
        g.direction = -1;

        this.special = 0;
    }

    @Override
    public void read(DataInputStream f)
            throws IOException {

        // ACHTUNG: the only situation where we'd
        // like to read memory-format sector_t's is from
        // savegames, and in vanilla savegames, not all info
        // is saved (or read) from disk.

        this.floorheight = DoomIO.readLEShort(f) << FRACBITS;
        this.ceilingheight = DoomIO.readLEShort(f) << FRACBITS;
        // MAES: it may be necessary to apply a hack in order to
        // read vanilla savegames.
        this.floorpic = (short) DoomIO.readLEShort(f);
        this.ceilingpic = (short) DoomIO.readLEShort(f);
        // f.skipBytes(4);
        this.lightlevel = DoomIO.readLEShort(f);
        this.special = DoomIO.readLEShort(f); // needed?
        this.tag = DoomIO.readLEShort(f); // needed?
    }

    @Override
    public void pack(ByteBuffer b) {

        b.putShort((short) (floorheight >> FRACBITS));
        b.putShort((short) (ceilingheight >> FRACBITS));
        // MAES: it may be necessary to apply a hack in order to
        // read vanilla savegames.
        b.putShort(floorpic);
        b.putShort(ceilingpic);
        // f.skipBytes(4);
        b.putShort(lightlevel);
        b.putShort(special);
        b.putShort(tag);
    }

    @Override
    public void reset() {
        floorheight = 0;
        ceilingheight = 0;
        floorpic = 0;
        ceilingpic = 0;
        lightlevel = 0;
        special = 0;
        tag = 0;
        soundtraversed = 0;
        soundtarget = null;
        Arrays.fill(blockbox, 0);
        soundorg = null;
        validcount = 0;
        thinglist = null;
        specialdata = null;
        linecount = 0;
        lines = null;
        id = -1;

    }
}
