package w;

import java.io.DataOutputStream;
import java.io.IOException;

public interface IWritableDoomObject {
    
    public void write(DataOutputStream dos) throws IOException ;
}
