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
import doom.player_t;

interface ActiveStatesSounds extends ActiveStatesAi, ActiveStatesMonstersHorrendousVisages, ActiveStatesWeapons {
    default void A_Scream(mobj_t actor) {
        final Actions.Registry obs = obs();
        int sound;

        switch (actor.info.deathsound) {
            case sfx_None:
                return;

            case sfx_podth1:
            case sfx_podth2:
            case sfx_podth3:
                sound = sounds.sfxenum_t.sfx_podth1.ordinal() + obs.DOOM.random.P_Random() % 3;
                break;

            case sfx_bgdth1:
            case sfx_bgdth2:
                sound = sounds.sfxenum_t.sfx_bgdth1.ordinal() + obs.DOOM.random.P_Random() % 2;
                break;

            default:
                sound = actor.info.deathsound.ordinal();
                break;
        }

        // Check for bosses.
        if (actor.type == mobjtype_t.MT_SPIDER
            || actor.type == mobjtype_t.MT_CYBORG) {
            // full volume
            obs.DOOM.doomSound.StartSound(null, sound);
        } else {
            obs.DOOM.doomSound.StartSound(actor, sound);
        }
    }
    default void A_Hoof(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_hoof);
        A_Chase(mo);
    }

    //
    // A_BFGsound
    //
    default void A_BFGsound(player_t player, pspdef_t psp) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(player.mo, sounds.sfxenum_t.sfx_bfg);
    }

    default void A_OpenShotgun2(player_t player, pspdef_t psp) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(player.mo, sounds.sfxenum_t.sfx_dbopn);
    }

    default void A_LoadShotgun2(player_t player, pspdef_t psp) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(player.mo, sounds.sfxenum_t.sfx_dbload);
    }

    default void A_CloseShotgun2(player_t player, pspdef_t psp) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(player.mo, sounds.sfxenum_t.sfx_dbcls);
        A_ReFire(player, psp);
    }

    default void A_BrainPain(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_bospn);
    }
    
    default void A_Metal(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_metal);
        A_Chase(mo);
    }

    default void A_BabyMetal(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_bspwlk);
        A_Chase(mo);
    }
    
    // travelling cube sound
    default void A_SpawnSound(mobj_t mo) {
        final Actions.Registry obs = obs();
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_boscub);
        A_SpawnFly(mo);
    }
    
    default void A_PlayerScream(mobj_t actor) {
        final Actions.Registry obs = obs();
        // Default death sound.
        sounds.sfxenum_t sound = sounds.sfxenum_t.sfx_pldeth;

        if (obs.DOOM.isCommercial() && (actor.health < -50)) {
            // IF THE PLAYER DIES
            // LESS THAN -50% WITHOUT GIBBING
            sound = sounds.sfxenum_t.sfx_pdiehi;
        }

        obs.DOOM.doomSound.StartSound(actor, sound);
    }

}
