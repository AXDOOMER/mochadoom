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

import p.ActionSystem.AbstractCommand;
import rr.line_t;

interface ActionsMoveEvents<R extends Actions.Registry & AbstractCommand<R>> extends ActionsCeilings<R>, ActionsFloors<R>, ActionsDoors<R>, ActionsTeleportation<R> {
    //
    //EVENTS
    //Events are operations triggered by using, crossing,
    //or shooting special lines, or by timed thinkers.
    //
    /**
     * P_CrossSpecialLine - TRIGGER Called every time a thing origin is about to cross a line with a non 0 special.
     */
    default void CrossSpecialLine(line_t line, int side, mobj_t thing) {
        final Actions.Registry obs = obs();
        //line_t line;
        boolean ok;

        //line = LL.lines[linenum];
        //  Triggers that other things can activate
        if (thing.player == null) {
            // Things that should NOT trigger specials...
            switch (thing.type) {
                case MT_ROCKET:
                case MT_PLASMA:
                case MT_BFG:
                case MT_TROOPSHOT:
                case MT_HEADSHOT:
                case MT_BRUISERSHOT:
                    return;
                // break;

                default:
                    break;
            }

            ok = false;
            switch (line.special) {
                case 39:  // TELEPORT TRIGGER
                case 97:  // TELEPORT RETRIGGER
                case 125: // TELEPORT MONSTERONLY TRIGGER
                case 126: // TELEPORT MONSTERONLY RETRIGGER
                case 4:   // RAISE DOOR
                case 10:  // PLAT DOWN-WAIT-UP-STAY TRIGGER
                case 88:  // PLAT DOWN-WAIT-UP-STAY RETRIGGER
                    ok = true;
                    break;
            }
            if (!ok) {
                return;
            }
        }

        // Note: could use some const's here.
        switch (line.special) {
            // TRIGGERS.
            // All from here to RETRIGGERS.
            case 2:
                // Open Door
                this.DoDoor(line, vldoor_e.open);
                line.special = 0;
                break;

            case 3:
                // Close Door
                this.DoDoor(line, vldoor_e.close);
                line.special = 0;
                break;

            case 4:
                // Raise Door
                this.DoDoor(line, vldoor_e.normal);
                line.special = 0;
                break;

            case 5:
                // Raise Floor
                this.DoFloor(line, floor_e.raiseFloor);
                line.special = 0;
                break;

            case 6:
                // Fast Ceiling Crush & Raise
                this.DoCeiling(line, ceiling_e.fastCrushAndRaise);
                line.special = 0;
                break;

            case 8:
                // Build Stairs
                this.BuildStairs(line, stair_e.build8);
                line.special = 0;
                break;

            case 10:
                // PlatDownWaitUp
                obs.PEV.DoPlat(line, plattype_e.downWaitUpStay, 0);
                line.special = 0;
                break;

            case 12:
                // Light Turn On - brightest near
                obs.LEV.LightTurnOn(line, 0);
                line.special = 0;
                break;

            case 13:
                // Light Turn On 255
                obs.LEV.LightTurnOn(line, 255);
                line.special = 0;
                break;

            case 16:
                // Close Door 30
                this.DoDoor(line, vldoor_e.close30ThenOpen);
                line.special = 0;
                break;

            case 17:
                // Start Light Strobing
                obs.LEV.StartLightStrobing(line);
                line.special = 0;
                break;

            case 19:
                // Lower Floor
                this.DoFloor(line, floor_e.lowerFloor);
                line.special = 0;
                break;

            case 22:
                // Raise floor to nearest height and change texture
                obs.PEV.DoPlat(line, plattype_e.raiseToNearestAndChange, 0);
                line.special = 0;
                break;

            case 25:
                // Ceiling Crush and Raise
                this.DoCeiling(line, ceiling_e.crushAndRaise);
                line.special = 0;
                break;

            case 30:
                // Raise floor to shortest texture height
                //  on either side of lines.
                this.DoFloor(line, floor_e.raiseToTexture);
                line.special = 0;
                break;

            case 35:
                // Lights Very Dark
                obs.LEV.LightTurnOn(line, 35);
                line.special = 0;
                break;

            case 36:
                // Lower Floor (TURBO)
                this.DoFloor(line, floor_e.turboLower);
                line.special = 0;
                break;

            case 37:
                // LowerAndChange
                this.DoFloor(line, floor_e.lowerAndChange);
                line.special = 0;
                break;

            case 38:
                // Lower Floor To Lowest
                this.DoFloor(line, floor_e.lowerFloorToLowest);
                line.special = 0;
                break;

            case 39:
                // TELEPORT!
                this.Teleport(line, side, thing);
                line.special = 0;
                break;

            case 40:
                // RaiseCeilingLowerFloor
                this.DoCeiling(line, ceiling_e.raiseToHighest);
                this.DoFloor(line, floor_e.lowerFloorToLowest);
                line.special = 0;
                break;

            case 44:
                // Ceiling Crush
                this.DoCeiling(line, ceiling_e.lowerAndCrush);
                line.special = 0;
                break;

            case 52:
                // EXIT!
                obs.DOOM.ExitLevel();
                break;

            case 53:
                // Perpetual Platform Raise
                obs.PEV.DoPlat(line, plattype_e.perpetualRaise, 0);
                line.special = 0;
                break;

            case 54:
                // Platform Stop
                obs.PEV.StopPlat(line);
                line.special = 0;
                break;

            case 56:
                // Raise Floor Crush
                this.DoFloor(line, floor_e.raiseFloorCrush);
                line.special = 0;
                break;

            case 57:
                // Ceiling Crush Stop
                this.CeilingCrushStop(line);
                line.special = 0;
                break;

            case 58:
                // Raise Floor 24
                this.DoFloor(line, floor_e.raiseFloor24);
                line.special = 0;
                break;

            case 59:
                // Raise Floor 24 And Change
                this.DoFloor(line, floor_e.raiseFloor24AndChange);
                line.special = 0;
                break;

            case 104:
                // Turn lights off in sector(tag)
                obs.LEV.TurnTagLightsOff(line);
                line.special = 0;
                break;

            case 108:
                // Blazing Door Raise (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeRaise);
                line.special = 0;
                break;

            case 109:
                // Blazing Door Open (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeOpen);
                line.special = 0;
                break;

            case 100:
                // Build Stairs Turbo 16
                this.BuildStairs(line, stair_e.turbo16);
                line.special = 0;
                break;

            case 110:
                // Blazing Door Close (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeClose);
                line.special = 0;
                break;

            case 119:
                // Raise floor to nearest surr. floor
                this.DoFloor(line, floor_e.raiseFloorToNearest);
                line.special = 0;
                break;

            case 121:
                // Blazing PlatDownWaitUpStay
                obs.PEV.DoPlat(line, plattype_e.blazeDWUS, 0);
                line.special = 0;
                break;

            case 124:
                // Secret EXIT
                obs.DOOM.SecretExitLevel();
                break;

            case 125:
                // TELEPORT MonsterONLY
                if (thing.player == null) {
                    this.Teleport(line, side, thing);
                    line.special = 0;
                }
                break;

            case 130:
                // Raise Floor Turbo
                this.DoFloor(line, floor_e.raiseFloorTurbo);
                line.special = 0;
                break;

            case 141:
                // Silent Ceiling Crush & Raise
                this.DoCeiling(line, ceiling_e.silentCrushAndRaise);
                line.special = 0;
                break;

            // RETRIGGERS.  All from here till end.
            case 72:
                // Ceiling Crush
                this.DoCeiling(line, ceiling_e.lowerAndCrush);
                break;

            case 73:
                // Ceiling Crush and Raise
                this.DoCeiling(line, ceiling_e.crushAndRaise);
                break;

            case 74:
                // Ceiling Crush Stop
                this.CeilingCrushStop(line);
                break;

            case 75:
                // Close Door
                this.DoDoor(line, vldoor_e.close);
                break;

            case 76:
                // Close Door 30
                this.DoDoor(line, vldoor_e.close30ThenOpen);
                break;

            case 77:
                // Fast Ceiling Crush & Raise
                this.DoCeiling(line, ceiling_e.fastCrushAndRaise);
                break;

            case 79:
                // Lights Very Dark
                obs.LEV.LightTurnOn(line, 35);
                break;

            case 80:
                // Light Turn On - brightest near
                obs.LEV.LightTurnOn(line, 0);
                break;

            case 81:
                // Light Turn On 255
                obs.LEV.LightTurnOn(line, 255);
                break;

            case 82:
                // Lower Floor To Lowest
                this.DoFloor(line, floor_e.lowerFloorToLowest);
                break;

            case 83:
                // Lower Floor
                this.DoFloor(line, floor_e.lowerFloor);
                break;

            case 84:
                // LowerAndChange
                this.DoFloor(line, floor_e.lowerAndChange);
                break;

            case 86:
                // Open Door
                this.DoDoor(line, vldoor_e.open);
                break;

            case 87:
                // Perpetual Platform Raise
                obs.PEV.DoPlat(line, plattype_e.perpetualRaise, 0);
                break;

            case 88:
                // PlatDownWaitUp
                obs.PEV.DoPlat(line, plattype_e.downWaitUpStay, 0);
                break;

            case 89:
                // Platform Stop
                obs.PEV.StopPlat(line);
                break;

            case 90:
                // Raise Door
                this.DoDoor(line, vldoor_e.normal);
                break;

            case 91:
                // Raise Floor
                this.DoFloor(line, floor_e.raiseFloor);
                break;

            case 92:
                // Raise Floor 24
                this.DoFloor(line, floor_e.raiseFloor24);
                break;

            case 93:
                // Raise Floor 24 And Change
                this.DoFloor(line, floor_e.raiseFloor24AndChange);
                break;

            case 94:
                // Raise Floor Crush
                this.DoFloor(line, floor_e.raiseFloorCrush);
                break;

            case 95:
                // Raise floor to nearest height
                // and change texture.
                obs.PEV.DoPlat(line, plattype_e.raiseToNearestAndChange, 0);
                break;

            case 96:
                // Raise floor to shortest texture height
                // on either side of lines.
                this.DoFloor(line, floor_e.raiseToTexture);
                break;

            case 97:
                // TELEPORT!
                this.Teleport(line, side, thing);
                break;

            case 98:
                // Lower Floor (TURBO)
                this.DoFloor(line, floor_e.turboLower);
                break;

            case 105:
                // Blazing Door Raise (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeRaise);
                break;

            case 106:
                // Blazing Door Open (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeOpen);
                break;

            case 107:
                // Blazing Door Close (faster than TURBO!)
                this.DoDoor(line, vldoor_e.blazeClose);
                break;

            case 120:
                // Blazing PlatDownWaitUpStay.
                obs.PEV.DoPlat(line, plattype_e.blazeDWUS, 0);
                break;

            case 126:
                // TELEPORT MonsterONLY.
                if (thing.player == null) {
                    this.Teleport(line, side, thing);
                }
                break;

            case 128:
                // Raise To Nearest Floor
                this.DoFloor(line, floor_e.raiseFloorToNearest);
                break;

            case 129:
                // Raise Floor Turbo
                this.DoFloor(line, floor_e.raiseFloorTurbo);
                break;
        }
    }    
}
