package p;

import static data.Defines.BT_ATTACK;
import static data.Defines.MELEERANGE;
import static data.Defines.MISSILERANGE;
import static data.Defines.PST_DEAD;
import static data.Defines.pw_strength;
import static data.Limits.MAXPLAYERS;
import static data.Limits.MAXRADIUS;
import static data.Limits.MAXSKULLS;
import static data.Limits.NUMBRAINTARGETS;
import data.Tables;
import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG45;
import static data.Tables.ANG90;
import static data.Tables.BITS32;
import static data.Tables.FINEANGLES;
import static data.Tables.FINEMASK;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.info.mobjinfo;
import static data.info.states;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.sounds.sfxenum_t;
import defines.skill_t;
import defines.statenum_t;
import doom.DoomMain;
import static doom.items.weaponinfo;
import doom.player_t;
import static doom.player_t.LOWERSPEED;
import static doom.player_t.RAISESPEED;
import static doom.player_t.WEAPONBOTTOM;
import static doom.player_t.WEAPONTOP;
import static doom.player_t.ps_flash;
import static doom.player_t.ps_weapon;
import doom.thinker_t;
import doom.weapontype_t;
import java.util.EnumMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static m.fixed_t.MAPFRACUNIT;
import static p.ActionFunction.*;
import p.ActionFunction.Observer;
import static p.ChaseDirections.DI_NODIR;
import static p.ChaseDirections.xspeed;
import static p.ChaseDirections.yspeed;
import static p.MapUtils.AproxDistance;
import static p.mobj_t.MF_AMBUSH;
import static p.mobj_t.MF_COUNTKILL;
import static p.mobj_t.MF_JUSTATTACKED;
import static p.mobj_t.MF_SHADOW;
import static p.mobj_t.MF_SHOOTABLE;
import static p.mobj_t.MF_SKULLFLY;
import static p.mobj_t.MF_SOLID;
import rr.line_t;
import static rr.line_t.ML_BLOCKING;
import static utils.C2JUtils.eval;

public class ActionFunctions<T, V> implements Observer {

    private final EnumMap<ActionFunction, Consumer<mobj_t>> actionf_p1 = new EnumMap<>(ActionFunction.class);
    private final EnumMap<ActionFunction, TypedAction<? extends thinker_t>> actionf_v = new EnumMap<>(ActionFunction.class);
    private final EnumMap<ActionFunction, BiConsumer<player_t, pspdef_t>> actionf_p2 = new EnumMap<>(ActionFunction.class);

    private static final long FATSPREAD = (ANG90 / 8);
    private static final int TRACEANGLE = 0xc000000;
    private static final int SKULLSPEED = (20 * MAPFRACUNIT);

    // Brain status
    private mobj_t[] braintargets = new mobj_t[NUMBRAINTARGETS];
    private int numbraintargets;
    private int braintargeton;

    private int easy = 0;

    // plasma cells for a bfg attack
    // IDEA: make action functions partially parametrizable?
    private static final int BFGCELLS = 40;

    public ActionFunctions(final DoomMain<T, V> DOOM) {
        /**
         * A_Chase
         * Actor has a melee attack,
         * so it tries to close as fast as possible
         */
        actionf_p1.put(A_Chase, (mobj_t actor) -> {
            int delta;
            boolean nomissile = false; // for the fugly goto

            if (actor.reactiontime != 0) {
                actor.reactiontime--;
            }

            // modify target threshold
            if (actor.threshold != 0) {
                if (actor.target == null || actor.target.health <= 0) {
                    actor.threshold = 0;
                } else {
                    actor.threshold--;
                }
            }

            // turn towards movement direction if not there yet
            if (actor.movedir < 8) {
                actor.angle &= (7 << 29);
                actor.angle &= BITS32;
                // Nice problem, here!
                delta = (int) (actor.angle - (actor.movedir << 29));

                if (delta > 0) {
                    actor.angle -= ANG45;
                } else if (delta < 0) {
                    actor.angle += ANG45;
                }

                actor.angle &= BITS32;
            }

            if (actor.target == null || !eval(actor.target.flags & MF_SHOOTABLE)) {
                // look for a new target
                if (DOOM.actions.EN.LookForPlayers(actor, true)) {
                    return;     // got a new target
                }
                actor.SetMobjState(actor.info.spawnstate);
                return;
            }

            // do not attack twice in a row
            if (eval(actor.flags & MF_JUSTATTACKED)) {
                actor.flags &= ~MF_JUSTATTACKED;
                if (DOOM.gameskill != skill_t.sk_nightmare && !DOOM.fastparm) {
                    DOOM.actions.NewChaseDir(actor);
                }
                return;
            }

            // check for melee attack
            if (actor.info.meleestate != statenum_t.S_NULL && DOOM.actions.EN.CheckMeleeRange(actor)) {
                if (actor.info.attacksound != null) {
                    DOOM.doomSound.StartSound(actor, actor.info.attacksound);
                }
                actor.SetMobjState(actor.info.meleestate);
                return;
            }

            // check for missile attack
            if (actor.info.missilestate != statenum_t.S_NULL) { //_D_: this caused a bug where Demon for example were disappearing
                // Assume that a missile attack is possible
                if (DOOM.gameskill.ordinal() < skill_t.sk_nightmare.ordinal()
                    && !DOOM.fastparm && actor.movecount != 0) {
                    // Uhm....no.
                    nomissile = true;
                } else if (!DOOM.actions.EN.CheckMissileRange(actor)) {
                    nomissile = true; // Out of range
                }
                if (!nomissile) {
                    // Perform the attack
                    actor.SetMobjState(actor.info.missilestate);
                    actor.flags |= MF_JUSTATTACKED;
                    return;
                }
            }

            // This should be executed always, if not averted by returns.
            // possibly choose another target
            if (DOOM.netgame && actor.threshold == 0 && !DOOM.actions.EN.CheckSight(actor, actor.target)) {
                if (DOOM.actions.EN.LookForPlayers(actor, true)) {
                    return; // got a new target
                }
            }

            // chase towards player
            if (--actor.movecount < 0 || !DOOM.actions.Move(actor)) {
                DOOM.actions.NewChaseDir(actor);
            }

            // make active sound
            if (actor.info.activesound != null && DOOM.random.P_Random() < 3) {
                DOOM.doomSound.StartSound(actor, actor.info.activesound);
            }
        });

        actionf_p1.put(A_Fire, (mobj_t actor) -> {
            mobj_t dest;
            //long    an;

            dest = actor.tracer;
            if (dest == null) {
                return;
            }

            // don't move it if the vile lost sight
            if (!DOOM.actions.EN.CheckSight(actor.target, dest)) {
                return;
            }

            // an = dest.angle >>> ANGLETOFINESHIFT;
            DOOM.actions.UnsetThingPosition(actor);
            actor.x = dest.x + FixedMul(24 * FRACUNIT, finecosine(dest.angle));
            actor.y = dest.y + FixedMul(24 * FRACUNIT, finesine(dest.angle));
            actor.z = dest.z;
            DOOM.levelLoader.SetThingPosition(actor);
        });

        actionf_p1.put(A_Fall, (mobj_t actor) -> {
            // actor is on ground, it can be walked over
            actor.flags &= ~MF_SOLID;

            // So change this if corpse objects
            // are meant to be obstacles.
        });

        /**
         * Causes object to move and perform DOOM.actions.
         * Can only be called through the Actions dispatcher.
         *
         * @param mobj
         */
        //
        //P_MobjThinker
        //
        actionf_p1.put(P_MobjThinker, (mobj_t mobj) -> {
            // momentum movement
            if (mobj.momx != 0 || mobj.momy != 0
                || (eval(mobj.flags & MF_SKULLFLY))) {
                DOOM.actions.XYMovement(mobj);

                // FIXME: decent NOP/NULL/Nil function pointer please.
                if (mobj.thinkerFunction == ActionFunction.NOP) {
                    return; // mobj was removed
                }
            }
            if ((mobj.z != mobj.floorz) || mobj.momz != 0) {
                mobj.ZMovement();

                // FIXME: decent NOP/NULL/Nil function pointer please.
                if (mobj.thinkerFunction == ActionFunction.NOP) {
                    return; // mobj was removed
                }
            }

            // cycle through states,
            // calling action functions at transitions
            if (mobj.mobj_tics != -1) {
                mobj.mobj_tics--;

                // you can cycle through multiple states in a tic
                if (!eval(mobj.mobj_tics)) {
                    if (!mobj.SetMobjState(mobj.mobj_state.nextstate)) {
                        // freed itself
                    }
                }
            } else {
                // check for nightmare respawn
                if (!eval(mobj.flags & MF_COUNTKILL)) {
                    return;
                }

                if (!DOOM.respawnmonsters) {
                    return;
                }

                mobj.movecount++;

                if (mobj.movecount < 12 * 35) {
                    return;
                }

                if (eval(DOOM.leveltime & 31)) {
                    return;
                }

                if (DOOM.random.P_Random() > 4) {
                    return;
                }

                DOOM.actions.NightmareRespawn(mobj);
            }
        });

        actionf_v.put(T_FireFlicker, (TypedAction<fireflicker_t>) fireflicker_t::FireFlicker);
        actionf_v.put(T_LightFlash, (TypedAction<lightflash_t>) lightflash_t::LightFlash);
        actionf_v.put(T_StrobeFlash, (TypedAction<strobe_t>) strobe_t::StrobeFlash);
        actionf_v.put(T_Glow, (TypedAction<glow_t>) glow_t::Glow);
        actionf_v.put(T_MoveCeiling, (TypedAction<ceiling_t>) DOOM.actions::MoveCeiling);
        actionf_v.put(T_MoveFloor, (TypedAction<floormove_t>) DOOM.actions::MoveFloor);
        actionf_v.put(T_VerticalDoor, (TypedAction<vldoor_t>) DOOM.actions::VerticalDoor);
        actionf_v.put(T_SlidingDoor, (slidedoor_t door) -> {
            switch (door.status) {
                case sd_opening:
                    if (door.timer-- == 0) {
                        if (++door.frame == SlideDoor.SNUMFRAMES) {
                            // IF DOOR IS DONE OPENING...
                            DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = 0;
                            DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = 0;
                            door.line.flags &= ML_BLOCKING ^ 0xff;

                            if (door.type == sdt_e.sdt_openOnly) {
                                door.frontsector.specialdata = null;
                                DOOM.actions.RemoveThinker(door);
                                break;
                            }

                            door.timer = SlideDoor.SDOORWAIT;
                            door.status = sd_e.sd_waiting;
                        } else {
                            // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                            door.timer = SlideDoor.SWAITTICS;

                            DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = (short) DOOM.actions.SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
                            DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = (short) DOOM.actions.SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
                        }
                    }
                    break;

                case sd_waiting:
                    // IF DOOR IS DONE WAITING...
                    if (door.timer-- == 0) {
                        // CAN DOOR CLOSE?
                        if (door.frontsector.thinglist != null
                            || door.backsector.thinglist != null) {
                            door.timer = SlideDoor.SDOORWAIT;
                            break;
                        }

                        // door.frame = SNUMFRAMES-1;
                        door.status = sd_e.sd_closing;
                        door.timer = SlideDoor.SWAITTICS;
                    }
                    break;

                case sd_closing:
                    if (door.timer-- == 0) {
                        if (--door.frame < 0) {
                            // IF DOOR IS DONE CLOSING...
                            door.line.flags |= ML_BLOCKING;
                            door.frontsector.specialdata = null;
                            DOOM.actions.RemoveThinker(door);
                            break;
                        } else {
                            // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                            door.timer = SlideDoor.SWAITTICS;

                            DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = (short) DOOM.actions.SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
                            DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = (short) DOOM.actions.SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
                        }
                    }
                    break;
            }
        });

        actionf_v.put(T_PlatRaise, (TypedAction<plat_t>) DOOM.actions::PlatRaise);

        //
        // A_FaceTarget
        //
        actionf_p1.put(A_FaceTarget, (mobj_t actor) -> {
            if (actor.target == null) {
                return;
            }

            actor.flags &= ~MF_AMBUSH;

            actor.angle = DOOM.sceneRenderer.PointToAngle2(actor.x,
                actor.y,
                actor.target.x,
                actor.target.y) & BITS32;

            if (eval(actor.target.flags & MF_SHADOW)) {
                actor.angle += (DOOM.random.P_Random() - DOOM.random.P_Random()) << 21;
            }
            actor.angle &= BITS32;
        });

        //
        // A_PosAttack
        //
        actionf_p1.put(A_PosAttack, (mobj_t actor) -> {
            int angle;
            int damage;
            int slope;

            if (actor.target == null) {
                return;
            }
            actionf_p1.get(A_FaceTarget).accept(actor);
            angle = (int) actor.angle;
            slope = DOOM.actions.AimLineAttack(actor, angle, MISSILERANGE);

            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_pistol);
            angle += (DOOM.random.P_Random() - DOOM.random.P_Random()) << 20;
            damage = ((DOOM.random.P_Random() % 5) + 1) * 3;
            DOOM.actions.LineAttack(actor, angle, MISSILERANGE, slope, damage);
        });

        actionf_p1.put(A_SPosAttack, (mobj_t actor) -> {
            int i;
            long angle;
            long bangle;
            int damage;
            int slope;

            if (actor.target == null) {
                return;
            }

            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_shotgn);
            actionf_p1.get(A_FaceTarget).accept(actor);
            bangle = actor.angle;
            slope = DOOM.actions.AimLineAttack(actor, bangle, MISSILERANGE);

            for (i = 0; i < 3; i++) {
                angle = bangle + ((DOOM.random.P_Random() - DOOM.random.P_Random()) << 20);
                damage = ((DOOM.random.P_Random() % 5) + 1) * 3;
                DOOM.actions.LineAttack(actor, angle, MISSILERANGE, slope, damage);
            }
        });

        actionf_p1.put(A_CPosAttack, (mobj_t actor) -> {
            long angle;
            long bangle;
            int damage;
            int slope;

            if (actor.target == null) {
                return;
            }

            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_shotgn);
            actionf_p1.get(A_FaceTarget).accept(actor);
            bangle = actor.angle;
            slope = DOOM.actions.AimLineAttack(actor, bangle, MISSILERANGE);

            angle = bangle + ((DOOM.random.P_Random() - DOOM.random.P_Random()) << 20);
            damage = ((DOOM.random.P_Random() % 5) + 1) * 3;
            DOOM.actions.LineAttack(actor, angle, MISSILERANGE, slope, damage);
        });

        actionf_p1.put(A_CPosRefire, (mobj_t actor) -> {
            // keep firing unless target got out of sight
            actionf_p1.get(A_FaceTarget).accept(actor);

            if (DOOM.random.P_Random() < 40) {
                return;
            }

            if (actor.target == null
                || actor.target.health <= 0
                || !DOOM.actions.EN.CheckSight(actor, actor.target)) {
                actor.SetMobjState(actor.info.seestate);
            }
        });

        actionf_p1.put(A_SpidRefire, (mobj_t actor) -> {
            // keep firing unless target got out of sight
            actionf_p1.get(A_FaceTarget).accept(actor);

            if (DOOM.random.P_Random() < 10) {
                return;
            }

            if (actor.target == null
                || actor.target.health <= 0
                || !DOOM.actions.EN.CheckSight(actor, actor.target)) {
                actor.SetMobjState(actor.info.seestate);
            }
        });

        actionf_p1.put(A_BspiAttack, (mobj_t actor) -> {
            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);

            // launch a missile
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_ARACHPLAZ);
        });

        //
        // A_TroopAttack
        //
        actionf_p1.put(A_TroopAttack, (mobj_t actor) -> {
            int damage;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            if (DOOM.actions.EN.CheckMeleeRange(actor)) {
                DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_claw);
                damage = (DOOM.random.P_Random() % 8 + 1) * 3;
                DOOM.actions.DamageMobj(actor.target, actor, actor, damage);
                return;
            }

            // launch a missile
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_TROOPSHOT);
        });

        actionf_p1.put(A_SargAttack, (mobj_t actor) -> {
            int damage;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            if (DOOM.actions.EN.CheckMeleeRange(actor)) {
                damage = ((DOOM.random.P_Random() % 10) + 1) * 4;
                DOOM.actions.DamageMobj(actor.target, actor, actor, damage);
            }
        });

        actionf_p1.put(A_HeadAttack, (mobj_t actor) -> {
            int damage;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            if (DOOM.actions.EN.CheckMeleeRange(actor)) {
                damage = (DOOM.random.P_Random() % 6 + 1) * 10;
                DOOM.actions.DamageMobj(actor.target, actor, actor, damage);
                return;
            }

            // launch a missile
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_HEADSHOT);
        });

        actionf_p1.put(A_CyberAttack, (mobj_t actor) -> {
            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_ROCKET);
        });

        actionf_p1.put(A_BruisAttack, (mobj_t actor) -> {
            int damage;

            if (actor.target == null) {
                return;
            }

            if (DOOM.actions.EN.CheckMeleeRange(actor)) {
                DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_claw);
                damage = (DOOM.random.P_Random() % 8 + 1) * 10;
                DOOM.actions.DamageMobj(actor.target, actor, actor, damage);
                return;
            }

            // launch a missile
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_BRUISERSHOT);
        });

        //
        // A_SkelMissile
        //
        actionf_p1.put(A_SkelMissile, (mobj_t actor) -> {
            mobj_t mo;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            actor.z += 16 * FRACUNIT;    // so missile spawns higher
            mo = DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_TRACER);
            actor.z -= 16 * FRACUNIT;    // back to normal

            mo.x += mo.momx;
            mo.y += mo.momy;
            mo.tracer = actor.target;
        });

        actionf_p1.put(A_Tracer, (mobj_t actor) -> {
            long exact; //angle_t
            int dist, slope; // fixed
            mobj_t dest;
            mobj_t th;
            if (eval(DOOM.gametic & 3)) {
                return;
            }
            // spawn a puff of smoke behind the rocket
            DOOM.actions.SpawnPuff(actor.x, actor.y, actor.z);
            th = DOOM.actions.SpawnMobj(actor.x - actor.momx,
                actor.y - actor.momy,
                actor.z, mobjtype_t.MT_SMOKE);
            th.momz = MAPFRACUNIT;
            th.mobj_tics -= DOOM.random.P_Random() & 3;
            if (th.mobj_tics < 1) {
                th.mobj_tics = 1;
            }
            // adjust direction
            dest = actor.tracer;
            if (dest == null || dest.health <= 0) {
                return;
            }
            // change angle
            exact = DOOM.sceneRenderer.PointToAngle2(actor.x,
                actor.y,
                dest.x,
                dest.y) & BITS32;
            // MAES: let's analyze the logic here...
            // So exact is the angle between the missile and its target.
            if (exact != actor.angle) // missile is already headed there dead-on.
            {
                if (exact - actor.angle > ANG180) {
                    actor.angle -= TRACEANGLE;
                    actor.angle &= BITS32;
                    if (((exact - actor.angle) & BITS32) < ANG180) {
                        actor.angle = exact;
                    }
                } else {
                    actor.angle += TRACEANGLE;
                    actor.angle &= BITS32;
                    if (((exact - actor.angle) & BITS32) > ANG180) {
                        actor.angle = exact;
                    }
                }
            }
            // MAES: fixed and sped up.
            int exact2 = Tables.toBAMIndex(actor.angle);
            actor.momx = FixedMul(actor.info.speed, finecosine[exact2]);
            actor.momy = FixedMul(actor.info.speed, finesine[exact2]);
            // change slope
            dist = AproxDistance(dest.x - actor.x,
                dest.y - actor.y);
            dist /= actor.info.speed;
            if (dist < 1) {
                dist = 1;
            }
            slope = (dest.z + 40 * FRACUNIT - actor.z) / dist;
            if (slope < actor.momz) {
                actor.momz -= FRACUNIT / 8;
            } else {
                actor.momz += FRACUNIT / 8;
            }
        });

        actionf_p1.put(A_SkelWhoosh, (mobj_t actor) -> {
            if (actor.target == null) {
                return;
            }
            actionf_p1.get(A_FaceTarget).accept(actor);
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_skeswg);
        });

        actionf_p1.put(A_SkelFist, (mobj_t actor) -> {
            int damage;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);

            if (DOOM.actions.EN.CheckMeleeRange(actor)) {
                damage = ((DOOM.random.P_Random() % 10) + 1) * 6;
                DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_skepch);
                DOOM.actions.DamageMobj(actor.target, actor, actor, damage);
            }
        });

        //
        // A_VileChase
        // Check for ressurecting a body
        //
        actionf_p1.put(A_VileChase, (mobj_t actor) -> {
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
                DOOM.actions.vileTryX
                    = actor.x + actor.info.speed * xspeed[actor.movedir];
                DOOM.actions.vileTryY
                    = actor.y + actor.info.speed * yspeed[actor.movedir];

                xl = DOOM.levelLoader.getSafeBlockX(DOOM.actions.vileTryX - DOOM.levelLoader.bmaporgx - MAXRADIUS * 2);
                xh = DOOM.levelLoader.getSafeBlockX(DOOM.actions.vileTryX - DOOM.levelLoader.bmaporgx + MAXRADIUS * 2);
                yl = DOOM.levelLoader.getSafeBlockY(DOOM.actions.vileTryY - DOOM.levelLoader.bmaporgy - MAXRADIUS * 2);
                yh = DOOM.levelLoader.getSafeBlockY(DOOM.actions.vileTryY - DOOM.levelLoader.bmaporgy + MAXRADIUS * 2);

                DOOM.actions.vileObj = actor;
                for (bx = xl; bx <= xh; bx++) {
                    for (by = yl; by <= yh; by++) {
                        // Call PIT_VileCheck to check
                        // whether object is a corpse
                        // that can be raised.
                        if (!DOOM.actions.BlockThingsIterator(bx, by, DOOM.actions::VileCheck)) {
                            // got one!
                            temp = actor.target;
                            actor.target = DOOM.actions.vileCorpseHit;
                            actionf_p1.get(A_FaceTarget).accept(actor);
                            actor.target = temp;

                            actor.SetMobjState(statenum_t.S_VILE_HEAL1);
                            DOOM.doomSound.StartSound(DOOM.actions.vileCorpseHit, sfxenum_t.sfx_slop);
                            info = DOOM.actions.vileCorpseHit.info;

                            DOOM.actions.vileCorpseHit.SetMobjState(info.raisestate);
                            DOOM.actions.vileCorpseHit.height <<= 2;
                            DOOM.actions.vileCorpseHit.flags = info.flags;
                            DOOM.actions.vileCorpseHit.health = info.spawnhealth;
                            DOOM.actions.vileCorpseHit.target = null;

                            return;
                        }
                    }
                }
            }

            // Return to normal attack.
            actionf_p1.get(A_Chase).accept(actor);
        });

        //
        // A_VileStart
        //
        actionf_p1.put(A_VileStart, (mobj_t actor) -> {
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_vilatk);
        });

        //
        // A_Fire
        // Keep fire in front of player unless out of sight
        //
        actionf_p1.put(A_StartFire, (mobj_t actor) -> {
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_flamst);
            actionf_p1.get(A_Fire).accept(actor);
        });

        actionf_p1.put(A_FireCrackle, (mobj_t actor) -> {
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_flame);
            actionf_p1.get(A_Fire).accept(actor);
        });

        //
        // A_FirePistol
        //
        actionf_p2.put(A_FirePistol, (player_t player, pspdef_t psp) -> {
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_pistol);

            player.mo.SetMobjState(statenum_t.S_PLAY_ATK2);
            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

            player.SetPsprite(
                ps_flash,
                weaponinfo[player.readyweapon.ordinal()].flashstate);

            DOOM.actions.P_BulletSlope(player.mo);
            DOOM.actions.P_GunShot(player.mo, !eval(player.refire));
        });

        //
        // A_FireShotgun
        //
        actionf_p2.put(A_FireShotgun, (player_t player, pspdef_t psp) -> {
            int i;

            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_shotgn);
            player.mo.SetMobjState(statenum_t.S_PLAY_ATK2);

            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

            player.SetPsprite(
                ps_flash,
                weaponinfo[player.readyweapon.ordinal()].flashstate);

            DOOM.actions.P_BulletSlope(player.mo);

            for (i = 0; i < 7; i++) {
                DOOM.actions.P_GunShot(player.mo, false);
            }
        });

        /**
         * A_FireShotgun2
         */
        actionf_p2.put(A_FireShotgun2, (player_t player, pspdef_t psp) -> {
            int i;
            long angle;
            int damage;

            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_dshtgn);
            player.mo.SetMobjState(statenum_t.S_PLAY_ATK2);

            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()] -= 2;

            player.SetPsprite(
                ps_flash,
                weaponinfo[player.readyweapon.ordinal()].flashstate);

            DOOM.actions.P_BulletSlope(player.mo);

            for (i = 0; i < 20; i++) {
                damage = 5 * (DOOM.random.P_Random() % 3 + 1);
                angle = player.mo.angle;
                angle += (DOOM.random.P_Random() - DOOM.random.P_Random()) << 19;
                DOOM.actions.LineAttack(player.mo,
                    angle,
                    MISSILERANGE,
                    DOOM.actions.bulletslope + ((DOOM.random.P_Random() - DOOM.random.P_Random()) << 5), damage);
            }
        });

        //
        // A_VileTarget
        // Spawn the hellfire
        //
        actionf_p1.put(A_VileTarget, (mobj_t actor) -> {
            mobj_t fog;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);

            fog = DOOM.actions.SpawnMobj(actor.target.x,
                actor.target.y,
                actor.target.z, mobjtype_t.MT_FIRE);

            actor.tracer = fog;
            fog.target = actor;
            fog.tracer = actor.target;
            actionf_p1.get(A_Fire).accept(fog);
        });

        //
        // A_VileAttack
        //
        actionf_p1.put(A_VileAttack, (mobj_t actor) -> {
            mobj_t fire;
            //int     an;

            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);

            if (!DOOM.actions.EN.CheckSight(actor, actor.target)) {
                return;
            }

            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_barexp);
            DOOM.actions.DamageMobj(actor.target, actor, actor, 20);
            actor.target.momz = 1000 * MAPFRACUNIT / actor.target.info.mass;

            // an = actor.angle >> ANGLETOFINESHIFT;
            fire = actor.tracer;

            if (fire == null) {
                return;
            }

            // move the fire between the vile and the player
            fire.x = actor.target.x - FixedMul(24 * FRACUNIT, finecosine(actor.angle));
            fire.y = actor.target.y - FixedMul(24 * FRACUNIT, finesine(actor.angle));
            DOOM.actions.RadiusAttack(fire, actor, 70);
        });

        //
        // Mancubus attack,
        // firing three missiles (bruisers)
        // in three different directions?
        // Doesn't look like it. 
        //
        actionf_p1.put(A_FatRaise, (mobj_t actor) -> {
            actionf_p1.get(A_FaceTarget).accept(actor);
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_manatk);
        });

        actionf_p1.put(A_FatAttack1, (mobj_t actor) -> {
            mobj_t mo;
            int an;

            actionf_p1.get(A_FaceTarget).accept(actor);
            // Change direction  to ...
            actor.angle += FATSPREAD;
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);

            mo = DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);
            mo.angle += FATSPREAD;
            an = Tables.toBAMIndex(mo.angle);
            mo.momx = FixedMul(mo.info.speed, finecosine[an]);
            mo.momy = FixedMul(mo.info.speed, finesine[an]);
        });

        actionf_p1.put(A_FatAttack2, (mobj_t actor) -> {
            mobj_t mo;
            int an;

            actionf_p1.get(A_FaceTarget).accept(actor);
            // Now here choose opposite deviation.
            actor.angle -= FATSPREAD;
            DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);

            mo = DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);
            mo.angle -= FATSPREAD * 2;
            an = Tables.toBAMIndex(mo.angle);
            mo.momx = FixedMul(mo.info.speed, finecosine[an]);
            mo.momy = FixedMul(mo.info.speed, finesine[an]);
        });

        actionf_p1.put(A_FatAttack3, (mobj_t actor) -> {
            mobj_t mo;
            int an;

            actionf_p1.get(A_FaceTarget).accept(actor);

            mo = DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);
            mo.angle -= FATSPREAD / 2;
            an = Tables.toBAMIndex(mo.angle);
            mo.momx = FixedMul(mo.info.speed, finecosine[an]);
            mo.momy = FixedMul(mo.info.speed, finesine[an]);

            mo = DOOM.actions.SpawnMissile(actor, actor.target, mobjtype_t.MT_FATSHOT);
            mo.angle += FATSPREAD / 2;
            an = Tables.toBAMIndex(mo.angle);
            mo.momx = FixedMul(mo.info.speed, finecosine[an]);
            mo.momy = FixedMul(mo.info.speed, finesine[an]);
        });

        /**
         * SkullAttack
         * Fly at the player like a missile.
         */
        actionf_p1.put(A_SkullAttack, (mobj_t actor) -> {
            mobj_t dest;
            int an;
            int dist;

            if (actor.target == null) {
                return;
            }

            dest = actor.target;
            actor.flags |= MF_SKULLFLY;

            DOOM.doomSound.StartSound(actor, actor.info.attacksound);
            actionf_p1.get(A_FaceTarget).accept(actor);
            an = Tables.toBAMIndex(actor.angle);
            actor.momx = FixedMul(SKULLSPEED, finecosine[an]);
            actor.momy = FixedMul(SKULLSPEED, finesine[an]);
            dist = AproxDistance(dest.x - actor.x, dest.y - actor.y);
            dist /= SKULLSPEED;

            if (dist < 1) {
                dist = 1;
            }
            actor.momz = (dest.z + (dest.height >> 1) - actor.z) / dist;
        });

        /**
         * A_PainShootSkull
         * Spawn a lost soul and launch it at the target
         * It's not a valid callback like the others, actually.
         * No idea if some DEH patch does use it to cause
         * mayhem though.
         *
         */
        final BiConsumer<mobj_t, Long> A_PainShootSkull = (mobj_t actor, Long angle) -> {
            int x, y, z; // fixed

            mobj_t newmobj;
            int an; // angle
            int prestep;
            int count;
            thinker_t currentthinker;

            // count total number of skull currently on the level
            count = 0;

            currentthinker = DOOM.actions.thinkercap.next;
            while (currentthinker != DOOM.actions.thinkercap) {
                if ((currentthinker.thinkerFunction == ActionFunction.P_MobjThinker)
                    && ((mobj_t) currentthinker).type == mobjtype_t.MT_SKULL) {
                    count++;
                }
                currentthinker = currentthinker.next;
            }

            // if there are allready 20 skulls on the level,
            // don't spit another one
            if (count > MAXSKULLS) {
                return;
            }

            // okay, there's playe for another one
            an = Tables.toBAMIndex(angle);

            prestep
                = 4 * FRACUNIT
                + 3 * (actor.info.radius + mobjinfo[mobjtype_t.MT_SKULL.ordinal()].radius) / 2;

            x = actor.x + FixedMul(prestep, finecosine[an]);
            y = actor.y + FixedMul(prestep, finesine[an]);
            z = actor.z + 8 * FRACUNIT;

            newmobj = DOOM.actions.SpawnMobj(x, y, z, mobjtype_t.MT_SKULL);

            // Check for movements.
            if (!DOOM.actions.TryMove(newmobj, newmobj.x, newmobj.y)) {
                // kill it immediately
                DOOM.actions.DamageMobj(newmobj, actor, actor, 10000);
                return;
            }

            newmobj.target = actor.target;
            actionf_p1.get(A_SkullAttack).accept(newmobj);
        };

        //
        // A_PainAttack
        // Spawn a lost soul and launch it at the target
        // 
        actionf_p1.put(A_PainAttack, (mobj_t actor) -> {
            if (actor.target == null) {
                return;
            }

            actionf_p1.get(A_FaceTarget).accept(actor);
            A_PainShootSkull.accept(actor, actor.angle);
        });

        actionf_p1.put(A_PainDie, (mobj_t actor) -> {
            actionf_p1.get(A_Fall).accept(actor);
            A_PainShootSkull.accept(actor, actor.angle + ANG90);
            A_PainShootSkull.accept(actor, actor.angle + ANG180);
            A_PainShootSkull.accept(actor, actor.angle + ANG270);
        });

        actionf_p1.put(A_Scream, (mobj_t actor) -> {
            int sound;

            switch (actor.info.deathsound) {
                case sfx_None:
                    return;

                case sfx_podth1:
                case sfx_podth2:
                case sfx_podth3:
                    sound = sfxenum_t.sfx_podth1.ordinal() + DOOM.random.P_Random() % 3;
                    break;

                case sfx_bgdth1:
                case sfx_bgdth2:
                    sound = sfxenum_t.sfx_bgdth1.ordinal() + DOOM.random.P_Random() % 2;
                    break;

                default:
                    sound = actor.info.deathsound.ordinal();
                    break;
            }

            // Check for bosses.
            if (actor.type == mobjtype_t.MT_SPIDER
                || actor.type == mobjtype_t.MT_CYBORG) {
                // full volume
                DOOM.doomSound.StartSound(null, sound);
            } else {
                DOOM.doomSound.StartSound(actor, sound);
            }
        });

        /**
         * A_WeaponReady
         * The player can fire the weapon
         * or change to another weapon at this time.
         * Follows after getting weapon up,
         * or after previous attack/fire sequence.
         */
        actionf_p2.put(A_WeaponReady, (player_t player, pspdef_t psp) -> {
            statenum_t newstate;
            int angle;

            // get out of attack state
            if (player.mo.mobj_state == states[statenum_t.S_PLAY_ATK1.ordinal()]
                || player.mo.mobj_state == states[statenum_t.S_PLAY_ATK2.ordinal()]) {
                player.mo.SetMobjState(statenum_t.S_PLAY);
            }

            if (player.readyweapon == weapontype_t.wp_chainsaw
                && psp.state == states[statenum_t.S_SAW.ordinal()]) {
                DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_sawidl);
            }

            // check for change
            //  if player is dead, put the weapon away
            if (player.pendingweapon != weapontype_t.wp_nochange || !eval(player.health[0])) {
                // change weapon
                //  (pending weapon should allready be validated)
                newstate = weaponinfo[player.readyweapon.ordinal()].downstate;
                player.SetPsprite(player_t.ps_weapon, newstate);
                return;
            }

            // check for fire
            //  the missile launcher and bfg do not auto fire
            if (eval(player.cmd.buttons & BT_ATTACK)) {
                if (!player.attackdown
                    || (player.readyweapon != weapontype_t.wp_missile
                    && player.readyweapon != weapontype_t.wp_bfg)) {
                    player.attackdown = true;
                    DOOM.actions.EN.FireWeapon(player);
                    return;
                }
            } else {
                player.attackdown = false;
            }

            // bob the weapon based on movement speed
            angle = (128 * DOOM.leveltime) & FINEMASK;
            psp.sx = FRACUNIT + FixedMul(player.bob, finecosine[angle]);
            angle &= FINEANGLES / 2 - 1;
            psp.sy = player_t.WEAPONTOP + FixedMul(player.bob, finesine[angle]);
        });

        //
        // A_Raise
        //
        actionf_p2.put(A_Raise, (player_t player, pspdef_t psp) -> {
            statenum_t newstate;

            //System.out.println("Trying to raise weapon");
            //System.out.println(player.readyweapon + " height: "+psp.sy);
            psp.sy -= RAISESPEED;

            if (psp.sy > WEAPONTOP) {
                //System.out.println("Not on top yet, exit and repeat.");
                return;
            }

            psp.sy = WEAPONTOP;

            // The weapon has been raised all the way,
            //  so change to the ready state.
            newstate = weaponinfo[player.readyweapon.ordinal()].readystate;
            //System.out.println("Weapon raised, setting new state.");

            player.SetPsprite(ps_weapon, newstate);
        });

        //
        // A_ReFire
        // The player can re-fire the weapon
        // without lowering it entirely.
        //
        actionf_p2.put(A_ReFire, (player_t player, pspdef_t psp) -> {
            // check for fire
            //  (if a weaponchange is pending, let it go through instead)
            if (eval(player.cmd.buttons & BT_ATTACK)
                && player.pendingweapon == weapontype_t.wp_nochange
                && eval(player.health[0])) {
                player.refire++;
                DOOM.actions.EN.FireWeapon(player);
            } else {
                player.refire = 0;
                player.CheckAmmo();
            }
        });

        //
        // A_GunFlash
        //
        actionf_p2.put(A_GunFlash, (player_t player, pspdef_t psp) -> {
            player.mo.SetMobjState(statenum_t.S_PLAY_ATK2);
            player.SetPsprite(ps_flash, weaponinfo[player.readyweapon.ordinal()].flashstate);
        });

        //
        // A_Punch
        //
        actionf_p2.put(A_Punch, (player_t player, pspdef_t psp) -> {
            long angle; //angle_t
            int damage;
            int slope;

            damage = (DOOM.random.P_Random() % 10 + 1) << 1;

            if (eval(player.powers[pw_strength])) {
                damage *= 10;
            }

            angle = player.mo.angle;
            //angle = (angle+(RND.P_Random()-RND.P_Random())<<18)/*&BITS32*/;
            // _D_: for some reason, punch didnt work until I change this
            // I think it's because of "+" VS "<<" prioritys...
            angle += (DOOM.random.P_Random() - DOOM.random.P_Random()) << 18;
            slope = DOOM.actions.AimLineAttack(player.mo, angle, MELEERANGE);
            DOOM.actions.LineAttack(player.mo, angle, MELEERANGE, slope, damage);

            // turn to face target
            if (eval(DOOM.actions.linetarget)) {
                DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_punch);
                player.mo.angle = DOOM.sceneRenderer.PointToAngle2(player.mo.x,
                    player.mo.y,
                    DOOM.actions.linetarget.x,
                    DOOM.actions.linetarget.y) & BITS32;
            }
        });

        //
        // A_Saw
        //
        actionf_p2.put(A_Saw, (player_t player, pspdef_t psp) -> {
            long angle; // angle_t
            int damage;
            int slope;

            damage = 2 * (DOOM.random.P_Random() % 10 + 1);
            angle = player.mo.angle;
            angle += (DOOM.random.P_Random() - DOOM.random.P_Random()) << 18;
            angle &= BITS32;

            // use meleerange + 1 se the puff doesn't skip the flash
            slope = DOOM.actions.AimLineAttack(player.mo, angle, MELEERANGE + 1);
            DOOM.actions.LineAttack(player.mo, angle, MELEERANGE + 1, slope, damage);

            if (!eval(DOOM.actions.linetarget)) {
                DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_sawful);
                return;
            }
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_sawhit);

            // turn to face target
            angle = DOOM.sceneRenderer.PointToAngle2(player.mo.x, player.mo.y,
                DOOM.actions.linetarget.x, DOOM.actions.linetarget.y) & BITS32;
            /* FIXME: this comparison is going to fail.... or not?
            If e.g. angle = 359 degrees (which will be mapped to a small negative number),
            and player.mo.angle = 160 degrees (a large, positive value), the result will be a
            large negative value, which will still be "greater" than ANG180.
            
            It seems that *differences* between angles will always compare correctly, but
            not direct inequalities.
            
             */

            // Yet another screwy place where unsigned BAM angles are used as SIGNED comparisons.
            long dangle = (angle - player.mo.angle);
            dangle &= BITS32;
            if (dangle > ANG180) {
                if ((int) dangle < -ANG90 / 20) {
                    player.mo.angle = angle + ANG90 / 21;
                } else {
                    player.mo.angle -= ANG90 / 20;
                }
            } else {
                if (dangle > ANG90 / 20) {
                    player.mo.angle = angle - ANG90 / 21;
                } else {
                    player.mo.angle += ANG90 / 20;
                }
            }
            player.mo.angle &= BITS32;
            player.mo.flags |= MF_JUSTATTACKED;
        });

        //
        // A_FireMissile
        //
        actionf_p2.put(A_FireMissile, (player_t player, pspdef_t psp) -> {
            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;
            DOOM.actions.SpawnPlayerMissile(player.mo, mobjtype_t.MT_ROCKET);
        });

        //
        // A_FireBFG
        //
        actionf_p2.put(A_FireBFG, (player_t player, pspdef_t psp) -> {
            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()] -= BFGCELLS;
            DOOM.actions.SpawnPlayerMissile(player.mo, mobjtype_t.MT_BFG);
        });

        //
        // A_FireCGun
        //
        actionf_p2.put(A_FireCGun, (player_t player, pspdef_t psp) -> {
            // For convenience.
            int readyweap = player.readyweapon.ordinal();
            int flashstate = weaponinfo[readyweap].flashstate.ordinal();
            int current_state = psp.state.id;

            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_pistol);
            if (!eval(player.ammo[weaponinfo[readyweap].ammo.ordinal()])) {
                return;
            }

            player.mo.SetMobjState(statenum_t.S_PLAY_ATK2);
            player.ammo[weaponinfo[readyweap].ammo.ordinal()]--;

            // MAES: Code to alternate between two different gun flashes
            // needed a clear rewrite, as it was way too messy.
            // We know that the flash states are a certain amount away from
            // the firing states. This amount is two frames.
            player.SetPsprite(ps_flash, statenum_t.values()[flashstate + current_state - statenum_t.S_CHAIN1.ordinal()]
            );

            DOOM.actions.P_BulletSlope(player.mo);

            DOOM.actions.P_GunShot(player.mo, !eval(player.refire));
        });

        //
        // A_FirePlasma
        //
        actionf_p2.put(A_FirePlasma, (player_t player, pspdef_t psp) -> {
            player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

            player.SetPsprite(
                ps_flash,
                weaponinfo[player.readyweapon.ordinal()].flashstate);

            DOOM.actions.SpawnPlayerMissile(player.mo, mobjtype_t.MT_PLASMA);
        });

        actionf_p1.put(A_XScream, (mobj_t actor) -> {
            DOOM.doomSound.StartSound(actor, sfxenum_t.sfx_slop);
        });

        actionf_p1.put(A_Pain, (mobj_t actor) -> {
            if (actor.info.painsound != null) {
                DOOM.doomSound.StartSound(actor, actor.info.painsound);
            }
        });

        //
        // A_Explode
        //
        actionf_p1.put(A_Explode, (mobj_t thingy) -> {
            DOOM.actions.RadiusAttack(thingy, thingy.target, 128);
        });

        /**
         * A_BossDeath
         * Possibly trigger special effects
         * if on first boss level
         *
         * TODO: find out how Plutonia/TNT does cope with this.
         * Special clauses?
         *
         */
        actionf_p1.put(A_BossDeath, (mobj_t mo) -> {
            thinker_t th;
            mobj_t mo2;
            line_t junk = new line_t();
            int i;

            if (DOOM.isCommercial()) {
                if (DOOM.gamemap != 7) {
                    return;
                }

                if ((mo.type != mobjtype_t.MT_FATSO)
                    && (mo.type != mobjtype_t.MT_BABY)) {
                    return;
                }
            } else {
                switch (DOOM.gameepisode) {
                    case 1:
                        if (DOOM.gamemap != 8) {
                            return;
                        }

                        if (mo.type != mobjtype_t.MT_BRUISER) {
                            return;
                        }
                        break;

                    case 2:
                        if (DOOM.gamemap != 8) {
                            return;
                        }

                        if (mo.type != mobjtype_t.MT_CYBORG) {
                            return;
                        }
                        break;

                    case 3:
                        if (DOOM.gamemap != 8) {
                            return;
                        }

                        if (mo.type != mobjtype_t.MT_SPIDER) {
                            return;
                        }

                        break;

                    case 4:
                        switch (DOOM.gamemap) {
                            case 6:
                                if (mo.type != mobjtype_t.MT_CYBORG) {
                                    return;
                                }
                                break;

                            case 8:
                                if (mo.type != mobjtype_t.MT_SPIDER) {
                                    return;
                                }
                                break;

                            default:
                                return;
                        }
                        break;

                    default:
                        if (DOOM.gamemap != 8) {
                            return;
                        }
                        break;
                }

            }

            // make sure there is a player alive for victory
            for (i = 0; i < MAXPLAYERS; i++) {
                if (DOOM.playeringame[i] && DOOM.players[i].health[0] > 0) {
                    break;
                }
            }

            if (i == MAXPLAYERS) {
                return; // no one left alive, so do not end game
            }
            // scan the remaining thinkers to see
            // if all bosses are dead
            for (th = DOOM.actions.thinkercap.next; th != DOOM.actions.thinkercap; th = th.next) {
                if (th.thinkerFunction != ActionFunction.P_MobjThinker) {
                    continue;
                }

                mo2 = (mobj_t) th;
                if (mo2 != mo
                    && mo2.type == mo.type
                    && mo2.health > 0) {
                    // other boss not dead
                    return;
                }
            }

            // victory!
            if (DOOM.isCommercial()) {
                if (DOOM.gamemap == 7) {
                    if (mo.type == mobjtype_t.MT_FATSO) {
                        junk.tag = 666;
                        DOOM.actions.DoFloor(junk, floor_e.lowerFloorToLowest);
                        return;
                    }

                    if (mo.type == mobjtype_t.MT_BABY) {
                        junk.tag = 667;
                        DOOM.actions.DoFloor(junk, floor_e.raiseToTexture);
                        return;
                    }
                }
            } else {
                switch (DOOM.gameepisode) {
                    case 1:
                        junk.tag = 666;
                        DOOM.actions.DoFloor(junk, floor_e.lowerFloorToLowest);
                        return;

                    case 4:
                        switch (DOOM.gamemap) {
                            case 6:
                                junk.tag = 666;
                                DOOM.actions.DoDoor(junk, vldoor_e.blazeOpen);
                                return;

                            case 8:
                                junk.tag = 666;
                                DOOM.actions.DoFloor(junk, floor_e.lowerFloorToLowest);
                                return;
                        }
                }
            }

            DOOM.ExitLevel();
        });

        actionf_p1.put(A_Hoof, (mobj_t mo) -> {
            DOOM.doomSound.StartSound(mo, sfxenum_t.sfx_hoof);
            actionf_p1.get(A_Chase).accept(mo);
        });

        actionf_p1.put(A_KeenDie, (mobj_t mo) -> {
            thinker_t th;
            mobj_t mo2;
            line_t junk = new line_t(); // MAES: fixed null 21/5/2011

            actionf_p1.get(A_Fall).accept(mo);

            // scan the remaining thinkers
            // to see if all Keens are dead
            for (th = DOOM.actions.thinkercap.next; th != DOOM.actions.thinkercap; th = th.next) {
                if (th.thinkerFunction != ActionFunction.P_MobjThinker) {
                    continue;
                }

                mo2 = (mobj_t) th;
                if (mo2 != mo
                    && mo2.type == mo.type
                    && mo2.health > 0) {
                    // other Keen not dead
                    return;
                }
            }

            junk.tag = 666;
            DOOM.actions.DoDoor(junk, vldoor_e.open);
        });

        actionf_p1.put(A_Metal, (mobj_t mo) -> {
            DOOM.doomSound.StartSound(mo, sfxenum_t.sfx_metal);
            actionf_p1.get(A_Chase).accept(mo);
        });

        actionf_p1.put(A_BabyMetal, (mobj_t mo) -> {
            DOOM.doomSound.StartSound(mo, sfxenum_t.sfx_bspwlk);
            actionf_p1.get(A_Chase).accept(mo);
        });

        //
        // A_BFGsound
        //
        actionf_p2.put(A_BFGsound, (player_t player, pspdef_t psp) -> {
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_bfg);
        });

        //
        // A_BFGSpray
        // Spawn a BFG explosion on every monster in view
        //
        actionf_p1.put(A_BFGSpray, (mobj_t mo) -> {
            int i;
            int j;
            int damage;
            long an; // angle_t

            // offset angles from its attack angle
            for (i = 0; i < 40; i++) {
                an = (mo.angle - ANG90 / 2 + ANG90 / 40 * i) & BITS32;

                // mo.target is the originator (player)
                //  of the missile
                DOOM.actions.AimLineAttack(mo.target, an, 16 * 64 * FRACUNIT);

                if (!eval(DOOM.actions.linetarget)) {
                    continue;
                }

                DOOM.actions.SpawnMobj(DOOM.actions.linetarget.x,
                    DOOM.actions.linetarget.y,
                    DOOM.actions.linetarget.z + (DOOM.actions.linetarget.height >> 2),
                    mobjtype_t.MT_EXTRABFG);

                damage = 0;
                for (j = 0; j < 15; j++) {
                    damage += (DOOM.random.P_Random() & 7) + 1;
                }

                DOOM.actions.DamageMobj(DOOM.actions.linetarget, mo.target, mo.target, damage);
            }
        });

        actionf_p2.put(A_OpenShotgun2, (player_t player, pspdef_t psp) -> {
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_dbopn);
        });

        actionf_p2.put(A_LoadShotgun2, (player_t player, pspdef_t psp) -> {
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_dbload);
        });

        //
        // A_Look
        // Stay in state until a player is sighted.
        //
        actionf_p1.put(A_Look, (mobj_t actor) -> {
            mobj_t targ;
            boolean seeyou = false; // to avoid the fugly goto

            actor.threshold = 0;   // any shot will wake up
            targ = actor.subsector.sector.soundtarget;

            if (targ != null
                && eval(targ.flags & MF_SHOOTABLE)) {
                actor.target = targ;

                if (eval(actor.flags & MF_AMBUSH)) {
                    seeyou = (DOOM.actions.EN.CheckSight(actor, actor.target));
                } else {
                    seeyou = true;
                }
            }
            if (!seeyou) {
                if (!DOOM.actions.EN.LookForPlayers(actor, false)) {
                    return;
                }
            }

            // go into chase state
            seeyou:
            if (actor.info.seesound != null && actor.info.seesound != sfxenum_t.sfx_None) {
                int sound;

                switch (actor.info.seesound) {
                    case sfx_posit1:
                    case sfx_posit2:
                    case sfx_posit3:
                        sound = sfxenum_t.sfx_posit1.ordinal() + DOOM.random.P_Random() % 3;
                        break;

                    case sfx_bgsit1:
                    case sfx_bgsit2:
                        sound = sfxenum_t.sfx_bgsit1.ordinal() + DOOM.random.P_Random() % 2;
                        break;

                    default:
                        sound = actor.info.seesound.ordinal();
                        break;
                }

                if (actor.type == mobjtype_t.MT_SPIDER
                    || actor.type == mobjtype_t.MT_CYBORG) {
                    // full volume
                    DOOM.doomSound.StartSound(null, sound);
                } else {
                    DOOM.doomSound.StartSound(actor, sound);
                }
            }

            actor.SetMobjState(actor.info.seestate);
        });

        actionf_p2.put(A_CloseShotgun2, (player_t player, pspdef_t psp) -> {
            DOOM.doomSound.StartSound(player.mo, sfxenum_t.sfx_dbcls);
            actionf_p2.get(A_ReFire).accept(player, psp);
        });

        //
        // ?
        //
        actionf_p2.put(A_Light0, (player_t player, pspdef_t psp) -> {
            player.extralight = 0;
        });

        actionf_p2.put(A_Light1, (player_t player, pspdef_t psp) -> {
            player.extralight = 1;
        });

        actionf_p2.put(A_Light2, (player_t player, pspdef_t psp) -> {
            player.extralight = 2;
        });

        //
        // A_Lower
        // Lowers current weapon,
        //  and changes weapon at bottom.
        //
        actionf_p2.put(A_Lower, (player_t player, pspdef_t psp) -> {
            psp.sy += LOWERSPEED;

            // Is already down.
            if (psp.sy < WEAPONBOTTOM) {
                return;
            }

            // Player is dead.
            if (player.playerstate == PST_DEAD) {
                psp.sy = WEAPONBOTTOM;

                // don't bring weapon back up
                return;
            }

            // The old weapon has been lowered off the screen,
            // so change the weapon and start raising it
            if (!eval(player.health[0])) {
                // Player is dead, so keep the weapon off screen.
                player.SetPsprite(ps_weapon, statenum_t.S_NULL);
                return;
            }

            player.readyweapon = player.pendingweapon;

            player.BringUpWeapon();
        });

        actionf_p2.put(A_CheckReload, (player_t player, pspdef_t psp) -> {
            player.CheckAmmo();
            /*
            if (player.ammo[am_shell]<2)
            P_SetPsprite (player, ps_weapon, S_DSNR1);
             */
        });

        actionf_p1.put(A_BrainAwake, (mobj_t mo) -> {
            thinker_t thinker;
            mobj_t m;

            // find all the target spots
            numbraintargets = 0;
            braintargeton = 0;

            //thinker = DOOM.actions.thinkercap.next;
            for (thinker = DOOM.actions.thinkercap.next;
                thinker != DOOM.actions.thinkercap;
                thinker = thinker.next) {
                if (thinker.thinkerFunction != ActionFunction.P_MobjThinker) {
                    continue;   // not a mobj
                }
                m = (mobj_t) thinker;

                if (m.type == mobjtype_t.MT_BOSSTARGET) {
                    braintargets[numbraintargets] = m;
                    numbraintargets++;
                }
            }

            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_bossit);
        });

        actionf_p1.put(A_BrainPain, (mobj_t mo) -> {
            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_bospn);
        });

        actionf_p1.put(A_BrainScream, (mobj_t mo) -> {
            int x;
            int y;
            int z;
            mobj_t th;

            for (x = mo.x - 196 * FRACUNIT; x < mo.x + 320 * FRACUNIT; x += FRACUNIT * 8) {
                y = mo.y - 320 * FRACUNIT;
                z = 128 + DOOM.random.P_Random() * 2 * FRACUNIT;
                th = DOOM.actions.SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET);
                th.momz = DOOM.random.P_Random() * 512;

                th.SetMobjState(statenum_t.S_BRAINEXPLODE1);

                th.mobj_tics -= DOOM.random.P_Random() & 7;
                if (th.mobj_tics < 1) {
                    th.mobj_tics = 1;
                }
            }

            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_bosdth);
        });

        actionf_p1.put(A_BrainExplode, (mobj_t mo) -> {
            int x;
            int y;
            int z;
            mobj_t th;

            x = mo.x + (DOOM.random.P_Random() - DOOM.random.P_Random()) * 2048;
            y = mo.y;
            z = 128 + DOOM.random.P_Random() * 2 * FRACUNIT;
            th = DOOM.actions.SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET);
            th.momz = DOOM.random.P_Random() * 512;

            th.SetMobjState(statenum_t.S_BRAINEXPLODE1);

            th.mobj_tics -= DOOM.random.P_Random() & 7;
            if (th.mobj_tics < 1) {
                th.mobj_tics = 1;
            }
        });

        actionf_p1.put(A_BrainDie, (mobj_t mo) -> {
            DOOM.ExitLevel();
        });

        actionf_p1.put(A_BrainSpit, (mobj_t mo) -> {
            mobj_t targ;
            mobj_t newmobj;

            easy ^= 1;
            if (DOOM.gameskill.ordinal() <= skill_t.sk_easy.ordinal() && (easy == 0)) {
                return;
            }

            // shoot a cube at current target
            targ = braintargets[braintargeton];

            // Load-time fix: awake on zero numbrain targets, if A_BrainSpit is called.
            if (numbraintargets == 0) {
                actionf_p1.get(A_BrainAwake).accept(mo);
                return;
            }
            braintargeton = (braintargeton + 1) % numbraintargets;

            // spawn brain missile
            newmobj = DOOM.actions.SpawnMissile(mo, targ, mobjtype_t.MT_SPAWNSHOT);
            newmobj.target = targ;
            newmobj.reactiontime = ((targ.y - mo.y) / newmobj.momy) / newmobj.mobj_state.tics;

            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_bospit);
        });

        actionf_p1.put(A_SpawnFly, (mobj_t mo) -> {
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
            fog = DOOM.actions.SpawnMobj(targ.x, targ.y, targ.z, mobjtype_t.MT_SPAWNFIRE);
            DOOM.doomSound.StartSound(fog, sfxenum_t.sfx_telept);

            // Randomly select monster to spawn.
            r = DOOM.random.P_Random();

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

            newmobj = DOOM.actions.SpawnMobj(targ.x, targ.y, targ.z, type);
            if (DOOM.actions.EN.LookForPlayers(newmobj, true)) {
                newmobj.SetMobjState(newmobj.info.seestate);
            }

            // telefrag anything in this spot
            DOOM.actions.TeleportMove(newmobj, newmobj.x, newmobj.y);

            // remove self (i.e., cube).
            DOOM.actions.RemoveMobj(mo);
        });

        // travelling cube sound
        actionf_p1.put(A_SpawnSound, (mobj_t mo) -> {
            DOOM.doomSound.StartSound(mo, sfxenum_t.sfx_boscub);
            actionf_p1.get(A_SpawnFly).accept(mo);
        });

        actionf_p1.put(A_PlayerScream, (mobj_t actor) -> {
            // Default death sound.
            sfxenum_t sound = sfxenum_t.sfx_pldeth;

            if (DOOM.isCommercial()
                && (actor.health < -50)) {
                // IF THE PLAYER DIES
                // LESS THAN -50% WITHOUT GIBBING
                sound = sfxenum_t.sfx_pdiehi;
            }

            DOOM.doomSound.StartSound(actor, sound);
        });
    }

    @Override
    public void callMobjFun(ActionFunction think_t, mobj_t mobj) {
        this.actionf_p1.get(think_t).accept(mobj);
    }

    @Override
    public void callPlayerSpriteFun(ActionFunction think_t, player_t player, pspdef_t pspdef) {
        this.actionf_p2.get(think_t).accept(player, pspdef);
    }

    @Override
    public void callThinkerFun(ActionFunction think_t, thinker_t thinker) {
        this.actionf_v.get(think_t).acceptThinker(thinker);
    }

    /**
     * Generic single-thinker argument action function.
     * Useful for handling stuff such as sector actions, doors, etc.
     * or special thinker objects that don't qualify as mobj_t's.
     *
     * @author velktron
     *
     * @param <T>
     */
    interface TypedAction<T extends thinker_t> extends Consumer<T> {
        default void acceptThinker(thinker_t currentthinker) {
            accept((T) currentthinker);
        }
    }
}
