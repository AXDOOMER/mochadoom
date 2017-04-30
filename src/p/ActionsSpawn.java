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
import data.state_t;
import defines.skill_t;
import defines.statenum_t;
import doom.SourceCode.P_Mobj;
import static doom.SourceCode.P_Mobj.P_SpawnMobj;
import static doom.SourceCode.P_Mobj.P_SpawnPlayer;
import doom.SourceCode.fixed_t;
import doom.player_t;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import p.ActionSystem.Observer;
import static p.mobj_t.MF_AMBUSH;
import static p.mobj_t.MF_COUNTITEM;
import static p.mobj_t.MF_COUNTKILL;
import static p.mobj_t.MF_NOTDMATCH;
import static p.mobj_t.MF_SPAWNCEILING;
import static p.mobj_t.MF_TRANSSHIFT;
import static utils.C2JUtils.eval;
import v.graphics.Palettes;

interface ActionsSpawn extends Observer<Actions.Registry> {
    /**
     * P_SpawnMobj
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     * @param type
     * @return
     */
    @P_Mobj.C(P_SpawnMobj)
    default mobj_t SpawnMobj(@fixed_t int x, @fixed_t int y, @fixed_t int z, mobjtype_t type) {
        final p.Actions.Registry obs = obs();
        mobj_t mobj;
        state_t st;
        mobjinfo_t info;

        Z_Malloc: {
            mobj = mobj_t.createOn(obs.DOOM);
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

        if (obs.DOOM.gameskill != skill_t.sk_nightmare) {
            mobj.reactiontime = info.reactiontime;
        }

        P_Random: {
            mobj.lastlook = obs.DOOM.random.P_Random() % MAXPLAYERS;
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
            obs.DOOM.levelLoader.SetThingPosition(mobj);
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
            obs.AddThinker(mobj);
        }

        return mobj;
    }
    
    /**
     * P_SpawnPlayer Called when a player is spawned on the level. Most of the player structure stays unchanged between
     * levels.
     */
    @P_Mobj.C(P_SpawnPlayer)
    default void SpawnPlayer(mapthing_t mthing) {
        final p.Actions.Registry obs = obs();
        player_t p;
        @fixed_t int x, y, z;
        mobj_t mobj;

        // not playing?
        if (!obs.DOOM.playeringame[mthing.type - 1]) {
            return;
        }

        p = obs.DOOM.players[mthing.type - 1];

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
        if (obs.DOOM.deathmatch) {
            for (int i = 0; i < NUMCARDS; i++) {
                p.cards[i] = true;
            }
        }

        if (mthing.type - 1 == obs.DOOM.consoleplayer) {
            // wake up the status bar
            ST_Start: {
                obs.DOOM.statusBar.Start();
            }
            // wake up the heads up text
            HU_Start: {
                obs.DOOM.handsUp.Start();
            }
        }
    }

    /**
     * P_SpawnMapThing The fields of the mapthing should already be in host byte order.
     */
    default mobj_t SpawnMapThing(mapthing_t mthing) {
        final p.Actions.Registry obs = obs();
        int i;
        int bit;
        mobj_t mobj;
        int x;
        int y;
        int z;

        // count deathmatch start positions
        if (mthing.type == 11) {
            if (obs.DOOM.deathmatch_p < 10/*DM.deathmatchstarts[10]*/) {
                // memcpy (deathmatch_p, mthing, sizeof(*mthing));
                obs.DOOM.deathmatchstarts[obs.DOOM.deathmatch_p] = new mapthing_t(mthing);
                obs.DOOM.deathmatch_p++;
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
            obs.DOOM.playerstarts[mthing.type - 1] = new mapthing_t(mthing);
            if (!obs.DOOM.deathmatch) {
                this.SpawnPlayer(mthing);
            }

            return null;
        }

        // check for apropriate skill level
        if (!obs.DOOM.netgame && eval(mthing.options & 16)) {
            return null;
        }

        switch (obs.DOOM.gameskill) {
            case sk_baby: bit = 1; break;
            case sk_nightmare: bit = 4; break;
            default:
                bit = 1 << (obs.DOOM.gameskill.ordinal() - 1);
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
            System.err.printf("P_SpawnMapThing: Unknown type %d at (%d, %d)", mthing.type, mthing.x, mthing.y);
            return null;
        }

        // don't spawn keycards and players in deathmatch
        if (obs.DOOM.deathmatch && eval(mobjinfo[i].flags & MF_NOTDMATCH)) {
            return null;
        }

        // don't spawn any monsters if -nomonsters
        if (obs.DOOM.nomonsters
                && (i == mobjtype_t.MT_SKULL.ordinal()
                || eval(mobjinfo[i].flags & MF_COUNTKILL))) {
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
            mobj.mobj_tics = 1 + (obs.DOOM.random.P_Random() % mobj.mobj_tics);
        }
        if (eval(mobj.flags & MF_COUNTKILL)) {
            obs.DOOM.totalkills++;
        }
        if (eval(mobj.flags & MF_COUNTITEM)) {
            obs.DOOM.totalitems++;
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
        final p.Actions.Registry obs = obs();
        mobj_t th;

        z += ((obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 10);
        th = this.SpawnMobj(x, y, z, mobjtype_t.MT_BLOOD);
        th.momz = FRACUNIT * 2;
        th.mobj_tics -= obs.DOOM.random.P_Random() & 3;

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
        final p.Actions.Registry obs = obs();
        mobj_t th;

        z += ((obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 10);

        th = this.SpawnMobj(x, y, z, mobjtype_t.MT_PUFF);
        th.momz = FRACUNIT;
        th.mobj_tics -= obs.DOOM.random.P_Random() & 3;

        if (th.mobj_tics < 1) {
            th.mobj_tics = 1;
        }

        // don't make punches spark on the wall
        if (obs.attackrange == MELEERANGE) {
            th.SetMobjState(statenum_t.S_PUFF3);
        }
    }
}
