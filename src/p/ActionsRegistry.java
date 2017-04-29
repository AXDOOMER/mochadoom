package p;

import data.Limits;
import doom.DoomMain;
import doom.SourceCode;
import m.fixed_t;
import rr.line_t;

public abstract class ActionsRegistry<T, V> extends UnifiedGameMap<T, V> {
    
    protected static final int FUDGE = 0x800; ///(FRACUNIT/MAPFRACUNIT);
    //
    // P_XYMovement
    //
    protected static final int STOPSPEED = 0x1000;
    protected static final int FRICTION = 0xe800;
    //
    // FLOORS
    //
    protected static final int FLOORSPEED = fixed_t.MAPFRACUNIT;
    //
    // CEILINGS
    //
    protected ceiling_t[] activeceilings = new ceiling_t[Limits.MAXCEILINGS];
    //
    // P_BulletSlope
    // Sets a slope so a near miss is at aproximately
    // the height of the intended target
    //
    int bulletslope;
    //dirtype
    protected int d1;
    protected int d2;
    ///////////////// MOVEMENT'S ACTIONS ////////////////////////
    @SourceCode.fixed_t
    int[] tmbbox = new int[4];
    mobj_t tmthing;
    long tmflags;
    @SourceCode.fixed_t
    int tmx;
    @SourceCode.fixed_t
    int tmy;
    /**
     * If "floatok" true, move would be ok if within "tmfloorz - tmceilingz".
     */
    public boolean floatok;
    @SourceCode.fixed_t
    public int tmfloorz;
    @SourceCode.fixed_t
    public int tmceilingz;
    @SourceCode.fixed_t
    public int tmdropoffz;
    // keep track of the line that lowers the ceiling,
    // so missiles don't explode against sky hack walls
    public line_t ceilingline;
    public line_t[] spechit = new line_t[Limits.MAXSPECIALCROSS];
    public int numspechit;
    ////////////////// PTR Traverse Interception Functions ///////////////////////
    mobj_t shootthing;
    @SourceCode.fixed_t
    int attackrange;
    // Height if not aiming up or down
    // ???: use slope for monsters?
    @SourceCode.fixed_t
    int shootz;
    int la_damage;
    @SourceCode.fixed_t
    int aimslope;
    //
    // SLIDE MOVE
    // Allows the player to slide along any angled walls.
    //
    mobj_t slidemo;
    int bestslidefrac; // fixed
    int secondslidefrac;
    line_t bestslideline;
    line_t secondslideline;
    @SourceCode.fixed_t
    int tmxmove;
    @SourceCode.fixed_t
    int tmymove;
    //
    // P_LineAttack
    //
    /**
     * who got hit (or NULL)
     */
    public mobj_t linetarget;
    //
    // USE LINES
    //
    mobj_t usething;
    //
    // RADIUS ATTACK
    //
    mobj_t bombsource;
    mobj_t bombspot;
    int bombdamage;
    //
    // SECTOR HEIGHT CHANGING
    // After modifying a sectors floor or ceiling height,
    // call this routine to adjust the positions
    // of all things that touch the sector.
    //
    // If anything doesn't fit anymore, true will be returned.
    // If crunch is true, they will take damage
    //  as they are being crushed.
    // If Crunch is false, you should set the sector height back
    //  the way it was and call P_ChangeSector again
    //  to undo the changes.
    //
    boolean crushchange;
    boolean nofit;
    ///////////////////// PIT AND PTR FUNCTIONS //////////////////
    /**
     * PIT_VileCheck Detect a corpse that could be raised.
     */
    mobj_t vileCorpseHit;
    mobj_t vileObj;
    int vileTryX;
    int vileTryY;

    public ActionsRegistry(DoomMain<T, V> DOOM) {
        super(DOOM);
    }
    
    public interface Observer<T, V> {
        ActionsRegistry<T, V> obs();
    }
}
