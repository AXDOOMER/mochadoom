package p;

import static data.Defines.ITEMQUESIZE;
import static data.Defines.MTF_AMBUSH;
import static data.Defines.ONCEILINGZ;
import static data.Defines.ONFLOORZ;
import static data.Limits.MAXPLAYERS;
import static data.Tables.ANG45;
import static data.info.mobjinfo;
import data.mapthing_t;
import data.mobjtype_t;
import data.sounds;
import doom.CommandVariable;
import doom.thinker_t;
import static m.fixed_t.FRACBITS;
import static p.ActionFunction.ParamType.Mobj;
import static p.ActionFunction.ParamType.Thinker;
import static p.DoorDefines.FASTDARK;
import static p.DoorDefines.SLOWDARK;
import static p.mobj_t.MF_AMBUSH;
import static p.mobj_t.MF_SPAWNCEILING;
import rr.sector_t;
import rr.subsector_t;
import static utils.C2JUtils.eval;

public interface ActionsThinkers<T, V> extends ActionsCeilings<T, V> {
    /**
     * P_SpawnSpecials After the map has been loaded, scan for specials that spawn thinkers
     */
    default void SpawnSpecials() {
        final ActionsRegistry<T, V> obs = obs();
        sector_t sector;

        /*int     episode;

        episode = 1;
        if (W.CheckNumForName("texture2") >= 0)
        episode = 2;
         */
        // See if -TIMER needs to be used.
        obs.SPECS.levelTimer = false;

        if (obs.DOOM.cVarManager.bool(CommandVariable.AVG) && obs.DOOM.deathmatch) {
            obs.SPECS.levelTimer = true;
            obs.SPECS.levelTimeCount = 20 * 60 * 35;
        }

        if (obs.DOOM.deathmatch) {
            obs.DOOM.cVarManager.with(CommandVariable.TIMER, 0, (Integer i) -> {
                obs.SPECS.levelTimer = true;
                obs.SPECS.levelTimeCount = i * 60 * 35;
            });
        }

        //  Init special SECTORs.
        //sector = LL.sectors;
        for (int i = 0; i < obs.DOOM.levelLoader.numsectors; i++) {
            sector = obs.DOOM.levelLoader.sectors[i];
            if (!eval(sector.special)) {
                continue;
            }

            switch (sector.special) {
                case 1:
                    // FLICKERING LIGHTS
                    sector.SpawnLightFlash();
                    break;

                case 2:
                    // STROBE FAST
                    sector.SpawnStrobeFlash(FASTDARK, 0);
                    break;

                case 3:
                    // STROBE SLOW
                    sector.SpawnStrobeFlash(SLOWDARK, 0);
                    break;

                case 4:
                    // STROBE FAST/DEATH SLIME
                    sector.SpawnStrobeFlash(FASTDARK, 0);
                    sector.special = 4;
                    break;

                case 8:
                    // GLOWING LIGHT
                    sector.SpawnGlowingLight();
                    break;
                case 9:
                    // SECRET SECTOR
                    obs.DOOM.totalsecret++;
                    break;

                case 10:
                    // DOOR CLOSE IN 30 SECONDS
                    sector.SpawnDoorCloseIn30();
                    break;

                case 12:
                    // SYNC STROBE SLOW
                    sector.SpawnStrobeFlash(SLOWDARK, 1);
                    break;

                case 13:
                    // SYNC STROBE FAST
                    sector.SpawnStrobeFlash(FASTDARK, 1);
                    break;

                case 14:
                    // DOOR RAISE IN 5 MINUTES
                    sector.SpawnDoorRaiseIn5Mins(i);
                    break;

                case 17:
                    sector.SpawnFireFlicker();
                    break;
            }
        }

        //  Init line EFFECTs
        obs.SPECS.numlinespecials = 0;
        for (int i = 0; i < obs.DOOM.levelLoader.numlines; i++) {
            switch (obs.DOOM.levelLoader.lines[i].special) {
                case 48:
                    // EFFECT FIRSTCOL SCROLL+
                    // Maes 6/4/2012: removed length limit.
                    if (obs.SPECS.numlinespecials == obs.SPECS.linespeciallist.length) {
                        obs.SPECS.resizeLinesSpecialList();
                    }
                    obs.SPECS.linespeciallist[obs.SPECS.numlinespecials] = obs.DOOM.levelLoader.lines[i];
                    obs.SPECS.numlinespecials++;
                    break;
            }
        }

        //  Init other misc stuff
        for (int i = 0; i < this.getMaxCeilings(); i++) {
            this.getActiveCeilings()[i] = null;
        }

        obs.PEV.initActivePlats();

        obs.SW.initButtonList();

        // UNUSED: no horizonal sliders.
        // if (SL!=null) {
        // SL.updateStatus(DM);
        //  SL.P_InitSlidingDoorFrames();
        //}
    }

    /**
     * P_RespawnSpecials
     */
    default void RespawnSpecials() {
        final ActionsRegistry<T, V> obs = obs();
        int x, y, z; // fixed

        subsector_t ss;
        mobj_t mo;
        mapthing_t mthing;

        int i;

        // only respawn items in deathmatch (deathmatch!=2)
        if (!obs.DOOM.altdeath) {
            return; // 
        }
        // nothing left to respawn?
        if (obs.iquehead == obs.iquetail) {
            return;
        }

        // wait at least 30 seconds
        if (obs.DOOM.leveltime - obs.itemrespawntime[obs.iquetail] < 30 * 35) {
            return;
        }

        mthing = obs.itemrespawnque[obs.iquetail];

        x = mthing.x << FRACBITS;
        y = mthing.y << FRACBITS;

        // spawn a teleport fog at the new spot
        ss = obs.DOOM.levelLoader.PointInSubsector(x, y);
        mo = this.SpawnMobj(x, y, ss.sector.floorheight, mobjtype_t.MT_IFOG);
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_itmbk);

        // find which type to spawn
        for (i = 0; i < mobjtype_t.NUMMOBJTYPES.ordinal(); i++) {
            if (mthing.type == mobjinfo[i].doomednum) {
                break;
            }
        }

        // spawn it
        if (eval(mobjinfo[i].flags & MF_SPAWNCEILING)) {
            z = ONCEILINGZ;
        } else {
            z = ONFLOORZ;
        }

        mo = this.SpawnMobj(x, y, z, mobjtype_t.values()[i]);
        mo.spawnpoint = mthing;
        mo.angle = ANG45 * (mthing.angle / 45);

        // pull it from the que
        obs.iquetail = (obs.iquetail + 1) & (ITEMQUESIZE - 1);
    }

    /**
     * P_NightmareRespawn
     */
    default void NightmareRespawn(mobj_t mobj) {
        final ActionsRegistry<T, V> obs = obs();
        int x, y, z; // fixed 
        subsector_t ss;
        mobj_t mo;
        mapthing_t mthing;

        x = mobj.spawnpoint.x << FRACBITS;
        y = mobj.spawnpoint.y << FRACBITS;

        // somthing is occupying it's position?
        if (!this.CheckPosition(mobj, x, y)) {
            return; // no respwan
        }
        // spawn a teleport fog at old spot
        // because of removal of the body?
        mo = this.SpawnMobj(mobj.x,
                mobj.y,
                mobj.subsector.sector.floorheight, mobjtype_t.MT_TFOG);
        // initiate teleport sound
        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_telept);

        // spawn a teleport fog at the new spot
        ss = obs.DOOM.levelLoader.PointInSubsector(x, y);

        mo = this.SpawnMobj(x, y, ss.sector.floorheight, mobjtype_t.MT_TFOG);

        obs.DOOM.doomSound.StartSound(mo, sounds.sfxenum_t.sfx_telept);

        // spawn the new monster
        mthing = mobj.spawnpoint;

        // spawn it
        if (eval(mobj.info.flags & MF_SPAWNCEILING)) {
            z = ONCEILINGZ;
        } else {
            z = ONFLOORZ;
        }

        // inherit attributes from deceased one
        mo = this.SpawnMobj(x, y, z, mobj.type);
        mo.spawnpoint = mobj.spawnpoint;
        mo.angle = ANG45 * (mthing.angle / 45);

        if (eval(mthing.options & MTF_AMBUSH)) {
            mo.flags |= MF_AMBUSH;
        }

        mo.reactiontime = 18;

        // remove the old monster,
        obs.RemoveMobj(mobj);
    }

    //
    // P_RunThinkers
    //
    default void RunThinkers() {
        final ActionsRegistry<T, V> obs = obs();
        thinker_t thinker;

        thinker = obs.thinkercap.next;
        while (thinker != obs.thinkercap) {
            if (thinker.thinkerFunction == ActionFunction.NOP) {
                // time to remove it
                thinker.next.prev = thinker.prev;
                thinker.prev.next = thinker.next;
                // Z_Free (currentthinker);
            } else {
                if (thinker.thinkerFunction.isParamType(Mobj)) {
                    thinker.thinkerFunction.callMobjFun(obs.DOOM.actionFunctions, (mobj_t) thinker);
                } else if (thinker.thinkerFunction.isParamType(Thinker)) {
                    thinker.thinkerFunction.callThinkerFun(obs.DOOM.actionFunctions, thinker);
                }
            }
            thinker = thinker.next;
        }
    }
    
    //
    //P_Ticker
    //
    default void Ticker() {
        final ActionsRegistry<T, V> obs = obs();
        int i;

        // run the tic
        if (obs.DOOM.paused) {
            return;
        }

        // pause if in menu and at least one tic has been run
        if (!obs.DOOM.netgame
                && obs.DOOM.menuactive
                && !obs.DOOM.demoplayback
                && obs.DOOM.players[obs.DOOM.consoleplayer].viewz != 1) {
            return;
        }

        for (i = 0; i < MAXPLAYERS; i++) {
            if (obs.DOOM.playeringame[i]) {
                obs.DOOM.players[i].PlayerThink();
            }
        }

        this.RunThinkers();
        obs.SPECS.UpdateSpecials(); // In specials. Merge?
        this.RespawnSpecials();

        // for par times
        obs.DOOM.leveltime++;
    }
}
