package st;

import m.IRandom;
import doom.DoomMain;
import doom.DoomStatus;
import i.DoomVideoInterface;
import i.IDoomSystem;
import rr.Renderer;
import s.IDoomSound;
import v.DoomVideoRenderer;
import v.IVideoScaleAware;
import w.IWadLoader;

public abstract class AbstractStatusBar implements IDoomStatusBar, IVideoScaleAware{

	 // /// STATUS //////////

    protected DoomVideoRenderer<?,?> V;

    protected IWadLoader W;

    protected Renderer<?,?> R;

    protected DoomMain<?,?> DM;

    protected IRandom RND;
    
    protected IDoomSystem I;
    
    protected DoomVideoInterface<?> VI;

    protected IDoomSound S;
    
	@Override
	public void updateStatus(DoomStatus<?,?> DC) {
        this.DM=DC.DM;
        this.V=DC.V;
        this.W=DC.W;
        this.RND=DC.RND;
        this.R= DC.R;
        this.VI=DC.VI;
        this.I=DC.I;
        this.S=DC.S;
    }
	
}
