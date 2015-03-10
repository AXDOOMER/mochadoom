package rr;

import static data.Defines.FF_FRAMEMASK;
import static data.Defines.FF_FULLBRIGHT;
import static data.Limits.MAXVISSPRITES;
import static data.Tables.ANG45;
import static data.Tables.BITS32;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.mobj_t.MF_SHADOW;
import static rr.LightsAndColors.*;
import static rr.Renderer.MINZ;

import i.IDoomSystem;

import java.util.Arrays;

import p.mobj_t;
import utils.C2JUtils;

/** Visualized sprite manager. Depends on: SpriteManager, DoomSystem,
 *  Colormaps, Current View.
 *  
 * @author velktron
 *
 * @param <V>
 */

public final class VisSprites<V>
        implements IVisSpriteManagement<V> {

    private final static boolean DEBUG = false;

    private final static boolean RANGECHECK = false;

    protected IDoomSystem I;

    protected ISpriteManager SM;

    protected ViewVars view;

    protected LightsAndColors<V> colormaps;

    protected RendererState<?, V> R;

    public VisSprites(RendererState<?, V> R) {
        updateStatus(R);
        vissprite_t<V> tmp = new vissprite_t<V>();
        vissprites = C2JUtils.createArrayOfObjects(tmp, MAXVISSPRITES);
    }

    public void updateStatus(RendererState<?, V> R) {
        this.R = R;
        this.view = R.view;
        this.I = R.I;
        this.SM = R.SM;
        this.colormaps = R.colormaps;

    }

    protected vissprite_t<V>[] vissprites;

    protected int vissprite_p;

    protected int newvissprite;

    // UNUSED
    // private final vissprite_t unsorted;
    // private final vissprite_t vsprsortedhead;

    // Cache those you get from the sprite manager
    protected int[] spritewidth, spriteoffset, spritetopoffset;

    /**
     * R_AddSprites During BSP traversal, this adds sprites by sector.
     */

    @Override
    public void AddSprites(sector_t sec) {
        if (DEBUG)
            System.out.println("AddSprites");
        mobj_t thing;
        int lightnum;

        // BSP is traversed by subsector.
        // A sector might have been split into several
        // subsectors during BSP building.
        // Thus we check whether its already added.
        if (sec.validcount == R.getValidCount())
            return;

        // Well, now it will be done.
        sec.validcount = R.getValidCount();

        lightnum = (sec.lightlevel >> LIGHTSEGSHIFT) + colormaps.extralight;

        if (lightnum < 0)
            colormaps.spritelights = colormaps.scalelight[0];
        else if (lightnum >= LIGHTLEVELS)
            colormaps.spritelights = colormaps.scalelight[LIGHTLEVELS - 1];
        else
            colormaps.spritelights = colormaps.scalelight[lightnum];

        // Handle all things in sector.
        for (thing = sec.thinglist; thing != null; thing = (mobj_t) thing.snext)
            ProjectSprite(thing);
    }

    /**
     * R_ProjectSprite Generates a vissprite for a thing if it might be visible.
     * 
     * @param thing
     */
    protected final void ProjectSprite(mobj_t thing) {
        int tr_x, tr_y;
        int gxt, gyt;
        int tx, tz;

        int xscale, x1, x2;

        spritedef_t sprdef;
        spriteframe_t sprframe;
        int lump;

        int rot;
        boolean flip;

        int index;

        vissprite_t<V> vis;

        long ang;
        int iscale;

        // transform the origin point
        tr_x = thing.x - view.x;
        tr_y = thing.y - view.y;

        gxt = FixedMul(tr_x, view.cos);
        gyt = -FixedMul(tr_y, view.sin);

        tz = gxt - gyt;

        // thing is behind view plane?
        if (tz < MINZ)
            return;
        /* MAES: so projection/tz gives horizontal scale */
        xscale = FixedDiv(view.projection, tz);

        gxt = -FixedMul(tr_x, view.sin);
        gyt = FixedMul(tr_y, view.cos);
        tx = -(gyt + gxt);

        // too far off the side?
        if (Math.abs(tx) > (tz << 2))
            return;

        // decide which patch to use for sprite relative to player
        if (RANGECHECK) {
            if (thing.sprite.ordinal() >= SM.getNumSprites())
                I.Error("R_ProjectSprite: invalid sprite number %d ",
                    thing.sprite);
        }
        sprdef = SM.getSprite(thing.sprite.ordinal());
        if (RANGECHECK) {
            if ((thing.frame & FF_FRAMEMASK) >= sprdef.numframes)
                I.Error("R_ProjectSprite: invalid sprite frame %d : %d ",
                    thing.sprite, thing.frame);
        }
        sprframe = sprdef.spriteframes[thing.frame & FF_FRAMEMASK];

        if (sprframe.rotate != 0) {
            // choose a different rotation based on player view
            ang = view.PointToAngle(thing.x, thing.y);
            rot = (int) ((ang - thing.angle + (ANG45 * 9) / 2) & BITS32) >>> 29;
            lump = sprframe.lump[rot];
            flip = (boolean) (sprframe.flip[rot] != 0);
        } else {
            // use single rotation for all views
            lump = sprframe.lump[0];
            flip = (boolean) (sprframe.flip[0] != 0);
        }

        // calculate edges of the shape
        tx -= spriteoffset[lump];
        x1 = (view.centerxfrac + FixedMul(tx, xscale)) >> FRACBITS;

        // off the right side?
        if (x1 > view.width)
            return;

        tx += spritewidth[lump];
        x2 = ((view.centerxfrac + FixedMul(tx, xscale)) >> FRACBITS) - 1;

        // off the left side
        if (x2 < 0)
            return;

        // store information in a vissprite
        vis = NewVisSprite();
        vis.mobjflags = thing.flags;
        vis.scale = xscale << view.detailshift;
        vis.gx = thing.x;
        vis.gy = thing.y;
        vis.gz = thing.z;
        vis.gzt = thing.z + spritetopoffset[lump];
        vis.texturemid = vis.gzt - view.z;
        vis.x1 = x1 < 0 ? 0 : x1;
        vis.x2 = x2 >= view.width ? view.width - 1 : x2;
        /*
         * This actually determines the general sprite scale) iscale = 1/xscale,
         * if this was floating point.
         */
        iscale = FixedDiv(FRACUNIT, xscale);

        if (flip) {
            vis.startfrac = spritewidth[lump] - 1;
            vis.xiscale = -iscale;
        } else {
            vis.startfrac = 0;
            vis.xiscale = iscale;
        }

        if (vis.x1 > x1)
            vis.startfrac += vis.xiscale * (vis.x1 - x1);
        vis.patch = lump;

        // get light level
        if ((thing.flags & MF_SHADOW) != 0) {
            // shadow draw
            vis.colormap = null;
        } else if (colormaps.fixedcolormap != null) {
            // fixed map
            vis.colormap = (V) colormaps.fixedcolormap;
            // vis.pcolormap=0;
        } else if ((thing.frame & FF_FULLBRIGHT) != 0) {
            // full bright
            vis.colormap = (V) colormaps.colormaps[0];
            // vis.pcolormap=0;
        }

        else {
            // diminished light
            index = xscale >> (LIGHTSCALESHIFT - view.detailshift);

            if (index >= MAXLIGHTSCALE)
                index = MAXLIGHTSCALE - 1;

            vis.colormap = colormaps.spritelights[index];
            // vis.pcolormap=index;
        }
    }

    /**
     * R_NewVisSprite Returns either a "new" sprite (actually, reuses a pool),
     * or a special "overflow sprite" which just gets overwritten with bogus
     * data. It's a bit of dumb thing to do, since the overflow sprite is never
     * rendered but we have to copy data over it anyway. Would make more sense
     * to check for it specifically and avoiding copying data, which should be
     * more time consuming. Fixed by making this fully limit-removing.
     * 
     * @return
     */
    protected final vissprite_t<V> NewVisSprite() {
        if (vissprite_p == (vissprites.length - 1)) {
            ResizeSprites();
        }
        // return overflowsprite;

        vissprite_p++;
        return vissprites[vissprite_p - 1];
    }

    @Override
    public void cacheSpriteManager(ISpriteManager SM) {
        this.spritewidth = SM.getSpriteWidth();
        this.spriteoffset = SM.getSpriteOffset();
        this.spritetopoffset = SM.getSpriteTopOffset();
    }

    /**
     * R_ClearSprites Called at frame start.
     */

    @Override
    public void ClearSprites() {
        // vissprite_p = vissprites;
        vissprite_p = 0;
    }

    // UNUSED private final vissprite_t overflowsprite = new vissprite_t();

    protected final void ResizeSprites() {
        vissprites =
            C2JUtils.resize(vissprites[0], vissprites, vissprites.length * 2); // Bye
                                                                               // bye,
                                                                               // old
                                                                               // vissprites.
    }

    /**
     * R_SortVisSprites UNUSED more efficient Comparable sorting + built-in
     * Arrays.sort function used.
     */

    @Override
    public final void SortVisSprites() {
        Arrays.sort(vissprites, 0, vissprite_p);

        // Maes: got rid of old vissprite sorting code. Java's is better
        // Hell, almost anything was better than that.

    }

    @Override
    public int getNumVisSprites() {
        return vissprite_p;
    }

    @Override
    public vissprite_t<V>[] getVisSprites() {
        return vissprites;
    }

    public void resetLimits() {
        vissprite_t<V>[] tmp =
            C2JUtils.createArrayOfObjects(vissprites[0], MAXVISSPRITES);
        System.arraycopy(vissprites, 0, tmp, 0, MAXVISSPRITES);

        // Now, that was quite a haircut!.
        vissprites = tmp;    }
}