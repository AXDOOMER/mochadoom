package m;

import doom.DoomStatus;
import doom.IDoomGame;
import hu.HU;
import i.DoomStatusAware;
import i.IDoomSystem;
import rr.RendererState;
import s.IDoomSound;
import timing.ITicker;
import v.DoomVideoRenderer;
import v.IVideoScaleAware;
import w.IWadLoader;

public abstract class AbstractDoomMenu
        implements IDoomMenu {

    ////////////////////// CONTEXT ///////////////////
    
    DoomStatus DM;
    IDoomGame DG;
    IWadLoader W;
    DoomVideoRenderer V;
    HU HU;
    RendererState R;
    IDoomSystem I;
    IDoomSound S;
    ITicker TICK;
    
    @Override
    public void updateStatus(DoomStatus DS) {
           this.DM=DS.DM;
           this.DG=DS.DG;
            this.V=DM.V;
            this.W=DM.W;
            this.HU=DM.HU;
            this.I=DM.I;
            this.S=DM.S;
            this.R=(RendererState) DM.R;
            this.TICK=DM.TICK;
        
    }
    
}
