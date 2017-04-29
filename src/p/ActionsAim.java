package p;

import static data.Defines.PT_ADDLINES;
import static data.Defines.PT_ADDTHINGS;
import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import doom.SourceCode;
import static doom.SourceCode.P_Map.PTR_AimTraverse;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.mobj_t.MF_SHOOTABLE;
import rr.line_t;
import static rr.line_t.ML_TWOSIDED;
import static utils.C2JUtils.eval;

public interface ActionsAim<T, V> extends ActionsMovement<T, V> {
    /**
     * P_CheckMissileSpawn Moves the missile forward a bit and possibly explodes it right there.
     *
     * @param th
     */
    default void CheckMissileSpawn(mobj_t th) {
        final ActionsRegistry<T, V> obs = obs();
        th.mobj_tics -= obs.DOOM.random.P_Random() & 3;
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1;
        }

        // move a little forward so an angle can
        // be computed if it immediately explodes
        th.x += (th.momx >> 1);
        th.y += (th.momy >> 1);
        th.z += (th.momz >> 1);

        if (!this.TryMove(th, th.x, th.y)) {
            obs.ExplodeMissile(th);
        }
    }
    
    /**
     * P_AimLineAttack
     *
     * @param t1
     * @param angle long
     * @param distance int
     */
    default int AimLineAttack(mobj_t t1, long angle, int distance) {
        final ActionsRegistry<T, V> obs = obs();
        int x2, y2;
        obs.shootthing = t1;

        x2 = t1.x + (distance >> FRACBITS) * finecosine(angle);
        y2 = t1.y + (distance >> FRACBITS) * finesine(angle);
        obs.shootz = t1.z + (t1.height >> 1) + 8 * FRACUNIT;

        // can't shoot outside view angles
        obs.topslope = 100 * FRACUNIT / 160;
        obs.bottomslope = -100 * FRACUNIT / 160;

        obs.attackrange = distance;
        obs.linetarget = null;

        this.PathTraverse(t1.x, t1.y, x2, y2, PT_ADDLINES | PT_ADDTHINGS, this::AimTraverse);

        if (obs.linetarget != null) {
            return obs.aimslope;
        }

        return 0;
    }

    //
    // P_BulletSlope
    // Sets a slope so a near miss is at aproximately
    // the height of the intended target
    //
    default void P_BulletSlope(mobj_t mo) {
        final ActionsRegistry<T, V> obs = obs();
        long an;

        // see which target is to be aimed at
        // FIXME: angle can already be negative here.
        // Not a problem if it's just moving about (accumulation will work)
        // but it needs to be sanitized before being used in any function.
        an = mo.angle;
        //_D_: &BITS32 will be used later in this function, by fine(co)sine()
        obs.bulletslope = this.AimLineAttack(mo, an/*&BITS32*/, 16 * 64 * FRACUNIT);

        if (!eval(obs.linetarget)) {
            an += 1 << 26;
            obs.bulletslope = this.AimLineAttack(mo, an/*&BITS32*/, 16 * 64 * FRACUNIT);
            if (!eval(obs.linetarget)) {
                an -= 2 << 26;
                obs.bulletslope = this.AimLineAttack(mo, an/*&BITS32*/, 16 * 64 * FRACUNIT);
            }

            // Give it one more try, with freelook
            if (mo.player.lookdir != 0 && !eval(obs.linetarget)) {
                an += 2 << 26;
                an &= BITS32;
                obs.bulletslope = (mo.player.lookdir << FRACBITS) / 173;
            }
        }
    }

    ////////////////// PTR Traverse Interception Functions ///////////////////////
    // Height if not aiming up or down
    // ???: use slope for monsters?

    @SourceCode.P_Map.C(PTR_AimTraverse) default boolean AimTraverse(intercept_t in) {
        final ActionsRegistry<T, V> obs = obs();
        line_t li;
        mobj_t th;
        int slope;
        int thingtopslope;
        int thingbottomslope;
        int dist;

        if (in.isaline) {
            li = (line_t) in.d();

            if (!eval(li.flags & ML_TWOSIDED)) {
                return false;       // stop
            }
            // Crosses a two sided line.
            // A two sided line will restrict
            // the possible target ranges.
            obs.LineOpening(li);

            if (obs.openbottom >= obs.opentop) {
                return false;       // stop
            }
            dist = FixedMul(obs.attackrange, in.frac);

            if (li.frontsector.floorheight != li.backsector.floorheight) {
                slope = FixedDiv(obs.openbottom - obs.shootz, dist);
                if (slope > obs.bottomslope) {
                    obs.bottomslope = slope;
                }
            }

            if (li.frontsector.ceilingheight != li.backsector.ceilingheight) {
                slope = FixedDiv(obs.opentop - obs.shootz, dist);
                if (slope < obs.topslope) {
                    obs.topslope = slope;
                }
            }
            
            // determine whether shot continues
            return obs.topslope > obs.bottomslope;            
        }

        // shoot a thing
        th = (mobj_t) in.d();
        if (th == obs.shootthing) {
            return true;            // can't shoot self
        }
        if (!eval(th.flags & MF_SHOOTABLE)) {
            return true;            // corpse or something
        }
        // check angles to see if the thing can be aimed at
        dist = FixedMul(obs.attackrange, in.frac);
        thingtopslope = FixedDiv(th.z + th.height - obs.shootz, dist);

        if (thingtopslope < obs.bottomslope) {
            return true;            // shot over the thing
        }
        thingbottomslope = FixedDiv(th.z - obs.shootz, dist);

        if (thingbottomslope > obs.topslope) {
            return true;            // shot under the thing
        }
        // this thing can be hit!
        if (thingtopslope > obs.topslope) {
            thingtopslope = obs.topslope;
        }

        if (thingbottomslope < obs.bottomslope) {
            thingbottomslope = obs.bottomslope;
        }

        obs.aimslope = (thingtopslope + thingbottomslope) / 2;
        obs.linetarget = th;

        return false;           // don't go any farther
    };

}
