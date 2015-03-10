package p;

import static p.DoorDefines.GLOWSPEED;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import rr.SectorAction;
import w.DoomIO;

public class glow_t extends SectorAction{
        public int     minlight;
        public int     maxlight;
        public int     direction;

        //
        // Spawn glowing light
        //

        public void Glow() {
            switch (direction) {
            case -1:
                // DOWN
                sector.lightlevel -= GLOWSPEED;
                if (sector.lightlevel <= minlight) {
                    sector.lightlevel += GLOWSPEED;
                    direction = 1;
                }
                break;

            case 1:
                // UP
                sector.lightlevel += GLOWSPEED;
                if (sector.lightlevel >= maxlight) {
                    sector.lightlevel -= GLOWSPEED;
                    direction = -1;
                }
                break;
            }
        }
        
        @Override
        public void read(DataInputStream f) throws IOException{

            super.read(f); // Call thinker reader first            
            super.sectorid=DoomIO.readLEInt(f); // Sector index
            minlight=DoomIO.readLEInt(f);
            maxlight=DoomIO.readLEInt(f);
            direction=DoomIO.readLEInt(f);
            }
        
        @Override
        public void pack(ByteBuffer b) throws IOException{
            super.pack(b); //12            
            b.putInt(super.sectorid); // 16
            b.putInt(minlight);//20
            b.putInt(maxlight);//24
            b.putInt(direction);//38
        }
        
    }