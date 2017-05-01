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

import static data.Limits.CEILSPEED;
import static data.Limits.MAXCEILINGS;
import data.sounds;
import doom.SourceCode;
import p.ActionSystem.AbstractCommand;

import static doom.SourceCode.P_Ceiling.EV_DoCeiling;
import static m.fixed_t.FRACUNIT;
import rr.line_t;
import rr.sector_t;
import utils.C2JUtils;
import static utils.C2JUtils.eval;

interface ActionsCeilings<R extends Actions.Registry & AbstractCommand<R>> extends ActionsPlanes<R> {
    /**
     * This needs to be called before loading, otherwise crushers won't be able to be restarted.
     */
    default void ClearCeilingsBeforeLoading() {
        obs().activeceilings = new ceiling_t[MAXCEILINGS];
    }

    /**
     * T_MoveCeiling
     */
    default void MoveCeiling(ceiling_t ceiling) {
        final Actions.Registry obs = obs();
        result_e res;

        switch (ceiling.direction) {
            case 0:
                // IN STASIS
                break;
            case 1:
                // UP
                res = this.MovePlane(ceiling.sector, ceiling.speed, ceiling.topheight, false, 1, ceiling.direction);

                if (!eval(obs.DOOM.leveltime & 7)) {
                    switch (ceiling.type) {
                        case silentCrushAndRaise:
                            break;
                        default:
                            obs.DOOM.doomSound.StartSound(ceiling.sector.soundorg, sounds.sfxenum_t.sfx_stnmov);
                    }
                }

                if (res == result_e.pastdest) {
                    switch (ceiling.type) {
                        case raiseToHighest:
                            this.RemoveActiveCeiling(ceiling);
                            break;
                        case silentCrushAndRaise:
                            obs.DOOM.doomSound.StartSound(ceiling.sector.soundorg, sounds.sfxenum_t.sfx_pstop);
                        case fastCrushAndRaise:
                        case crushAndRaise:
                            ceiling.direction = -1;
                    }
                }
                break;

            case -1:
                // DOWN
                res = this.MovePlane(ceiling.sector, ceiling.speed, ceiling.bottomheight, ceiling.crush, 1, ceiling.direction);

                if (!eval(obs.DOOM.leveltime & 7)) {
                    switch (ceiling.type) {
                        case silentCrushAndRaise:
                            break;
                        default:
                            obs.DOOM.doomSound.StartSound(ceiling.sector.soundorg, sounds.sfxenum_t.sfx_stnmov);
                    }
                }

                if (res == result_e.pastdest) {
                    switch (ceiling.type) {
                        case silentCrushAndRaise:
                            obs.DOOM.doomSound.StartSound(ceiling.sector.soundorg, sounds.sfxenum_t.sfx_pstop);
                        case crushAndRaise:
                            ceiling.speed = CEILSPEED;
                        case fastCrushAndRaise:
                            ceiling.direction = 1;
                            break;
                        case lowerAndCrush:
                        case lowerToFloor:
                            this.RemoveActiveCeiling(ceiling);
                            break;
                        default:
                            break;
                    }
                } else { // ( res != result_e.pastdest )
                    if (res == result_e.crushed) {
                        switch (ceiling.type) {
                            case silentCrushAndRaise:
                            case crushAndRaise:
                            case lowerAndCrush:
                                ceiling.speed = CEILSPEED / 8;
                                break;
                            default:
                                break;
                        }
                    }
                }
        }
    }

    //
    // EV.DoCeiling
    // Move a ceiling up/down and all around!
    //
    @SourceCode.P_Ceiling.C(EV_DoCeiling)
    default boolean DoCeiling(line_t line, ceiling_e type) {
        final Actions.Registry obs = obs();
        int secnum = -1;
        boolean rtn = false;
        sector_t sec;
        ceiling_t ceiling;

        //  Reactivate in-stasis ceilings...for certain types.
        switch (type) {
            case fastCrushAndRaise:
            case silentCrushAndRaise:
            case crushAndRaise:
                this.ActivateInStasisCeiling(line);
            default:
                break;
        }

        while ((secnum = obs.FindSectorFromLineTag(line, secnum)) >= 0) {
            sec = obs.DOOM.levelLoader.sectors[secnum];
            if (sec.specialdata != null) {
                continue;
            }

            // new door thinker
            rtn = true;
            ceiling = new ceiling_t();
            sec.specialdata = ceiling;
            ceiling.thinkerFunction = ActiveStates.T_MoveCeiling;
            obs.AddThinker(ceiling);
            ceiling.sector = sec;
            ceiling.crush = false;

            switch (type) {
                case fastCrushAndRaise:
                    ceiling.crush = true;
                    ceiling.topheight = sec.ceilingheight;
                    ceiling.bottomheight = sec.floorheight + (8 * FRACUNIT);
                    ceiling.direction = -1;
                    ceiling.speed = CEILSPEED * 2;
                    break;

                case silentCrushAndRaise:
                case crushAndRaise:
                    ceiling.crush = true;
                    ceiling.topheight = sec.ceilingheight;
                case lowerAndCrush:
                case lowerToFloor:
                    ceiling.bottomheight = sec.floorheight;
                    if (type != ceiling_e.lowerToFloor) {
                        ceiling.bottomheight += 8 * FRACUNIT;
                    }
                    ceiling.direction = -1;
                    ceiling.speed = CEILSPEED;
                    break;

                case raiseToHighest:
                    ceiling.topheight = sec.FindHighestCeilingSurrounding();
                    ceiling.direction = 1;
                    ceiling.speed = CEILSPEED;
                    break;
            }

            ceiling.tag = sec.tag;
            ceiling.type = type;
            this.AddActiveCeiling(ceiling);
        }
        return rtn;
    }

    //
    // Add an active ceiling
    //
    default void AddActiveCeiling(ceiling_t c) {
        final Actions.Registry obs = obs();
        for (int i = 0; i < this.getMaxCeilings(); ++i) {
            if (this.getActiveCeilings()[i] == null) {
                this.getActiveCeilings()[i] = c;
                return;
            }
        }
        // Needs rezising
        this.setActiveceilings(C2JUtils.resize(c, this.getActiveCeilings(), 2 * this.getActiveCeilings().length));
    }

    //
    // Remove a ceiling's thinker
    //
    default void RemoveActiveCeiling(ceiling_t c) {
        final Actions.Registry obs = obs();
        for (int i = 0; i < this.getMaxCeilings(); ++i) {
            if (this.getActiveCeilings()[i] == c) {
                this.getActiveCeilings()[i].sector.specialdata = null;
                obs.RemoveThinker(this.getActiveCeilings()[i]);
                this.getActiveCeilings()[i] = null;
                break;
            }
        }
    }

    //
    // Restart a ceiling that's in-stasis
    //
    default void ActivateInStasisCeiling(line_t line) {
        final Actions.Registry obs = obs();
        for (int i = 0; i < this.getMaxCeilings(); ++ i) {
            if (this.getActiveCeilings()[i] != null
            && (this.getActiveCeilings()[i].tag == line.tag)
            && (this.getActiveCeilings()[i].direction == 0))
            {
                this.getActiveCeilings()[i].direction = this.getActiveCeilings()[i].olddirection;
                this.getActiveCeilings()[i].thinkerFunction = ActiveStates.T_MoveCeiling;
            }
        }
    }
    
    //
    // EV_CeilingCrushStop
    // Stop a ceiling from crushing!
    //
    default int CeilingCrushStop(line_t line) {
        final Actions.Registry obs = obs();
        int i;
        int rtn;

        rtn = 0;
        for (i = 0; i < this.getMaxCeilings(); i++) {
            if (this.getActiveCeilings()[i] != null
            && (this.getActiveCeilings()[i].tag == line.tag)
            && (this.getActiveCeilings()[i].direction != 0))
            {
                this.getActiveCeilings()[i].olddirection = this.getActiveCeilings()[i].direction;
                // MAES: don't set it to NOP here, otherwise its thinker will be
                // removed and it won't be possible to restart it.
                this.getActiveCeilings()[i].thinkerFunction = null;
                this.getActiveCeilings()[i].direction = 0;       // in-stasis
                rtn = 1;
            }
        }

        return rtn;
    }

    default void setActiveceilings(ceiling_t[] activeceilings) {
        final Actions.Registry obs = obs();
        obs.activeceilings = activeceilings;
    }

    default ceiling_t[] getActiveCeilings() {
        final Actions.Registry obs = obs();
        return obs.activeceilings;
    }

    default int getMaxCeilings() {
        final Actions.Registry obs = obs();
        return obs.activeceilings.length;
    }
    
}
