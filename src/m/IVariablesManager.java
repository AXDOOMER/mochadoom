package m;

public interface IVariablesManager {
    
    /** Does two things: sets a variable, and then calls setme.
     * This way, immediate updates of a single variables user 
     * are possible, avoiding some of the overhead. 
     * 
     * Sadly, users with too many variables will still incur an
     * O(n) penalty.
     * 
     * @param setme
     * @param name
     * @param value
     */
    
    public void applySetting(IUseVariables setme,String name, String value);

    public DoomSetting getSetting(String name);
    
    public DoomSetting getSetting(Settings name);

    public boolean isSettingLiteral(String name,String value);
    
 /** Creates a new setting, overriding any existing ones.
  *   
  * @param name
  * @param value
  * @param persist
  */
    
    public void putSetting(String name, Object value, boolean persist);
    
    /** Puts a new setting or updates an existing one. In this case,
     * the value of the "persist" field is kept unaltered, so that persistent
     * settings are not lost during updates. 
     * 
     * @param name
     * @param value
     */
    
    public void putSetting(String name, Object value);
    public void putSetting(Settings name, Object value);

    void LoadDefaults(String defaultfile);

    void SaveDefaults(String defaultfile);
    
    String getDefaultFile();
    
}
