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

import static data.Defines.FLOATSPEED;
import static data.Defines.PT_ADDLINES;
import static data.Limits.MAXMOVE;
import static data.Tables.ANG180;
import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import defines.slopetype_t;
import defines.statenum_t;
import doom.SourceCode;
import static doom.SourceCode.P_Map.PTR_SlideTraverse;
import doom.player_t;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static p.ChaseDirections.DI_EAST;
import static p.ChaseDirections.DI_NODIR;
import static p.ChaseDirections.DI_NORTH;
import static p.ChaseDirections.DI_SOUTH;
import static p.ChaseDirections.DI_SOUTHEAST;
import static p.ChaseDirections.DI_WEST;
import static p.ChaseDirections.diags;
import static p.ChaseDirections.opposite;
import static p.ChaseDirections.xspeed;
import static p.ChaseDirections.yspeed;
import static p.MapUtils.AproxDistance;
import static p.mobj_t.MF_CORPSE;
import static p.mobj_t.MF_DROPOFF;
import static p.mobj_t.MF_FLOAT;
import static p.mobj_t.MF_INFLOAT;
import static p.mobj_t.MF_MISSILE;
import static p.mobj_t.MF_NOCLIP;
import static p.mobj_t.MF_SKULLFLY;
import static p.mobj_t.MF_TELEPORT;
import rr.line_t;
import static rr.line_t.ML_TWOSIDED;
import static utils.C2JUtils.eval;

interface ActionsMovement extends ActionsPathTraverse, ActionsClipping, ActionsMoveEvents, ActionsUseEvents {
    ///////////////// MOVEMENT'S ACTIONS ////////////////////////
    /**
     * If "floatok" true, move would be ok if within "tmfloorz - tmceilingz".
     */

    default boolean Move(mobj_t actor) {
        final Actions.Registry obs = obs();
        @SourceCode.fixed_t int tryx, tryy;

        line_t ld;

        // warning: 'catch', 'throw', and 'try'
        // are all C++ reserved words
        boolean try_ok;
        boolean good;

        if (actor.movedir == DI_NODIR) {
            return false;
        }

        if (actor.movedir >= 8) {
            obs.DOOM.doomSystem.Error("Weird actor.movedir!");
        }

        tryx = actor.x + actor.info.speed * xspeed[actor.movedir];
        tryy = actor.y + actor.info.speed * yspeed[actor.movedir];

        try_ok = this.TryMove(actor, tryx, tryy);

        if (!try_ok) {
            // open any specials
            if (eval(actor.flags & MF_FLOAT) && obs.floatok) {
                // must adjust height
                if (actor.z < obs.tmfloorz) {
                    actor.z += FLOATSPEED;
                } else {
                    actor.z -= FLOATSPEED;
                }

                actor.flags |= MF_INFLOAT;
                return true;
            }

            if (obs.numspechit == 0) {
                return false;
            }

            actor.movedir = DI_NODIR;
            good = false;
            while ((obs.numspechit--) > 0) {
                ld = obs.spechit[obs.numspechit];
                // if the special is not a door
                // that can be opened,
                // return false
                if (this.UseSpecialLine(actor, ld, false)) {
                    good = true;
                }
            }
            return good;
        } else {
            actor.flags &= ~MF_INFLOAT;
        }

        if (!eval(actor.flags & MF_FLOAT)) {
            actor.z = actor.floorz;
        }
        return true;
    }

    /**
     * // P_TryMove // Attempt to move to a new position, // crossing special lines unless MF_TELEPORT is set.
     *
     * @param x fixed_t
     * @param y fixed_t
     *
     */
    default boolean TryMove(mobj_t thing, @SourceCode.fixed_t int x, @SourceCode.fixed_t int y) {
        final Actions.Registry obs = obs();
        @SourceCode.fixed_t int oldx, oldy;
        boolean side, oldside; // both were int
        line_t ld;

        obs.floatok = false;
        if (!this.CheckPosition(thing, x, y)) {
            return false;       // solid wall or thing
        }
        if (!eval(thing.flags & MF_NOCLIP)) {
            if (obs.tmceilingz - obs.tmfloorz < thing.height) {
                return false;   // doesn't fit
            }
            obs.floatok = true;

            if (!eval(thing.flags & MF_TELEPORT)
                    && obs.tmceilingz - thing.z < thing.height) {
                return false;   // mobj must lower itself to fit
            }
            if (!eval(thing.flags & MF_TELEPORT)
                    && obs.tmfloorz - thing.z > 24 * FRACUNIT) {
                return false;   // too big a step up
            }
            if (!eval(thing.flags & (MF_DROPOFF | MF_FLOAT))
                    && obs.tmfloorz - obs.tmdropoffz > 24 * FRACUNIT) {
                return false;   // don't stand over a dropoff
            }
        }

        // the move is ok,
        // so link the thing into its new position
        obs.UnsetThingPosition(thing);

        oldx = thing.x;
        oldy = thing.y;
        thing.floorz = obs.tmfloorz;
        thing.ceilingz = obs.tmceilingz;
        thing.x = x;
        thing.y = y;

        obs.DOOM.levelLoader.SetThingPosition(thing);

        // if any special lines were hit, do the effect
        if (!eval(thing.flags & (MF_TELEPORT | MF_NOCLIP))) {
            while (obs.numspechit-- > 0) {
                // see if the line was crossed
                ld = obs.spechit[obs.numspechit];
                side = ld.PointOnLineSide(thing.x, thing.y);
                oldside = ld.PointOnLineSide(oldx, oldy);
                if (side != oldside) {
                    if (ld.special != 0) {
                        this.CrossSpecialLine(ld, oldside ? 1 : 0, thing);
                    }
                }
            }
        }

        return true;
    }

    default void NewChaseDir(mobj_t actor) {
        final Actions.Registry obs = obs();
        @SourceCode.fixed_t int deltax, deltay;

        int tdir;
        int olddir;
        // dirtypes
        int turnaround;

        if (actor.target == null) {
            obs.DOOM.doomSystem.Error("P_NewChaseDir: called with no target");
        }

        olddir = actor.movedir;
        turnaround = opposite[olddir];

        deltax = actor.target.x - actor.x;
        deltay = actor.target.y - actor.y;

        if (deltax > 10 * FRACUNIT) {
            obs.d1 = DI_EAST;
        } else if (deltax < -10 * FRACUNIT) {
            obs.d1 = DI_WEST;
        } else {
            obs.d1 = DI_NODIR;
        }

        if (deltay < -10 * FRACUNIT) {
            obs.d2 = DI_SOUTH;
        } else if (deltay > 10 * FRACUNIT) {
            obs.d2 = DI_NORTH;
        } else {
            obs.d2 = DI_NODIR;
        }

        // try direct route
        if (obs.d1 != DI_NODIR
                && obs.d2 != DI_NODIR) {
            actor.movedir = diags[(eval(deltay < 0) << 1) + eval(deltax > 0)];
            if (actor.movedir != turnaround && this.TryWalk(actor)) {
                return;
            }
        }

        // try other directions
        if (obs.DOOM.random.P_Random() > 200
                || Math.abs(deltay) > Math.abs(deltax)) {
            tdir = obs.d1;
            obs.d1 = obs.d2;
            obs.d2 = tdir;
        }

        if (obs.d1 == turnaround) {
            obs.d1 = DI_NODIR;
        }
        if (obs.d2 == turnaround) {
            obs.d2 = DI_NODIR;
        }

        if (obs.d1 != DI_NODIR) {
            actor.movedir = obs.d1;
            if (this.TryWalk(actor)) {
                // either moved forward or attacked
                return;
            }
        }

        if (obs.d2 != DI_NODIR) {
            actor.movedir = obs.d2;

            if (this.TryWalk(actor)) {
                return;
            }
        }

        // there is no direct path to the player,
        // so pick another direction.
        if (olddir != DI_NODIR) {
            actor.movedir = olddir;

            if (this.TryWalk(actor)) {
                return;
            }
        }

        // randomly determine direction of search
        if (eval(obs.DOOM.random.P_Random() & 1)) {
            for (tdir = DI_EAST;
                    tdir <= DI_SOUTHEAST;
                    tdir++) {
                if (tdir != turnaround) {
                    actor.movedir = tdir;

                    if (this.TryWalk(actor)) {
                        return;
                    }
                }
            }
        } else {
            for (tdir = DI_SOUTHEAST;
                    tdir != (DI_EAST - 1);
                    tdir--) {
                if (tdir != turnaround) {
                    actor.movedir = tdir;

                    if (this.TryWalk(actor)) {
                        return;
                    }
                }
            }
        }

        if (turnaround != DI_NODIR) {
            actor.movedir = turnaround;
            if (this.TryWalk(actor)) {
                return;
            }
        }

        actor.movedir = DI_NODIR;  // can not move
    }

    /**
     * TryWalk Attempts to move actor on in its current (ob.moveangle) direction. If blocked by either a wall or an
     * actor returns FALSE If move is either clear or blocked only by a door, returns TRUE and sets... If a door is in
     * the way, an OpenDoor call is made to start it opening.
     */
    default boolean TryWalk(mobj_t actor) {
        final Actions.Registry obs = obs();
        if (!this.Move(actor)) {
            return false;
        }

        actor.movecount = obs.DOOM.random.P_Random() & 15;
        return true;
    }

    //
    // P_HitSlideLine
    // Adjusts the xmove / ymove
    // so that the next move will slide along the wall.
    //
    default void HitSlideLine(line_t ld) {
        final Actions.Registry obs = obs();
        boolean side;

        // all angles
        long lineangle, moveangle, deltaangle;

        @SourceCode.fixed_t int movelen, newlen;

        if (ld.slopetype == slopetype_t.ST_HORIZONTAL) {
            obs.tmymove = 0;
            return;
        }

        if (ld.slopetype == slopetype_t.ST_VERTICAL) {
            obs.tmxmove = 0;
            return;
        }

        side = ld.PointOnLineSide(obs.slidemo.x, obs.slidemo.y);

        lineangle = obs.DOOM.sceneRenderer.PointToAngle2(0, 0, ld.dx, ld.dy);

        if (side == true) {
            lineangle += ANG180;
        }

        moveangle = obs.DOOM.sceneRenderer.PointToAngle2(0, 0, obs.tmxmove, obs.tmymove);
        deltaangle = (moveangle - lineangle) & BITS32;

        if (deltaangle > ANG180) {
            deltaangle += ANG180;
        }
        //  system.Error ("SlideLine: ang>ANG180");

        //lineangle >>>= ANGLETOFINESHIFT;
        //deltaangle >>>= ANGLETOFINESHIFT;
        movelen = AproxDistance(obs.tmxmove, obs.tmymove);
        newlen = FixedMul(movelen, finecosine(deltaangle));

        obs.tmxmove = FixedMul(newlen, finecosine(lineangle));
        obs.tmymove = FixedMul(newlen, finesine(lineangle));
    }
    ///(FRACUNIT/MAPFRACUNIT);


    //
    // P_SlideMove
    // The momx / momy move is bad, so try to slide
    // along a wall.
    // Find the first line hit, move flush to it,
    // and slide along it
    //
    // This is a kludgy mess.
    //
    default void SlideMove(mobj_t mo) {
        final Actions.Registry obs = obs();
        @SourceCode.fixed_t int leadx, leady, trailx, traily, newx, newy;
        int hitcount;

        obs.slidemo = mo;
        hitcount = 0;

        do {
            if (++hitcount == 3) {
                // goto stairstep
                this.stairstep(mo);
                return;
            }     // don't loop forever

            // trace along the three leading corners
            if (mo.momx > 0) {
                leadx = mo.x + mo.radius;
                trailx = mo.x - mo.radius;
            } else {
                leadx = mo.x - mo.radius;
                trailx = mo.x + mo.radius;
            }

            if (mo.momy > 0) {
                leady = mo.y + mo.radius;
                traily = mo.y - mo.radius;
            } else {
                leady = mo.y - mo.radius;
                traily = mo.y + mo.radius;
            }

            obs.bestslidefrac = FRACUNIT + 1;

            this.PathTraverse(leadx, leady, leadx + mo.momx, leady + mo.momy, PT_ADDLINES, this::SlideTraverse);
            this.PathTraverse(trailx, leady, trailx + mo.momx, leady + mo.momy, PT_ADDLINES, this::SlideTraverse);
            this.PathTraverse(leadx, traily, leadx + mo.momx, traily + mo.momy, PT_ADDLINES, this::SlideTraverse);

            // move up to the wall
            if (obs.bestslidefrac == FRACUNIT + 1) {
                // the move most have hit the middle, so stairstep
                this.stairstep(mo);
                return;
            }     // don't loop forever

            // fudge a bit to make sure it doesn't hit
            obs.bestslidefrac -= Actions.Registry.FUDGE;
            if (obs.bestslidefrac > 0) {
                newx = FixedMul(mo.momx, obs.bestslidefrac);
                newy = FixedMul(mo.momy, obs.bestslidefrac);

                if (!this.TryMove(mo, mo.x + newx, mo.y + newy)) {
                    // goto stairstep
                    this.stairstep(mo);
                    return;
                }     // don't loop forever
            }

            // Now continue along the wall.
            // First calculate remainder.
            obs.bestslidefrac = FRACUNIT - (obs.bestslidefrac + Actions.Registry.FUDGE);

            if (obs.bestslidefrac > FRACUNIT) {
                obs.bestslidefrac = FRACUNIT;
            }

            if (obs.bestslidefrac <= 0) {
                return;
            }

            obs.tmxmove = FixedMul(mo.momx, obs.bestslidefrac);
            obs.tmymove = FixedMul(mo.momy, obs.bestslidefrac);

            this.HitSlideLine(obs.bestslideline); // clip the moves

            mo.momx = obs.tmxmove;
            mo.momy = obs.tmymove;

        } // goto retry
        while (!this.TryMove(mo, mo.x + obs.tmxmove, mo.y + obs.tmymove));
    }

    /**
     * Fugly "goto stairstep" simulation
     *
     * @param mo
     */
    default void stairstep(mobj_t mo) {
        final Actions.Registry obs = obs();
        if (!this.TryMove(mo, mo.x, mo.y + mo.momy)) {
            this.TryMove(mo, mo.x + mo.momx, mo.y);
        }
    }
    //
    // P_XYMovement  
    //


    default void XYMovement(mobj_t mo) {
        final Actions.Registry obs = obs();
        //System.out.println("XYMovement");
        @SourceCode.fixed_t int ptryx, ptryy; // pointers to fixed_t ???
        player_t player;
        @SourceCode.fixed_t int xmove, ymove;

        if ((mo.momx == 0) && (mo.momy == 0)) {
            if ((mo.flags & MF_SKULLFLY) != 0) {
                // the skull slammed into something
                mo.flags &= ~MF_SKULLFLY;
                mo.momx = mo.momy = mo.momz = 0;

                mo.SetMobjState(mo.info.spawnstate);
            }
            return;
        }

        player = mo.player;

        if (mo.momx > MAXMOVE) {
            mo.momx = MAXMOVE;
        } else if (mo.momx < -MAXMOVE) {
            mo.momx = -MAXMOVE;
        }

        if (mo.momy > MAXMOVE) {
            mo.momy = MAXMOVE;
        } else if (mo.momy < -MAXMOVE) {
            mo.momy = -MAXMOVE;
        }

        xmove = mo.momx;
        ymove = mo.momy;

        do {
            if (xmove > MAXMOVE / 2 || ymove > MAXMOVE / 2) {
                ptryx = mo.x + xmove / 2;
                ptryy = mo.y + ymove / 2;
                xmove >>= 1;
                ymove >>= 1;
            } else {
                ptryx = mo.x + xmove;
                ptryy = mo.y + ymove;
                xmove = ymove = 0;
            }

            if (!this.TryMove(mo, ptryx, ptryy)) {
                // blocked move
                if (mo.player != null) {   // try to slide along it
                    this.SlideMove(mo);
                } else if (eval(mo.flags & MF_MISSILE)) {
                    // explode a missile
                    if (obs.ceilingline != null
                            && obs.ceilingline.backsector != null
                            && obs.ceilingline.backsector.ceilingpic == obs.DOOM.textureManager.getSkyFlatNum()) {
                        // Hack to prevent missiles exploding
                        // against the sky.
                        // Does not handle sky floors.
                        obs.RemoveMobj(mo);
                        return;
                    }
                    obs.ExplodeMissile(mo);
                } else {
                    mo.momx = mo.momy = 0;
                }
            }
        } while ((xmove | ymove) != 0);

        // slow down
        if (player != null && eval(player.cheats & player_t.CF_NOMOMENTUM)) {
            // debug option for no sliding at all
            mo.momx = mo.momy = 0;
            return;
        }

        if (eval(mo.flags & (MF_MISSILE | MF_SKULLFLY))) {
            return;     // no friction for missiles ever
        }
        if (mo.z > mo.floorz) {
            return;     // no friction when airborne
        }
        if (eval(mo.flags & MF_CORPSE)) {
            // do not stop sliding
            //  if halfway off a step with some momentum
            if (mo.momx > FRACUNIT / 4
                    || mo.momx < -FRACUNIT / 4
                    || mo.momy > FRACUNIT / 4
                    || mo.momy < -FRACUNIT / 4) {
                if (mo.floorz != mo.subsector.sector.floorheight) {
                    return;
                }
            }
        }

        if (mo.momx > -Actions.Registry.STOPSPEED
                && mo.momx < Actions.Registry.STOPSPEED
                && mo.momy > -Actions.Registry.STOPSPEED
                && mo.momy < Actions.Registry.STOPSPEED
                && (player == null
                || (player.cmd.forwardmove == 0
                && player.cmd.sidemove == 0))) {
            // if in a walking frame, stop moving
            // TODO: we need a way to get state indexed inside of states[], to sim pointer arithmetic.
            // FIX: added an "id" field.
            if (player != null && player.mo.mobj_state.id - statenum_t.S_PLAY_RUN1.ordinal() < 4) {
                player.mo.SetMobjState(statenum_t.S_PLAY);
            }

            mo.momx = 0;
            mo.momy = 0;
        } else {
            mo.momx = FixedMul(mo.momx, Actions.Registry.FRICTION);
            mo.momy = FixedMul(mo.momy, Actions.Registry.FRICTION);
        }
    }
    
    //
    // SLIDE MOVE
    // Allows the player to slide along any angled walls.
    //
    // fixed
    //
    // PTR_SlideTraverse
    //   
    @SourceCode.P_Map.C(PTR_SlideTraverse) default boolean SlideTraverse(intercept_t in) {
        final Actions.Registry obs = obs();
        line_t li;

        if (!in.isaline) {
            obs.DOOM.doomSystem.Error("PTR_SlideTraverse: not a line?");
        }

        li = (line_t) in.d();

        if (!eval(li.flags & ML_TWOSIDED)) {
            if (li.PointOnLineSide(obs.slidemo.x, obs.slidemo.y)) {
                // don't hit the back side
                return true;
            }
            return this.isblocking(in, li);
        }

        // set openrange, opentop, openbottom
        obs.LineOpening(li);

        if ((obs.openrange < obs.slidemo.height)
                || // doesn't fit
                (obs.opentop - obs.slidemo.z < obs.slidemo.height)
                || // mobj is too high
                (obs.openbottom - obs.slidemo.z > 24 * FRACUNIT)) // too big a step up
        {
            if (in.frac < obs.bestslidefrac) {
                obs.secondslidefrac = obs.bestslidefrac;
                obs.secondslideline = obs.bestslideline;
                obs.bestslidefrac = in.frac;
                obs.bestslideline = li;
            }

            return false;   // stop
        } else // this line doesn't block movement
        {
            return true;
        }

    };
}
