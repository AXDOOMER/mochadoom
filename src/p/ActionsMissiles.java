package p;

import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import data.mobjtype_t;
import doom.SourceCode;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static p.MapUtils.AproxDistance;
import static p.mobj_t.MF_SHADOW;
import static utils.C2JUtils.eval;

public interface ActionsMissiles<T, V> extends ActionsAim<T, V> {

    /**
     * P_SpawnMissile
     */
    default mobj_t SpawnMissile(mobj_t source, mobj_t dest, mobjtype_t type) {
        final ActionsRegistry<T, V> obs = obs();
        mobj_t th;
        @SourceCode.angle_t long an;
        int dist;

        th = this.SpawnMobj(source.x,
                source.y,
                source.z + 4 * 8 * FRACUNIT, type);

        if (th.info.seesound != null) {
            obs.DOOM.doomSound.StartSound(th, th.info.seesound);
        }

        th.target = source;    // where it came from
        an = obs.DOOM.sceneRenderer.PointToAngle2(source.x, source.y, dest.x, dest.y) & BITS32;

        // fuzzy player
        if (eval(dest.flags & MF_SHADOW)) {
            an += (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 20;
        }

        th.angle = an & BITS32;
        //an >>= ANGLETOFINESHIFT;
        th.momx = FixedMul(th.info.speed, finecosine(an));
        th.momy = FixedMul(th.info.speed, finesine(an));

        dist = AproxDistance(dest.x - source.x, dest.y - source.y);
        dist /= th.info.speed;

        if (dist < 1) {
            dist = 1;
        }

        th.momz = (dest.z - source.z) / dist;
        this.CheckMissileSpawn(th);

        return th;
    }

    /**
     * P_SpawnPlayerMissile Tries to aim at a nearby monster
     */
    default void SpawnPlayerMissile(mobj_t source, mobjtype_t type) {
        final ActionsRegistry<T, V> obs = obs();
        mobj_t th;
        @SourceCode.angle_t long an;
        int x, y, z, slope; // ActionFunction

        // see which target is to be aimed at
        an = source.angle;
        slope = this.AimLineAttack(source, an, 16 * 64 * FRACUNIT);

        if (obs.linetarget == null) {
            an += 1 << 26;
            an &= BITS32;
            slope = this.AimLineAttack(source, an, 16 * 64 * FRACUNIT);

            if (obs.linetarget == null) {
                an -= 2 << 26;
                an &= BITS32;
                slope = this.AimLineAttack(source, an, 16 * 64 * FRACUNIT);
            }

            if (obs.linetarget == null) {
                an = source.angle & BITS32;
                // angle should be "sane"..right?
                // Just this line allows freelook.
                slope = ((source.player.lookdir) << FRACBITS) / 173;
            }
        }

        x = source.x;
        y = source.y;
        z = source.z + 4 * 8 * FRACUNIT + slope;

        th = this.SpawnMobj(x, y, z, type);

        if (th.info.seesound != null) {
            obs.DOOM.doomSound.StartSound(th, th.info.seesound);
        }

        th.target = source;
        th.angle = an;
        th.momx = FixedMul(th.info.speed, finecosine(an));
        th.momy = FixedMul(th.info.speed, finesine(an));
        th.momz = FixedMul(th.info.speed, slope);

        this.CheckMissileSpawn(th);
    }

    
}
