package w;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** This is an interface implemented by objects that must be read form disk.
 *  Every object is supposed to do its own umarshalling. This way,
 *  structured and hierchical reads are possible. Another superior innovation
 *  of Mocha Doom ;-) Err....ok :-p
 *  
 * @author Velktron
 *
 */

public interface IReadableDoomObject {
    
    public void read(DataInputStream f) throws IOException ;
}
