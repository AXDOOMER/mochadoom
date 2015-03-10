package w;

import java.io.IOException;
import rr.patch_t;

public interface IWadLoader {

	/**
	 * W_Reload Flushes any of the reloadable lumps in memory and reloads the
	 * directory.
	 * 
	 * @throws Exception
	 */
	public abstract void Reload() throws Exception;

	/**
	 * W_InitMultipleFiles
	 *
	 * Pass a null terminated list of files to use (actually
	 * a String[] array in Java).
	 * 
	 * All files are optional, but at least one file
	 * must be found. 
	 * 
	 * Files with a .wad extension are idlink files
	 * with multiple lumps.
	 * 
	 * Other files are single lumps with the base filename
	 * for the lump name.
	 * 
	 * Lump names can appear multiple times.
	 * The name searcher looks backwards, so a later file
	 * does override all earlier ones.
	 * 
	 * @param filenames
	 * 
	 */

	public abstract void InitMultipleFiles(String[] filenames) throws Exception;

	/**
	 * W_InitFile
	 * 
	 * Just initialize from a single file.
	 * 
	 * @param filename 
	 * 
	 */
	public abstract void InitFile(String filename) throws Exception;

	/**
	 * W_NumLumps
	 * 
	 * Returns the total number of lumps loaded in this Wad manager. Awesome. 
	 * 
	 */
	public abstract int NumLumps();

	/**
	 * Returns actual lumpinfo_t object for a given name. Useful if you want to
	 * access something on a file, I guess?
	 * 
	 * @param name
	 * @return
	 */

	public abstract lumpinfo_t GetLumpinfoForName(String name);

	/**
	 * W_GetNumForName
	 * Calls W_CheckNumForName, but bombs out if not found.
	 */

	public abstract int GetNumForName(String name);

	/**
	 *          
	 * @param lumpnum
	 * @return
	 */
	public abstract String GetNameForNum(int lumpnum);

	//
	// W_LumpLength
	// Returns the buffer size needed to load the given lump.
	//
	public abstract int LumpLength(int lump);


    /**
     * W_CacheLumpNum Modified to read a lump as a specific type of
     * CacheableDoomObject. If the class is not identified or is null, then a
     * generic DoomBuffer object is left in the lump cache and returned.
     * @param <T>
     */
    public abstract <T> T CacheLumpNum(int lump, int tag,
            Class<T> what);

	// MAES 24/8/2011: superseded by auto-allocating version with proper 
	// container-based caching.

	@Deprecated
	public abstract void CacheLumpNumIntoArray(int lump, int tag,
			Object[] array, Class what) throws IOException;

	/**
	 * Return a cached lump based on its name, as raw bytes, no matter what.
	 * It's rare, but has its uses.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public abstract byte[] CacheLumpNameAsRawBytes(String name, int tag);

	/**
	 * Return a cached lump based on its num, as raw bytes, no matter what.
	 * It's rare, but has its uses.
	 * 
	 * @param name
	 * @param tag
	 * @param what
	 * @return
	 */

	public abstract byte[] CacheLumpNumAsRawBytes(int num, int tag);

	/** Get a DoomBuffer of the specified lump name
	 * 
	 * @param name
	 * @param tag
	 * @return
	 */
	
	public abstract DoomBuffer CacheLumpName(String name, int tag);

    /** Get a DoomBuffer of the specified lump num
     * 
     * @param lump
     * @return
     */

	public abstract DoomBuffer CacheLumpNumAsDoomBuffer(int lump);

	/**
	 * Specific method for loading cached patches by name, since it's by FAR the
	 * most common operation.
	 * 
	 * @param name
	 * @return
	 */

	public abstract patch_t CachePatchName(String name);

	/**
	 * Specific method for loading cached patches, since it's by FAR the most
	 * common operation.
	 * 
	 * @param name
	 * @param tag
	 * @return
	 */

	public abstract patch_t CachePatchName(String name, int tag);

	/**
	 * Specific method for loading cached patches by number.
	 * 
	 * @param num
	 * @return
	 */

	public abstract patch_t CachePatchNum(int num);

	public abstract <T extends CacheableDoomObject> T CacheLumpName(String name, int tag, Class<T> what);

	/** A lump with size 0 is a marker. This means that it
	 *  can/must be skipped, and if we want actual data we must
	 *  read the next one. 
	 * 
	 * @param lump
	 * @return
	 */
	public abstract boolean isLumpMarker(int lump);

	public abstract String GetNameForLump(int lump);

	public abstract int CheckNumForName(String name/* , int namespace */);
	
	/** Return ALL possible results for a given name, in order to resolve name clashes without
	 *  using namespaces
	 *  
	 * @param name
	 * @return
	 */
	
	public abstract int[] CheckNumsForName(String name);

	public abstract lumpinfo_t GetLumpInfo(int i);

	/** A way to cleanly close open file handles still pointed at by lumps.
	 *  Is also called upon finalize */
	public void CloseAllHandles();

	/** Null the disk lump associated with a particular object,
	 *  if any. This will NOT induce a garbage collection, unless
	 *  you also null any references you have to that object.
	 *  	  
	 * @param lump
	 */
	
	void UnlockLumpNum(int lump);

	void UnlockLumpNum(CacheableDoomObject lump);
	
	public <T extends CacheableDoomObject> T[] CacheLumpNumIntoArray(int lump, int num,
			Class<T> what);
	
	/** Verify whether a certain lump number is valid and has
	 *  the expected name.
	 *  
	 * @param lump
	 * @param lumpname
	 * @return
	 */

	boolean verifyLumpName(int lump, String lumpname);
	
	/** The index of a known loaded wadfile
	 * 
	 * @param wad1
	 * @return
	 */

    public abstract int GetWadfileIndex(wadfile_info_t wad1);

    /** The number of loaded wadfile
     * 
     * @return
     */
    public abstract int GetNumWadfiles();

    /** Force a lump (in memory) to be equal to a dictated content. Useful
     *  for when you are e.g. repairing palette lumps or doing other sanity
     *  checks.
     * 
     * @param lump
     * @param obj
     */
	void InjectLumpNum(int lump, CacheableDoomObject obj);

	/** Read a lump into a bunch of bytes straight. No caching, no frills.
	 * 
	 * @param lump
	 * @return
	 */
    byte[] ReadLump(int lump);

    /** Use your own buffer, of proper size of course.
     * 
     * @param lump
     * @param buf
     */
    void ReadLump(int lump, byte[] buf);
    
    /** Use your own buffer, of proper size AND offset.
     * 
     * @param lump
     * @param buf
     */

    void ReadLump(int lump, byte[] buf, int offset);
	
}