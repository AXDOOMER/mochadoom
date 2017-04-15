package p;

import static data.Defines.ITEMQUESIZE;
import static data.Defines.MELEERANGE;
import static data.Defines.NF_SUBSECTOR;
import static data.Defines.NUMAMMO;
import static data.Defines.RANGECHECK;
import static data.Defines.pw_allmap;
import static data.Defines.pw_infrared;
import static data.Defines.pw_invisibility;
import static data.Defines.pw_invulnerability;
import static data.Defines.pw_ironfeet;
import static data.Defines.pw_strength;
import data.Limits;
import static data.Limits.BUTTONTIME;
import static data.Limits.MAXANIMS;
import static data.Limits.MAXBUTTONS;
import static data.Limits.MAXINT;
import static data.Limits.MAXINTERCEPTS;
import static data.Limits.MAXSPECIALCROSS;
import static data.Limits.MAXSWITCHES;
import static data.Limits.PLATSPEED;
import static data.Limits.PLATWAIT;
import static data.Tables.ANG270;
import static data.Tables.ANG90;
import static data.Tables.BITS32;
import static data.info.mobjinfo;
import static data.info.states;
import data.mapthing_t;
import data.mobjtype_t;
import data.sounds.sfxenum_t;
import defines.ammotype_t;
import defines.card_t;
import defines.statenum_t;
import doom.DoomMain;
import static doom.englsh.GOTARMBONUS;
import static doom.englsh.GOTARMOR;
import static doom.englsh.GOTBACKPACK;
import static doom.englsh.GOTBERSERK;
import static doom.englsh.GOTBFG9000;
import static doom.englsh.GOTBLUECARD;
import static doom.englsh.GOTBLUESKUL;
import static doom.englsh.GOTCELL;
import static doom.englsh.GOTCELLBOX;
import static doom.englsh.GOTCHAINGUN;
import static doom.englsh.GOTCHAINSAW;
import static doom.englsh.GOTCLIP;
import static doom.englsh.GOTCLIPBOX;
import static doom.englsh.GOTHTHBONUS;
import static doom.englsh.GOTINVIS;
import static doom.englsh.GOTINVUL;
import static doom.englsh.GOTLAUNCHER;
import static doom.englsh.GOTMAP;
import static doom.englsh.GOTMEDIKIT;
import static doom.englsh.GOTMEDINEED;
import static doom.englsh.GOTMEGA;
import static doom.englsh.GOTMSPHERE;
import static doom.englsh.GOTPLASMA;
import static doom.englsh.GOTREDCARD;
import static doom.englsh.GOTREDSKULL;
import static doom.englsh.GOTROCKBOX;
import static doom.englsh.GOTROCKET;
import static doom.englsh.GOTSHELLBOX;
import static doom.englsh.GOTSHELLS;
import static doom.englsh.GOTSHOTGUN;
import static doom.englsh.GOTSHOTGUN2;
import static doom.englsh.GOTSTIM;
import static doom.englsh.GOTSUIT;
import static doom.englsh.GOTSUPER;
import static doom.englsh.GOTVISOR;
import static doom.englsh.GOTYELWCARD;
import static doom.englsh.GOTYELWSKUL;
import static doom.items.weaponinfo;
import doom.player_t;
import doom.th_class;
import doom.think_t;
import doom.thinker_t;
import doom.weapontype_t;
import java.util.Arrays;
import m.Settings;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.MAPFRACUNIT;
import static p.DoorDefines.SLOWDARK;
import static p.MapUtils.AproxDistance;
import static p.MapUtils.InterceptVector;
import static p.MobjFlags.*;
import rr.ISpriteManager;
import rr.line_t;
import static rr.line_t.*;
import rr.node_t;
import rr.sector_t;
import rr.side_t;
import rr.subsector_t;
import utils.C2JUtils;
import static utils.C2JUtils.eval;
import static utils.C2JUtils.flags;

// // FROM SIGHT

public abstract class UnifiedGameMap<T, V> implements ThinkerList {
    
    
    public UnifiedGameMap(DoomMain<T, V> DOOM){
        this.SW = new Switches();
        this.LEV = new Lights();
        this.SPECS = new Specials();
        this.PEV = new Plats();
        this.See = new Sight(); // Didn't initialize that.
        this.EN = new Enemies();
        this.thinkercap = new thinker_t();
        for (int i=0; i<th_class.NUMTHCLASS; i++)  // killough 8/29/98: initialize threaded lists
            thinkerclasscap[i]=new thinker_t();
        
        this.RemoveThinkerDelayed=new P_RemoveThinkerDelayed();
        
        intercepts = new intercept_t[MAXINTERCEPTS];
        Arrays.setAll(intercepts, i -> new intercept_t());

        // Normally unused. It clashes with line attribute 124, and looks like ass
        // anyway. However it's fully implemented.
        //this.SL=new SlideDoor(DS);
        //DS.SL=SL;
        this.DOOM = DOOM;
        this.FUNS = new ActionFunctions(this.DOOM, EN);
        
        // "Wire" all states to the proper functions.
        for (int i=0;i<states.length;i++){
        	FUNS.doWireState(states[i]);
        }

        
    }
    
    /////////////////// STATUS ///////////////////

    final DoomMain<T, V> DOOM;

    
    // //////////// Internal singletons //////////////
    public Actions A;

    protected Specials SPECS;

    // DoorsFloors EV;
    protected Plats PEV;

    protected Lights LEV;

    protected Switches SW;

    protected Sight See;

    protected Enemies EN;
    
    protected ActionFunctions FUNS;
    
    protected SlideDoor SL;

    // ////////////////////////////////////////////

    public int topslope;

    public int bottomslope; // slopes to top and bottom of target

    int attackrange;
    
    

    //
    // UTILITIES
    //

    //
    // getSide()
    // Will return a side_t*
    // given the number of the current sector,
    // the line number, and the side (0/1) that you want.
    //
    side_t getSide(int currentSector, int line, int side) {
        return DOOM.levelLoader.sides[(DOOM.levelLoader.sectors[currentSector].lines[line]).sidenum[side]];
    }

    /**
     * getSector()
     * Will return a sector_t
     * given the number of the current sector,
     * the line number and the side (0/1) that you want.
     */
    
    sector_t getSector(int currentSector, int line, int side) {
        return DOOM.levelLoader.sides[(DOOM.levelLoader.sectors[currentSector].lines[line]).sidenum[side]].sector;
    }

    /**
     * twoSided()
     * Given the sector number and the line number,
     * it will tell you whether the line is two-sided or not.
     */
    
    protected boolean twoSided(int sector, int line) {
        return eval((DOOM.levelLoader.sectors[sector].lines[line]).flags& ML_TWOSIDED);
    }

    /**
     * RETURN NEXT SECTOR # THAT LINE TAG REFERS TO
     */
    
    protected int FindSectorFromLineTag(line_t line, int start) {
        int i;

        for (i = start + 1; i < DOOM.levelLoader.numsectors; i++)
            if (DOOM.levelLoader.sectors[i].tag == line.tag)
                return i;

        return -1;
    }

    

    // //////////////////// FROM p_maputl.c ////////////////////

    /** fixed_t */
    protected int opentop, openbottom, openrange, lowfloor;

    /**
     * P_LineOpening Sets opentop and openbottom to the window through a two
     * sided line. OPTIMIZE: keep this precalculated
     */

    public void LineOpening(line_t linedef) {
        sector_t front;
        sector_t back;

        if (linedef.sidenum[1] == line_t.NO_INDEX) {
            // single sided line
            openrange = 0;
            return;
        }

        front = linedef.frontsector;
        back = linedef.backsector;

        if (front.ceilingheight < back.ceilingheight)
            opentop = front.ceilingheight;
        else
            opentop = back.ceilingheight;

        if (front.floorheight > back.floorheight) {
            openbottom = front.floorheight;
            lowfloor = back.floorheight;
        } else {
            openbottom = back.floorheight;
            lowfloor = front.floorheight;
        }

        openrange = opentop - openbottom;
    }

    //
    // THING POSITION SETTING
    //

    /**
     * P_UnsetThingPosition Unlinks a thing from block map and sectors. On each
     * position change, BLOCKMAP and other lookups maintaining lists ot things
     * inside these structures need to be updated.
     */

    public void UnsetThingPosition(mobj_t thing) {
        final int blockx;
        final int blocky;

        if (!eval(thing.flags& MF_NOSECTOR)) {
            // inert things don't need to be in blockmap?
            // unlink from subsector
            if (thing.snext != null)
                ((mobj_t) thing.snext).sprev = thing.sprev;

            if (thing.sprev != null)
                ((mobj_t) thing.sprev).snext = thing.snext;
            else
                thing.subsector.sector.thinglist = (mobj_t) thing.snext;
        }

        if (!eval(thing.flags& MF_NOBLOCKMAP)) {
            // inert things don't need to be in blockmap
            // unlink from block map
            if (thing.bnext != null)
                ((mobj_t) thing.bnext).bprev = thing.bprev;

            if (thing.bprev != null)
                ((mobj_t) thing.bprev).bnext = thing.bnext;
            else {
                blockx = DOOM.levelLoader.getSafeBlockX(thing.x - DOOM.levelLoader.bmaporgx);
                blocky = DOOM.levelLoader.getSafeBlockY(thing.y - DOOM.levelLoader.bmaporgy);
                
                if (blockx >= 0 && blockx < DOOM.levelLoader.bmapwidth && blocky >= 0
                        && blocky < DOOM.levelLoader.bmapheight) {
                    DOOM.levelLoader.blocklinks[blocky * DOOM.levelLoader.bmapwidth + blockx] =
                        (mobj_t) thing.bnext;
                }
            }
        }
    }

    //
    // BLOCK MAP ITERATORS
    // For each line/thing in the given mapblock,
    // call the passed PIT_* function.
    // If the function returns false,
    // exit with false without checking anything else.
    //

    //
    // INTERCEPT ROUTINES
    //
    protected intercept_t[] intercepts;

    int intercept_p;

    public divline_t trace=new divline_t();

    boolean earlyout;

    int ptflags;
    
    //
    //P_TraverseIntercepts
    //Returns true if the traverser function returns true
    //for all lines.
    //
    boolean
    TraverseIntercepts
    ( PTR_InterceptFunc   func,
    int   maxfrac )
    {
     int         count;
     int     dist; //fixed_t
     intercept_t    in=null;  // shut up compiler warning
     
     count = intercept_p;

     while (count-->0)
     {
     dist = MAXINT;
     for (int scan = 0 ; scan<intercept_p ; scan++)
     {
         if (intercepts[scan].frac < dist)
         {
         dist = intercepts[scan].frac;
         in = intercepts[scan];
         }
     }
     
     if (dist > maxfrac)
         return true;    // checked everything in range      

    /*  // UNUSED
     {
     // don't check these yet, there may be others inserted
     in = scan = intercepts;
     for ( scan = intercepts ; scan<intercept_p ; scan++)
         if (scan.frac > maxfrac)
         *in++ = *scan;
     intercept_p = in;
     return false;
     }
    */

         if ( !func.invoke(in) )
         return false;   // don't bother going farther

     in.frac = MAXINT;
     }
     
     return true;        // everything was traversed
    }
    
    protected void UpdateThinker(thinker_t thinker)
    {
      thinker_t th;
      // find the class the thinker belongs to

      th_class cls =
        thinker.function == think_t.NOP ? th_class.th_delete :
        thinker.function == think_t.P_MobjThinker &&
        ((mobj_t) thinker).health > 0 &&
        (eval((((mobj_t) thinker).flags) & MF_COUNTKILL) ||
         ((mobj_t) thinker).type == mobjtype_t.MT_SKULL) ?
        eval((((mobj_t) thinker).flags) & MF_FRIEND) ?
        th_class.th_friends : th_class.th_enemies : th_class.th_misc;

      {
        /* Remove from current thread, if in one */
        if ((th = thinker.cnext)!= null)
          (th.cprev = thinker.cprev).cnext = th;
      }

      // Add to appropriate thread
      th = thinkerclasscap[cls.ordinal()];
      th.cprev.cnext = thinker;
      thinker.cnext = th;
      thinker.cprev = th.cprev;
      th.cprev = thinker;
    }
    
    protected final thinker_t[] thinkerclasscap=new thinker_t[th_class.NUMTHCLASS];

    public boolean sight_debug;
    
    protected final void ResizeIntercepts() {
        intercepts=C2JUtils.resize(intercepts[0],intercepts,intercepts.length*2);
    	}    

    class Lights {

        //
        // Start strobing lights (usually from a trigger)
        //
        void StartLightStrobing(line_t line) {
            int secnum;
            sector_t sec;

            secnum = -1;
            while ((secnum = FindSectorFromLineTag(line, secnum)) >= 0) {
                sec = DOOM.levelLoader.sectors[secnum];
                if (sec.specialdata != null)
                    continue;

                sec.SpawnStrobeFlash(SLOWDARK, 0);
            }
        }

        //
        // TURN LINE'S TAG LIGHTS OFF
        //
        void TurnTagLightsOff(line_t line) {
            int i;
            int min;
            sector_t sector;
            sector_t tsec;
            line_t templine;

            for (int j = 0; j < DOOM.levelLoader.numsectors; j++) {
                sector = DOOM.levelLoader.sectors[j];
                if (sector.tag == line.tag) {

                    min = sector.lightlevel;
                    for (i = 0; i < sector.linecount; i++) {
                        templine = sector.lines[i];
                        tsec = templine.getNextSector(sector);
                        if (tsec == null)
                            continue;
                        if (tsec.lightlevel < min)
                            min = tsec.lightlevel;
                    }
                    sector.lightlevel = (short) min;
                }
            }
        }

        //
        // TURN LINE'S TAG LIGHTS ON
        //
        void LightTurnOn(line_t line, int bright) {

            sector_t sector;
            sector_t temp;
            line_t templine;

            for (int i = 0; i < DOOM.levelLoader.numsectors; i++) {
                sector = DOOM.levelLoader.sectors[i];
                if (sector.tag == line.tag) {
                    // bright = 0 means to search
                    // for highest light level
                    // surrounding sector
                    if (bright == 0) {
                        for (int j = 0; j < sector.linecount; j++) {
                            templine = sector.lines[j];
                            temp = templine.getNextSector( sector);

                            if (temp == null)
                                continue;

                            if (temp.lightlevel > bright)
                                bright = temp.lightlevel;
                        }
                    }
                    sector.lightlevel = (short) bright;
                }
            }
        }

    }

    class Enemies {

        // void A_Fall (mobj_t *actor);

        //
        // ENEMY THINKING
        // Enemies are allways spawned
        // with targetplayer = -1, threshold = 0
        // Most monsters are spawned unaware of all players,
        // but some can be made preaware
        //

        /**
         * P_CheckMeleeRange
         */

        boolean CheckMeleeRange(mobj_t actor) {
            mobj_t pl;
            int dist; // fixed_t

            if (actor.target == null)
                return false;

            pl = actor.target;
            dist = AproxDistance(pl.x - actor.x, pl.y - actor.y);

            if (dist >= MELEERANGE - 20 * FRACUNIT + pl.info.radius)
                return false;

            if (!EN.CheckSight(actor, actor.target))
                return false;

            return true;
        }

        /**
         * P_CheckMissileRange
         */

        boolean CheckMissileRange(mobj_t actor) {
            int dist; // fixed_t

            if (!CheckSight(actor, actor.target))
                return false;

            if ((actor.flags & MF_JUSTHIT) != 0) {
                // the target just hit the enemy,
                // so fight back!
                actor.flags &= ~MF_JUSTHIT;
                return true;
            }

            if (actor.reactiontime != 0)
                return false; // do not attack yet

            // OPTIMIZE: get this from a global checksight
            dist =
                AproxDistance(actor.x - actor.target.x, actor.y
                        - actor.target.y)
                        - 64 * FRACUNIT;

            // [SYNC}: Major desync cause of desyncs.
            // DO NOT compare with null!
            if (actor.info.meleestate == statenum_t.S_NULL)
                dist -= 128 * FRACUNIT; // no melee attack, so fire more

            dist >>= 16;

            if (actor.type == mobjtype_t.MT_VILE) {
                if (dist > 14 * 64)
                    return false; // too far away
            }

            if (actor.type == mobjtype_t.MT_UNDEAD) {
                if (dist < 196)
                    return false; // close for fist attack
                dist >>= 1;
            }

            if (actor.type == mobjtype_t.MT_CYBORG
                    || actor.type == mobjtype_t.MT_SPIDER
                    || actor.type == mobjtype_t.MT_SKULL) {
                dist >>= 1;
            }

            if (dist > 200)
                dist = 200;

            if (actor.type == mobjtype_t.MT_CYBORG && dist > 160)
                dist = 160;

            if (DOOM.random.P_Random() < dist)
                return false;

            return true;
        }

        /**
         * P_CheckSight Returns true if a straight line between t1 and t2 is
         * unobstructed. Uses REJECT.
         */

        boolean CheckSight(mobj_t t1, mobj_t t2) {
            int s1;
            int s2;
            int pnum;
            int bytenum;
            int bitnum;

            // First check for trivial rejection.

            // Determine subsector entries in REJECT table.
            s1 = t1.subsector.sector.id; // (t1.subsector.sector - sectors);
            s2 = t2.subsector.sector.id;// - sectors);
            pnum = s1 * DOOM.levelLoader.numsectors + s2;
            bytenum = pnum >> 3;
            bitnum = 1 << (pnum & 7);

            // Check in REJECT table.
            if (eval(DOOM.levelLoader.rejectmatrix[bytenum]& bitnum)) {
                See.sightcounts[0]++;

                // can't possibly be connected
                return false;
            }

            // An unobstructed LOS is possible.
            // Now look from eyes of t1 to any part of t2.
            See.sightcounts[1]++;

            DOOM.sceneRenderer.increaseValidCount(1);

            See.sightzstart = t1.z + t1.height - (t1.height >> 2);
            topslope = (t2.z + t2.height) - See.sightzstart;
            bottomslope = (t2.z) - See.sightzstart;

            See.strace.x = t1.x;
            See.strace.y = t1.y;
            See.t2x = t2.x;
            See.t2y = t2.y;
            See.strace.dx = t2.x - t1.x;
            See.strace.dy = t2.y - t1.y;

            // the head node is the last node output
            return See.CrossBSPNode(DOOM.levelLoader.numnodes - 1);
        }

        //
        // Called by P_NoiseAlert.
        // Recursively traverse adjacent sectors,
        // sound blocking lines cut off traversal.
        //

        mobj_t soundtarget;

        private void RecursiveSound(sector_t sec, int soundblocks) {
            int i;
            line_t check;
            sector_t other;

            // wake up all monsters in this sector
            if (sec.validcount == DOOM.sceneRenderer.getValidCount()
                    && sec.soundtraversed <= soundblocks + 1) {
                return; // already flooded
            }

            sec.validcount = DOOM.sceneRenderer.getValidCount();
            sec.soundtraversed = soundblocks + 1;
            sec.soundtarget = soundtarget;

            // "peg" to the level loader for syntactic sugar
            side_t[] sides = DOOM.levelLoader.sides;

            for (i = 0; i < sec.linecount; i++) {
                check = sec.lines[i];
                if ((check.flags & ML_TWOSIDED) == 0)
                    continue;

                LineOpening(check);

                if (openrange <= 0)
                    continue; // closed door

                if (sides[check.sidenum[0]].sector == sec)
                    other = sides[check.sidenum[1]].sector;
                else
                    other = sides[check.sidenum[0]].sector;

                if ((check.flags & ML_SOUNDBLOCK) != 0) {
                    if (soundblocks == 0)
                        RecursiveSound(other, 1);
                } else
                    RecursiveSound(other, soundblocks);
            }
        }

        /**
         * P_NoiseAlert
         * If a monster yells at a player,
         * it will alert other monsters to the player.
         */
        
        void NoiseAlert(mobj_t target, mobj_t emmiter) {
            soundtarget = target;
            DOOM.sceneRenderer.increaseValidCount(1);
            RecursiveSound(emmiter.subsector.sector, 0);
        }

        /**
         * P_FireWeapon. Originally in pspr
         */
        public void FireWeapon(player_t player) {
            statenum_t newstate;

            if (!player.CheckAmmo())
                return;

            player.mo.SetMobjState(statenum_t.S_PLAY_ATK1);
            newstate = weaponinfo[player.readyweapon.ordinal()].atkstate;
            player.SetPsprite(player_t.ps_weapon, newstate);
            NoiseAlert(player.mo, player.mo);
        }

        //
        // P_Move
        // Move in the current direction,
        // returns false if the move is blocked.
        //

        // Peg to map movement
        line_t[] spechitp = new line_t[MAXSPECIALCROSS];

        int numspechit;

        /**
         * P_LookForPlayers If allaround is false, only look 180 degrees in
         * front. Returns true if a player is targeted.
         */

        boolean LookForPlayers(mobj_t actor, boolean allaround) {
            int c;
            int stop;
            player_t player;
            // sector_t sector;
            long an; // angle
            int dist; // fixed

            // sector = actor.subsector.sector;

            c = 0;
            stop = (actor.lastlook - 1) & 3;

            for (;; actor.lastlook = (actor.lastlook + 1) & 3) {
                if (!DOOM.playeringame[actor.lastlook])
                    continue;

                if (c++ == 2 || actor.lastlook == stop) {
                    // done looking
                    return false;
                }

                player = DOOM.players[actor.lastlook];

                if (player.health[0] <= 0)
                    continue; // dead

                if (!CheckSight(actor, player.mo))
                    continue; // out of sight

                if (!allaround) {
                    an =
                        (DOOM.sceneRenderer.PointToAngle2(actor.x, actor.y, player.mo.x,
                            player.mo.y)
                                - actor.angle)&BITS32;

                    if (an > ANG90 && an < ANG270) {
                        dist =
                            AproxDistance(player.mo.x - actor.x, player.mo.y
                                    - actor.y);
                        // if real close, react anyway
                        if (dist > MELEERANGE)
                            continue; // behind back
                    }
                }

                actor.target = player.mo;
                return true;
            }
            // The compiler complains that this is unreachable
            // return false;
        }

    }

    class Plats {

        public Plats() {
        	initActivePlats();
        }

        plat_t[] activeplats;

        //
        // Do Platforms
        // "amount" is only used for SOME platforms.
        //
        boolean DoPlat(line_t line, plattype_e type, int amount) {
            plat_t plat;
            int secnum = -1;
            boolean rtn = false;
            sector_t sec;

            // Activate all <type> plats that are in_stasis
            switch (type) {
            case perpetualRaise:
                ActivateInStasis(line.tag);
                break;

            default:
                break;
            }

            while ((secnum = FindSectorFromLineTag(line, secnum)) >= 0) {
                sec = DOOM.levelLoader.sectors[secnum];

                if (sec.specialdata != null)
                    continue;

                // Find lowest & highest floors around sector
                rtn = true;
                plat = new plat_t();


                plat.type = type;
                plat.sector = sec;
                plat.sector.specialdata = plat;
                plat.function = think_t.T_PlatRaise;
                AddThinker(plat);
                plat.crush = false;
                plat.tag = line.tag;

                switch (type) {
                case raiseToNearestAndChange:
                    plat.speed = PLATSPEED / 2;
                    sec.floorpic = DOOM.levelLoader.sides[line.sidenum[0]].sector.floorpic;
                    plat.high = sec.FindNextHighestFloor(sec.floorheight);
                    plat.wait = 0;
                    plat.status = plat_e.up;
                    // NO MORE DAMAGE, IF APPLICABLE
                    sec.special = 0;

                    DOOM.doomSound.StartSound(sec.soundorg,sfxenum_t.sfx_stnmov);
                    break;

                case raiseAndChange:
                    plat.speed = PLATSPEED / 2;
                    sec.floorpic = DOOM.levelLoader.sides[line.sidenum[0]].sector.floorpic;
                    plat.high = sec.floorheight + amount * FRACUNIT;
                    plat.wait = 0;
                    plat.status = plat_e.up;

                    DOOM.doomSound.StartSound(sec.soundorg,sfxenum_t.sfx_stnmov);
                    break;

                case downWaitUpStay:
                    plat.speed = PLATSPEED * 4;
                    plat.low = sec.FindLowestFloorSurrounding();

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = sec.floorheight;
                    plat.wait = 35 * PLATWAIT;
                    plat.status = plat_e.down;
                    DOOM.doomSound.StartSound(sec.soundorg,sfxenum_t.sfx_pstart);
                    break;

                case blazeDWUS:
                    plat.speed = PLATSPEED * 8;
                    plat.low = sec.FindLowestFloorSurrounding();

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = sec.floorheight;
                    plat.wait = 35 * PLATWAIT;
                    plat.status = plat_e.down;
                    DOOM.doomSound.StartSound(sec.soundorg,sfxenum_t.sfx_pstart);
                    break;

                case perpetualRaise:
                    plat.speed = PLATSPEED;
                    plat.low = sec.FindLowestFloorSurrounding();

                    if (plat.low > sec.floorheight)
                        plat.low = sec.floorheight;

                    plat.high = sec.FindHighestFloorSurrounding();

                    if (plat.high < sec.floorheight)
                        plat.high = sec.floorheight;

                    plat.wait = 35 * PLATWAIT;
                    // Guaranteed to be 0 or 1.
                    plat.status = plat_e.values()[DOOM.random.P_Random() & 1];

                    DOOM.doomSound.StartSound(sec.soundorg,sfxenum_t.sfx_pstart);
                    break;
                }
                AddActivePlat(plat);
            }
            return rtn;
        }

        void ActivateInStasis(int tag) {
            int i;

            for (i = 0; i < activeplats.length; i++)
                if ((activeplats[i] != null) && (activeplats[i].tag == tag)
                        && (activeplats[i].status == plat_e.in_stasis)) {
                    (activeplats[i]).status = (activeplats[i]).oldstatus;
                    (activeplats[i]).function = think_t.T_PlatRaise;
                    FUNS.doWireThinker(activeplats[i]);
                }
        }

        void StopPlat(line_t line) {
            int j;

            for (j = 0; j < activeplats.length; j++)
                if ((activeplats[j] != null)
                        && (activeplats[j].status != plat_e.in_stasis)
                        && (activeplats[j].tag == line.tag)) {
                    (activeplats[j]).oldstatus = (activeplats[j]).status;
                    (activeplats[j]).status = plat_e.in_stasis;
                    (activeplats[j]).function = null;
                    FUNS.doWireThinker(activeplats[j]);
                }
        }

        
        /*
        void AddActivePlat(plat_t plat) {
            int i;

            for (i = 0; i < MAXPLATS; i++)
                if (activeplats[i] == null) {
                    activeplats[i] = plat;
                    return;
                }
            I.Error("P_AddActivePlat: no more plats!");
        } */

        void RemoveActivePlat(plat_t plat) {
            int i;
            for (i = 0; i < activeplats.length; i++)
                if (plat == activeplats[i]) {
                    (activeplats[i]).sector.specialdata = null;
                    RemoveThinker(activeplats[i]);
                    activeplats[i] = null;

                    return;
                }
            DOOM.doomSystem.Error("P_RemoveActivePlat: can't find plat!");
        }

		public void initActivePlats() {
			// activeplats is just a placeholder. Plat objects aren't
			// actually reused, so we don't need an initialized array.
			// Same rule when resizing.
			activeplats=new plat_t[data.Limits.MAXPLATS];
		}

    }

    class Sight {
        
        public Sight(){
            strace=new divline_t();
            sightcounts= new int[2];
        }
        
        int sightzstart; // eye z of looker

        divline_t strace; // from t1 to t2

        int t2x;

        int t2y;

        int[] sightcounts ;

        

        /**
         * P_CrossSubsector Returns true if strace crosses the given subsector
         * successfully.
         */

        boolean CrossSubsector(int num) {
            int seg; // pointer inside segs
            line_t line;
            int s1;
            int s2;
            int count;
            subsector_t sub;
            sector_t front;
            sector_t back;
            int opentop; // fixed_t
            int openbottom;
            divline_t divl = new divline_t();
            //vertex_t v1;
            //vertex_t v2;
            int frac; // fixed_t
            int slope;

            if (RANGECHECK) {
                if (num >= DOOM.levelLoader.numsubsectors)
                    DOOM.doomSystem.Error("P_CrossSubsector: ss %d with numss = %d",
                        num, DOOM.levelLoader.numsubsectors);
            }

            sub = DOOM.levelLoader.subsectors[num];

            // check lines
            count = sub.numlines;
            seg = sub.firstline;// LL.segs[sub.firstline];

            for (; count > 0; seg++, count--) {
                line = DOOM.levelLoader.segs[seg].linedef;

                // allready checked other side?
                if (line.validcount == DOOM.sceneRenderer.getValidCount())
                    continue;

                line.validcount = DOOM.sceneRenderer.getValidCount();

                //v1 = line.v1;
                //v2 = line.v2;
                s1 = strace.DivlineSide(line.v1x, line.v1y);
                s2 = strace.DivlineSide(line.v2x, line.v2y);

                // line isn't crossed?
                if (s1 == s2)
                    continue;

                divl.x = line.v1x;
                divl.y = line.v1y;
                divl.dx = line.v2x - line.v1x;
                divl.dy = line.v2y - line.v1y;
                s1 = divl.DivlineSide(strace.x, strace.y);
                s2 = divl.DivlineSide(t2x, t2y);

                // line isn't crossed?
                if (s1 == s2)
                    continue;

                // stop because it is not two sided anyway
                // might do this after updating validcount?
                if (!flags(line.flags,ML_TWOSIDED))
                    return false;

                // crosses a two sided line
                front = DOOM.levelLoader.segs[seg].frontsector;
                back = DOOM.levelLoader.segs[seg].backsector;

                // no wall to block sight with?
                if (front.floorheight == back.floorheight
                        && front.ceilingheight == back.ceilingheight)
                    continue;

                // possible occluder
                // because of ceiling height differences
                if (front.ceilingheight < back.ceilingheight)
                    opentop = front.ceilingheight;
                else
                    opentop = back.ceilingheight;

                // because of ceiling height differences
                if (front.floorheight > back.floorheight)
                    openbottom = front.floorheight;
                else
                    openbottom = back.floorheight;

                // quick test for totally closed doors
                if (openbottom >= opentop)
                    return false; // stop

                frac = MapUtils.P_InterceptVector(strace, divl);

                if (front.floorheight != back.floorheight) {
                    slope = FixedDiv(openbottom - sightzstart, frac);
                    if (slope > bottomslope)
                        bottomslope = slope;
                }

                if (front.ceilingheight != back.ceilingheight) {
                    slope = FixedDiv(opentop - sightzstart, frac);
                    if (slope < topslope)
                        topslope = slope;
                }

                if (topslope <= bottomslope)
                    return false; // stop
            }
            // passed the subsector ok
            return true;
        }

        /**
         * P_CrossBSPNode Returns true if strace crosses the given node
         * successfully.
         */

        boolean CrossBSPNode(int bspnum) {
            node_t bsp;
            int side;

            if (eval(bspnum& NF_SUBSECTOR)) {
                if (bspnum == -1)
                    return CrossSubsector(0);
                else
                    return CrossSubsector(bspnum & (~NF_SUBSECTOR));
            }

            bsp = DOOM.levelLoader.nodes[bspnum];

            // decide which side the start point is on
            side = bsp.DivlineSide(strace.x, strace.y);
            if (side == 2)
                side = 0; // an "on" should cross both sides

            // cross the starting side
            if (!CrossBSPNode(bsp.children[side]))
                return false;

            // the partition plane is crossed here
            if (side == bsp.DivlineSide(t2x, t2y)) {
                // the line doesn't touch the other side
                return true;
            }

            // cross the ending side
            return CrossBSPNode(bsp.children[side ^ 1]);
        }

    }

    //
    // P_InitPicAnims
    //

    /**
     * Floor/ceiling animation sequences, defined by first and last frame, i.e.
     * the flat (64x64 tile) name to be used. The full animation sequence is
     * given using all the flats between the start and end entry, in the order
     * found in the WAD file.
     */

    private final animdef_t[] animdefs =
        {
                new animdef_t(false, "NUKAGE3", "NUKAGE1", 8),
                new animdef_t(false, "FWATER4", "FWATER1", 8),
                new animdef_t(false, "SWATER4", "SWATER1", 8),
                new animdef_t(false, "LAVA4", "LAVA1", 8),
                new animdef_t(false, "BLOOD3", "BLOOD1", 8),

                // DOOM II flat animations.
                new animdef_t(false, "RROCK08", "RROCK05", 8),
                new animdef_t(false, "SLIME04", "SLIME01", 8),
                new animdef_t(false, "SLIME08", "SLIME05", 8),
                new animdef_t(false, "SLIME12", "SLIME09", 8),

                new animdef_t(true, "BLODGR4", "BLODGR1", 8),
                new animdef_t(true, "SLADRIP3", "SLADRIP1", 8),

                new animdef_t(true, "BLODRIP4", "BLODRIP1", 8),
                new animdef_t(true, "FIREWALL", "FIREWALA", 8),
                new animdef_t(true, "GSTFONT3", "GSTFONT1", 8),
                new animdef_t(true, "FIRELAVA", "FIRELAV3", 8),
                new animdef_t(true, "FIREMAG3", "FIREMAG1", 8),
                new animdef_t(true, "FIREBLU2", "FIREBLU1", 8),
                new animdef_t(true, "ROCKRED3", "ROCKRED1", 8),

                new animdef_t(true, "BFALL4", "BFALL1", 8),
                new animdef_t(true, "SFALL4", "SFALL1", 8),
                new animdef_t(true, "WFALL4", "WFALL1", 8),
                new animdef_t(true, "DBRAIN4", "DBRAIN1", 8)
        };
                // MAES: this was a cheap trick to mark the end of the sequence
                // with a value of "-1".
                // It won't work in Java, so just use animdefs.length-1
                // new animdef_t(false, "", "", 0) };

    

    //
    // SPECIAL SPAWNING
    //

    class Specials {
        public static final int ok = 0, crushed = 1, pastdest = 2;

        protected line_t[] linespeciallist = new line_t[Limits.MAXLINEANIMS];
        public short numlinespecials;
        
        /**
         * These are NOT the same anims found in defines. Dunno why they fucked up
         * this one so badly. Even the type has the same name, but is entirely
         * different. No way they could be overlapped/unionized either. So WTF.
         * Really. WTF.
         */
        public anim_t[] anims = new anim_t[MAXANIMS];

        // MAES: was a pointer
        public int lastanim;

        //
        // P_UpdateSpecials
        // Animate planes, scroll walls, etc.
        //
        boolean levelTimer;

        int levelTimeCount;

        public void UpdateSpecials() {
            int pic;
            line_t line;
            anim_t anim;

            // LEVEL TIMER
            if (levelTimer == true) {
                levelTimeCount--;
                if (levelTimeCount == 0)
                    DOOM.ExitLevel();
            }

            // ANIMATE FLATS AND TEXTURES GLOBALLY

            for (int j = 0; j < lastanim; j++) {
                anim = anims[j];
                
                for (int i = anim.basepic; i < anim.basepic + anim.numpics; i++) {
                    pic =
                        anim.basepic
                                + ((DOOM.leveltime / anim.speed + i) % anim.numpics);
                    if (anim.istexture)
                        DOOM.textureManager.setTextureTranslation(i,pic);
                    else
                        DOOM.textureManager.setFlatTranslation(i,pic);
                }
            }

            // ANIMATE LINE SPECIALS
            for (int i = 0; i < numlinespecials; i++) {
                line = linespeciallist[i];
                switch (line.special) {
                case 48:
                    // EFFECT FIRSTCOL SCROLL +
                    DOOM.levelLoader.sides[line.sidenum[0]].textureoffset += MAPFRACUNIT;
                    break;
                }
            }

            // DO BUTTONS
            SW.doButtons();
        }

        public void InitPicAnims() {
            Arrays.setAll(anims, i -> new anim_t());
            anim_t lstanim; 
            // Init animation. MAES: sneaky base pointer conversion ;-)
            this.lastanim = 0;
            // MAES: for (i=0 ; animdefs[i].istexture != -1 ; i++)
            for (int i = 0; i<animdefs.length-1; i++) {
            	lstanim= anims[this.lastanim];
                if (animdefs[i].istexture) {
                    // different episode ?
                    if (DOOM.textureManager.CheckTextureNumForName(animdefs[i].startname) == -1)
                        continue;
                    // So, if it IS a valid texture, it goes straight into anims.
                    lstanim.picnum = DOOM.textureManager.TextureNumForName(animdefs[i].endname);
                    lstanim.basepic = DOOM.textureManager.TextureNumForName(animdefs[i].startname);
                } else { // If not a texture, it's a flat.
                    if (DOOM.wadLoader.CheckNumForName(animdefs[i].startname) == -1)
                        continue;
                    System.out.println(animdefs[i]);
                    // Otherwise, lstanim seems to go nowhere :-/
                    lstanim.picnum = DOOM.textureManager.FlatNumForName(animdefs[i].endname);
                    lstanim.basepic = DOOM.textureManager.FlatNumForName(animdefs[i].startname);
                }

                lstanim.istexture = animdefs[i].istexture;
                lstanim.numpics = lstanim.picnum - lstanim.basepic + 1;

                if (lstanim.numpics < 2)
                    DOOM.doomSystem.Error("P_InitPicAnims: bad cycle from %s to %s",
                        animdefs[i].startname, animdefs[i].endname);

                lstanim.speed = animdefs[i].speed;
                this.lastanim++;
            }
        }

        protected final void resizeLinesSpecialList() {
        	linespeciallist=C2JUtils.resize(linespeciallist[0],linespeciallist,linespeciallist.length*2);
            }   
        
        
    }

    class Switches {

    	public Switches(){
    		switchlist= new int[MAXSWITCHES];
    		initButtonList();
    	}
        public void doButtons() {
        	for (int i = 0; i < buttonlist.length; i++)
                if (eval(buttonlist[i].btimer)) {
                    buttonlist[i].btimer--;
                    if (!eval(buttonlist[i].btimer)) {
                        switch (buttonlist[i].where) {
                        case top:
                            DOOM.levelLoader.sides[buttonlist[i].line.sidenum[0]].toptexture =
                                (short) buttonlist[i].btexture;
                            break;

                        case middle:
                            DOOM.levelLoader.sides[buttonlist[i].line.sidenum[0]].midtexture =
                                (short) buttonlist[i].btexture;
                            break;

                        case bottom:
                            DOOM.levelLoader.sides[buttonlist[i].line.sidenum[0]].bottomtexture =
                                (short) buttonlist[i].btexture;
                            break;
                        }
                        DOOM.doomSound.StartSound(buttonlist[i].soundorg,sfxenum_t.sfx_swtchn);
                        buttonlist[i].reset();
                    }
                }
			
		}
		//
        // CHANGE THE TEXTURE OF A WALL SWITCH TO ITS OPPOSITE
        //
        switchlist_t[] alphSwitchList =
            {
                    // Doom shareware episode 1 switches
                    new switchlist_t("SW1BRCOM", "SW2BRCOM", 1),
                    new switchlist_t("SW1BRN1", "SW2BRN1", 1),
                    new switchlist_t("SW1BRN2", "SW2BRN2", 1),
                    new switchlist_t("SW1BRNGN", "SW2BRNGN", 1),
                    new switchlist_t("SW1BROWN", "SW2BROWN", 1),
                    new switchlist_t("SW1COMM", "SW2COMM", 1),
                    new switchlist_t("SW1COMP", "SW2COMP", 1),
                    new switchlist_t("SW1DIRT", "SW2DIRT", 1),
                    new switchlist_t("SW1EXIT", "SW2EXIT", 1),
                    new switchlist_t("SW1GRAY", "SW2GRAY", 1),
                    new switchlist_t("SW1GRAY1", "SW2GRAY1", 1),
                    new switchlist_t("SW1METAL", "SW2METAL", 1),
                    new switchlist_t("SW1PIPE", "SW2PIPE", 1),
                    new switchlist_t("SW1SLAD", "SW2SLAD", 1),
                    new switchlist_t("SW1STARG", "SW2STARG", 1),
                    new switchlist_t("SW1STON1", "SW2STON1", 1),
                    new switchlist_t("SW1STON2", "SW2STON2", 1),
                    new switchlist_t("SW1STONE", "SW2STONE", 1),
                    new switchlist_t("SW1STRTN", "SW2STRTN", 1),

                    // Doom registered episodes 2&3 switches
                    new switchlist_t("SW1BLUE", "SW2BLUE", 2),
                    new switchlist_t("SW1CMT", "SW2CMT", 2),
                    new switchlist_t("SW1GARG", "SW2GARG", 2),
                    new switchlist_t("SW1GSTON", "SW2GSTON", 2),
                    new switchlist_t("SW1HOT", "SW2HOT", 2),
                    new switchlist_t("SW1LION", "SW2LION", 2),
                    new switchlist_t("SW1SATYR", "SW2SATYR", 2),
                    new switchlist_t("SW1SKIN", "SW2SKIN", 2),
                    new switchlist_t("SW1VINE", "SW2VINE", 2),
                    new switchlist_t("SW1WOOD", "SW2WOOD", 2),

                    // Doom II switches
                    new switchlist_t("SW1PANEL", "SW2PANEL", 3),
                    new switchlist_t("SW1ROCK", "SW2ROCK", 3),
                    new switchlist_t("SW1MET2", "SW2MET2", 3),
                    new switchlist_t("SW1WDMET", "SW2WDMET", 3),
                    new switchlist_t("SW1BRIK", "SW2BRIK", 3),
                    new switchlist_t("SW1MOD1", "SW2MOD1", 3),
                    new switchlist_t("SW1ZIM", "SW2ZIM", 3),
                    new switchlist_t("SW1STON6", "SW2STON6", 3),
                    new switchlist_t("SW1TEK", "SW2TEK", 3),
                    new switchlist_t("SW1MARB", "SW2MARB", 3),
                    new switchlist_t("SW1SKULL", "SW2SKULL", 3),

                    new switchlist_t("\0", "\0", 0) };

        /** A (runtime generated) list of the KNOWN button types */
        int[] switchlist;

        int numswitches;

        button_t[] buttonlist;
        //
        // P_InitSwitchList
        // Only called at game initialization.
        //
        public void InitSwitchList() {
            int i;
            int index;
            int episode;

            episode = 1;

            // MAES: if this isn't changed Ultimate Doom's switches
            // won't work visually.
            if (DOOM.isRegistered())
                episode = 2;
            else if (DOOM.isCommercial())
                episode = 3;

            for (index = 0, i = 0; i < MAXSWITCHES; i++) {
            	
            	if (index>=switchlist.length) {
            		// Remove limit
            		switchlist=Arrays.copyOf(switchlist,switchlist.length>0?switchlist.length*2:8);
            		}
            	// Trickery. Looks for "end of list" marker
            	// Since the list has pairs of switches, the
            	// actual number of distinct switches is index/2
                if (alphSwitchList[i].episode == 0) {
                    numswitches = index / 2;
                    switchlist[index] = -1;
                    break;
                }

                if (alphSwitchList[i].episode <= episode) {
                    /*
                     * // UNUSED - debug? int value; if
                     * (R_CheckTextureNumForName(alphSwitchList[i].name1) < 0) {
                     * system.Error("Can't find switch texture '%s'!",
                     * alphSwitchList[i].name1); continue; } value =
                     * R_TextureNumForName(alphSwitchList[i].name1);
                     */
                    switchlist[index++] =
                        DOOM.textureManager.TextureNumForName(alphSwitchList[i].name1);
                    switchlist[index++] =
                        DOOM.textureManager.TextureNumForName(alphSwitchList[i].name2);
                }
            }
        }

        //
        // Start a button counting down till it turns off.
        //
        void StartButton(line_t line, bwhere_e w, int texture, int time) {
            int i;

            // See if button is already pressed
            for (i = 0; i < buttonlist.length; i++) {
                if (buttonlist[i].btimer != 0 && buttonlist[i].line == line) {

                    return;
                }
            }
            
            // At this point, it may mean that THE button of that particular
            // line was not active, or simply that there were not enough 
            // buttons in buttonlist to support an additional entry.

            // Search for a free button slot.
            for (i = 0; i < buttonlist.length; i++) {
                if (buttonlist[i].btimer == 0) {
                    buttonlist[i].line = line;
                    buttonlist[i].where = w;
                    buttonlist[i].btexture = texture;
                    buttonlist[i].btimer = time;
                    buttonlist[i].soundorg = line.soundorg;
                    return;
                }
            }
            
            // Extremely rare event, We must be able to push more than MAXBUTTONS buttons
            // in one tic, which can't normally happen except in really pathological maps.
            // In any case, resizing should solve this problem.
            buttonlist=C2JUtils.resize(buttonlist[0], buttonlist, buttonlist.length*2);
            // Try again
            StartButton(line,w,texture,time);
            // I.Error("P_StartButton: no button slots left!");
        }

        //
        // Function that changes wall texture.
        // Tell it if switch is ok to use again (true=yes, it's a button).
        //
        void ChangeSwitchTexture(line_t line, boolean useAgain) {
            int texTop;
            int texMid;
            int texBot;
            int i;
            int sound;

            if (!useAgain)
                line.special = 0;

            texTop = DOOM.levelLoader.sides[line.sidenum[0]].toptexture;
            texMid = DOOM.levelLoader.sides[line.sidenum[0]].midtexture;
            texBot = DOOM.levelLoader.sides[line.sidenum[0]].bottomtexture;

            sound = sfxenum_t.sfx_swtchn.ordinal();

            // EXIT SWITCH?
            if (line.special == 11)
                sound = sfxenum_t.sfx_swtchx.ordinal();

            for (i = 0; i < numswitches * 2; i++) {
                if (switchlist[i] == texTop) {
                    DOOM.doomSound.StartSound(buttonlist[0].soundorg,sound);
                    DOOM.levelLoader.sides[line.sidenum[0]].toptexture =
                        (short) switchlist[i ^ 1];

                    if (useAgain)
                        StartButton(line, bwhere_e.top, switchlist[i],
                            BUTTONTIME);

                    return;
                } else {
                    if (switchlist[i] == texMid) {
                    	 DOOM.doomSound.StartSound(buttonlist[0].soundorg,sound);
                        DOOM.levelLoader.sides[line.sidenum[0]].midtexture =
                            (short) switchlist[i ^ 1];

                        if (useAgain)
                            StartButton(line, bwhere_e.middle, switchlist[i],
                                BUTTONTIME);

                        return;
                    } else {
                        if (switchlist[i] == texBot) {
                        	 DOOM.doomSound.StartSound(buttonlist[0].soundorg,sound);
                            DOOM.levelLoader.sides[line.sidenum[0]].bottomtexture =
                                (short) switchlist[i ^ 1];

                            if (useAgain)
                                StartButton(line, bwhere_e.bottom,
                                    switchlist[i], BUTTONTIME);

                            return;
                        }
                    }
                }
            }
        }
		public void initButtonList() {
			// Unlike plats, buttonlist needs statically allocated and reusable
			// objects. The MAXBUTTONS limit actually applied to buttons PRESSED
			// or ACTIVE at once, not how many there can actually be in a map.
			
    		buttonlist =  C2JUtils.createArrayOfObjects(button_t.class,MAXBUTTONS);
			}

    }

    //
    // MOVEMENT ITERATOR FUNCTIONS
    //

    interface PIT_LineFunction {
        public boolean invoke(line_t ld);
    }

    interface PIT_MobjFunction {
        public boolean invoke(mobj_t thing);
    }
    
    interface PTR_InterceptFunc {
    	public boolean invoke(intercept_t in);
    }

   /* enum PTR {
        SlideTraverse,
        AimTraverse,
        ShootTraverse,
        UseTraverse
    } */

    //////////////// PIT FUNCTION OBJECTS ///////////////////
    
    //
 // PIT_AddLineIntercepts.
 // Looks for lines in the given block
 // that intercept the given trace
 // to add to the intercepts list.
 //
 // A line is crossed if its endpoints
 // are on opposite sides of the trace.
 // Returns true if earlyout and a solid line hit.
 //

 protected class PIT_AddLineIntercepts implements PIT_LineFunction{

 divline_t dl = new divline_t();

 public boolean invoke(line_t ld) {
     boolean s1;
     boolean s2;
     int frac;
     // avoid precision problems with two routines
     if (trace.dx > FRACUNIT * 16 || trace.dy > FRACUNIT * 16
             || trace.dx < -FRACUNIT * 16 || trace.dy < -FRACUNIT * 16) {
         s1 = trace.PointOnDivlineSide(ld.v1x, ld.v1y);
         s2 = trace.PointOnDivlineSide(ld.v2x, ld.v2y);
         //s1 = trace.DivlineSide(ld.v1x, ld.v1.y);
         //s2 = trace.DivlineSide(ld.v2x, ld.v2y);
     } else {
         s1 = ld.PointOnLineSide(trace.x, trace.y);
         s2 = ld.PointOnLineSide(trace.x + trace.dx, trace.y + trace.dy);
         //s1 = new divline_t(ld).DivlineSide(trace.x, trace.y);
         //s2 = new divline_t(ld).DivlineSide(trace.x + trace.dx, trace.y + trace.dy);
     }

     if (s1 == s2)
         return true; // line isn't crossed

     // hit the line
     dl.MakeDivline(ld);
     frac = InterceptVector(trace, dl);

     if (frac < 0)
         return true; // behind source

     // try to early out the check
     if (earlyout && frac < FRACUNIT && ld.backsector == null) {
         return false; // stop checking
     }

     // "create" a new intercept in the static intercept pool.
     if (intercept_p>=intercepts.length){
         ResizeIntercepts();
     }
     
     intercepts[intercept_p].frac = frac;
     intercepts[intercept_p].isaline = true;
     intercepts[intercept_p].line = ld;
     intercept_p++;

     return true; // continue
 	}

 }


 //
 // PIT_AddThingIntercepts
 //

 protected class PIT_AddThingIntercepts implements PIT_MobjFunction{
	 

     // maybe make this a shared instance variable?
     private divline_t dl = new divline_t();
	 
 public boolean invoke(mobj_t thing) {
     int x1, y1, x2, y2; // fixed_t

     boolean s1, s2;

     boolean tracepositive;


     int frac; // fixed_t

     tracepositive = (trace.dx ^ trace.dy) > 0;

     // check a corner to corner crossection for hit
     if (tracepositive) {
         x1 = thing.x - thing.radius;
         y1 = thing.y + thing.radius;

         x2 = thing.x + thing.radius;
         y2 = thing.y - thing.radius;
     } else {
         x1 = thing.x - thing.radius;
         y1 = thing.y - thing.radius;

         x2 = thing.x + thing.radius;
         y2 = thing.y + thing.radius;
     }

     s1 = trace.PointOnDivlineSide(x1, y1);
     s2 = trace.PointOnDivlineSide(x2, y2);

     if (s1 == s2)
         return true; // line isn't crossed

     dl.x = x1;
     dl.y = y1;
     dl.dx = x2 - x1;
     dl.dy = y2 - y1;

     frac = InterceptVector(trace, dl);

     if (frac < 0)
         return true; // behind source

     // "create" a new intercept in the static intercept pool.
     if (intercept_p>=intercepts.length){
         ResizeIntercepts();
     }
     intercepts[intercept_p].frac = frac;
     intercepts[intercept_p].isaline = false;
     intercepts[intercept_p].thing = thing;
     intercept_p++;

     return true; // keep going
 	}
 }

   
    
    
   /////////// BEGIN MAP OBJECT CODE, USE AS BASIC

    /**
     * P_ExplodeMissile
     */
    
    protected void ExplodeMissile(mobj_t mo) {
        mo.momx = mo.momy = mo.momz = 0;

        // MAES 9/5/2011: using mobj code for that.
        mo.SetMobjState(mobjinfo[mo.type.ordinal()].deathstate);

        mo.tics -= DOOM.random.P_Random() & 3;

        if (mo.tics < 1)
            mo.tics = 1;

        mo.flags &= ~MF_MISSILE;

        if (mo.info.deathsound != null)
        DOOM.doomSound.StartSound(mo, mo.info.deathsound);
    }

    //
    // P_RemoveMobj
    //
    mapthing_t[] itemrespawnque = new mapthing_t[ITEMQUESIZE];

    int[] itemrespawntime = new int[ITEMQUESIZE];

    int iquehead;

    int iquetail;

    public void RemoveMobj(mobj_t mobj) {
        if (eval(mobj.flags& MF_SPECIAL) && !eval(mobj.flags& MF_DROPPED)
                && (mobj.type != mobjtype_t.MT_INV)
                && (mobj.type != mobjtype_t.MT_INS)) {
            itemrespawnque[iquehead] = mobj.spawnpoint;
            itemrespawntime[iquehead] = DOOM.leveltime;
            iquehead = (iquehead + 1) & (ITEMQUESIZE - 1);

            // lose one off the end?
            if (iquehead == iquetail)
                iquetail = (iquetail + 1) & (ITEMQUESIZE - 1);
        }

        // unlink from sector and block lists
        UnsetThingPosition(mobj);
        
        // stop any playing sound
        DOOM.doomSound.StopSound (mobj);
        
        // free block
        RemoveThinker((thinker_t) mobj);
    }

    // //////////////////////////////// THINKER CODE, GLOBALLY VISIBLE
    // /////////////////

    //
    // THINKERS
    // All thinkers should be allocated by Z_Malloc
    // so they can be operated on uniformly.
    // The actual structures will vary in size,
    // but the first element must be thinker_t.
    //

    /** Both the head and the tail of the thinkers list */
    public thinker_t thinkercap;

    //
    // P_InitThinkers
    //
    @Override
    public void InitThinkers() {
    	
        // mobjpool.drain();
        
        for (int i=0; i<th_class.NUMTHCLASS; i++)  // killough 8/29/98: initialize threaded lists
            thinkerclasscap[i].cprev = thinkerclasscap[i].cnext = thinkerclasscap[i];
        
    	thinker_t next=thinkercap.next;
    	thinker_t prev=thinkercap.prev;
    	
    	// Unlink the "dangling" thinkers that may still be attached
    	// to the thinkercap. When loading a new level, they DO NOT get unloaded,
    	// wtf...
    	if (next!=null && next!=thinkercap) {
    		//System.err.println("Next link to thinkercap nulled");
    		next.prev=null;
    	}

    	if (prev!=null && prev!=thinkercap) {
    		//System.err.println("Prev link to thinkercap nulled");
    		prev.next=null;
    	}
    	
        thinkercap.next = thinkercap;
        thinkercap.prev = thinkercap;
    }

    /** cph 2002/01/13 - iterator for thinker list
     * WARNING: Do not modify thinkers between calls to this functin
     */
    thinker_t NextThinker(thinker_t th, th_class cl)
    {
      thinker_t top = thinkerclasscap[cl.ordinal()];
      if (th==null) th = top;
      th = cl == th_class.th_all ? th.next : th.cnext;
      return th == top ? null : th;
    }
    
    /**
     * P_AddThinker Adds a new thinker at the end of the list.
     */

    public void AddThinker(thinker_t thinker) {
    	
    	// If something was too weird to be wired before, it will
    	// be wired here for sure, so don't worry about searching 
    	// all of the code.
    	if (thinker.function!=null && (thinker.acp1==null && thinker.acp2==null))
    		FUNS.doWireThinker(thinker);
    	
        thinkercap.prev.next = thinker;
        thinker.next = thinkercap;
        thinker.prev = thinkercap.prev;
        thinkercap.prev = thinker;
        
        // killough 8/29/98: set sentinel pointers, and then add to appropriate list
        thinker.cnext = thinker.cprev = null;
        UpdateThinker(thinker);
        
        // [Maes] seems only used for interpolations
        //newthinkerpresent = true;
    }
   
    public void ClearPlatsBeforeLoading(){
        for (int i = 0; i < PEV.activeplats.length; i++) {
                PEV.activeplats[i] = null;
            }
    }
    
    public void AddActivePlat(plat_t plat) {
        int i;

        for (i = 0; i < PEV.activeplats.length; i++)
            if (PEV.activeplats[i] == null) {
                PEV.activeplats[i] = plat;
                return;
            }
        // Uhh... lemme guess. Needs to resize?
        // Resize but leave extra items empty.
        PEV.activeplats=C2JUtils.resizeNoAutoInit(PEV.activeplats,2*PEV.activeplats.length);
        AddActivePlat(plat);
        
        //I.Error("P_AddActivePlat: no more plats!");
    }
        
    // MAES: works, but not worth it.
    // MobjPool mobjpool;
        
    //
    // P_RemoveThinker
    // Deallocation is lazy -- it will not actually be freed
    // until its thinking turn comes up.
    //
    //
    // killough 4/25/98:
    //
    // Instead of marking the function with -1 value cast to a function pointer,
    // set the function to P_RemoveThinkerDelayed(), so that later, it will be
    // removed automatically as part of the thinker process.
    //
    
    public void RemoveThinker(thinker_t thinker) {
        //thinker.function = think_t.NOP;
        // Wire to this special function.
        thinker.function=think_t.NOP;
        thinker.acpss = this.RemoveThinkerDelayed;
        // Remove any type 1 or 2 special functions.
        thinker.acp1 = null;

        thinker.acp2 = null;
        
    }

    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    public void AllocateThinker(thinker_t thinker) {
        // UNUSED
    }


    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    public thinker_t getRandomThinker() {    	
    	
    	int pick=(int) (Math.random()*128);
    	thinker_t th=this.getThinkerCap();
    	
    	for (int i=0;i<pick;i++){
    		th=th.next;
    	}
    	
    	return th;
    }
    

    //
    // P_Init
    //
    public void Init() {
        SW.InitSwitchList();
        SPECS.InitPicAnims();
        DOOM.spriteManager.InitSprites(ISpriteManager.doomsprnames);
    }

    /**
     * P_TouchSpecialThing LIKE ROMERO's ASS!!!
     */
    public void TouchSpecialThing(mobj_t special, mobj_t toucher) {
        player_t player;
        int i;
        int delta;// fixed_t
        sfxenum_t sound;

        delta = special.z - toucher.z;

        if (delta > toucher.height || delta < -8 * FRACUNIT) {
            // out of reach
            return;
        }

        sound = sfxenum_t.sfx_itemup;
        player = toucher.player;

        // Dead thing touching.
        // Can happen with a sliding player corpse.
        if (toucher.health <= 0)
            return;

        // Identify by sprite.
        switch (special.sprite) {
        // armor
        case SPR_ARM1:
            if (!player.GiveArmor(1))
                return;
            player.message = GOTARMOR;
            break;

        case SPR_ARM2:
            if (!player.GiveArmor(2))
                return;
            player.message = GOTMEGA;
            break;

        // bonus items
        case SPR_BON1:
            player.health[0]++; // can go over 100%
            if (player.health[0] > 200)
                player.health[0] = 200;
            player.mo.health = player.health[0];
            player.message = GOTHTHBONUS;
            break;

        case SPR_BON2:
            player.armorpoints[0]++; // can go over 100%
            if (player.armorpoints[0] > 200)
                player.armorpoints[0] = 200;
            if (player.armortype == 0)
                player.armortype = 1;
            player.message = GOTARMBONUS;
            break;

        case SPR_SOUL:
            player.health[0] += 100;
            if (player.health[0] > 200)
                player.health[0] = 200;
            player.mo.health = player.health[0];
            player.message = GOTSUPER;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_MEGA:
            if (!DOOM.isCommercial())
                return;
            player.health[0] = 200;
            player.mo.health = player.health[0];
            player.GiveArmor(2);
            player.message = GOTMSPHERE;
            sound = sfxenum_t.sfx_getpow;
            break;

        // cards
        // leave cards for everyone
        case SPR_BKEY:
            if (!player.cards[card_t.it_bluecard.ordinal()])
                player.message = GOTBLUECARD;
            player.GiveCard(card_t.it_bluecard);
            if (!DOOM.netgame)
                break;
            return;

        case SPR_YKEY:
            if (!player.cards[card_t.it_yellowcard.ordinal()])
                player.message = GOTYELWCARD;
            player.GiveCard(card_t.it_yellowcard);
            if (!DOOM.netgame)
                break;
            return;

        case SPR_RKEY:
            if (!player.cards[card_t.it_redcard.ordinal()])
                player.message = GOTREDCARD;
            player.GiveCard(card_t.it_redcard);
            if (!DOOM.netgame)
                break;
            return;

        case SPR_BSKU:
            if (!player.cards[card_t.it_blueskull.ordinal()])
                player.message = GOTBLUESKUL;
            player.GiveCard(card_t.it_blueskull);
            if (!DOOM.netgame)
                break;
            return;

        case SPR_YSKU:
            if (!player.cards[card_t.it_yellowskull.ordinal()])
                player.message = GOTYELWSKUL;
            player.GiveCard(card_t.it_yellowskull);
            if (!DOOM.netgame)
                break;
            return;

        case SPR_RSKU:
            if (!player.cards[card_t.it_redskull.ordinal()])
                player.message = GOTREDSKULL;
            player.GiveCard(card_t.it_redskull);
            if (!DOOM.netgame)
                break;
            return;

            // medikits, heals
        case SPR_STIM:
            if (!player.GiveBody(10))
                return;
            player.message = GOTSTIM;
            break;

        case SPR_MEDI:
            /**
             * Another fix with switchable option to enable
             * - Good Sign 2017/04/03
             */
            boolean need = player.health[0] < 25;
            
            if (!player.GiveBody(25))
                return;
            
            if (DOOM.CM.equals(Settings.fix_medi_need, Boolean.FALSE))
                // default behavior - with bug
                player.message = player.health[0] < 25 ? GOTMEDINEED : GOTMEDIKIT;
            else //proper behavior
                player.message = need ? GOTMEDINEED : GOTMEDIKIT;
            
            break;

        // power ups
        case SPR_PINV:
            if (!player.GivePower(pw_invulnerability))
                return;
            player.message = GOTINVUL;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PSTR:
            if (!player.GivePower(pw_strength))
                return;
            player.message = GOTBERSERK;
            if (player.readyweapon != weapontype_t.wp_fist)
                player.pendingweapon = weapontype_t.wp_fist;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PINS:
            if (!player.GivePower(pw_invisibility))
                return;
            player.message = GOTINVIS;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_SUIT:
            if (!player.GivePower(pw_ironfeet))
                return;
            player.message = GOTSUIT;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PMAP:
            if (!player.GivePower(pw_allmap))
                return;
            player.message = GOTMAP;
            sound = sfxenum_t.sfx_getpow;
            break;

        case SPR_PVIS:
            if (!player.GivePower(pw_infrared))
                return;
            player.message = GOTVISOR;
            sound = sfxenum_t.sfx_getpow;
            break;

        // ammo
        case SPR_CLIP:
            if ((special.flags & MF_DROPPED) != 0) {
                if (!player.GiveAmmo(ammotype_t.am_clip, 0))
                    return;
            } else {
                if (!player.GiveAmmo(ammotype_t.am_clip, 1))
                    return;
            }
            player.message = GOTCLIP;
            break;

        case SPR_AMMO:
            if (!player.GiveAmmo(ammotype_t.am_clip, 5))
                return;
            player.message = GOTCLIPBOX;
            break;

        case SPR_ROCK:
            if (!player.GiveAmmo(ammotype_t.am_misl, 1))
                return;
            player.message = GOTROCKET;
            break;

        case SPR_BROK:
            if (!player.GiveAmmo(ammotype_t.am_misl, 5))
                return;
            player.message = GOTROCKBOX;
            break;

        case SPR_CELL:
            if (!player.GiveAmmo(ammotype_t.am_cell, 1))
                return;
            player.message = GOTCELL;
            break;

        case SPR_CELP:
            if (!player.GiveAmmo(ammotype_t.am_cell, 5))
                return;
            player.message = GOTCELLBOX;
            break;

        case SPR_SHEL:
            if (!player.GiveAmmo(ammotype_t.am_shell, 1))
                return;
            player.message = GOTSHELLS;
            break;

        case SPR_SBOX:
            if (!player.GiveAmmo(ammotype_t.am_shell, 5))
                return;
            player.message = GOTSHELLBOX;
            break;

        case SPR_BPAK:
            if (!player.backpack) {
                for (i = 0; i < NUMAMMO; i++)
                    player.maxammo[i] *= 2;
                player.backpack = true;
            }
            for (i = 0; i < NUMAMMO; i++)
                player.GiveAmmo(ammotype_t.values()[i], 1);
            player.message = GOTBACKPACK;
            break;

        // weapons
        case SPR_BFUG:
            if (!player.GiveWeapon(weapontype_t.wp_bfg, false))
                return;
            player.message = GOTBFG9000;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_MGUN:
            if (!player.GiveWeapon(weapontype_t.wp_chaingun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTCHAINGUN;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_CSAW:
            if (!player.GiveWeapon(weapontype_t.wp_chainsaw, false))
                return;
            player.message = GOTCHAINSAW;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_LAUN:
            if (!player.GiveWeapon(weapontype_t.wp_missile, false))
                return;
            player.message = GOTLAUNCHER;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_PLAS:
            if (!player.GiveWeapon(weapontype_t.wp_plasma, false))
                return;
            player.message = GOTPLASMA;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_SHOT:
            if (!player.GiveWeapon(weapontype_t.wp_shotgun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTSHOTGUN;
            sound = sfxenum_t.sfx_wpnup;
            break;

        case SPR_SGN2:
            if (!player.GiveWeapon(weapontype_t.wp_supershotgun,
                (special.flags & MF_DROPPED) != 0))
                return;
            player.message = GOTSHOTGUN2;
            sound = sfxenum_t.sfx_wpnup;
            break;

        default:
            DOOM.doomSystem.Error("P_SpecialThing: Unknown gettable thing");
        }

        if ((special.flags & MF_COUNTITEM) != 0)
            player.itemcount++;
        RemoveMobj(special);
        player.bonuscount += player_t.BONUSADD;
        if (player == DOOM.players[DOOM.consoleplayer])
        DOOM.doomSound.StartSound (null, sound);
    }

    
    @Override
    public thinker_t getThinkerCap() {
        return thinkercap;
    }
    
 /**
  * killough 11/98:
  *
  * Make currentthinker external, so that P_RemoveThinkerDelayed
  * can adjust currentthinker when thinkers self-remove.
  */
    
    protected thinker_t currentthinker;
    
    protected final P_RemoveThinkerDelayed RemoveThinkerDelayed; 
    
    public class P_RemoveThinkerDelayed implements ActionTypeSS <thinker_t>{
        
    @Override
    public void invoke(thinker_t thinker) {
        
    	/*
        try {
        System.err.printf("Delete: %s %d<= %s %d => %s %d\n",
            ((mobj_t)thinker.prev).type,((mobj_t)thinker.prev).thingnum,
            ((mobj_t)thinker).type,((mobj_t)thinker).thingnum,
            ((mobj_t)thinker.next).type,((mobj_t)thinker.next).thingnum);
        } catch (ClassCastException e){
            
        } */
        
        // Unlike Boom, if we reach here it gets zapped anyway
        //if (!thinker->references)
        //{
          { /* Remove from main thinker list */
            thinker_t next = thinker.next;
            /* Note that currentthinker is guaranteed to point to us,
             * and since we're freeing our memory, we had better change that. So
             * point it to thinker->prev, so the iterator will correctly move on to
             * thinker->prev->next = thinker->next */
            (next.prev = currentthinker = thinker.prev).next = next;
            //thinker.next=thinker.prev=null;
            try {
           // System.err.printf("Delete: %s %d <==> %s %d\n",
           //     ((mobj_t)currentthinker.prev).type,((mobj_t)currentthinker.prev).thingnum,
           //     ((mobj_t)currentthinker.next).type,((mobj_t)currentthinker.next).thingnum);
            } catch (ClassCastException e){
                
            }
            
          }
          {
            /* Remove from current thinker class list */
            thinker_t th = thinker.cnext;
            (th.cprev = thinker.cprev).cnext = th;
            //thinker.cnext=thinker.cprev=null;
          }
        }
    }
    
} // End unified map