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

import static data.Limits.MAXRADIUS;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.sounds;
import defines.statenum_t;
import p.ActionSystem.AbstractCommand;

import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static m.fixed_t.MAPFRACUNIT;
import static p.ChaseDirections.DI_NODIR;
import static p.ChaseDirections.xspeed;
import static p.ChaseDirections.yspeed;

interface ActiveStatesMonstersViles<R extends Actions.Registry & AbstractCommand<R>> extends ActiveStatesAi<R>, ActionsAttacks<R> {
    //
    // A_VileChase
    // Check for ressurecting a body
    //
    default void A_VileChase(mobj_t actor) {
        final Actions.Registry obs = obs();
        int xl;
        int xh;
        int yl;
        int yh;

        int bx;
        int by;

        mobjinfo_t info;
        mobj_t temp;

        if (actor.movedir != DI_NODIR) {
            // check for corpses to raise
            obs.vileTryX
                = actor.x + actor.info.speed * xspeed[actor.movedir];
            obs.vileTryY
                = actor.y + actor.info.speed * yspeed[actor.movedir];

            xl = obs.DOOM.levelLoader.getSafeBlockX(obs.vileTryX - obs.DOOM.levelLoader.bmaporgx - MAXRADIUS * 2);
            xh = obs.DOOM.levelLoader.getSafeBlockX(obs.vileTryX - obs.DOOM.levelLoader.bmaporgx + MAXRADIUS * 2);
            yl = obs.DOOM.levelLoader.getSafeBlockY(obs.vileTryY - obs.DOOM.levelLoader.bmaporgy - MAXRADIUS * 2);
            yh = obs.DOOM.levelLoader.getSafeBlockY(obs.vileTryY - obs.DOOM.levelLoader.bmaporgy + MAXRADIUS * 2);

            obs.vileObj = actor;
            for (bx = xl; bx <= xh; bx++) {
                for (by = yl; by <= yh; by++) {
                    // Call PIT_VileCheck to check
                    // whether object is a corpse
                    // that can be raised.
                    if (!BlockThingsIterator(bx, by, this::VileCheck)) {
                        // got one!
                        temp = actor.target;
                        actor.target = obs.vileCorpseHit;
                        A_FaceTarget(actor);
                        actor.target = temp;

                        actor.SetMobjState(statenum_t.S_VILE_HEAL1);
                        obs.DOOM.doomSound.StartSound(obs.vileCorpseHit, sounds.sfxenum_t.sfx_slop);
                        info = obs.vileCorpseHit.info;

                        obs.vileCorpseHit.SetMobjState(info.raisestate);
                        obs.vileCorpseHit.height <<= 2;
                        obs.vileCorpseHit.flags = info.flags;
                        obs.vileCorpseHit.health = info.spawnhealth;
                        obs.vileCorpseHit.target = null;

                        return;
                    }
                }
            }
        }

        // Return to normal attack.
        A_Chase(actor);
    }

    //
    // A_VileStart
    //
    default void A_VileStart(mobj_t actor) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_vilatk);
    }
    
    //
    // A_Fire
    // Keep fire in front of player unless out of sight
    //
    default void A_StartFire(mobj_t actor) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_flamst);
        A_Fire(actor);
    }

    default void A_FireCrackle(mobj_t actor) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_flame);
        A_Fire(actor);
    }

    default void A_Fire(mobj_t actor) {
        final Actions.Registry obs = obs();
        mobj_t dest;
        //long    an;

        dest = actor.tracer;
        if (dest == null) {
            return;
        }

        // don't move it if the vile lost sight
        if (!obs.EN.CheckSight(actor.target, dest)) {
            return;
        }

        // an = dest.angle >>> ANGLETOFINESHIFT;
        obs.UnsetThingPosition(actor);
        actor.x = dest.x + FixedMul(24 * FRACUNIT, finecosine(dest.angle));
        actor.y = dest.y + FixedMul(24 * FRACUNIT, finesine(dest.angle));
        actor.z = dest.z;
        obs.DOOM.levelLoader.SetThingPosition(actor);
    }
    
    //
    // A_VileTarget
    // Spawn the hellfire
    //
    default void A_VileTarget(mobj_t actor) {
        final Actions.Registry obs = obs();
        mobj_t fog;

        if (actor.target == null) {
            return;
        }

        A_FaceTarget(actor);

        fog = SpawnMobj(actor.target.x,
            actor.target.y,
            actor.target.z, mobjtype_t.MT_FIRE);

        actor.tracer = fog;
        fog.target = actor;
        fog.tracer = actor.target;
        A_Fire(fog);
    }

    //
    // A_VileAttack
    //
    default void A_VileAttack(mobj_t actor) {
        final Actions.Registry obs = obs();
        mobj_t fire;
        //int     an;

        if (actor.target == null) {
            return;
        }

        A_FaceTarget(actor);

        if (!obs.EN.CheckSight(actor, actor.target)) {
            return;
        }

        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_barexp);
        DamageMobj(actor.target, actor, actor, 20);
        actor.target.momz = 1000 * MAPFRACUNIT / actor.target.info.mass;

        // an = actor.angle >> ANGLETOFINESHIFT;
        fire = actor.tracer;

        if (fire == null) {
            return;
        }

        // move the fire between the vile and the player
        fire.x = actor.target.x - FixedMul(24 * FRACUNIT, finecosine(actor.angle));
        fire.y = actor.target.y - FixedMul(24 * FRACUNIT, finesine(actor.angle));
        RadiusAttack(fire, actor, 70);
    }
}
