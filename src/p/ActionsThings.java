package p;

import static data.Defines.BASETHRESHOLD;
import static data.Defines.ONFLOORZ;
import static data.Defines.PST_DEAD;
import static data.Defines.pw_invulnerability;
import static data.Tables.ANG180;
import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.info.states;
import data.mobjtype_t;
import defines.skill_t;
import defines.statenum_t;
import doom.SourceCode;
import static doom.SourceCode.P_Map.PIT_CheckThing;
import static doom.SourceCode.P_Map.PIT_StompThing;
import doom.player_t;
import doom.weapontype_t;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static m.fixed_t.MAPFRACUNIT;
import static p.mobj_t.MF_CORPSE;
import static p.mobj_t.MF_COUNTKILL;
import static p.mobj_t.MF_DROPOFF;
import static p.mobj_t.MF_DROPPED;
import static p.mobj_t.MF_FLOAT;
import static p.mobj_t.MF_JUSTHIT;
import static p.mobj_t.MF_MISSILE;
import static p.mobj_t.MF_NOCLIP;
import static p.mobj_t.MF_NOGRAVITY;
import static p.mobj_t.MF_PICKUP;
import static p.mobj_t.MF_SHOOTABLE;
import static p.mobj_t.MF_SKULLFLY;
import static p.mobj_t.MF_SOLID;
import static p.mobj_t.MF_SPECIAL;
import static utils.C2JUtils.eval;

public interface ActionsThings<T, V> extends ActionsSpawn<T, V> {
    /**
     * PIT_CheckThing
     */
    @SourceCode.P_Map.C(PIT_CheckThing) default boolean CheckThing(mobj_t thing) {
        final ActionsRegistry<T, V> obs = obs();
        @SourceCode.fixed_t int blockdist;
        boolean solid;
        int damage;

        if ((thing.flags & (MF_SOLID | MF_SPECIAL | MF_SHOOTABLE)) == 0) {
            return true;
        }

        blockdist = thing.radius + obs.tmthing.radius;

        if (Math.abs(thing.x - obs.tmx) >= blockdist
                || Math.abs(thing.y - obs.tmy) >= blockdist) {
            // didn't hit it
            return true;
        }

        // don't clip against self
        if (thing == obs.tmthing) {
            return true;
        }

        // check for skulls slamming into things
        if ((obs.tmthing.flags & MF_SKULLFLY) != 0) {
            damage = ((obs.DOOM.random.P_Random() % 8) + 1) * obs.tmthing.info.damage;

            this.DamageMobj(thing, obs.tmthing, obs.tmthing, damage);

            obs.tmthing.flags &= ~MF_SKULLFLY;
            obs.tmthing.momx = obs.tmthing.momy = obs.tmthing.momz = 0;

            obs.tmthing.SetMobjState(obs.tmthing.info.spawnstate);

            return false;       // stop moving
        }

        // missiles can hit other things
        if (eval(obs.tmthing.flags & MF_MISSILE)) {
            // see if it went over / under
            if (obs.tmthing.z > thing.z + thing.height) {
                return true;        // overhead
            }
            if (obs.tmthing.z + obs.tmthing.height < thing.z) {
                return true;        // underneath
            }
            if (obs.tmthing.target != null && (obs.tmthing.target.type == thing.type
                    || (obs.tmthing.target.type == mobjtype_t.MT_KNIGHT && thing.type == mobjtype_t.MT_BRUISER)
                    || (obs.tmthing.target.type == mobjtype_t.MT_BRUISER && thing.type == mobjtype_t.MT_KNIGHT))) {
                // Don't hit same species as originator.
                if (thing == obs.tmthing.target) {
                    return true;
                }

                if (thing.type != mobjtype_t.MT_PLAYER) {
                    // Explode, but do no damage.
                    // Let players missile other players.
                    return false;
                }
            }

            if (!eval(thing.flags & MF_SHOOTABLE)) {
                // didn't do any damage
                return !eval(thing.flags & MF_SOLID);
            }

            // damage / explode
            damage = ((obs.DOOM.random.P_Random() % 8) + 1) * obs.tmthing.info.damage;
            this.DamageMobj(thing, obs.tmthing, obs.tmthing.target, damage);

            // don't traverse any more
            return false;
        }

        // check for special pickup
        if (eval(thing.flags & MF_SPECIAL)) {
            solid = eval(thing.flags & MF_SOLID);
            if (eval(obs.tmflags & MF_PICKUP)) {
                // can remove thing
                obs.TouchSpecialThing(thing, obs.tmthing);
            }
            return !solid;
        }

        return !eval(thing.flags & MF_SOLID);
    };

    //
    // P_DamageMobj
    // Damages both enemies and players
    // "inflictor" is the thing that caused the damage
    //  creature or missile, can be NULL (slime, etc)
    // "source" is the thing to target after taking damage
    //  creature or NULL
    // Source and inflictor are the same for melee attacks.
    // Source can be NULL for slime, barrel explosions
    // and other environmental stuff.
    //
    default void DamageMobj(mobj_t target, mobj_t inflictor, mobj_t source, int damage) {
        final ActionsRegistry<T, V> obs = obs();
        long ang; // unsigned
        int saved;
        player_t player;
        @SourceCode.fixed_t int thrust;
        int temp;

        if (!eval(target.flags & MF_SHOOTABLE)) {
            return; // shouldn't happen...
        }
        if (target.health <= 0) {
            return;
        }

        if (eval(target.flags & MF_SKULLFLY)) {
            target.momx = target.momy = target.momz = 0;
        }

        player = target.player;
        if ((player != null) && obs.DOOM.gameskill == skill_t.sk_baby) {
            damage >>= 1;   // take half damage in trainer mode
        }

        // Some close combat weapons should not
        // inflict thrust and push the victim out of reach,
        // thus kick away unless using the chainsaw.
        if ((inflictor != null)
                && !eval(target.flags & MF_NOCLIP)
                && (source == null
                || source.player == null
                || source.player.readyweapon != weapontype_t.wp_chainsaw)) {
            ang = obs.DOOM.sceneRenderer.PointToAngle2(inflictor.x,
                    inflictor.y,
                    target.x,
                    target.y) & BITS32;

            thrust = damage * (MAPFRACUNIT >> 3) * 100 / target.info.mass;

            // make fall forwards sometimes
            if ((damage < 40)
                    && (damage > target.health)
                    && (target.z - inflictor.z > 64 * FRACUNIT)
                    && eval(obs.DOOM.random.P_Random() & 1)) {
                ang += ANG180;
                thrust *= 4;
            }

            //ang >>= ANGLETOFINESHIFT;
            target.momx += FixedMul(thrust, finecosine(ang));
            target.momy += FixedMul(thrust, finesine(ang));
        }

        // player specific
        if (player != null) {
            // end of game hell hack
            if (target.subsector.sector.special == 11
                    && damage >= target.health) {
                damage = target.health - 1;
            }

            // Below certain threshold,
            // ignore damage in GOD mode, or with INVUL power.
            if (damage < 1000
                    && (eval(player.cheats & player_t.CF_GODMODE))
                    || player.powers[pw_invulnerability] != 0) {
                return;
            }

            if (player.armortype != 0) {
                if (player.armortype == 1) {
                    saved = damage / 3;
                } else {
                    saved = damage / 2;
                }

                if (player.armorpoints[0] <= saved) {
                    // armor is used up
                    saved = player.armorpoints[0];
                    player.armortype = 0;
                }
                player.armorpoints[0] -= saved;
                damage -= saved;
            }
            player.health[0] -= damage;   // mirror mobj health here for Dave
            if (player.health[0] < 0) {
                player.health[0] = 0;
            }

            player.attacker = source;
            player.damagecount += damage;  // add damage after armor / invuln

            if (player.damagecount > 100) {
                player.damagecount = 100;  // teleport stomp does 10k points...
            }
            temp = damage < 100 ? damage : 100;

            if (player == obs.DOOM.players[obs.DOOM.consoleplayer]) {
                obs.DOOM.doomSystem.Tactile(40, 10, 40 + temp * 2);
            }
        }

        // do the damage    
        target.health -= damage;
        if (target.health <= 0) {
            this.KillMobj(source, target);
            return;
        }

        if ((obs.DOOM.random.P_Random() < target.info.painchance)
                && !eval(target.flags & MF_SKULLFLY)) {
            target.flags |= MF_JUSTHIT;    // fight back!

            target.SetMobjState(target.info.painstate);
        }

        target.reactiontime = 0;       // we're awake now...   

        if (((target.threshold == 0) || (target.type == mobjtype_t.MT_VILE))
                && (source != null) && (source != target)
                && (source.type != mobjtype_t.MT_VILE)) {
            // if not intent on another player,
            // chase after this one
            target.target = source;
            target.threshold = BASETHRESHOLD;
            if (target.mobj_state == states[target.info.spawnstate.ordinal()]
                    && target.info.seestate != statenum_t.S_NULL) {
                target.SetMobjState(target.info.seestate);
            }
        }

    }

    //
    // KillMobj
    //
    default void KillMobj(mobj_t source, mobj_t target) {
        final ActionsRegistry<T, V> obs = obs();
        mobjtype_t item;
        mobj_t mo;

        // Maes: this seems necessary in order for barrel damage
        // to propagate inflictors.
        target.target = source;

        target.flags &= ~(MF_SHOOTABLE | MF_FLOAT | MF_SKULLFLY);

        if (target.type != mobjtype_t.MT_SKULL) {
            target.flags &= ~MF_NOGRAVITY;
        }

        target.flags |= MF_CORPSE | MF_DROPOFF;
        target.height >>= 2;

        if (source != null && source.player != null) {
            // count for intermission
            if ((target.flags & MF_COUNTKILL) != 0) {
                source.player.killcount++;
            }

            if (target.player != null) //; <-- _D_: that semicolon caused a bug!
            {
                source.player.frags[target.player.identify()]++;
            }
            // It's probably intended to increment the frags of source player vs target player. Lookup? 
        } else if (!obs.DOOM.netgame && ((target.flags & MF_COUNTKILL) != 0)) {
            // count all monster deaths,
            // even those caused by other monsters
            obs.DOOM.players[0].killcount++;
        }

        if (target.player != null) {
            // count environment kills against you
            if (source == null) // TODO: some way to indentify which one of the 
            // four possiblelayers is the current player
            {
                target.player.frags[target.player.identify()]++;
            }

            target.flags &= ~MF_SOLID;
            target.player.playerstate = PST_DEAD;
            target.player.DropWeapon(); // in PSPR

            if (target.player == obs.DOOM.players[obs.DOOM.consoleplayer]
                    && obs.DOOM.automapactive) {
                // don't die in auto map,
                // switch view prior to dying
                obs.DOOM.autoMap.Stop();
            }

        }

        if (target.health < -target.info.spawnhealth
                && target.info.xdeathstate != statenum_t.S_NULL) {
            target.SetMobjState(target.info.xdeathstate);
        } else {
            target.SetMobjState(target.info.deathstate);
        }
        target.mobj_tics -= obs.DOOM.random.P_Random() & 3;

        if (target.mobj_tics < 1) {
            target.mobj_tics = 1;
        }

        //  I_StartSound (&actor.r, actor.info.deathsound);
        // Drop stuff.
        // This determines the kind of object spawned
        // during the death frame of a thing.
        switch (target.type) {
            case MT_WOLFSS:
            case MT_POSSESSED:
                item = mobjtype_t.MT_CLIP;
                break;

            case MT_SHOTGUY:
                item = mobjtype_t.MT_SHOTGUN;
                break;

            case MT_CHAINGUY:
                item = mobjtype_t.MT_CHAINGUN;
                break;

            default:
                return;
        }

        mo = this.SpawnMobj(target.x, target.y, ONFLOORZ, item);
        mo.flags |= MF_DROPPED;    // special versions of items
    }

    /**
     * PIT_StompThing
     */
    @SourceCode.P_Map.C(PIT_StompThing) default boolean StompThing(mobj_t thing) {
        final ActionsRegistry<T, V> obs = obs();
        int blockdist;
        fixed_t: {
            blockdist:;
        }

        if ((thing.flags & MF_SHOOTABLE) == 0) {
            return true;
        }

        blockdist = thing.radius + obs.tmthing.radius;

        if (Math.abs(thing.x - obs.tmx) >= blockdist
                || Math.abs(thing.y - obs.tmy) >= blockdist) {
            // didn't hit it
            return true;
        }

        // don't clip against self
        if (thing == obs.tmthing) {
            return true;
        }

        // monsters don't stomp things except on boss level
        if ((obs.tmthing.player == null) && (obs.DOOM.gamemap != 30)) {
            return false;
        }

        this.DamageMobj(thing, obs.tmthing, obs.tmthing, 10000); // in interaction

        return true;
    };

}
