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

import data.mapthing_t;
import data.mobjtype_t;
import doom.DoomMain;
import doom.SourceCode;
import doom.SourceCode.P_Map;
import static doom.SourceCode.P_Map.P_CheckPosition;
import doom.SourceCode.P_Mobj;
import static doom.SourceCode.P_Mobj.P_SpawnMobj;
import doom.player_t;
import doom.thinker_t;
import p.ActionSystem.GoverningRegistry;

public class Actions implements ThinkerList {
    private final GoverningRegistry gov;
    
    public Actions(DoomMain<?, ?> DOOM) {
        this.gov = new GoverningRegistry(DOOM);
    }

    @Override
    public GoverningRegistry obs() {
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

    @SourceCode.Compatible
    @P_Map.C(P_CheckPosition)
    public boolean CheckPosition(mobj_t mo, int x, int y) {
        return gov.CheckPosition(mo, x, y);
    }

    @SourceCode.Exact
    @P_Mobj.C(P_SpawnMobj)
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
}
