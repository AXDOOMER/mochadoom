package doom;

/** Mocha Doom uses the think_t type as an ENUM for available action functions. This
 * makes enumerations etc. easy to keep track of.
 * 
 * @author velktron
 *
 */

public enum think_t {
    /** Here's your "decent NOP" function */
	NOP,
	A_Light0(2),
    A_WeaponReady(2),
    A_Lower(2),
    A_Raise(2),
    A_Punch(2),
    A_ReFire(2),
    A_FirePistol(2),
    A_Light1(2),
    A_FireShotgun(2),
    A_Light2(2),
    A_FireShotgun2(2),
    A_CheckReload(2),
    A_OpenShotgun2(2),
    A_LoadShotgun2(2),
    A_CloseShotgun2(2),
    A_FireCGun(2),
    A_GunFlash(2),
    A_FireMissile(2),
    A_Saw(2),
    A_FirePlasma(2),
    A_BFGsound(2),
    A_FireBFG(2),
    A_BFGSpray(1),
    A_Explode(1),
    A_Pain(1),
    A_PlayerScream(1),
    A_Fall(1),
    A_XScream(1),
    A_Look(1),
    A_Chase(1),
    A_FaceTarget(1),
    A_PosAttack(1),
    A_Scream(1),
    A_SPosAttack(1),
    A_VileChase(1),
    A_VileStart(1),
    A_VileTarget(1),
    A_VileAttack(1),
    A_StartFire(1),
    A_Fire(1),
    A_FireCrackle(1),
    A_Tracer(1),
    A_SkelWhoosh(1),
  //Historically, "think_t" is yet another
  //function pointer to a routine to handle
  //an actor.

  //
  //Experimental stuff.
  //To compile this as "ANSI C with classes"
  //we will need to handle the various
  //action functions cleanly.
  //
  //typedef  void (*actionf_v)();
  //typedef  void (*actionf_p1)( void* );
  //typedef  void (*actionf_p2)( void*, void* );

  /*typedef union
  {
  actionf_p1  acp1;
  actionf_v   acv;
  actionf_p2  acp2;

  } actionf_t;

  */

    A_SkelFist(1),
    A_SkelMissile(1),
    A_FatRaise(1),
    A_FatAttack1(1),
    A_FatAttack2(1),
    A_FatAttack3(1),
    A_BossDeath(1),
    A_CPosAttack(1),
    A_CPosRefire(1),
    A_TroopAttack(1),
    A_SargAttack(1),
    A_HeadAttack(1),
    A_BruisAttack(1),
    A_SkullAttack(1),
    A_Metal(1),
    A_SpidRefire(1),
    A_BabyMetal(1),
    A_BspiAttack(1),
    A_Hoof(1),
    A_CyberAttack(1),
    A_PainAttack(1),
    A_PainDie(1),
    A_KeenDie(1),
    A_BrainPain(1),
    A_BrainScream(1),
    A_BrainDie(1),
    A_BrainAwake(1),
    A_BrainSpit(1),
    A_SpawnSound(1),
    A_SpawnFly(1),
    A_BrainExplode(1),
    P_MobjThinker(1),
    T_FireFlicker(1),
	T_LightFlash(1),
	T_StrobeFlash(1),
	T_Glow(1),
	T_MoveCeiling(1),
	T_MoveFloor(1),
	T_VerticalDoor(1),
	T_PlatRaise(1), 
	T_SlidingDoor(1),
	// The following are dummies that exist only for demo sync debugging
	DeathMatchSpawnPlayer,
	PlayerInSpecialSector, 
	SpawnLightFlash, 
	SpawnStrobeFlash, 
	ExplodeMissile,
	CheckMissileRange,
	DoPlat,
	CheckMissileSpawn,
	DamageMobj, 
	KillMobj, 
	NewChaseDir, 
	P_GunShot, 
	PIT_ChangeSector, 
	PIT_CheckThing, 
	TryWalk, 
	SpawnBlood, 
	SpawnMapThing, 
	SpawnMobj, 
	SpawnMissile,
	SpawnPuff;
    
	
	think_t(){
	    type=0; // Default, but Doom has no "type 0" functions!
	}
	
    think_t(int type){
        this.type=type;
    }

    /** 0 for void, 1 for acp1, 2 for acp2 */
    public int getType() {
        return type;
    }

    private int type;
    
    public String ToString(){
        return this.name()+" Type: "+type;
    }
    
    public static final int acpv=0;
    public static final int acp1=1;
    public static final int acp2=2;
	
}
