/*
 * Copyright (C) 1993-1996 by id Software, Inc.
 * Copyright (C) 2017 Good Sign
 *
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
package p;

import static data.Defines.ITEMQUESIZE;
import static data.Defines.MELEERANGE;
import static data.Defines.MTF_AMBUSH;
import static data.Defines.NUMCARDS;
import static data.Defines.ONCEILINGZ;
import static data.Defines.ONFLOORZ;
import static data.Defines.PST_LIVE;
import static data.Defines.PST_REBORN;
import static data.Defines.VIEWHEIGHT;
import static data.Limits.MAXPLAYERS;
import static data.Limits.NUMMOBJTYPES;
import static data.Tables.ANG45;
import static data.info.mobjinfo;
import static data.info.states;
import data.mapthing_t;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.sounds;
import data.state_t;
import defines.skill_t;
import defines.statenum_t;
import doom.CommandVariable;
import doom.DoomMain;
import doom.SourceCode;
import doom.SourceCode.P_Mobj;
import static doom.SourceCode.P_Mobj.P_SpawnMobj;
import static doom.SourceCode.P_Mobj.P_SpawnPlayer;
import doom.SourceCode.fixed_t;
import doom.player_t;
import doom.thinker_t;
import java.util.logging.Level;
import java.util.logging.Logger;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import mochadoom.Loggers;
import static p.DoorDefines.FASTDARK;
import static p.DoorDefines.SLOWDARK;
import static p.mobj_t.MF_AMBUSH;
import static p.mobj_t.MF_COUNTITEM;
import static p.mobj_t.MF_COUNTKILL;
import static p.mobj_t.MF_NOTDMATCH;
import static p.mobj_t.MF_SPAWNCEILING;
import static p.mobj_t.MF_TRANSSHIFT;
import rr.sector_t;
import rr.subsector_t;
import static utils.C2JUtils.eval;
import utils.TraitFactory.ContextKey;
import v.graphics.Palettes;

interface ActionsSpawn extends ActionTrait {
    final ContextKey<RespawnQueue> KEY_RESP_QUEUE = KEY_CHAIN.newKey(ActionsSpawn.class, RespawnQueue.class);
    final ContextKey<Spawn> KEY_SPAWN = KEY_CHAIN.newKey(ActionsSpawn.class, Spawn.class);
    
    int getMaxCeilings();
    ceiling_t[] getActiveCeilings();
    void RemoveThinker(thinker_t thinker);
    void RemoveMobj(mobj_t mobj);
    
    final class RespawnQueue {
        //
        // P_RemoveMobj
        //
        mapthing_t[] itemrespawnque = new mapthing_t[ITEMQUESIZE];
        int[] itemrespawntime = new int[ITEMQUESIZE];
        int iquehead;
        int iquetail;
    }
    
    final class Spawn {
        final static Logger LOGGER = Loggers.getLogger(ActionsSpawn.class.getName());
        
        /**
         * who got hit (or NULL)
         */
        mobj_t linetarget;

        @fixed_t
        int attackrange;
        
        mobj_t shootthing;
        // Height if not aiming up or down
        // ???: use slope for monsters?
        @fixed_t
        int shootz;
        
        int la_damage;
        
        @fixed_t
        int aimslope;
        
        divline_t trace = new divline_t();
        
        int topslope, bottomslope; // slopes to top and bottom of target

        //
        // P_BulletSlope
        // Sets a slope so a near miss is at aproximately
        // the height of the intended target
        //
        int bulletslope;

        boolean isMeleeRange() {
            return attackrange == MELEERANGE;
        }
    }
    
    /**
     * P_SpawnSpecials After the map has been loaded, scan for specials that spawn thinkers
     */
    default void SpawnSpecials() {
        final DoomMain<?, ?> D = DOOM();
        final AbstractLevelLoader ll = levelLoader();
        final UnifiedGameMap.Specials SPECS = getSpecials();
        sector_t sector;

        /*int     episode;

        episode = 1;
        if (W.CheckNumForName("texture2") >= 0)
        episode = 2;
         */
        // See if -TIMER needs to be used.
        SPECS.levelTimer = false;

        if (D.cVarManager.bool(CommandVariable.AVG) && IsDeathMatch()) {
            SPECS.levelTimer = true;
            SPECS.levelTimeCount = 20 * 60 * 35;
        }

        if (IsDeathMatch()) {
            D.cVarManager.with(CommandVariable.TIMER, 0, (Integer i) -> {
                SPECS.levelTimer = true;
                SPECS.levelTimeCount = i * 60 * 35;
            });
        }
        
        //  Init special SECTORs.
        //sector = LL.sectors;
        for (int i = 0; i < ll.numsectors; i++) {
            sector = ll.sectors[i];
            if (!eval(sector.special)) {
                continue;
            }

            switch (sector.special) {
                case 1:
                    // FLICKERING LIGHTS
                    sector.SpawnLightFlash();
                    break;

                case 2:
                    // STROBE FAST
                    sector.SpawnStrobeFlash(FASTDARK, 0);
                    break;

                case 3:
                    // STROBE SLOW
                    sector.SpawnStrobeFlash(SLOWDARK, 0);
                    break;

                case 4:
                    // STROBE FAST/DEATH SLIME
                    sector.SpawnStrobeFlash(FASTDARK, 0);
                    sector.special = 4;
                    break;

                case 8:
                    // GLOWING LIGHT
                    sector.SpawnGlowingLight();
                    break;
                case 9:
                    // SECRET SECTOR
                    D.totalsecret++;
                    break;

                case 10:
                    // DOOR CLOSE IN 30 SECONDS
                    sector.SpawnDoorCloseIn30();
                    break;

                case 12:
                    // SYNC STROBE SLOW
                    sector.SpawnStrobeFlash(SLOWDARK, 1);
                    break;

                case 13:
                    // SYNC STROBE FAST
                    sector.SpawnStrobeFlash(FASTDARK, 1);
                    break;

                case 14:
                    // DOOR RAISE IN 5 MINUTES
                    sector.SpawnDoorRaiseIn5Mins(i);
                    break;

                case 17:
                    sector.SpawnFireFlicker();
                    break;
            }
        }

        //  Init line EFFECTs
        SPECS.numlinespecials = 0;
        for (int i = 0; i < ll.numlines; i++) {
            switch (ll.lines[i].special) {
                case 48:
                    // EFFECT FIRSTCOL SCROLL+
                    // Maes 6/4/2012: removed length limit.
                    if (SPECS.numlinespecials == SPECS.linespeciallist.length) {
                        SPECS.resizeLinesSpecialList();
                    }
                    SPECS.linespeciallist[SPECS.numlinespecials] = ll.lines[i];
                    SPECS.numlinespecials++;
                    break;
            }
        }

        //  Init other misc stuff
        for (int i = 0; i < this.getMaxCeilings(); i++) {
            this.getActiveCeilings()[i] = null;
        }

        getSwitches().initButtonList();

        // UNUSED: no horizonal sliders.
        // if (SL!=null) {
        // SL.updateStatus(DM);
        //  SL.P_InitSlidingDoorFrames();
        //}
    }

    /**
     * P_RespawnSpecials
     */
    default void RespawnSpecials() {
        final RespawnQueue resp = contextRequire(KEY_RESP_QUEUE);
        int x, y, z; // fixed

        subsector_t ss;
        mobj_t mo;
        mapthing_t mthing;

        int i;

        // only respawn items in deathmatch (deathmatch!=2)
        if (!DOOM().altdeath) {
            return; // 
        }
        // nothing left to respawn?
        if (resp.iquehead == resp.iquetail) {
            return;
        }

        // wait at least 30 seconds
        if (LevelTime() - resp.itemrespawntime[resp.iquetail] < 30 * 35) {
            return;
        }

        mthing = resp.itemrespawnque[resp.iquetail];

        x = mthing.x << FRACBITS;
        y = mthing.y << FRACBITS;

        // spawn a teleport fog at the new spot
        ss = levelLoader().PointInSubsector(x, y);
        mo = SpawnMobj(x, y, ss.sector.floorheight, mobjtype_t.MT_IFOG);
        StartSound(mo, sounds.sfxenum_t.sfx_itmbk);

        // find which type to spawn
        for (i = 0; i < mobjtype_t.NUMMOBJTYPES.ordinal(); i++) {
            if (mthing.type == mobjinfo[i].doomednum) {
                break;
            }
        }

        // spawn it
        if (eval(mobjinfo[i].flags & MF_SPAWNCEILING)) {
            z = ONCEILINGZ;
        } else {
            z = ONFLOORZ;
        }

        mo = SpawnMobj(x, y, z, mobjtype_t.values()[i]);
        mo.spawnpoint = mthing;
        mo.angle = ANG45 * (mthing.angle / 45);

        // pull it from the que
        resp.iquetail = (resp.iquetail + 1) & (ITEMQUESIZE - 1);
    }

    /**
     * P_NightmareRespawn
     */
    default void NightmareRespawn(mobj_t mobj) {
        int x, y, z; // fixed 
        subsector_t ss;
        mobj_t mo;
        mapthing_t mthing;

        x = mobj.spawnpoint.x << FRACBITS;
        y = mobj.spawnpoint.y << FRACBITS;

        // somthing is occupying it's position?
        if (!CheckPosition(mobj, x, y)) {
            return; // no respwan
        }
        // spawn a teleport fog at old spot
        // because of removal of the body?
        mo = SpawnMobj(mobj.x, mobj.y, mobj.subsector.sector.floorheight, mobjtype_t.MT_TFOG);
        
        // initiate teleport sound
        StartSound(mo, sounds.sfxenum_t.sfx_telept);

        // spawn a teleport fog at the new spot
        ss = levelLoader().PointInSubsector(x, y);

        mo = SpawnMobj(x, y, ss.sector.floorheight, mobjtype_t.MT_TFOG);

        StartSound(mo, sounds.sfxenum_t.sfx_telept);

        // spawn the new monster
        mthing = mobj.spawnpoint;

        // spawn it
        if (eval(mobj.info.flags & MF_SPAWNCEILING)) {
            z = ONCEILINGZ;
        } else {
            z = ONFLOORZ;
        }

        // inherit attributes from deceased one
        mo = SpawnMobj(x, y, z, mobj.type);
        mo.spawnpoint = mobj.spawnpoint;
        mo.angle = ANG45 * (mthing.angle / 45);

        if (eval(mthing.options & MTF_AMBUSH)) {
            mo.flags |= MF_AMBUSH;
        }

        mo.reactiontime = 18;

        // remove the old monster,
        RemoveMobj(mobj);
    }
    
    /**
     * P_SpawnMobj
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     * @param type
     * @return
     */
    @SourceCode.Exact
    @P_Mobj.C(P_SpawnMobj)
    default mobj_t SpawnMobj(@fixed_t int x, @fixed_t int y, @fixed_t int z, mobjtype_t type) {
        mobj_t mobj;
        state_t st;
        mobjinfo_t info;

        Z_Malloc: {
            mobj = createMobj();
        }
        info = mobjinfo[type.ordinal()];

        mobj.type = type;
        mobj.info = info;
        mobj.x = x;
        mobj.y = y;
        mobj.radius = info.radius;
        mobj.height = info.height;
        mobj.flags = info.flags;
        mobj.health = info.spawnhealth;

        if (getGameSkill() != skill_t.sk_nightmare) {
            mobj.reactiontime = info.reactiontime;
        }

        P_Random: {
            mobj.lastlook = P_Random() % MAXPLAYERS;
        }
        // do not set the state with P_SetMobjState,
        // because action routines can not be called yet
        st = states[info.spawnstate.ordinal()];

        mobj.mobj_state = st;
        mobj.mobj_tics = st.tics;
        mobj.mobj_sprite = st.sprite;
        mobj.mobj_frame = st.frame;

        // set subsector and/or block links
        P_SetThingPosition: {
            SetThingPosition(mobj);
        }

        mobj.floorz = mobj.subsector.sector.floorheight;
        mobj.ceilingz = mobj.subsector.sector.ceilingheight;

        if (z == ONFLOORZ) {
            mobj.z = mobj.floorz;
        } else if (z == ONCEILINGZ) {
            mobj.z = mobj.ceilingz - mobj.info.height;
        } else {
            mobj.z = z;
        }

        mobj.thinkerFunction = ActiveStates.P_MobjThinker;
        P_AddThinker: {
            AddThinker(mobj);
        }

        return mobj;
    }
    
    /**
     * P_SpawnPlayer
     * Called when a player is spawned on the level.
     * Most of the player structure stays unchanged
     * between levels.
     */
    @SourceCode.Exact
    @P_Mobj.C(P_SpawnPlayer)
    default void SpawnPlayer(mapthing_t mthing) {
        player_t p;
        @fixed_t int x, y, z;
        mobj_t mobj;

        // not playing?
        if (!PlayerInGame(mthing.type - 1)) {
            return;
        }

        p = getPlayer(mthing.type - 1);

        if (p.playerstate == PST_REBORN) {
            G_PlayerReborn: {
                p.PlayerReborn();
            }
        }
        //DM.PlayerReborn (mthing.type-1);

        x = mthing.x << FRACBITS;
        y = mthing.y << FRACBITS;
        z = ONFLOORZ;
        P_SpawnMobj: {
            mobj = this.SpawnMobj(x, y, z, mobjtype_t.MT_PLAYER);
        }

        // set color translations for player sprites
        if (mthing.type > 1) {
            mobj.flags |= (mthing.type - 1) << MF_TRANSSHIFT;
        }

        mobj.angle = ANG45 * (mthing.angle / 45);
        mobj.player = p;
        mobj.health = p.health[0];

        p.mo = mobj;
        p.playerstate = PST_LIVE;
        p.refire = 0;
        p.message = null;
        p.damagecount = 0;
        p.bonuscount = 0;
        p.extralight = 0;
        p.fixedcolormap = Palettes.COLORMAP_FIXED;
        p.viewheight = VIEWHEIGHT;

        // setup gun psprite
        P_SetupPsprites: {
            p.SetupPsprites();
        }

        // give all cards in death match mode
        if (IsDeathMatch()) {
            for (int i = 0; i < NUMCARDS; i++) {
                p.cards[i] = true;
            }
        }

        if (mthing.type - 1 == ConsolePlayerNumber()) {
            // wake up the status bar
            ST_Start: {
                getStatusBar().Start();
            }
            // wake up the heads up text
            HU_Start: {
                getHeadsUp().Start();
            }
        }
    }

    /**
     * P_SpawnMapThing The fields of the mapthing should already be in host byte order.
     */
    default mobj_t SpawnMapThing(mapthing_t mthing) {
        final DoomMain<?, ?> D = DOOM();
        int i;
        int bit;
        mobj_t mobj;
        int x;
        int y;
        int z;

        // count deathmatch start positions
        if (mthing.type == 11) {
            if (D.deathmatch_p < 10/*DM.deathmatchstarts[10]*/) {
                // memcpy (deathmatch_p, mthing, sizeof(*mthing));
                D.deathmatchstarts[D.deathmatch_p] = new mapthing_t(mthing);
                D.deathmatch_p++;
            }
            return null;
        }

        if (mthing.type <= 0) {
            // Ripped from Chocolate Doom :-p
            // Thing type 0 is actually "player -1 start".  
            // For some reason, Vanilla Doom accepts/ignores this.
            // MAES: no kidding.

            return null;
        }

        // check for players specially
        if (mthing.type <= 4 && mthing.type > 0) // killough 2/26/98 -- fix crashes
        {
            // save spots for respawning in network games
            D.playerstarts[mthing.type - 1] = new mapthing_t(mthing);
            if (!IsDeathMatch()) {
                this.SpawnPlayer(mthing);
            }

            return null;
        }

        // check for apropriate skill level
        if (!IsNetGame() && eval(mthing.options & 16)) {
            return null;
        }

        switch (getGameSkill()) {
            case sk_baby: bit = 1; break;
            case sk_nightmare: bit = 4; break;
            default:
                bit = 1 << (getGameSkill().ordinal() - 1);
                break;
        }

        if (!eval(mthing.options & bit)) {
            return null;
        }

        // find which type to spawn
        for (i = 0; i < NUMMOBJTYPES; i++) {
            if (mthing.type == mobjinfo[i].doomednum) {
                break;
            }
        }

        // phares 5/16/98:
        // Do not abort because of an unknown thing. Ignore it, but post a
        // warning message for the player.
        if (i == NUMMOBJTYPES) {
            Spawn.LOGGER.log(Level.WARNING,
                String.format("P_SpawnMapThing: Unknown type %d at (%d, %d)", mthing.type, mthing.x, mthing.y));
            return null;
        }

        // don't spawn keycards and players in deathmatch
        if (IsDeathMatch() && eval(mobjinfo[i].flags & MF_NOTDMATCH)) {
            return null;
        }

        // don't spawn any monsters if -nomonsters
        if (D.nomonsters && (i == mobjtype_t.MT_SKULL.ordinal() || eval(mobjinfo[i].flags & MF_COUNTKILL))) {
            return null;
        }

        // spawn it
        x = mthing.x << FRACBITS;
        y = mthing.y << FRACBITS;

        if (eval(mobjinfo[i].flags & MF_SPAWNCEILING)) {
            z = ONCEILINGZ;
        } else {
            z = ONFLOORZ;
        }

        mobj = this.SpawnMobj(x, y, z, mobjtype_t.values()[i]);
        mobj.spawnpoint.copyFrom(mthing);

        if (mobj.mobj_tics > 0) {
            mobj.mobj_tics = 1 + (P_Random() % mobj.mobj_tics);
        }
        if (eval(mobj.flags & MF_COUNTKILL)) {
            D.totalkills++;
        }
        if (eval(mobj.flags & MF_COUNTITEM)) {
            D.totalitems++;
        }

        mobj.angle = ANG45 * (mthing.angle / 45);
        if (eval(mthing.options & MTF_AMBUSH)) {
            mobj.flags |= MF_AMBUSH;
        }

        return mobj;

    }

    /**
     * P_SpawnBlood
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     * @param damage
     */
    default void SpawnBlood(int x, int y, int z, int damage) {
        mobj_t th;

        z += ((P_Random() - P_Random()) << 10);
        th = this.SpawnMobj(x, y, z, mobjtype_t.MT_BLOOD);
        th.momz = FRACUNIT * 2;
        th.mobj_tics -= P_Random() & 3;

        if (th.mobj_tics < 1) {
            th.mobj_tics = 1;
        }

        if (damage <= 12 && damage >= 9) {
            th.SetMobjState(statenum_t.S_BLOOD2);
        } else if (damage < 9) {
            th.SetMobjState(statenum_t.S_BLOOD3);
        }
    }

    /**
     * P_SpawnPuff
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     *
     */
    default void SpawnPuff(int x, int y, int z) {
        mobj_t th;

        z += ((P_Random() - P_Random()) << 10);

        th = this.SpawnMobj(x, y, z, mobjtype_t.MT_PUFF);
        th.momz = FRACUNIT;
        th.mobj_tics -= P_Random() & 3;

        if (th.mobj_tics < 1) {
            th.mobj_tics = 1;
        }

        // don't make punches spark on the wall
        if (contextTest(KEY_SPAWN, Spawn::isMeleeRange)) {
            th.SetMobjState(statenum_t.S_PUFF3);
        }
    }
}
