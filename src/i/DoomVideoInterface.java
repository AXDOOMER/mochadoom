package i;

/** Methods specific to Doom-System video interfacing. 
 *  In essence, whatever you are using as a final system-specific way to display
 *  the screens, should be able to respond to these commands. In particular,
 *  screen update requests must be honored, and palette/gamma request changes
 *  must be intercepted before they are forwarded to the renderers (in case they
 *  are system-specific, rather than renderer-specific).
 *  
 *  The idea is that the final screen rendering module sees/handles as less as
 *  possible, and only gets a screen to render, no matter what depth it is.
 * 
 *  
 */

public interface DoomVideoInterface<K> {

	public void StartFrame();

	public void StartTic();

	public void SetPalette(int palette);

	public void InitGraphics();

	public void FinishUpdate();

	public void UpdateNoBlit();

	public void ShutdownGraphics();

	public void SetGamma(int gammalevel);
	
	public void ReadScreen(K linear);

}
