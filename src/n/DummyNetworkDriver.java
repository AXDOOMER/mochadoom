package n;

import i.DoomStatusAware;
import i.IDoomSystem;
import doom.DoomMain;
import doom.DoomStatus;
import doom.IDoomGameNetworking;
import doom.NetConsts;
import doom.doomcom_t;

/** Does nothing.
 *  Allows running single-player games without an actual network.
 *  Hopefully, it will be replaced by a real UDP-based driver one day.
 *  
 * @author Velktron
 *
 */

public class DummyNetworkDriver implements NetConsts,DoomSystemNetworking, DoomStatusAware{

	////////////// STATUS ///////////

	IDoomSystem I;
	DoomMain DM;
	IDoomGameNetworking DGN;

	public DummyNetworkDriver(DoomStatus DC){
		updateStatus(DC);
	}

	@Override
	public void InitNetwork() {
		doomcom_t doomcom =new doomcom_t();
		doomcom.id=DOOMCOM_ID;
		doomcom.ticdup=1;

		// single player game
		DM.netgame = false;
		doomcom.id = DOOMCOM_ID;
		doomcom.numplayers = doomcom.numnodes = 1;
		doomcom.deathmatch = 0;
		doomcom.consoleplayer = 0;
		DGN.setDoomCom(doomcom);
	}

	@Override
	public void NetCmd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStatus(DoomStatus DC) {
		this.DM=DC.DM;
		this.DGN=DC.DM;
	}

}
