package doom;

import static utils.C2JUtils.eval;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import w.DoomIO;

/**
 * A class to handle the command-line args in an unified manner, and without
 * them being tied to DoomMain or DoomStatus.
 * 
 * 
 * @author velktron
 *
 */

public class CommandLine implements ICommandLineManager {

    
    /* What bullshit, those are supposed to carry over from the very first main */
    protected int myargc;
    protected String[] myargv;
    
    public CommandLine(String[] argv){

        // Bump argcount up by 1 to maintain CheckParm behavior
        // In C, argument 0 is the executable itself, and most
        // of Doom's code is tailored to reflect that, with 0
        // meaning "not found" with regards to a parameter.
        // It's easier to change this here.
    	// TODO: this is really bad practice, eliminate once a 
    	// cvar system is in place.

        myargv=new String[argv.length+1];
        System.arraycopy(argv, 0, myargv, 1, argv.length);        
        myargc=argv.length+1;
        cvars=new HashMap<String, Object>();
    }
    
    /* (non-Javadoc)
     * @see doom.ICommandLineManager#getArgv(int)
     */
    @Override
    public String getArgv(int index){
        if (index>myargc) return null;
        else return myargv[index];
    }
    
    /* (non-Javadoc)
     * @see doom.ICommandLineManager#getArgc()
     */
    @Override
    public int getArgc(){
        return myargc;
    }
    

    /* (non-Javadoc)
     * @see doom.ICommandLineManager#CheckParm(java.lang.String)
     */

    @Override
    public int CheckParm(String check) {
        int i;

        for (i = 1; i < myargc; i++) {
            if (check.compareToIgnoreCase(myargv[i]) == 0)
                return i;
        }

        return 0;
    }
    
    @Override
    public boolean CheckParmBool(String check) {
        int i;

        for (i = 1; i < myargc; i++) {
            if (check.compareToIgnoreCase(myargv[i]) == 0)
                return true;
        }

        return false;
    }
    
    /**
     * Find a Response File
     * 
     * Not very well documented, but Doom apparently could use a sort of 
     * script file with command line arguments inside, if you prepend @ to
     * the command-like argument itself. The arguments themselves could
     * be separated by any sort of whitespace or ASCII characters exceeding "z"
     * in value.
     * 
     * E.g. doom @crap
     * 
     * would load a file named "crap".
     * 
     * Now, the original function is crap for several reasons: for one,
     * it will bomb if more than 100 arguments <i>total</i> are formed.
     * Memory allocation will also fail because the tokenizer used only
     * stops at file size limit, not at maximum parsed arguments limit
     * (MACARGVS = 100).
     * 
     * This is the wiki's entry:
     * 
     * doom @<response>
     * This parameter tells the Doom engine to read from a response file, 
     * a text file that may store additional command line parameters. 
     * The file may have any name that is valid to the system, optionally 
     * with an extension. The parameters are typed as in the command line 
     * (-episode 2, for example), but one per line, where up to 100 lines
     *  may be used. The additional parameters may be disabled for later 
     *  use by placing a vertical bar (the | character) between the 
     *  prefixing dash (-) and the rest of the parameter name.
     * 
     * 
     */
    @Override
    public void FindResponseFile ()
    {
        try{

            for (int i = 1;i < getArgc();i++)
                if (getArgv(i).charAt(0)=='@')
                {
                    DataInputStream handle;
                    // save o       
                    int             size;
                    int             indexinfile;
                    char[]   infile=null;
                    char[]    file=null;
                    // Fuck that, we're doing it properly.
                    ArrayList<String>  parsedargs=new ArrayList<String>();
                    ArrayList<String>    moreargs=new ArrayList<String>();
                    String    firstargv;

                    // READ THE RESPONSE FILE INTO MEMORY
                    handle = new DataInputStream(new BufferedInputStream(new FileInputStream(myargv[i].substring(1))));
                    if (!eval(handle))
                    {
                        System.out.print ("\nNo such response file!");
                        System.exit(1);
                    }
                    System.out.println("Found response file "+myargv[i].substring(1));
                    size = (int) handle.available();
                    file=new char[size];
                    
                    DoomIO.readNonUnicodeCharArray(handle, file,size);
                    handle.close();

                    // Save first argument.
                    firstargv = myargv[0];

                    // KEEP ALL CMDLINE ARGS FOLLOWING @RESPONSEFILE ARG
                    // This saves the old references.
                    for (int k = i+1; k < myargc; k++)
                        moreargs.add(myargv[k]);

                    infile = file;
                    indexinfile = 0;
                    indexinfile++;  // SKIP PAST ARGV[0] (KEEP IT)
                    // HMM? StringBuffer build=new StringBuffer();

                    /* MAES: the code here looked like some primitive tokenizer.
           that assigned C-strings to memory locations.
           Instead, we'll tokenize the file input correctly here.
                     */

                    StringTokenizer tk=new StringTokenizer(String.copyValueOf(infile));



                    //myargv = new String[tk.countTokens()+argc];
                    parsedargs.add(firstargv);

                    while(tk.hasMoreTokens())
                    {
                        parsedargs.add(tk.nextToken());
                    }

                    // Append the other args to the end.
                    parsedargs.addAll(moreargs);

                    /* NOW the original myargv is reset, but the old values still survive in 
                     * the listarray.*/

                    myargv= new String[parsedargs.size()];
                    myargv=parsedargs.toArray(myargv);
                    myargc = myargv.length;

                    // DISPLAY ARGS
                    System.out.println(myargc+" command-line args:");
                    for (int k=0;k<myargc;k++)
                        System.out.println(myargv[k]);

                    // Stops at the first one. Pity, because we could do funky recursive stuff with that :-p
                    break;
                }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void setArgv(int index, String string) {
        this.myargv[index]=string;
        
    }
    
    private HashMap<String, Object> cvars;
    
    public boolean cvarExists(String name){
    	return cvars.containsKey(name);
    	}
    
    public Object removeCvar(String name){
    	return cvars.remove(name);
    	}
    
    public void putCvar(String name, int value){
    	cvars.put(name, value);
    	}

    public void putCvar(String name, double value){
    	cvars.put(name, value);
    	}
    
    public void putCvar(String name, String value){
    	cvars.put(name, value);
    	}

    public void putCvar(String name, boolean value){
    	cvars.put(name, value);
    	}

    public void putCvar(String name, String[] value){
    	cvars.put(name, value);
    	}
    
    public Integer getInt(String name){
    	Object stuff=cvars.get(name);
    	if (stuff !=null){
    		if (stuff instanceof Integer){
    			return (Integer) stuff;
    		}
    	}    	
    	return null;
    }
    
    public Double getFloat(String name){
    	Object stuff=cvars.get(name);
    	if (stuff !=null){
    		if (stuff instanceof Double){
    			return (Double) stuff;
    		}
    	}    	
    	return null;
    }
    
    public String getString(String name){
    	Object stuff=cvars.get(name);
    	if (stuff !=null){
    		if (stuff instanceof String){
    			return (String) stuff;
    		}
    	}    	
    	return null;
    }
    
    public String[] getStringArray(String name){
    	Object stuff=cvars.get(name);
    	if (stuff !=null){
    		if (stuff instanceof String[]){
    			return (String[]) stuff;
    		}
    	}    	
    	return null;
    }
    
    public Boolean getBoolean(String name){
    	Object stuff=cvars.get(name);
    	if (stuff !=null){
    		if (stuff instanceof Boolean){
    			return (Boolean) stuff;
    		}
    	}    	
    	return null;
    }
    
    public <K> void setCvar(String name, K value){
    	if (cvars.containsKey(name)){
    		cvars.put(name, value);
    	}
    }

    public static String checkParameterCouple(String check, String[] myargv) {
        int found=-1;

        for (int i = 0; i < myargv.length; i++) {
            if (check.compareToIgnoreCase(myargv[i]) == 0){
                found=i; // found. Break on first.
                break;
            }
        }
        
        // Found, and there's room to spare for one more?
        if ((found>=0)&&(found<myargv.length-1)){
            if (myargv[found+1]!=null){
                // Not null, not empty and not a parameter switch
                if ((myargv[found+1].length()>=1) &&
                    (myargv[found+1].charAt(0)!='-'))                        
                        return myargv[found+1];
            }
        }

        // Well duh.
        return null;
    }
 
    /** Is a parameter based on an prefix identifier e.g. '-'
     * 
     * @param what
     * @param identifier
     * @return
     */
    public static boolean isParameter(String what, char identifier){
        if (what!=null && what.length()>-0){
        	return (what.charAt(0)!=identifier); 
        }
        
        return false;        
    }
    
    public static int parameterMultipleValues(String check, String[] myargv) {
        int found=-1;

        // It's not even a valid parameter name
        if (!isParameter(check,'-')) return -1;
        
        // Does it exist?
        if ((found=checkParm(check,myargv))==-1) return found;
        
        // Found, and there are still some to spare
        int rest=myargv.length-found-1;
        int count=0;
        
        for (int i=found+1;i<myargv.length;i++){
        	if (isParameter(myargv[i],'-')) break;
        		else
        	count++; // not a parameter, count up        	
        }
        
        // Well duh.
        return count;
    }
    
    
    public static int checkParm(String check, String[] params) {
        int i;

        for (i = 0; i < params.length; i++) {
            if (check.compareToIgnoreCase(params[i]) == 0)
                return i;
        }

        return -1;
    }
    
}
