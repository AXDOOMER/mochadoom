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

import data.Limits;
import data.Tables;
import data.mapthing_t;
import data.mobjtype_t;
import doom.DoomMain;
import doom.SourceCode;
import doom.player_t;
import doom.thinker_t;
import m.fixed_t;
import p.ActionSystem.Governor;
import p.ActionSystem.Observer;
import rr.line_t;

public class Actions implements Observer<Actions.Registry>, ThinkerList {
    private final Registry gov;
    
    public Actions(DoomMain DOOM) {
        this.gov = new Registry(DOOM);
    }

    @Override
    public Registry obs() {
        return gov;
    }

    public void RemoveMobj(mobj_t mobj_t) {
        gov.RemoveMobj(mobj_t);
    }

    @Override
    public void InitThinkers() {
        gov.InitThinkers();
    }
    
    void ClearRespawnQueue() {
        // clear special respawning que
        gov.iquehead = gov.iquetail = 0;
    }

    @Override
    public thinker_t getThinkerCap() {
        return gov.getThinkerCap();
    }

    public void SpawnSpecials() {
        gov.SpawnSpecials();
    }

    @Override
    public void AddThinker(thinker_t thinker) {
        gov.AddThinker(thinker);
    }

    @Override
    public void RemoveThinker(thinker_t thinker) {
        gov.AddThinker(thinker);
    }

    @Override
    public thinker_t getRandomThinker() {
        return gov.getRandomThinker();
    }
    
    public void ClearPlatsBeforeLoading() {
        gov.ClearPlatsBeforeLoading();
    }
    
    public void AddActivePlat(plat_t plat) {
        gov.AddActivePlat(plat);
    }

    mobj_t SpawnMapThing(mapthing_t mt) {
        return gov.SpawnMapThing(mt);
    }

    public int getMaxCeilings() {
        return gov.getMaxCeilings();
    }

    public ceiling_t[] getActiveCeilings() {
        return gov.getActiveCeilings();
    }

    public void ClearCeilingsBeforeLoading() {
        gov.ClearCeilingsBeforeLoading();
    }

    public void AddActiveCeiling(ceiling_t ceiling) {
        gov.AddActiveCeiling(ceiling);
    }

    public void Ticker() {
        gov.Ticker();
    }

    public boolean CheckPosition(mobj_t mo, int x, int y) {
        return gov.CheckPosition(mo, x, y);
    }

    public mobj_t SpawnMobj(int i, int i0, int floorheight, mobjtype_t mobjtype_t) {
        return gov.SpawnMobj(i, i0, floorheight, mobjtype_t);
    }

    public void SpawnPlayer(mapthing_t deathmatchstart) {
        gov.SpawnPlayer(deathmatchstart);
    }

    public void DamageMobj(mobj_t target, mobj_t inflictor, mobj_t source, int damage) {
        gov.DamageMobj(target, inflictor, source, damage);
    }

    public void UseLines(player_t player) {
        gov.UseLines(player);
    }

    public void Init() {
        gov.Init();
    }

    public static final class Registry extends UnifiedGameMap implements Governor {

        static final long FATSPREAD = Tables.ANG90 / 8;
        static final int TRACEANGLE = 201326592;
        static final int SKULLSPEED = 20 * fixed_t.MAPFRACUNIT;
        // plasma cells for a bfg attack
        // IDEA: make action functions partially parametrizable?
        static final int BFGCELLS = 40;
        static final int FUDGE = 2048; ///(FRACUNIT/MAPFRACUNIT);
        //
        // P_XYMovement
        //
        static final int STOPSPEED = 4096;
        static final int FRICTION = 59392;
        //
        // FLOORS
        //
        static final int FLOORSPEED = fixed_t.MAPFRACUNIT;
        //
        // CEILINGS
        //
        ceiling_t[] activeceilings = new ceiling_t[Limits.MAXCEILINGS];
        //
        // P_BulletSlope
        // Sets a slope so a near miss is at aproximately
        // the height of the intended target
        //
        int bulletslope;
        //dirtype
        int d1;
        int d2;
        ///////////////// MOVEMENT'S ACTIONS ////////////////////////
        @SourceCode.fixed_t
        int[] tmbbox = new int[4];
        mobj_t tmthing;
        long tmflags;
        @SourceCode.fixed_t
        int tmx;
        @SourceCode.fixed_t
        int tmy;
        /**
         * If "floatok" true, move would be ok if within "tmfloorz - tmceilingz".
         */
        public boolean floatok;
        @SourceCode.fixed_t
        public int tmfloorz;
        @SourceCode.fixed_t
        public int tmceilingz;
        @SourceCode.fixed_t
        public int tmdropoffz;
        // keep track of the line that lowers the ceiling,
        // so missiles don't explode against sky hack walls
        public line_t ceilingline;
        public line_t[] spechit = new line_t[Limits.MAXSPECIALCROSS];
        public int numspechit;
        ////////////////// PTR Traverse Interception Functions ///////////////////////
        mobj_t shootthing;
        @SourceCode.fixed_t
        int attackrange;
        // Height if not aiming up or down
        // ???: use slope for monsters?
        @SourceCode.fixed_t
        int shootz;
        int la_damage;
        @SourceCode.fixed_t
        int aimslope;
        //
        // SLIDE MOVE
        // Allows the player to slide along any angled walls.
        //
        mobj_t slidemo;
        int bestslidefrac; // fixed
        int secondslidefrac;
        line_t bestslideline;
        line_t secondslideline;
        @SourceCode.fixed_t
        int tmxmove;
        @SourceCode.fixed_t
        int tmymove;
        //
        // P_LineAttack
        //
        /**
         * who got hit (or NULL)
         */
        public mobj_t linetarget;
        //
        // USE LINES
        //
        mobj_t usething;
        //
        // RADIUS ATTACK
        //
        mobj_t bombsource;
        mobj_t bombspot;
        int bombdamage;
        //
        // SECTOR HEIGHT CHANGING
        // After modifying a sectors floor or ceiling height,
        // call this routine to adjust the positions
        // of all things that touch the sector.
        //
        // If anything doesn't fit anymore, true will be returned.
        // If crunch is true, they will take damage
        //  as they are being crushed.
        // If Crunch is false, you should set the sector height back
        //  the way it was and call P_ChangeSector again
        //  to undo the changes.
        //
        boolean crushchange;
        boolean nofit;
        ///////////////////// PIT AND PTR FUNCTIONS //////////////////
        /**
         * PIT_VileCheck Detect a corpse that could be raised.
         */
        mobj_t vileCorpseHit;
        mobj_t vileObj;
        int vileTryX;
        int vileTryY;
        // Brain status
        mobj_t[] braintargets = new mobj_t[Limits.NUMBRAINTARGETS];
        int numbraintargets;
        int braintargeton;
        int easy = 0;

        public Registry(DoomMain DOOM) {
            super(DOOM);
        }

        @Override
        public Registry obs() {
            return this;
        }
    }
}
