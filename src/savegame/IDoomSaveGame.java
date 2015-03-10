package savegame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import i.DoomStatusAware;
import p.ThinkerList;

public interface IDoomSaveGame extends DoomStatusAware{    
    void setThinkerList(ThinkerList li);
    boolean doLoad(DataInputStream f);
    IDoomSaveGameHeader getHeader();
    void setHeader(IDoomSaveGameHeader header);
    boolean doSave(DataOutputStream f);
}
