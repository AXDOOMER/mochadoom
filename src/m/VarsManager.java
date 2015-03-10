package m;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import doom.ICommandLineManager;

import utils.C2JUtils;

/** Variables manager registers and retrieves variables (or "settings")
 *  For the purposes of Doom, three types will suffice: 
 *  boolean, integer (maybe float?), and string.
 *  
 *  Variables can be registered at startup, through *.cfg files,
 *  and through a (future) console. They can also be deleted 
 *  and updated.
 *  
 *  The variables manager just has the job of managing them. 
 *  Applying/making use of the variables themselves is the job 
 *  of the code that specifically uses them. 
 *  
 *  Proposed way of proceeding: making an "IUseVariables" interface,
 *  which is module-specific, and is called whenever new settings must
 *  be applied.
 *  
 *  E.g. stuff is read from config.cfg, and the the updateSettings()
 *  method of e.g. DoomMain is called, applying whatever changes need 
 *  to be done. Same thing with menus.
 *  
 *  TODO: a similar system was being planned for the CommandLine manager,
 *  but never implemented, wtf.... fix now?
 *  
 */

public class VarsManager implements IVariablesManager {
    
    private final HashMap<String,DoomSetting> settings;
    private final ICommandLineManager CLM;
    
    public VarsManager(ICommandLineManager CLM){
        this.settings=new HashMap<String,DoomSetting>();
        this.CLM=CLM;
    }
    
    @Override
    public DoomSetting getSetting(String name){
        
        DoomSetting tmp=settings.get(name);
        if (tmp!=null) {
            return tmp;
        }
        
        // Good question...what to do here?
        return DoomSetting.NULL_SETTING;
    }
    
    @Override
    public DoomSetting getSetting(Settings name){
        
        DoomSetting tmp=settings.get(name.name());
        if (tmp!=null) {
            return tmp;
        }
        
        // Good question...what to do here?
        return DoomSetting.NULL_SETTING;
    }
    
    @Override
    public boolean isSettingLiteral(String name,String value){
        
        return getSetting(name).getString().equalsIgnoreCase(value);
    }
    
    
    /** Creates a new setting, overriding any existing ones */

    @Override
    public void putSetting(String name,Object value,boolean persist){        
        DoomSetting tmp=new DoomSetting(name,value.toString(),persist);
        settings.put(name,tmp);
    }

    @Override
    public void putSetting(Settings name,Object value){        
        DoomSetting tmp=new DoomSetting(name.name(),value.toString(),true);
        settings.put(name.name(),tmp);
    }
    
    @Override
    public void putSetting(String name,Object value){        
        DoomSetting tmp=new DoomSetting(name,value.toString(),true);
        settings.put(name,tmp);
    }
    
    //
    // M_LoadDefaults
    //

    @Override
    public void LoadDefaults (String defaultfile)
    {
        int     i;
        BufferedReader  in;

        // Set everything to base values. These are persistent.
        int numdefaults = Settings.values().length;
        for (i=0 ; i<numdefaults ; i++){            
            putSetting(Settings.values()[i].name(),Settings.values()[i].value,true);
        }

        try {

            // read the file in, overriding any set defaults
            in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(defaultfile))));
            if (in!=null)
            {
                String name = null, value = null;
                String s;
                
                // Let's make this the sane way...read a string.
                while (( s=in.readLine())!=null)
                {

                    StringTokenizer tk=new StringTokenizer(s);

                    // They should be exactly two.
                    int tokens=tk.countTokens();
                    if (tokens==2){
                        name=tk.nextToken();
                        value=tk.nextToken();
                        
                        // TODO default value?
                        if (value==null) value="0";
                    } else if (tokens>2){
                        // Not clear what to do in case of multiple values on
                        // the same line. Either introduce multi-value vars (ugh...)
                        // or "join" the tokens, assuming that they are unquoted strings.
                        name=tk.nextToken();
                        value = C2JUtils.unquote(s,'"');
                        if (value==null) continue;
                    }

                        // All var names should be lower case.
                        name=name.toLowerCase();
                        //System.out.printf("NAME: %s VALUE: %s\n",name,value);

                        // Everything read from the file should be marked
                        // as "persistent". There are no "unknown" settings. 
                        // Unusable, maybe.
                        settings.put(name, new DoomSetting(name,value, true));
                        
                } // end-while


                in.close();
            } // not null
        }catch (IOException e){
            // This won't destroy successfully read values, though.
            System.err.printf("I just can't read the settings file %s, will use defaults.\n",defaultfile);
        }
    }

    @Override
    public void applySetting(IUseVariables setme, String name, String value) {
        // TODO Auto-generated method stub
        
    }
    
    //
    // M_SaveDefaults
    //
    
    @Override
    public void SaveDefaults (String defaultfile)
    {
        OutputStream f;
        PrintStream  ps;

        try {
            f = new FileOutputStream(defaultfile);
        } catch (FileNotFoundException e) {
            // can't write the file, but don't complain
            return;
        }

        ps=new PrintStream(f);
        
        Set<Entry<String,DoomSetting>> stuff=settings.entrySet();
        List<DoomSetting> save=new ArrayList<DoomSetting>();
        
        for (Entry<String,DoomSetting> entry :stuff)
        {
            
            DoomSetting set=entry.getValue();
            
            // Save only stuff marked as "persistent"
            if (set.getPersist()){
                save.add(set);
            }
        }
        
        // Nicely sorted alphabetically :-)
        Collections.sort(save);
        
        for (DoomSetting var :save){
            if (C2JUtils.flags(var.getTypeFlag(),DoomSetting.BOOLEAN)){
                ps.printf("%s\t\t%s\n",var.getName(),var.getBoolean());
                continue;
            }
            
            if (C2JUtils.flags(var.getTypeFlag(),DoomSetting.CHAR)){
                ps.printf("%s\t\t\'%c\'\n",var.getName(),var.getChar());
                continue;
            }
            
            if (C2JUtils.flags(var.getTypeFlag(),DoomSetting.INTEGER)){
                ps.printf("%s\t\t%s\n",var.getName(),var.getString());
                continue;
            }
        
            ps.printf("%s\t\t\"%s\"\n",var.getName(),var.getString());
        }

        
        try {
            f.close();}
        catch (IOException e) {
            // Well duh....
            return;
        }
    }
    

    public String getDefaultFile(){
    // check for a custom default file

    int i = CLM.CheckParm("-config");
    if ((i>0) && i<CLM.getArgc()-1)
    {
        return CLM.getArgv(i+1);
        //System.out.printf("   default file: %s\n",defaultfile);
    }
    else
        return Settings.basedefault;
    }

    
}
