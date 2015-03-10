package i;

import doom.DoomStatus;
import rr.patch_t;
import v.DoomVideoRenderer;
import v.IVideoScale;
import w.IWadLoader;

public class DiskDrawer implements IDiskDrawer,DoomStatusAware {

	private patch_t disk;
	private IWadLoader W;
	private DoomVideoRenderer<?,?> V;
	private IVideoScale VS;
	private int timer=0;
	private String diskname;
	
	public static final String STDISK="STDISK";
	public static final String STCDROM="STCDROM";
	
	public DiskDrawer(DoomStatus<?,?> DM,String icon){		
		this.updateStatus(DM);
		this.diskname=icon;
	}

	@Override
	public void Init(){
		this.disk=W.CachePatchName(diskname);
	}
	
	@Override
	public void Drawer() {
		if (timer>0){
			if (timer%2==0)
		V.DrawScaledPatch(304,184,DoomVideoRenderer.SCREEN_FG,VS, disk);
		}
		if (timer>=0)
			timer--;
	}

	@Override
	public void updateStatus(DoomStatus<?,?> DC) {
		this.W=DC.W;
		this.V=DC.V;		
	    }

	@Override
	public void setVideoScale(IVideoScale vs) {
		this.VS = vs;
	}

	@Override
	public void initScaling() {

	}

	@Override
	public void setReading(int reading) {
		timer=reading;
	}

	@Override
	public boolean isReading() {
		return timer>0;
	}

	@Override
	public boolean justDoneReading() {
		return timer==0;
	}
	
}
