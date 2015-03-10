package v;

public interface IVideoScaleAware {    
    
    
    /** Set the video scale for a certain object. Setting
     * does NOT (re)initialize an object yet. This is only done
     * by calling the init() method at a safe moment.
     * 
     * @param vs
     */
    public void setVideoScale(IVideoScale vs);
    
    /** Initialize an object according to the current video scale
     * settings. This should adapt multipliers, static constants,
     * etc. and should be set before the object is first used
     * or after a dynamic (if ever implemented) resolution change.
     * 
     * The proposed method is to initialize everything en-bloc
     * before entering the display loop, and after initializing
     * 
     */    
    public void initScaling();

}
