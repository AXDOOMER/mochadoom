package f;

import doom.DoomStatus;
import m.IRandom;
import v.DoomVideoRenderer;
import v.IVideoScale;
import v.IVideoScaleAware;
import i.DoomStatusAware;
import i.DoomVideoInterface;

public abstract class AbstractWiper<T,V> implements IWiper, DoomStatusAware, IVideoScaleAware{
    
    //
    //                           SCREEN WIPE PACKAGE
    //

    /** These don't seem to be used anywhere */
    
    public static enum wipe
    {
        // simple gradual pixel change for 8-bit only
        // MAES: this transition isn't guaranteed to always terminate
        // see Chocolate Strife develpment. Unused in Doom anyway.
        ColorXForm,
        
        // weird screen melt
        Melt,  

        NUMWIPES
    };
    
    /** when false, stop the wipe */
    protected volatile boolean  go = false;

    protected V    wipe_scr_start;
    protected V    wipe_scr_end;
    protected V    wipe_scr;
    
////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

    protected int SCREENWIDTH;
    protected int SCREENHEIGHT;
    protected int Y_SCALE;
    protected IVideoScale vs;


    @Override
    public void setVideoScale(IVideoScale vs) {
        this.vs=vs;
    }

    @Override
    public void initScaling() {
        this.SCREENHEIGHT=vs.getScreenHeight();
        this.SCREENWIDTH=vs.getScreenWidth();
        this.Y_SCALE=vs.getScalingY();

        // Pre-scale stuff.
    }

    ///////////////////// STATUS /////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public void updateStatus(DoomStatus<?,?> DS){
    this.RND=DS.RND;
    this.V=(DoomVideoRenderer<T, V>) DS.V;
    this.VI=(DoomVideoInterface<V>) DS.VI;
    }
    
    IRandom RND;
    DoomVideoRenderer<T,V> V;
    DoomVideoInterface<V> VI;

    
}
