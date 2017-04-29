package p;

import static data.Defines.PT_ADDLINES;
import static data.Defines.USERANGE;
import data.Tables;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import data.sounds;
import doom.SourceCode;
import static doom.SourceCode.P_Map.PTR_UseTraverse;
import doom.player_t;
import static m.fixed_t.FRACBITS;
import rr.line_t;
import static rr.line_t.ML_SECRET;
import static utils.C2JUtils.eval;

public interface ActionsUseEvents<T, V> extends ActionsPathTraverse<T, V>, ActionsCeilings<T, V>, ActionsFloors<T, V>, ActionsDoors<T, V>, ActionsTeleportation<T, V> {
    /**
     * P_UseSpecialLine Called when a thing uses a special line. Only the front sides of lines are usable.
     */
    default boolean UseSpecialLine(mobj_t thing, line_t line, boolean side) {
        final ActionsRegistry<T, V> obs = obs();

        // Err...
        // Use the back sides of VERY SPECIAL lines...
        if (side) {
            switch (line.special) {
                case 124:
                    // Sliding door open&close
                    // SL.EV_SlidingDoor(line, thing);
                    break;

                default:
                    return false;
                //break;
            }
        }

        // Switches that other things can activate.
        //_D_: little bug fixed here, see linuxdoom source
        if (thing.player ==/*!=*/ null) {
            // never open secret doors
            if (eval(line.flags & ML_SECRET)) {
                return false;
            }

            switch (line.special) {
                case 1:   // MANUAL DOOR RAISE
                case 32:  // MANUAL BLUE
                case 33:  // MANUAL RED
                case 34:  // MANUAL YELLOW
                    break;

                default:
                    return false;
                //break;
            }
        }

        // do something  
        switch (line.special) {
            // MANUALS
            case 1:      // Vertical Door
            case 26:     // Blue Door/Locked
            case 27:     // Yellow Door /Locked
            case 28:     // Red Door /Locked

            case 31:     // Manual door open
            case 32:     // Blue locked door open
            case 33:     // Red locked door open
            case 34:     // Yellow locked door open

            case 117:        // Blazing door raise
            case 118:        // Blazing door open
                this.VerticalDoor(line, thing);
                break;

            //UNUSED - Door Slide Open&Close
            case 124:
                // NOTE: clashes with secret level exit.
                //SL.EV_SlidingDoor (line, thing);
                break;

            // SWITCHES
            case 7:
                // Build Stairs
                if (this.BuildStairs(line, stair_e.build8)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 9:
                // Change Donut
                if (this.DoDonut(line)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 11:
                // Exit level
                obs.SW.ChangeSwitchTexture(line, false);
                obs.DOOM.ExitLevel();
                break;

            case 14:
                // Raise Floor 32 and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseAndChange, 32)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 15:
                // Raise Floor 24 and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseAndChange, 24)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 18:
                // Raise Floor to next highest floor
                if (this.DoFloor(line, floor_e.raiseFloorToNearest)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 20:
                // Raise Plat next highest floor and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseToNearestAndChange, 0)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 21:
                // PlatDownWaitUpStay
                if (obs.PEV.DoPlat(line, plattype_e.downWaitUpStay, 0)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 23:
                // Lower Floor to Lowest
                if (this.DoFloor(line, floor_e.lowerFloorToLowest)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 29:
                // Raise Door
                if (this.DoDoor(line, vldoor_e.normal)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 41:
                // Lower Ceiling to Floor
                if (this.DoCeiling(line, ceiling_e.lowerToFloor)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 71:
                // Turbo Lower Floor
                if (this.DoFloor(line, floor_e.turboLower)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 49:
                // Ceiling Crush And Raise
                if (this.DoCeiling(line, ceiling_e.crushAndRaise)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 50:
                // Close Door
                if (this.DoDoor(line, vldoor_e.close)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 51:
                // Secret EXIT
                obs.SW.ChangeSwitchTexture(line, false);
                obs.DOOM.SecretExitLevel();
                break;

            case 55:
                // Raise Floor Crush
                if (this.DoFloor(line, floor_e.raiseFloorCrush)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 101:
                // Raise Floor
                if (this.DoFloor(line, floor_e.raiseFloor)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 102:
                // Lower Floor to Surrounding floor height
                if (this.DoFloor(line, floor_e.lowerFloor)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 103:
                // Open Door
                if (this.DoDoor(line, vldoor_e.open)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 111:
                // Blazing Door Raise (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeRaise)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 112:
                // Blazing Door Open (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeOpen)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 113:
                // Blazing Door Close (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeClose)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 122:
                // Blazing PlatDownWaitUpStay
                if (obs.PEV.DoPlat(line, plattype_e.blazeDWUS, 0)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 127:
                // Build Stairs Turbo 16
                if (this.BuildStairs(line, stair_e.turbo16)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 131:
                // Raise Floor Turbo
                if (this.DoFloor(line, floor_e.raiseFloorTurbo)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 133:
            // BlzOpenDoor BLUE
            case 135:
            // BlzOpenDoor RED
            case 137:
                // BlzOpenDoor YELLOW
                if (this.DoLockedDoor(line, vldoor_e.blazeOpen, thing)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            case 140:
                // Raise Floor 512
                if (this.DoFloor(line, floor_e.raiseFloor512)) {
                    obs.SW.ChangeSwitchTexture(line, false);
                }
                break;

            // BUTTONS
            case 42:
                // Close Door
                if (this.DoDoor(line, vldoor_e.close)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 43:
                // Lower Ceiling to Floor
                if (this.DoCeiling(line, ceiling_e.lowerToFloor)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 45:
                // Lower Floor to Surrounding floor height
                if (this.DoFloor(line, floor_e.lowerFloor)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 60:
                // Lower Floor to Lowest
                if (this.DoFloor(line, floor_e.lowerFloorToLowest)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 61:
                // Open Door
                if (this.DoDoor(line, vldoor_e.open)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 62:
                // PlatDownWaitUpStay
                if (obs.PEV.DoPlat(line, plattype_e.downWaitUpStay, 1)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 63:
                // Raise Door
                if (this.DoDoor(line, vldoor_e.normal)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 64:
                // Raise Floor to ceiling
                if (this.DoFloor(line, floor_e.raiseFloor)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 66:
                // Raise Floor 24 and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseAndChange, 24)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 67:
                // Raise Floor 32 and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseAndChange, 32)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 65:
                // Raise Floor Crush
                if (this.DoFloor(line, floor_e.raiseFloorCrush)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 68:
                // Raise Plat to next highest floor and change texture
                if (obs.PEV.DoPlat(line, plattype_e.raiseToNearestAndChange, 0)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 69:
                // Raise Floor to next highest floor
                if (this.DoFloor(line, floor_e.raiseFloorToNearest)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 70:
                // Turbo Lower Floor
                if (this.DoFloor(line, floor_e.turboLower)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 114:
                // Blazing Door Raise (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeRaise)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 115:
                // Blazing Door Open (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeOpen)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 116:
                // Blazing Door Close (faster than TURBO!)
                if (this.DoDoor(line, vldoor_e.blazeClose)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 123:
                // Blazing PlatDownWaitUpStay
                if (obs.PEV.DoPlat(line, plattype_e.blazeDWUS, 0)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 132:
                // Raise Floor Turbo
                if (this.DoFloor(line, floor_e.raiseFloorTurbo)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 99:
            // BlzOpenDoor BLUE
            case 134:
            // BlzOpenDoor RED
            case 136:
                // BlzOpenDoor YELLOW
                if (this.DoLockedDoor(line, vldoor_e.blazeOpen, thing)) {
                    obs.SW.ChangeSwitchTexture(line, true);
                }
                break;

            case 138:
                // Light Turn On
                obs.LEV.LightTurnOn(line, 255);
                obs.SW.ChangeSwitchTexture(line, true);
                break;

            case 139:
                // Light Turn Off
                obs.LEV.LightTurnOn(line, 35);
                obs.SW.ChangeSwitchTexture(line, true);
                break;

        }

        return true;
    }

    /**
     * P_UseLines Looks for special lines in front of the player to activate.
     */
    default void UseLines(player_t player) {
        final ActionsRegistry<T, V> obs = obs();
        int angle;
        int x1, y1, x2, y2;
        //System.out.println("Uselines");
        obs.usething = player.mo;

        // Normally this shouldn't cause problems?
        angle = Tables.toBAMIndex(player.mo.angle);

        x1 = player.mo.x;
        y1 = player.mo.y;
        x2 = x1 + (USERANGE >> FRACBITS) * finecosine[angle];
        y2 = y1 + (USERANGE >> FRACBITS) * finesine[angle];

        this.PathTraverse(x1, y1, x2, y2, PT_ADDLINES, this::UseTraverse);
    }
    
    //
    // USE LINES
    //
    @SourceCode.P_Map.C(PTR_UseTraverse) default boolean UseTraverse(intercept_t in) {
        final ActionsRegistry<T, V> obs = obs();
        boolean side;
        // FIXME: some sanity check here?
        line_t line = (line_t) in.d();

        if (line.special == 0) {
            obs.LineOpening(line);
            if (obs.openrange <= 0) {
                obs.DOOM.doomSound.StartSound(obs.usething, sounds.sfxenum_t.sfx_noway);

                // can't use through a wall
                return false;
            }
            // not a special line, but keep checking
            return true;
        }

        side = false;
        if (line.PointOnLineSide(obs.usething.x, obs.usething.y)) {
            side = true;
        }

        //  return false;       // don't use back side
        this.UseSpecialLine(obs.usething, line, side);

        // can't use for than one special line in a row
        return false;
    };

}
