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

import data.mobjtype_t;
import data.sounds;
import defines.skill_t;
import defines.statenum_t;
import doom.thinker_t;
import static m.fixed_t.FRACUNIT;

interface ActiveStatesMonstersHorrendousVisages extends ActionsSpawn, ActionsMissiles {
    default void A_BrainAwake(mobj_t mo) {
        final Actions.Registry obs = obs();
        thinker_t thinker;
        mobj_t m;

        // find all the target spots
        obs.numbraintargets = 0;
        obs.braintargeton = 0;

        //thinker = obs.thinkercap.next;
        for (thinker = obs.thinkercap.next;
            thinker != obs.thinkercap;
            thinker = thinker.next) {
            if (thinker.thinkerFunction != ActiveStates.P_MobjThinker) {
                continue;   // not a mobj
            }
            m = (mobj_t) thinker;

            if (m.type == mobjtype_t.MT_BOSSTARGET) {
                obs.braintargets[obs.numbraintargets] = m;
                obs.numbraintargets++;
            }
        }

        obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_bossit);
    }

    default void A_BrainScream(mobj_t mo) {
        final Actions.Registry obs = obs();
        int x;
        int y;
        int z;
        mobj_t th;

        for (x = mo.x - 196 * FRACUNIT; x < mo.x + 320 * FRACUNIT; x += FRACUNIT * 8) {
            y = mo.y - 320 * FRACUNIT;
            z = 128 + obs.DOOM.random.P_Random() * 2 * FRACUNIT;
            th = SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET);
            th.momz = obs.DOOM.random.P_Random() * 512;

            th.SetMobjState(statenum_t.S_BRAINEXPLODE1);

            th.mobj_tics -= obs.DOOM.random.P_Random() & 7;
            if (th.mobj_tics < 1) {
                th.mobj_tics = 1;
            }
        }

        obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_bosdth);
    }

    default void A_BrainExplode(mobj_t mo) {
        final Actions.Registry obs = obs();
        int x;
        int y;
        int z;
        mobj_t th;

        x = mo.x + (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) * 2048;
        y = mo.y;
        z = 128 + obs.DOOM.random.P_Random() * 2 * FRACUNIT;
        th = SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET);
        th.momz = obs.DOOM.random.P_Random() * 512;

        th.SetMobjState(statenum_t.S_BRAINEXPLODE1);

        th.mobj_tics -= obs.DOOM.random.P_Random() & 7;
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1;
        }
    }

    default void A_BrainDie(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.ExitLevel();
    }

    default void A_BrainSpit(mobj_t mo) {
        final Actions.Registry obs = obs();
        mobj_t targ;
        mobj_t newmobj;

        obs.easy ^= 1;
        if (obs.DOOM.gameskill.ordinal() <= skill_t.sk_easy.ordinal() && (obs.easy == 0)) {
            return;
        }

        // shoot a cube at current target
        targ = obs.braintargets[obs.braintargeton];

        // Load-time fix: awake on zero numbrain targets, if A_BrainSpit is called.
        if (obs.numbraintargets == 0) {
            A_BrainAwake(mo);
            return;
        }
        obs.braintargeton = (obs.braintargeton + 1) % obs.numbraintargets;

        // spawn brain missile
        newmobj = SpawnMissile(mo, targ, mobjtype_t.MT_SPAWNSHOT);
        newmobj.target = targ;
        newmobj.reactiontime = ((targ.y - mo.y) / newmobj.momy) / newmobj.mobj_state.tics;

        obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_bospit);
    }

    default void A_SpawnFly(mobj_t mo) {
        final Actions.Registry obs = obs();
        mobj_t newmobj;
        mobj_t fog;
        mobj_t targ;
        int r;
        mobjtype_t type;

        if (--mo.reactiontime != 0) {
            return; // still flying
        }
        targ = mo.target;

        // First spawn teleport fog.
        fog = SpawnMobj(targ.x, targ.y, targ.z, mobjtype_t.MT_SPAWNFIRE);
        obs.DOOM.doomSound.StartSound(fog, sounds.sfxenum_t.sfx_telept);

        // Randomly select monster to spawn.
        r = obs.DOOM.random.P_Random();

        // Probability distribution (kind of :),
        // decreasing likelihood.
        if (r < 50) {
            type = mobjtype_t.MT_TROOP;
        } else if (r < 90) {
            type = mobjtype_t.MT_SERGEANT;
        } else if (r < 120) {
            type = mobjtype_t.MT_SHADOWS;
        } else if (r < 130) {
            type = mobjtype_t.MT_PAIN;
        } else if (r < 160) {
            type = mobjtype_t.MT_HEAD;
        } else if (r < 162) {
            type = mobjtype_t.MT_VILE;
        } else if (r < 172) {
            type = mobjtype_t.MT_UNDEAD;
        } else if (r < 192) {
            type = mobjtype_t.MT_BABY;
        } else if (r < 222) {
            type = mobjtype_t.MT_FATSO;
        } else if (r < 246) {
            type = mobjtype_t.MT_KNIGHT;
        } else {
            type = mobjtype_t.MT_BRUISER;
        }

        newmobj = SpawnMobj(targ.x, targ.y, targ.z, type);
        if (obs.EN.LookForPlayers(newmobj, true)) {
            newmobj.SetMobjState(newmobj.info.seestate);
        }

        // telefrag anything in this spot
        TeleportMove(newmobj, newmobj.x, newmobj.y);

        // remove self (i.e., cube).
        obs.RemoveMobj(mo);
    }
}
