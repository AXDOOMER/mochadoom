package doom;

import n.DoomSystemNetworking;
import f.EndLevel;
import f.Finale;
import f.Wiper;
import hu.HU;
import i.IDiskDrawer;
import i.IDoomSystem;
import i.DoomVideoInterface;
import m.IDoomMenu;
import m.IRandom;
import m.IVariablesManager;
import p.AbstractLevelLoader;
import p.Actions;
import rr.ISpriteManager;
import rr.Renderer;
import rr.TextureManager;
import s.IDoomSound;
import s.IMusic;
import s.ISoundDriver;
import st.AbstractStatusBar;
import timing.ITicker;
import v.DoomVideoRenderer;
import w.IWadLoader;
import automap.IAutoMap;

/** Since a lot of stuff requires shared/global access to 
 *  the WadLoader, the Renderer, the Video system etc. and
 *  we're trying to depart from the global/static mentality,
 *  a common sharing is required. Ideally, this would be a perfect
 *  example of where multiple inheritance could be adopted, since most
 *  stuff needs to share this status anyway. The next best thing is
 *  to have local references of any used fields in the classes that use them.
 * 
 *  About generics: T refers to the type of the graphics resources, and is
 *  currently byte[], as all graphics resources are 8-bit indexed. There are
 *  no plans that this will change anytime soon. Some classes should allow 
 *  different types in theory, but it would be too complex and pointless to 
 *  make everything fully compliant at the moment.
 *  
 *  V refers to the type of DISPLAY, and can be 8-bit (byte[]), 16-bit (short[] 
 *  for HiColor and lesser modes such as ARGB4444, etc.), and, in the future, 
 *  int[] (truecolor).
 * 
 *  The general approach is sharing as much code as possible between different 
 *  implementations (e.g. rendering code), and only specialize methods/classes when
 *  the abstraction of generics isn't enough (typically, when you have to assign
 *  directly to primitive arrays or deal with primitive method signatures).
 *  
 *  Classes that have specialized code for indexed and hicolor modes should be top-level
 *  classes in their package, and contain two nested, static, extending classes called
 *  Indexed and HiColor e.g. new MyClass.Indexed() and new MyClass.HiColor(), while any common 
 *  code should reside in MyClass.
 * 
 * @author velktron
 *
 */

public class DoomContext<T,V>{
	
    public DoomMain<T,V> DM;
    public IDoomGame DG;
    public IWadLoader W;
    public IRandom RND;
    public IDoomSystem I;
    public IDoomSound S;
    public ISoundDriver ISND;
    public IMusic IMUS;
    public DoomVideoInterface<V> VI;
    public AbstractStatusBar ST;
    public DoomVideoRenderer<T,V> V;
    public DoomSystemNetworking DNI;
    public IDoomGameNetworking DGN;
    public AbstractLevelLoader LL;
    public IDoomMenu M;
    public Actions P;
    public Renderer<T,V> R;
    public HU HU;
    public IAutoMap<T,V> AM;
    public Finale<T> F;
    public EndLevel<T,V> WI;
    public Wiper<T,V> WIPE;
    public TextureManager<T> TM;
    public ISpriteManager SM;
    public ICommandLineManager CM;
    public ITicker TICK; 
    public IDiskDrawer DD;
    public IVariablesManager VM;
}
