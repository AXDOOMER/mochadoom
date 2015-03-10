package m;

public interface IUseVariables {
    
    
    /** Register a variable manager with this module.
     * 
     * @param manager
     */
    public void registerVariableManager(IVariablesManager manager);
    
    
    /** Apply listener-specific variables, asking the manager for them.
     *  Every listener should concern itself with its own variables/settings.
     *  
     *  This method should be called by the manager on every registered
     *  listener. Each listener then "knows" which settings it must update.
     *  
     *  Good for block updates, but maybe a more lightweight mechanism should
     *  be provided, e.g. being able to update just one setting for a listener.
     * 
     */
    public void update();
    
    /** If the variables user makes too many changes, it may be better to 
     * communicate them back to the manager in-block. This shouldn't be needed,
     * if everywhere a certain setting has to be modified tis done through the
     * manager.
     */
    
    public void commit();


}
