import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class CmdLib {

	/*
	================
	=
	= filelength
	=
	================
	*/

	public static long filelength (FileInputStream handle)
	{
	    try {
	    	return handle.getChannel().size();
	    } catch (Exception e){
			System.err.print("Error fstating");
			System.exit(1);
		}

		return -1;
	}

	public static long tell (FileInputStream handle) throws IOException
	{
		return handle.getChannel().position();
	}

	/*
	char *getcwd (File path, int length)
	{
		return path.getgetwd(path);
	}
	*/

	/*
	=============================================================================

							MISC FUNCTIONS

	=============================================================================
	*/

	/*
	=================
	=
	= Error
	=
	= For abnormal program terminations
	=
	=================
	*/

	public static void Error (String error, String... params)
	{

		System.err.printf(error,params);
		System.err.printf ("\n");
		System.exit (1);
	}


	/*
	=================
	=
	= CheckParm
	=
	= Checks for the given parameter in the program's command line arguments
	=
	= Returns the argument number (1 to argc-1) or 0 if not present
	=
	=================
	*/

	public static int CheckParm (String check,String[] myargv)
	{
		int             i;
		char    parm;

		for (i = 1;i<myargv.length;i++)
		{
			parm = myargv[i].charAt(0);

			if ( !isAlpha(parm) )  // skip - / \ etc.. in front of parm
				if (!*++parm)
					continue;               // parm was only one char

			if ( !stricmp(check,parm) )
				return i;
		}

		return 0;
	}

	public static  final boolean isAlpha(char c){
		return (c=='-' || c=='/' || c=='\\');
	}



	public static FileOutputStream SafeOpenWrite (String filename)
	{
		FileOutputStream     handle=null;
		try{
		handle = new FileOutputStream(filename);
		} catch (Exception e){
			Error ("Error opening %s: %s",filename,e.getCause().getMessage());
		}

		return handle;
	}

	public static FileInputStream SafeOpenRead (String filename)
	{
		FileInputStream     handle=null;
		try{
		handle = new FileInputStream(filename);
		} catch (Exception e){
			Error ("Error opening %s: %s",filename,e.getCause().getMessage());
		}
		
		return handle;
	}


	public static void SafeRead (InputStream handle, byte[] buffer) throws IOException
	{
		int        iocount;
		int read=0;
		int count=buffer.length;
		
		BufferedInputStream bis=new BufferedInputStream(handle,0x8000);
		
		while (count!=0)
		{
			iocount=bis.read(buffer,read,count-read);
			read+=iocount;
			count -= iocount;
		}
	}


	public static void SafeWrite (OutputStream handle, byte[] buffer, int count) throws IOException
	{
		handle.write(buffer,0,count);
	}

	public static void SafeWrite (OutputStream handle, short[] buffer) throws IOException
	{
		DataOutputStream dos=new DataOutputStream(handle);
		
		for (int i=0;i<buffer.length;i++){
			dos.writeShort(LittleShort(buffer[i]));
			System.out.printf("%x %x\n",buffer[i],LittleShort(buffer[i]));
		}
	}

	/*
	==============
	=
	= LoadFile
	=
	==============
	*/

	public static long    LoadFile (String filename, byte[][] bufferptr) throws IOException
	{
		FileInputStream             handle;
		int    length;
		byte[]    buffer;

		handle = SafeOpenRead (filename);
		length = (int) filelength (handle);
		buffer =new byte[length];
		SafeRead (handle, buffer);
		handle.close();

		bufferptr[0] = buffer;
		return length;
	}


	/*
	==============
	=
	= SaveFile
	=
	==============
	*/

	public static void    SaveFile (String filename, byte[] buffer) throws IOException
	{
		OutputStream             handle;

		handle = SafeOpenWrite (filename);
		SafeWrite (handle, buffer, buffer.length);
		handle.close();
	}
	
	public static void    SaveFile (String filename, byte[][] buffer) throws IOException
	{
		OutputStream             handle;

		handle = SafeOpenWrite (filename);
		for (int i=0;i<buffer.length;i++){
			SafeWrite (handle, buffer[i], buffer[i].length);
		}
		handle.close();
	}

	public static void    SaveFile (String filename, short[][] buffer) throws IOException
	{
		OutputStream             handle;

		handle = SafeOpenWrite (filename);
		for (int i=0;i<buffer.length;i++){
			SafeWrite (handle, buffer[i]);
		}
		handle.close();
	}

	public static String DefaultExtension (String path, String extension)
	{
		int src;
	//
	// if path doesn't have a .EXT, append extension
	// (extension should include the .)
	//
		src=path.length() - 1;

		char PATHSEPERATOR=System.getProperty("path.separator").charAt(0);
		
		while (path.charAt(src)!= PATHSEPERATOR && src>=0)
		{
			if (path.charAt(src) == '.')
				return path;                 // it has an extension
			src--;
		}

		return path.concat(extension);
	}

	public static String    StripFilename (String path)
	{
		File tmp=new File(path);

		return tmp.getName();
	}

	/** Return the filename without extension, and stripped
     * of the path.
     * 
     * @param s
     * @return
     */
    
    public static final String StripExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }


	 /**
     * This method is supposed to return the "name" part of a filename. It was
     * intended to return length-limited (max 8 chars) strings to use as lump
     * indicators. There's normally no need to enforce this behavior, as there's
     * nothing preventing the engine from INTERNALLY using lump names with >8
     * chars. However, just to be sure...
     * 
     * @param path
     * @param limit  Set to any value >0 to enforce a length limit
     * @param whole keep extension if set to true
     * @return
     */

    public static final String ExtractFileBase(String path, int limit, boolean whole) {
    	
    	if (path==null) return path;
    	
        int src = path.length() - 1;

        String separator = System.getProperty("file.separator");
        src = path.lastIndexOf(separator)+1;

        if (src < 0) // No separator
            src = 0;

        int len = path.lastIndexOf('.');
        if (whole || len<0 ) len=path.length()-src; // No extension.
        else  len-= src;        

        // copy UP to the specific number of characters, or all        
        if (limit > 0) len = Math.min(limit, len);
        
        return path.substring(src, src + len);
    }

	public static long ParseNum (String str)
	{
		if (str.charAt(0) == '$')
			return Integer.parseInt(str.substring(1), 16);
		if (str.charAt(0) == '0' && str.charAt(1) == 'x')
			return Integer.parseInt(str.substring(2), 16);
		return Integer.parseInt(str);
	}


	public static int GetKey () throws IOException
	{

		return 0;// System.in..read()&0xff;
	}


	/*
	============================================================================

						BYTE ORDER FUNCTIONS

	============================================================================
	*/

//	#ifdef __BIG_ENDIAN__

	public static  short   LittleShort (short l)
	{
		byte    b1,b2;

		b1 = (byte) (l&0xFF);
		b2 = (byte) (l>>8);

		return (short) ((b1<<8)|(b2&0xFF));
	}

	public static short   BigShort (short l)
	{
		return l;
	}


	public static  long    LittleLong (long l)
	{
		byte    b1,b2,b3,b4;

		b1 = (byte) (l&255);
		b2 = (byte) ((l>>8)&255);
		b3 = (byte) ((l>>16)&255);
		b4 = (byte) ((l>>24)&255);

		return ((long)b1<<24) + ((long)b2<<16) + ((long)b3<<8) + b4;
	}

	public static long    BigLong (long l)
	{
		return l;
	}


	/*


	short   BigShort (short l)
	{
		byte    b1,b2;

		b1 = l&255;
		b2 = (l>>8)&255;

		return (b1<<8) + b2;
	}

	short   LittleShort (short l)
	{
		return l;
	}


	long    BigLong (long l)
	{
		byte    b1,b2,b3,b4;

		b1 = l&255;
		b2 = (l>>8)&255;
		b3 = (l>>16)&255;
		b4 = (l>>24)&255;

		return ((long)b1<<24) + ((long)b2<<16) + ((long)b3<<8) + b4;
	}

	public static long LittleLong (long l)
	{
		return l;
	} */


}
