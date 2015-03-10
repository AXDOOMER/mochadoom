package rr.parallel;

import static data.Limits.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import rr.patch_t;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.DoomStatus;
import doom.player_t;

/** This is a second attempt at building a seg-focused parallel renderer, instead of
 * column-based. It does function, but is broken and has unsolved data dependencies.
 * It's therefore not used in official releases, and I chose to deprecate it.
 * If you still want to develop it, be my guest.
 * 
 * @author velktron
 *
 */

public class ParallelRenderer2 extends AbstractParallelRenderer {
    
    public ParallelRenderer2(DoomStatus DS, int wallthread, int floorthreads,int nummaskedthreads) {

        super(DS,wallthread,floorthreads,nummaskedthreads);
        System.out.println("Parallel Renderer 2 (Seg-based)");
        
        this.MySegs=new ParallelSegs2();
        this.MyPlanes=new ParallelPlanes();
        this.MyThings=new ParallelThings2(); 

    }

	public ParallelRenderer2(DoomStatus DS, int wallthread, int floorthreads) {

		super(DS,wallthread,floorthreads);
		System.out.println("Parallel Renderer 2 (Seg-based)");
		
		this.MySegs=new ParallelSegs2();
		this.MyPlanes=new ParallelPlanes();
		this.MyThings=new ParallelThings2(); 

	}

	/** Default constructor, 2 wall threads and one floor thread.
	 * 
	 * @param DM
	 */
	public ParallelRenderer2(DoomMain DM) {
		this(DM,2,1);
	}

	@Override
	protected void InitParallelStuff() {
		// Prepare parallel stuff
		RSIExec=new RenderSegExecutor[NUMWALLTHREADS];
		tp=   Executors.newFixedThreadPool(NUMWALLTHREADS+NUMFLOORTHREADS);
		// Prepare the barrier for MAXTHREADS + main thread.
		//wallbarrier=new CyclicBarrier(NUMWALLTHREADS+1);
		visplanebarrier=new CyclicBarrier(NUMFLOORTHREADS+NUMWALLTHREADS+1);

		vpw=new VisplaneWorker[NUMFLOORTHREADS];
		
		// Uses "seg" parallel drawer, so RSI.
        InitRSISubsystem();
        
        // Masked workers.
        maskedworkers=new MaskedWorker[NUMMASKEDTHREADS];

        maskedbarrier =
            new CyclicBarrier(NUMMASKEDTHREADS + 1);
        
        InitMaskedWorkers();
        
        // If using masked threads, set these too.
        
        smp_composite=new boolean[NUMMASKEDTHREADS];// = false;
        smp_lasttex=new int[NUMMASKEDTHREADS];// = -1;
        smp_lastlump=new int[NUMMASKEDTHREADS];// = -1;
        smp_lastpatch=new patch_t[NUMMASKEDTHREADS];// = null;
        
	}

	///////////////////////// The actual rendering calls ///////////////////////

	/**
	 * R_RenderView
	 * 
	 * As you can guess, this renders the player view of a particular player object.
	 * In practice, it could render the view of any mobj too, provided you adapt the
	 * SetupFrame method (where the viewing variables are set).
	 * 
	 */

	public void RenderPlayerView (player_t player)
	{   

		// Viewing variables are set according to the player's mobj. Interesting hacks like
		// free cameras or monster views can be done.
		SetupFrame (player);



		/* Uncommenting this will result in a very existential experience
  if (Math.random()>0.999){
	  thinker_t shit=P.getRandomThinker();
	  try {
	  mobj_t crap=(mobj_t)shit;
	  player.mo=crap;
	  } catch (ClassCastException e){

	  }
  	}*/

		// Clear buffers. 
		MyBSP.ClearClipSegs ();
		MyBSP.ClearDrawSegs ();
		MyPlanes.ClearPlanes ();
		MySegs.ClearClips();
		VIS.ClearSprites ();

		// Check for new console commands.
		DGN.NetUpdate ();

		// The head node is the last node output.
		MyBSP.RenderBSPNode (LL.numnodes-1);
		
		RenderRSIPipeline();
		// Check for new console commands.
		DGN.NetUpdate ();

		// "Warped floor" fixed, same-height visplane merging fixed.
		MyPlanes.DrawPlanes ();

		try {
			visplanebarrier.await();
		} catch (Exception e){
			e.printStackTrace();
		}



		// Check for new console commands.
		DGN.NetUpdate ();

		MyThings.DrawMasked ();

		colfunc=basecolfunc;

		// Check for new console commands.
		DGN.NetUpdate ();           
	}

	/**
	 * R_Init
	 */

	//public int  detailLevel;
	//public int  screenblocks=9; // has defa7ult

	public void Init ()

	{
		// Any good reason for this to be here?
		//drawsegs=new drawseg_t[MAXDRAWSEGS];
		//C2JUtils.initArrayOfObjects(drawsegs);

		// DON'T FORGET ABOUT MEEEEEE!!!11!!!
		this.screen=V.getScreen(DoomVideoRenderer.SCREEN_FG);

		System.out.print("\nR_InitData");
		InitData ();
		//InitPointToAngle ();
		System.out.print("\nR_InitPointToAngle");

		// ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
		System.out.print ("\nR_InitTables");
		InitTables ();

		SetViewSize (DM.M.getScreenBlocks(), DM.M.getDetailLevel());

		System.out.print ("\nR_InitPlanes");
		MyPlanes.InitPlanes ();

		System.out.print("\nR_InitLightTables");
		InitLightTables ();

		System.out.print("\nR_InitSkyMap: "+TexMan.InitSkyMap ());

		System.out.print("\nR_InitTranslationsTables");
		InitTranslationTables ();
		
        System.out.print("\nR_InitParallelStuff: ");
        InitParallelStuff();

		System.out.print("\nR_InitTranMap: ");
		R_InitTranMap(0);
		
		System.out.print("\nR_InitDrawingFunctions: ");
        R_InitDrawingFunctions();       		

		framecount = 0;
	}

	@Override
	public void initScaling(){
		super.initScaling();
		this.RSI=new RenderSegInstruction[MAXSEGS*3];
		C2JUtils.initArrayOfObjects(RSI);
		}


}
