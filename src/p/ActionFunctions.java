package p;

import static rr.line_t.ML_BLOCKING;
import static data.Defines.BT_ATTACK;
import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.MELEERANGE;
import static data.Defines.MISSILERANGE;
import static data.Defines.PST_DEAD;
import static data.Defines.pw_strength;
import static data.Limits.MAXPLAYERS;
import static data.Limits.MAXRADIUS;
import static data.Limits.MAXSKULLS;
import static data.Limits.NUMBRAINTARGETS;
import static data.Tables.ANG180;
import static data.Tables.ANG270;
import static data.Tables.ANG45;
import static data.Tables.ANG90;
import static data.Tables.BITS32;
import static data.Tables.FINEANGLES;
import static data.Tables.FINEMASK;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.info.mobjinfo;
import static data.info.states;
import static doom.items.weaponinfo;
import static doom.player_t.LOWERSPEED;
import static doom.player_t.RAISESPEED;
import static doom.player_t.WEAPONBOTTOM;
import static doom.player_t.WEAPONTOP;
import static doom.player_t.ps_flash;
import static doom.player_t.ps_weapon;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.MAPFRACUNIT;
import static m.fixed_t.FixedMul;
import static p.ChaseDirections.DI_NODIR;
import static p.ChaseDirections.xspeed;
import static p.ChaseDirections.yspeed;
import static p.MapUtils.AproxDistance;
import static p.mobj_t.MF_AMBUSH;
import static p.mobj_t.MF_COUNTKILL;
import static p.mobj_t.MF_JUSTATTACKED;
import static p.mobj_t.MF_SHADOW;
import static p.mobj_t.MF_SHOOTABLE;
import static p.mobj_t.MF_SKULLFLY;
import static p.mobj_t.MF_SOLID;
import static utils.C2JUtils.eval;
import static utils.C2JUtils.flags;
import i.DoomStatusAware;
import p.UnifiedGameMap.Enemies;
import m.IRandom;
import rr.Renderer;
import rr.line_t;
import s.IDoomSound;
import data.Tables;
import data.mobjinfo_t;
import data.mobjtype_t;
import data.state_t;
import data.sounds.sfxenum_t;
import defines.skill_t;
import defines.statenum_t;
import doom.DoomStatus;
import doom.IDoomGame;
import doom.player_t;
import doom.think_t;
import doom.thinker_t;
import doom.weapontype_t;

public class ActionFunctions implements DoomStatusAware{


	public ActionFunctions(){
		MobjThinker=new P_MobjThinker();
		  WeaponReady=new A_WeaponReady();
		  Lower=new A_Lower();
		  Raise=new A_Raise();
		  Punch=new A_Punch();
		  ReFire=new A_ReFire();
		  FirePistol=new A_FirePistol();
		  Light0=new A_Light0();
		  Light1=new A_Light1();
		  Light2=new A_Light2();
		   FireShotgun=new A_FireShotgun();	
		   FireShotgun2=new A_FireShotgun2();	
		  CheckReload=new A_CheckReload();
		  OpenShotgun2=new A_OpenShotgun2();
		  LoadShotgun2=new A_LoadShotgun2();
		  CloseShotgun2=new A_CloseShotgun2();
		  FireCGun=new A_FireCGun();
		  GunFlash=new A_GunFlash();
		  FireMissile=new A_FireMissile();
		  Saw=new A_Saw();
		  FirePlasma=new A_FirePlasma();
		  BFGsound=new A_BFGsound();
		  FireBFG=new A_FireBFG();
	      BFGSpray=new A_BFGSpray();
	      Explode=new A_Explode();
	      Pain=new A_Pain();
	      PlayerScream=new A_PlayerScream();
	      Fall=new A_Fall();
	      XScream=new A_XScream();
	      Look=new A_Look();
	      Chase=new A_Chase();
	      FaceTarget=new A_FaceTarget();
	      PosAttack=new A_PosAttack();
	      Scream=new A_Scream();
	      SPosAttack=new A_SPosAttack();
	      VileChase=new A_VileChase();
	      VileStart=new A_VileStart();
	      VileTarget=new A_VileTarget();
	      VileAttack=new A_VileAttack();
	      StartFire=new A_StartFire();
	      Fire=new A_Fire();
	      FireCrackle=new A_FireCrackle();
	      Tracer=new A_Tracer();
	      SkelWhoosh=new A_SkelWhoosh();
	      SkelFist=new A_SkelFist();
	      SkelMissile=new A_SkelMissile();
	      FatRaise=new A_FatRaise();
	      FatAttack1=new A_FatAttack1();
	      FatAttack2=new A_FatAttack2();
	      FatAttack3=new A_FatAttack3();
	      BossDeath=new A_BossDeath();
	      CPosAttack=new A_CPosAttack();
	      CPosRefire=new A_CPosRefire();
	      TroopAttack=new A_TroopAttack();
	      SargAttack=new A_SargAttack();
	      HeadAttack=new A_HeadAttack();
	     BruisAttack=new A_BruisAttack();
	      SkullAttack=new A_SkullAttack();
	      Metal=new A_Metal();
	      SpidRefire=new A_SpidRefire();
	     BabyMetal=new A_BabyMetal();
	     BspiAttack=new A_BspiAttack();
	      Hoof=new A_Hoof();
	      CyberAttack=new A_CyberAttack();
	      PainAttack=new A_PainAttack();
	      PainDie=new A_PainDie();
	      KeenDie=new A_KeenDie();
	      BrainPain=new A_BrainPain();
	      BrainScream=new A_BrainScream();
	      BrainDie=new A_BrainDie();
	      BrainAwake=new A_BrainAwake();
	      BrainSpit=new A_BrainSpit();
	      SpawnSound=new A_SpawnSound();
	      SpawnFly=new A_SpawnFly();
	      BrainExplode=new A_BrainExplode();
	      
	      // these need special handling.
	      FireFlicker=new T_FireFlicker();
	      LightFlash=new T_LightFlash();
		  StrobeFlash=new T_StrobeFlash();
		  Glow=new T_Glow();
		  MoveCeiling=new T_MoveCeiling();
		  MoveFloor=new T_MoveFloor();
		  VerticalDoor=new T_VerticalDoor();
		  PlatRaise=new T_PlatRaise();
		  SlidingDoor=new T_SlidingDoor();
		
	}
		
	public ActionFunctions(DoomStatus<?,?> DS, Enemies EN) {
		this();
		this.EN=EN;     
		updateStatus(DS);
	}

	@Override
	public void updateStatus(DoomStatus<?,?> DS){
		this.A=DS.P;
        this.RND=DS.RND;       
        this.R=DS.R;
        this.S=DS.S;
        this.LL=DS.LL;
        this.DS=DS.DM;
        this.DG=DS.DG;
//        this.SL=DS.SL;
	}
	
	protected Actions A;
	protected IRandom RND;
	protected Renderer<?,?> R;
	protected IDoomSound S;
	protected Enemies EN;
	protected AbstractLevelLoader LL;
	protected DoomStatus<?,?> DS;
	protected IDoomGame DG;
	protected SlideDoor SL;
	

	ActionType2  WeaponReady;
	ActionType2  Lower;
	ActionType2  Raise;
	ActionType2  Punch;
	ActionType2  ReFire;
	ActionType2  FirePistol;
	ActionType2  Light0;
	ActionType2  Light1;
	ActionType2  Light2;
	ActionType2   FireShotgun;	
	ActionType2  FireShotgun2;
	ActionType2  CheckReload;
	ActionType2  OpenShotgun2;
	ActionType2  LoadShotgun2;
	ActionType2  CloseShotgun2;
	ActionType2  FireCGun;
	ActionType2  GunFlash;
	ActionType2  FireMissile;
	ActionType2  Saw;
	ActionType2  FirePlasma;
	ActionType2  BFGsound;
	ActionType2  FireBFG;
    ActionType1  BFGSpray;
    ActionType1  Explode;
    ActionType1  Pain;
    ActionType1  PlayerScream;
    ActionType1 Fall;
    ActionType1  XScream;
    ActionType1  Look;
    ActionType1  Chase;
    ActionType1  FaceTarget;
    ActionType1  PosAttack;
    ActionType1  Scream;
    ActionType1  SPosAttack;
    ActionType1  VileChase;
    ActionType1  VileStart;
    ActionType1  VileTarget;
    ActionType1  VileAttack;
    ActionType1 StartFire;
    ActionType1  Fire;
    ActionType1  FireCrackle;
    ActionType1  Tracer;
    ActionType1  SkelWhoosh;
    ActionType1  SkelFist;
    ActionType1  SkelMissile;
    ActionType1  FatRaise;
    ActionType1  FatAttack1;
    ActionType1  FatAttack2;
    ActionType1 FatAttack3;
    ActionType1  BossDeath;
    ActionType1  CPosAttack;
    ActionType1  CPosRefire;
    ActionType1  TroopAttack;
    ActionType1  SargAttack;
    ActionType1  HeadAttack;
    ActionType1 BruisAttack;
    ActionType1  SkullAttack;
    ActionType1  Metal;
    ActionType1  SpidRefire;
    ActionType1 BabyMetal;
    ActionType1 BspiAttack;
    ActionType1  Hoof;
    ActionType1  CyberAttack;
    ActionType1  PainAttack;
    ActionType1  PainDie;
    ActionType1 KeenDie;
    ActionType1  BrainPain;
    ActionType1  BrainScream;
    ActionType1  BrainDie;
    ActionType1  BrainAwake;
    ActionType1  BrainSpit;
    ActionType1  SpawnSound;
    ActionType1  SpawnFly;
    ActionType1  BrainExplode;
    ActionType1  MobjThinker;
    ActionTypeSS<?>  FireFlicker;
    ActionTypeSS<?>  LightFlash;
	ActionTypeSS<?> StrobeFlash;
	ActionTypeSS<?>  Glow;
	ActionTypeSS<?>  MoveCeiling;
	ActionTypeSS<?>  MoveFloor;
	ActionTypeSS<?>  VerticalDoor;
	ActionTypeSS<?> PlatRaise;
	ActionTypeSS<?> SlidingDoor;
	
	/** Wires a state to an actual callback depending on its
	 *  enum. This eliminates the need to have a giant
	 *  switch statements in Actions, and should allow DEH
	 *  rewiring.
	 * @param st
	 */
	
	public void doWireState(state_t st){
	      //System.out.println("Dispatching: "+action);
		if (st.action==null) return;
	      switch (st.action){
	          case P_MobjThinker:
	        	  st.acp1=MobjThinker;
	        	  break;
	          case A_Light0:
	        	  st.acp2=Light0;
	        	  break;
	          case A_WeaponReady:
	        	  st.acp2=WeaponReady;
	        	  break;        	  
	          case  A_Lower:
	        	  st.acp2=Lower;
	        	  break;
	          case  A_Raise:
	        	  st.acp2=Raise;
	        	  break;
	          case  A_Punch:
	        	  st.acp2=Punch;
	        	  break;
	          case A_ReFire:
	        	  st.acp2=ReFire;
	        	  break;
	          case A_FirePistol:
	        	  st.acp2= FirePistol;
	        	  break;
	          case  A_Light1:
	        	  st.acp2=Light1;
	        	  break;
	          case A_FireShotgun:
	        	  st.acp2=FireShotgun;
	        	  break;
	          case  A_Light2:
	        	  st.acp2=Light2;
	        	  break;
	          case A_FireShotgun2:
	        	  st.acp2=FireShotgun2;
	        	  break;
	          case  A_CheckReload:
	        	  st.acp2=CheckReload;
	        	  break;
	          case A_OpenShotgun2:
	        	  st.acp2=OpenShotgun2;
	        	  break;
	          case A_LoadShotgun2:
	        	  st.acp2=LoadShotgun2;
	        	  break;
	          case A_CloseShotgun2:
	        	  st.acp2=CloseShotgun2;
	        	  break;
	          case A_FireCGun:
	        	  st.acp2=FireCGun;
	        	  break;
	          case A_GunFlash:
	        	  st.acp2=GunFlash;
	        	  break;
	          case A_FireMissile:
	        	  st.acp2=FireMissile;
	        	  break;
	          case A_Saw:
	        	  st.acp2=Saw;
	        	  break;
	          case A_FirePlasma:
	        	  st.acp2=FirePlasma;
	        	  break;
	          case A_BFGsound:
	        	  st.acp2=BFGsound;
	        	  break;
	          case A_FireBFG:
	        	  st.acp2=FireBFG;
	        	  break;
	          case A_BFGSpray:
	        	  st.acp1=BFGSpray;
	        	  break;
	          case A_Explode:
	        	  st.acp1=Explode;
	        	  break;
	          case A_Pain:
	        	  st.acp1=Pain;
	        	  break;
	          case A_PlayerScream:
	        	  st.acp1=PlayerScream;
	        	  break;
	          case A_Fall:
	        	  st.acp1=Fall;
	        	  break;
	          case  A_XScream:
	        	  st.acp1=XScream;
	        	  break;
	          case A_Look:
	        	  st.acp1=Look;
	        	  break;
	          case  A_Chase:
	        	  st.acp1=Chase;
	        	  break;
	          case  A_FaceTarget:
	        	  st.acp1=FaceTarget;
	        	  break;
	          case  A_PosAttack:
	        	  st.acp1=PosAttack;
	        	  break;
	          case  A_Scream:
	        	  st.acp1=Scream;
	        	  break;
	          case  A_SPosAttack:
	        	  st.acp1=SPosAttack;
	        	  break;
	          case  A_VileChase:
	        	  st.acp1=VileChase;
	        	  break;
	          case  A_VileStart:
	        	  st.acp1=VileStart;
	        	  break;
	          case  A_VileTarget:
	        	  st.acp1=VileTarget;
	        	  break;
	          case  A_VileAttack:
	        	  st.acp1=VileAttack;
	        	  break;
	          case  A_StartFire:
	        	  st.acp1=StartFire;
	        	  break;
	          case  A_Fire:
	        	  st.acp1=Fire;
	        	  break;
	          case  A_FireCrackle:
	        	  st.acp1=FireCrackle;
	        	  break;
	          case  A_Tracer:
	        	  st.acp1=Tracer;
	        	  break;
	          case  A_SkelWhoosh:
	        	  st.acp1=SkelWhoosh;
	        	  break;
	          case  A_SkelFist:
	        	  st.acp1=SkelFist;
	        	  break;
	          case  A_SkelMissile:
	        	  st.acp1=SkelMissile;
	        	  break;
	          case  A_FatRaise:
	        	  st.acp1=FatRaise;
	        	  break;
	          case  A_FatAttack1:
	        	  st.acp1=FatAttack1;
	        	  break;
	          case  A_FatAttack2:
	        	  st.acp1=FatAttack2;
	        	  break;
	          case A_FatAttack3:
	        	  st.acp1= FatAttack3;
	        	  break;
	          case  A_BossDeath:
	        	  st.acp1=BossDeath;
	        	  break;
	          case  A_CPosAttack:
	        	  st.acp1=CPosAttack;
	        	  break;
	          case  A_CPosRefire:
	        	  st.acp1=CPosRefire;
	        	  break;
	          case  A_TroopAttack:
	        	  st.acp1=TroopAttack;
	        	  break;
	          case  A_SargAttack:
	        	  st.acp1=SargAttack;
	        	  break;
	          case  A_HeadAttack:
	        	  st.acp1=HeadAttack;
	        	  break;
	          case  A_BruisAttack:
	        	  st.acp1=BruisAttack;
	        	  break;
	          case  A_SkullAttack:
	        	  st.acp1=SkullAttack;
	        	  break;
	          case  A_Metal:
	        	  st.acp1=Metal;
	        	  break;
	          case  A_SpidRefire:
	        	  st.acp1=SpidRefire;
	        	  break;
	          case  A_BabyMetal:
	        	  st.acp1=BabyMetal;
	        	  break;
	          case  A_BspiAttack:
	        	  st.acp1=BspiAttack;
	        	  break;
	          case  A_Hoof:
	        	  st.acp1=Hoof;
	        	  break;
	          case  A_CyberAttack:
	        	  st.acp1=CyberAttack;
	        	  break;
	          case  A_PainAttack:
	        	  st.acp1=PainAttack;
	        	  break;
	          case  A_PainDie:
	        	  st.acp1=PainDie;
	        	  break;
	          case  A_KeenDie:
	        	  st.acp1=KeenDie;
	              break;
	          case  A_BrainPain:
	        	  st.acp1= BrainPain;
	        	  break;
	          case  A_BrainScream:
	        	  st.acp1=BrainScream;
	        	  break;
	          case  A_BrainDie:
	        	  st.acp1= BrainDie;
	        	  break;
	          case  A_BrainAwake:
	        	  st.acp1=BrainAwake;
	        	  break;        	  
	          case  A_BrainSpit:
	        	  st.acp1=BrainSpit;
	        	  break;
	          case  A_SpawnSound:
	        	  st.acp1=SpawnSound;
	        	  break;
	          case  A_SpawnFly:
	        	  st.acp1=SpawnFly;
	        	  break;
	          case  A_BrainExplode:
	        	  st.acp1=BrainExplode;
	        	  break;
	          case  T_FireFlicker:
	        	  st.acpss=FireFlicker;
	        	  break;
	          case 	T_LightFlash:
	        	  st.acpss=LightFlash;
	        	  break;
	          case	T_StrobeFlash:
	        	  st.acpss=StrobeFlash;
	        	  break;
	          case	T_Glow:
	        	  st.acpss=Glow;
	        	  break;
	          case	T_MoveCeiling:
	        	  st.acpss=MoveCeiling;
	        	  break;
	          case	T_MoveFloor:
	        	  st.acpss=MoveFloor;
	        	  break;
	          case    T_VerticalDoor:
	              //_D_: changed this to make it work
	        	  st.acpss=VerticalDoor;
	              break;
	          case	T_PlatRaise:
	        	  st.acpss=PlatRaise;
	          	break;
	      }
	      
	  }
	
	public void doWireThinker(thinker_t st){
	      //System.out.println("Dispatching: "+action);
		if (st==null) return;
		
		// we don't want anything to be called on this.
		if (st.function==null){
			st.acp1=null;
			st.acp2=null;
			st.acpss=null;
			return;
		}
		
	      switch (st.function){
	          case P_MobjThinker:
	        	  st.acp1=MobjThinker;
	        	  break;
	          case A_Light0:
	        	  st.acp2=Light0;
	        	  break;
	          case A_WeaponReady:
	        	  st.acp2=WeaponReady;
	        	  break;        	  
	          case  A_Lower:
	        	  st.acp2=Lower;
	        	  break;
	          case  A_Raise:
	        	  st.acp2=Raise;
	        	  break;
	          case  A_Punch:
	        	  st.acp2=Punch;
	        	  break;
	          case A_ReFire:
	        	  st.acp2=ReFire;
	        	  break;
	          case A_FirePistol:
	        	  st.acp2= FirePistol;
	        	  break;
	          case  A_Light1:
	        	  st.acp2=Light1;
	        	  break;
	          case A_FireShotgun:
	        	  st.acp2=FireShotgun;
	        	  break;
	          case  A_Light2:
	        	  st.acp2=Light2;
	        	  break;
	          case A_FireShotgun2:
	        	  st.acp2=FireShotgun2;
	        	  break;
	          case  A_CheckReload:
	        	  st.acp2=CheckReload;
	        	  break;
	          case A_OpenShotgun2:
	        	  st.acp2=OpenShotgun2;
	        	  break;
	          case A_LoadShotgun2:
	        	  st.acp2=LoadShotgun2;
	        	  break;
	          case A_CloseShotgun2:
	        	  st.acp2=CloseShotgun2;
	        	  break;
	          case A_FireCGun:
	        	  st.acp2=FireCGun;
	        	  break;
	          case A_GunFlash:
	        	  st.acp2=GunFlash;
	        	  break;
	          case A_FireMissile:
	        	  st.acp2=FireMissile;
	        	  break;
	          case A_Saw:
	        	  st.acp2=Saw;
	        	  break;
	          case A_FirePlasma:
	        	  st.acp2=FirePlasma;
	        	  break;
	          case A_BFGsound:
	        	  st.acp2=BFGsound;
	        	  break;
	          case A_FireBFG:
	        	  st.acp2=FireBFG;
	        	  break;
	          case A_BFGSpray:
	        	  st.acp1=BFGSpray;
	        	  break;
	          case A_Explode:
	        	  st.acp1=Explode;
	        	  break;
	          case A_Pain:
	        	  st.acp1=Pain;
	        	  break;
	          case A_PlayerScream:
	        	  st.acp1=PlayerScream;
	        	  break;
	          case A_Fall:
	        	  st.acp1=Fall;
	        	  break;
	          case  A_XScream:
	        	  st.acp1=XScream;
	        	  break;
	          case A_Look:
	        	  st.acp1=Look;
	        	  break;
	          case  A_Chase:
	        	  st.acp1=Chase;
	        	  break;
	          case  A_FaceTarget:
	        	  st.acp1=FaceTarget;
	        	  break;
	          case  A_PosAttack:
	        	  st.acp1=PosAttack;
	        	  break;
	          case  A_Scream:
	        	  st.acp1=Scream;
	        	  break;
	          case  A_SPosAttack:
	        	  st.acp1=SPosAttack;
	        	  break;
	          case  A_VileChase:
	        	  st.acp1=VileChase;
	        	  break;
	          case  A_VileStart:
	        	  st.acp1=VileStart;
	        	  break;
	          case  A_VileTarget:
	        	  st.acp1=VileTarget;
	        	  break;
	          case  A_VileAttack:
	        	  st.acp1=VileAttack;
	        	  break;
	          case  A_StartFire:
	        	  st.acp1=StartFire;
	        	  break;
	          case  A_Fire:
	        	  st.acp1=Fire;
	        	  break;
	          case  A_FireCrackle:
	        	  st.acp1=FireCrackle;
	        	  break;
	          case  A_Tracer:
	        	  st.acp1=Tracer;
	        	  break;
	          case  A_SkelWhoosh:
	        	  st.acp1=SkelWhoosh;
	        	  break;
	          case  A_SkelFist:
	        	  st.acp1=SkelFist;
	        	  break;
	          case  A_SkelMissile:
	        	  st.acp1=SkelMissile;
	        	  break;
	          case  A_FatRaise:
	        	  st.acp1=FatRaise;
	        	  break;
	          case  A_FatAttack1:
	        	  st.acp1=FatAttack1;
	        	  break;
	          case  A_FatAttack2:
	        	  st.acp1=FatAttack2;
	        	  break;
	          case A_FatAttack3:
	        	  st.acp1= FatAttack3;
	        	  break;
	          case  A_BossDeath:
	        	  st.acp1=BossDeath;
	        	  break;
	          case  A_CPosAttack:
	        	  st.acp1=CPosAttack;
	        	  break;
	          case  A_CPosRefire:
	        	  st.acp1=CPosRefire;
	        	  break;
	          case  A_TroopAttack:
	        	  st.acp1=TroopAttack;
	        	  break;
	          case  A_SargAttack:
	        	  st.acp1=SargAttack;
	        	  break;
	          case  A_HeadAttack:
	        	  st.acp1=HeadAttack;
	        	  break;
	          case  A_BruisAttack:
	        	  st.acp1=BruisAttack;
	        	  break;
	          case  A_SkullAttack:
	        	  st.acp1=SkullAttack;
	        	  break;
	          case  A_Metal:
	        	  st.acp1=Metal;
	        	  break;
	          case  A_SpidRefire:
	        	  st.acp1=SpidRefire;
	        	  break;
	          case  A_BabyMetal:
	        	  st.acp1=BabyMetal;
	        	  break;
	          case  A_BspiAttack:
	        	  st.acp1=BspiAttack;
	        	  break;
	          case  A_Hoof:
	        	  st.acp1=Hoof;
	        	  break;
	          case  A_CyberAttack:
	        	  st.acp1=CyberAttack;
	        	  break;
	          case  A_PainAttack:
	        	  st.acp1=PainAttack;
	        	  break;
	          case  A_PainDie:
	        	  st.acp1=PainDie;
	        	  break;
	          case  A_KeenDie:
	        	  st.acp1=KeenDie;
	              break;
	          case  A_BrainPain:
	        	  st.acp1= BrainPain;
	        	  break;
	          case  A_BrainScream:
	        	  st.acp1=BrainScream;
	        	  break;
	          case  A_BrainDie:
	        	  st.acp1= BrainDie;
	        	  break;
	          case  A_BrainAwake:
	        	  st.acp1=BrainAwake;
	        	  break;        	  
	          case  A_BrainSpit:
	        	  st.acp1=BrainSpit;
	        	  break;
	          case  A_SpawnSound:
	        	  st.acp1=SpawnSound;
	        	  break;
	          case  A_SpawnFly:
	        	  st.acp1=SpawnFly;
	        	  break;
	          case  A_BrainExplode:
	        	  st.acp1=BrainExplode;
	        	  break;
	          case  T_FireFlicker:
	        	  st.acpss=FireFlicker;
	        	  break;
	          case 	T_LightFlash:
	        	  st.acpss=LightFlash;
	        	  break;
	          case	T_StrobeFlash:
	        	  st.acpss=StrobeFlash;
	        	  break;
	          case	T_Glow:
	        	  st.acpss=Glow;
	        	  break;
	          case	T_MoveCeiling:
	        	  st.acpss=MoveCeiling;
	        	  break;
	          case	T_MoveFloor:
	        	  st.acpss=MoveFloor;
	        	  break;
	          case    T_VerticalDoor:
	              //_D_: changed this to make it work
	        	  st.acpss=VerticalDoor;
	              break;
	          case	T_PlatRaise:
	        	  st.acpss=PlatRaise;
	          	break;
	          case	T_SlidingDoor:
	        	  st.acpss=SlidingDoor;
	          	break;
	      }
	      
	  }
	
	
	/** Causes object to move and perform actions.
	 *  Can only be called through the Actions dispatcher.
	 * 
	 * @param mobj
	 */
	      

	class P_MobjThinker implements ActionType1{
	    public void invoke(mobj_t  mobj){
			// momentum movement
			if (mobj.momx != 0 || mobj.momy != 0
					|| (eval(mobj.flags& MF_SKULLFLY))) {
				A.XYMovement(mobj);

				// FIXME: decent NOP/NULL/Nil function pointer please.
				if (mobj.function == think_t.NOP){
					return; // mobj was removed
				}
			}
			if ((mobj.z != mobj.floorz) || mobj.momz != 0) {
				mobj.ZMovement();

				// FIXME: decent NOP/NULL/Nil function pointer please.
				if (mobj.function == think_t.NOP){
					return; // mobj was removed
				}	
			}

			// cycle through states,
			// calling action functions at transitions
			if (mobj.tics != -1) {
				mobj.tics--;

				// you can cycle through multiple states in a tic
				if (!eval(mobj.tics))
					if (!mobj.SetMobjState(mobj.state.nextstate))
						return; // freed itself
			} else {
				// check for nightmare respawn
				if (!eval(mobj.flags& MF_COUNTKILL))
					return;

				if (!DS.respawnmonsters)
					return;

				mobj.movecount++;

				if (mobj.movecount < 12 * 35)
					return;

				if (eval(DS.leveltime& 31))
					return;

				if (RND.P_Random() > 4)
					return;

				A.NightmareRespawn(mobj);
			}
		}
	}

	class T_FireFlicker implements ActionTypeSS<fireflicker_t>{

		@Override
		public void invoke(fireflicker_t a) {
			a.FireFlicker();
			
		}
		
	}
	
	class T_LightFlash implements ActionTypeSS<lightflash_t>{

		@Override
		public void invoke(lightflash_t a) {
			a.LightFlash();			
		}
		
	}
	
	class T_StrobeFlash implements ActionTypeSS<strobe_t>{

		@Override
		public void invoke(strobe_t a) {
			a.StrobeFlash();			
		}
		
	}
	
	class T_Glow implements ActionTypeSS<glow_t>{

		@Override
		public void invoke(glow_t a) {
			a.Glow();
			
		}
		
	}
	
	class	T_MoveCeiling implements ActionTypeSS<ceiling_t>{

		@Override
		public void invoke(ceiling_t a) {
			A.MoveCeiling(a);			
		}
	
	}

	class T_MoveFloor implements ActionTypeSS<floormove_t>{

		@Override
		public void invoke(floormove_t a) {
			A.MoveFloor(a);			
		}
	
	}
	
	class	T_VerticalDoor implements ActionTypeSS<vldoor_t>{

		@Override
		public void invoke(vldoor_t a) {
			A.VerticalDoor(a);	
		}
	
	}

	class T_SlidingDoor implements ActionTypeSS<slidedoor_t> {

		@Override
		public void invoke(slidedoor_t door) {

			switch (door.status) {
			case sd_opening:
				if (door.timer-- == 0) {
					if (++door.frame == SlideDoor.SNUMFRAMES) {
						// IF DOOR IS DONE OPENING...
						LL.sides[door.line.sidenum[0]].midtexture = 0;
						LL.sides[door.line.sidenum[1]].midtexture = 0;
						door.line.flags &= ML_BLOCKING ^ 0xff;

						if (door.type == sdt_e.sdt_openOnly) {
							door.frontsector.specialdata = null;
							A.RemoveThinker(door);
							break;
						}

						door.timer = SlideDoor.SDOORWAIT;
						door.status = sd_e.sd_waiting;
					} else {
						// IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
						door.timer = SlideDoor.SWAITTICS;

						LL.sides[door.line.sidenum[0]].midtexture = (short) SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
						LL.sides[door.line.sidenum[1]].midtexture = (short) SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
					}
				}
				break;

			case sd_waiting:
				// IF DOOR IS DONE WAITING...
				if (door.timer-- == 0) {
					// CAN DOOR CLOSE?
					if (door.frontsector.thinglist != null
							|| door.backsector.thinglist != null) {
						door.timer = SlideDoor.SDOORWAIT;
						break;
					}

					// door.frame = SNUMFRAMES-1;
					door.status = sd_e.sd_closing;
					door.timer = SlideDoor.SWAITTICS;
				}
				break;

			case sd_closing:
				if (door.timer-- == 0) {
					if (--door.frame < 0) {
						// IF DOOR IS DONE CLOSING...
						door.line.flags |= ML_BLOCKING;
						door.frontsector.specialdata = null;
						A.RemoveThinker(door);
						break;
					} else {
						// IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
						door.timer = SlideDoor.SWAITTICS;

						LL.sides[door.line.sidenum[0]].midtexture = (short) SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
						LL.sides[door.line.sidenum[1]].midtexture = (short) SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
					}
				}
				break;
			}
		}
	}
	
	
	class	T_PlatRaise implements ActionTypeSS<plat_t>{

		@Override
		public void invoke(plat_t a) {
			A.PlatRaise(a);
		}
	
	}

	
	//
    // A_FaceTarget
    //
	
	public class A_FaceTarget implements ActionType1{
    public void invoke(mobj_t  actor)
    {   
        if (actor.target==null)
        return;
        
        actor.flags &= ~MF_AMBUSH;
        
        actor.angle = R.PointToAngle2 (actor.x,
                        actor.y,
                        actor.target.x,
                        actor.target.y)&BITS32;
        
        if (eval(actor.target.flags & MF_SHADOW))
        actor.angle += (RND.P_Random()-RND.P_Random())<<21;
        actor.angle&=BITS32;
    	}
    }


    //
    // A_PosAttack
    //
	class A_PosAttack implements ActionType1{
	    public void invoke(mobj_t  actor)
	    {   
        int     angle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;
        FaceTarget.invoke(actor);
        angle = (int) actor.angle;
        slope = A.AimLineAttack (actor, angle, MISSILERANGE);

        S.StartSound(actor, sfxenum_t.sfx_pistol);
        angle += (RND.P_Random()-RND.P_Random())<<20;
        damage = ((RND.P_Random()%5)+1)*3;
        A.LineAttack (actor, angle, MISSILERANGE, slope, damage);
	    }
	}


	class A_SPosAttack implements ActionType1{
        public void invoke(mobj_t  actor)
        {   
        int     i;
        long     angle;
        long     bangle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;

        S.StartSound(actor, sfxenum_t.sfx_shotgn);
        A_FaceTarget (actor);
        bangle = actor.angle;
        slope = A.AimLineAttack (actor, bangle, MISSILERANGE);

        for (i=0 ; i<3 ; i++)
        {
        angle = bangle + ((RND.P_Random()-RND.P_Random())<<20);
        damage = ((RND.P_Random()%5)+1)*3;
        A.LineAttack (actor, angle, MISSILERANGE, slope, damage);
        }
        }
	}

	class A_CPosAttack implements ActionType1{
        public void invoke(mobj_t  actor)
        {   
        long     angle;
        long     bangle;
        int     damage;
        int     slope;
        
        if (actor.target==null)
        return;

        S.StartSound(actor, sfxenum_t.sfx_shotgn);
        A_FaceTarget (actor);
        bangle = actor.angle;
        slope = A.AimLineAttack (actor, bangle, MISSILERANGE);

        angle = bangle + ((RND.P_Random()-RND.P_Random())<<20);
        damage = ((RND.P_Random()%5)+1)*3;
        A.LineAttack (actor, angle, MISSILERANGE, slope, damage);
        }
	}

	class A_CPosRefire implements ActionType1{
        public void invoke(mobj_t  actor){
        // keep firing unless target got out of sight
        A_FaceTarget (actor);

        if (RND.P_Random () < 40)
        return;

        if (actor.target==null
        || actor.target.health <= 0
        || !EN.CheckSight (actor, actor.target) )
        {
        actor.SetMobjState ( actor.info.seestate);
        }
        }
	}


	class A_SpidRefire implements ActionType1{
        public void invoke(mobj_t  actor){
        // keep firing unless target got out of sight
        A_FaceTarget (actor);

        if (RND.P_Random () < 10)
        return;

        if (actor.target==null
        || actor.target.health <= 0
        || !EN.CheckSight (actor, actor.target) )
        {
        actor.SetMobjState ( actor.info.seestate);
        	}
        }
	}

	class A_BspiAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);

        // launch a missile
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_ARACHPLAZ);
        }
	}


    //
    // A_TroopAttack
    //
	class A_TroopAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int     damage;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (A.EN.CheckMeleeRange (actor))
        {
        	S.StartSound(actor, sfxenum_t.sfx_claw);
        damage = (RND.P_Random()%8+1)*3;
        A.DamageMobj (actor.target, actor, actor, damage);
        return;
        }

        
        // launch a missile
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_TROOPSHOT);
        }
	}

    class A_SargAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int     damage;

        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (EN.CheckMeleeRange (actor))
        {
        damage = ((RND.P_Random()%10)+1)*4;
        A.DamageMobj (actor.target, actor, actor, damage);
        }
        }
    }


    class A_HeadAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int     damage;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        if (EN.CheckMeleeRange (actor))
        {
        damage = (RND.P_Random()%6+1)*10;
        A.DamageMobj (actor.target, actor, actor, damage);
        return;
        }
        
        // launch a missile
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_HEADSHOT);
        }
    }

    class A_CyberAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_ROCKET);
        }
    }

    class A_BruisAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int     damage;
        
        if (actor.target==null)
        return;
            
        if (EN.CheckMeleeRange (actor))
        {
        	S.StartSound(actor, sfxenum_t.sfx_claw);
        damage = (RND.P_Random()%8+1)*10;
        A.DamageMobj (actor.target, actor, actor, damage);
        return;
        }
        
        // launch a missile
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_BRUISERSHOT);
        }
       }


    //
    // A_SkelMissile
    //
    class A_SkelMissile implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  mo;
        
        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        actor.z += 16*FRACUNIT;    // so missile spawns higher
        mo = A.SpawnMissile (actor, actor.target, mobjtype_t.MT_TRACER);
        actor.z -= 16*FRACUNIT;    // back to normal

        mo.x += mo.momx;
        mo.y += mo.momy;
        mo.tracer = actor.target;
        }
    }

    private static final int TRACEANGLE = 0xc000000;

    class A_Tracer implements ActionType1{
        public void invoke(mobj_t  actor){ 
        long exact; //angle_t
        int dist,slope; // fixed
        mobj_t  dest;
        mobj_t  th;
            
        if (eval(DS.gametic &3))
        return;
        
        // spawn a puff of smoke behind the rocket      
        A.SpawnPuff (actor.x, actor.y, actor.z);
        
        th = A.SpawnMobj (actor.x-actor.momx,
                  actor.y-actor.momy,
                  actor.z, mobjtype_t.MT_SMOKE);
        
        th.momz = MAPFRACUNIT;
        th.tics -= RND.P_Random()&3;
        if (th.tics < 1)
        th.tics = 1;
        
        // adjust direction
        dest = actor.tracer;
        
        if (dest==null || dest.health <= 0)
        return;
        
        // change angle 
        exact = R.PointToAngle2 (actor.x,
                     actor.y,
                     dest.x,
                     dest.y)&BITS32;
        
        // MAES: let's analyze the logic here...
        // So exact is the angle between the missile and its target. 

        if (exact != actor.angle) // missile is already headed there dead-on.
        {
        if (exact - actor.angle > ANG180)
        {
            actor.angle -= TRACEANGLE;
            actor.angle&=BITS32;
            if (((exact - actor.angle)&BITS32) < ANG180)
            actor.angle = exact;
        }
        else
        {
            actor.angle += TRACEANGLE;
            actor.angle&=BITS32;
            if (((exact - actor.angle)&BITS32) > ANG180)
            actor.angle = exact;
        }
        }
        
        // MAES: fixed and sped up.
        int exact2 = Tables.toBAMIndex(actor.angle);
        actor.momx = FixedMul (actor.info.speed, finecosine[exact2]);
        actor.momy = FixedMul (actor.info.speed, finesine[exact2]);
        
        // change slope
        dist = AproxDistance (dest.x - actor.x,
                    dest.y - actor.y);
        
        dist = dist / actor.info.speed;

        if (dist < 1)
        dist = 1;
        slope = (dest.z+40*FRACUNIT - actor.z) / dist;

        if (slope < actor.momz)
        actor.momz -= FRACUNIT/8;
        else
        actor.momz += FRACUNIT/8;
        }
    }

    class A_SkelWhoosh implements ActionType1{
        public void invoke(mobj_t  actor){ 
        if (actor.target==null)
        return;
        A_FaceTarget (actor);
        S.StartSound(actor,sfxenum_t.sfx_skeswg);
        }
    }
    
    class A_SkelFist implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int     damage;

        if (actor.target==null)
        return;
            
        A_FaceTarget (actor);
        
        if (EN.CheckMeleeRange (actor))
        {
        damage = ((RND.P_Random()%10)+1)*6;
        S.StartSound(actor, sfxenum_t.sfx_skepch);
        A.DamageMobj (actor.target, actor, actor, damage);
        }
        }
    }

    //
    // A_VileChase
    // Check for ressurecting a body
    //
    class A_VileChase implements ActionType1{
        public void invoke(mobj_t  actor){ 
        int         xl;
        int         xh;
        int         yl;
        int         yh;
        
        int         bx;
        int         by;

        mobjinfo_t     info;
        mobj_t      temp;
        
        if (actor.movedir != DI_NODIR)
        {
        // check for corpses to raise
        A.VileCheck.viletryx =
            actor.x + actor.info.speed*xspeed[actor.movedir];
        A.VileCheck.viletryy =
            actor.y + actor.info.speed*yspeed[actor.movedir];

        xl = LL.getSafeBlockX(A.VileCheck.viletryx - LL.bmaporgx - MAXRADIUS*2);
        xh = LL.getSafeBlockX(A.VileCheck.viletryx - LL.bmaporgx + MAXRADIUS*2);
        yl = LL.getSafeBlockY(A.VileCheck.viletryy - LL.bmaporgy - MAXRADIUS*2);
        yh = LL.getSafeBlockY(A.VileCheck.viletryy - LL.bmaporgy + MAXRADIUS*2);
        
        A.VileCheck.vileobj = actor;
        for (bx=xl ; bx<=xh ; bx++)
        {
            for (by=yl ; by<=yh ; by++)
            {
            // Call PIT_VileCheck to check
            // whether object is a corpse
            // that can be raised.
            if (!A.BlockThingsIterator(bx,by,A.VileCheck))
            {
                // got one!
                temp = actor.target;
                actor.target = A.VileCheck.corpsehit;
                A_FaceTarget (actor);
                actor.target = temp;
                        
                actor.SetMobjState ( statenum_t.S_VILE_HEAL1);
                S.StartSound(A.VileCheck.corpsehit, sfxenum_t.sfx_slop);
                info = A.VileCheck.corpsehit.info;
                
                A.VileCheck.corpsehit.SetMobjState (info.raisestate);
                A.VileCheck.corpsehit.height <<= 2;
                A.VileCheck.corpsehit.flags = info.flags;
                A.VileCheck.corpsehit.health = info.spawnhealth;
                A.VileCheck.corpsehit.target = null;

                return;
            }
            }
        }
        }

        // Return to normal attack.
        Chase.invoke (actor);
        }
    }


    //
    // A_VileStart
    //
    class A_VileStart implements ActionType1{
        public void invoke(mobj_t  actor){ 
    	S.StartSound(actor, sfxenum_t.sfx_vilatk);
        }
    }


    //
    // A_Fire
    // Keep fire in front of player unless out of sight
    //
    
    class A_StartFire implements ActionType1{
        public void invoke(mobj_t  actor){ 
    	S.StartSound(actor,sfxenum_t.sfx_flamst);
        Fire.invoke(actor);
        }
    }

    class A_FireCrackle implements ActionType1{
        public void invoke(mobj_t  actor){ 
    	S.StartSound(actor,sfxenum_t.sfx_flame);
        Fire.invoke(actor);
        }
     }
    
    //
    // A_FirePistol
    //
    class A_FirePistol implements ActionType2{
    public void invoke( player_t player, pspdef_t psp ) 
    {
  	  S.StartSound(player.mo, sfxenum_t.sfx_pistol);

        player.mo.SetMobjState ( statenum_t.S_PLAY_ATK2);
        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

        player.SetPsprite (
              ps_flash,
              weaponinfo[player.readyweapon.ordinal()].flashstate);

        A.P_BulletSlope (player.mo);
        A.P_GunShot (player.mo, !eval(player.refire));
    	}
    }

    //
    // A_FireShotgun
    //
    class A_FireShotgun implements ActionType2{
        public void invoke( player_t player, pspdef_t psp ) 
        {
        int     i;
        
        S.StartSound(player.mo, sfxenum_t.sfx_shotgn);
        player.mo.SetMobjState ( statenum_t.S_PLAY_ATK2);

        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

        player.SetPsprite (
              ps_flash,
              weaponinfo[player.readyweapon.ordinal()].flashstate);

        A.P_BulletSlope (player.mo);
        
        for (i=0 ; i<7 ; i++)
        A.P_GunShot (player.mo, false);
        }
    }


    /**
     * A_FireShotgun2
     */
    
    class A_FireShotgun2 implements ActionType2{
        public void invoke( player_t player, pspdef_t psp ) 
        {
        int     i;
        long angle;
        int     damage;
            
        
        S.StartSound (player.mo, sfxenum_t.sfx_dshtgn);
        player.mo.SetMobjState (statenum_t.S_PLAY_ATK2);

        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]-=2;

        player.SetPsprite (
              ps_flash,
              weaponinfo[player.readyweapon.ordinal()].flashstate);

        A.P_BulletSlope (player.mo);
        
        for (i=0 ; i<20 ; i++)
        {
        damage = 5*(RND.P_Random ()%3+1);
        angle = player.mo.angle;
        angle += (RND.P_Random()-RND.P_Random())<<19;
        A.LineAttack (player.mo,
                  angle,
                  MISSILERANGE,
                  A.bulletslope + ((RND.P_Random()-RND.P_Random())<<5), damage);
        }
    }
    }
    
    
    class A_Fire implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  dest;
        //long    an;
            
        dest = actor.tracer;
        if (dest==null)
        return;
            
        // don't move it if the vile lost sight
        if (!EN.CheckSight (actor.target, dest) )
        return;

        // an = dest.angle >>> ANGLETOFINESHIFT;

        A.UnsetThingPosition (actor);
        actor.x = dest.x + FixedMul (24*FRACUNIT, finecosine(dest.angle));
        actor.y = dest.y + FixedMul (24*FRACUNIT, finesine(dest.angle));
        actor.z = dest.z;
        LL.SetThingPosition (actor);
        }
    }



    //
    // A_VileTarget
    // Spawn the hellfire
    //
    class A_VileTarget implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  fog;
        
        if (actor.target==null)
        return;

        A_FaceTarget (actor);

        fog = A.SpawnMobj (actor.target.x,
                   actor.target.y,
                   actor.target.z, mobjtype_t.MT_FIRE);
        
        actor.tracer = fog;
        fog.target = actor;
        fog.tracer = actor.target;
        Fire.invoke(fog);
        }
    }




    //
    // A_VileAttack
    //
    class A_VileAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  fire;
        //int     an;
        
        if (actor.target==null)
        return;
        
        A_FaceTarget (actor);

        if (!EN.CheckSight (actor, actor.target) )
        return;

        S.StartSound(actor, sfxenum_t.sfx_barexp);
        A.DamageMobj (actor.target, actor, actor, 20);
        actor.target.momz = 1000*MAPFRACUNIT/actor.target.info.mass;
        
       // an = actor.angle >> ANGLETOFINESHIFT;

        fire = actor.tracer;

        if (fire==null)
        return;
            
        // move the fire between the vile and the player
        fire.x = actor.target.x - FixedMul (24*FRACUNIT, finecosine(actor.angle));
        fire.y = actor.target.y - FixedMul (24*FRACUNIT, finesine(actor.angle));  
        A.RadiusAttack (fire, actor, 70 );
        }
    }



    //
    // Mancubus attack,
    // firing three missiles (bruisers)
    // in three different directions?
    // Doesn't look like it. 
    //
    private static final long FATSPREAD =  (ANG90/8);

    class A_FatRaise implements ActionType1{
        public void invoke(mobj_t  actor){ 
        A_FaceTarget (actor);
        S.StartSound(actor, sfxenum_t.sfx_manatk);
        }
    }


    class A_FatAttack1 implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  mo;
        int     an;
        
        A_FaceTarget (actor);
        // Change direction  to ...
        actor.angle += FATSPREAD;
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);

        mo = A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle += FATSPREAD;
        an = Tables.toBAMIndex(mo.angle);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
        }
    }

    class A_FatAttack2 implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  mo;
        int     an;

        A_FaceTarget (actor);
        // Now here choose opposite deviation.
        actor.angle -= FATSPREAD;
        A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);

        mo = A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle -= FATSPREAD*2;
        an = Tables.toBAMIndex(mo.angle);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
        }
    }

    class A_FatAttack3 implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t  mo;
        int     an;

        A_FaceTarget (actor);
        
        mo = A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle -= FATSPREAD/2;
        an = Tables.toBAMIndex(mo.angle);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);

        mo = A.SpawnMissile (actor, actor.target, mobjtype_t.MT_FATSHOT);
        mo.angle += FATSPREAD/2;
        an = Tables.toBAMIndex(mo.angle);
        mo.momx = FixedMul (mo.info.speed, finecosine[an]);
        mo.momy = FixedMul (mo.info.speed, finesine[an]);
        }
    }



    private static final int SKULLSPEED  =    (20*MAPFRACUNIT);
    /**      
     * SkullAttack
     * Fly at the player like a missile.
     */
    class A_SkullAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        mobj_t      dest;
        int     an;
        int         dist;

        if (actor.target==null)
        return;
            
        dest = actor.target;   
        actor.flags |= MF_SKULLFLY;

        S.StartSound(actor, actor.info.attacksound);
        FaceTarget.invoke(actor);
        an = Tables.toBAMIndex(actor.angle);
        actor.momx = FixedMul (SKULLSPEED, finecosine[an]);
        actor.momy = FixedMul (SKULLSPEED, finesine[an]);
        dist = AproxDistance (dest.x - actor.x, dest.y - actor.y);
        dist = dist / SKULLSPEED;
        
        if (dist < 1)
        dist = 1;
        actor.momz = (dest.z+(dest.height>>1) - actor.z) / dist;
        }
    }


    /**
     * A_PainShootSkull
     * Spawn a lost soul and launch it at the target
     * It's not a valid callback like the others, actually.
     * No idea if some DEH patch does use it to cause
     * mayhem though.
     * 
     */
    
    private void A_PainShootSkull
    ( mobj_t    actor,
      long   angle )
    {
        int x,y, z; // fixed
        
        mobj_t  newmobj;
        int an; // angle
        int     prestep;
        int     count;
        thinker_t  currentthinker;

        // count total number of skull currently on the level
        count = 0;

        currentthinker = A.thinkercap.next;
        while (currentthinker != A.thinkercap)
        {
        if (   (currentthinker.function == think_t.P_MobjThinker)
            && ((mobj_t)currentthinker).type == mobjtype_t.MT_SKULL)
            count++;
        currentthinker = currentthinker.next;
        }

        // if there are allready 20 skulls on the level,
        // don't spit another one
        if (count > MAXSKULLS)
        return;


        // okay, there's playe for another one
        an = Tables.toBAMIndex(angle);
        
        prestep =
        4*FRACUNIT
        + 3*(actor.info.radius + mobjinfo[mobjtype_t.MT_SKULL.ordinal()].radius)/2;
        
        x = actor.x + FixedMul (prestep, finecosine[an]);
        y = actor.y + FixedMul (prestep, finesine[an]);
        z = actor.z + 8*FRACUNIT;
            
        newmobj = A.SpawnMobj (x , y, z, mobjtype_t.MT_SKULL);

        // Check for movements.
        if (!A.TryMove (newmobj, newmobj.x, newmobj.y))
        {
        // kill it immediately
        A.DamageMobj (newmobj,actor,actor,10000);   
        return;
        }
            
        newmobj.target = actor.target;
        SkullAttack.invoke (newmobj);
    }


    //
    // A_PainAttack
    // Spawn a lost soul and launch it at the target
    // 
    class A_PainAttack implements ActionType1{
        public void invoke(mobj_t  actor){ 
        if (actor.target==null)
        return;

        A_FaceTarget (actor);
        A_PainShootSkull (actor, actor.angle);
        }
    }


    class A_PainDie implements ActionType1{
        public void invoke(mobj_t  actor){ 
        Fall.invoke (actor);
        A_PainShootSkull (actor, actor.angle+ANG90);
        A_PainShootSkull (actor, actor.angle+ANG180);
        A_PainShootSkull (actor, actor.angle+ANG270);
        }
    }






    class A_Scream implements ActionType1 {
        public void invoke(mobj_t  actor){ 
        int     sound;
        
        switch (actor.info.deathsound)
        {
          case sfx_None:
        return;
            
          case sfx_podth1:
          case sfx_podth2:
          case sfx_podth3:
        sound = sfxenum_t.sfx_podth1.ordinal() + RND.P_Random ()%3;
        break;
            
          case sfx_bgdth1:
          case sfx_bgdth2:
        sound = sfxenum_t.sfx_bgdth1.ordinal() + RND.P_Random ()%2;
        break;
        
          default:
        sound = actor.info.deathsound.ordinal();
        break;
        }

        // Check for bosses.
        if (actor.type==mobjtype_t.MT_SPIDER
        || actor.type == mobjtype_t.MT_CYBORG)
        {
        // full volume
        S.StartSound (null, sound);
        }
        else
        S.StartSound (actor, sound);
        }
    }


    /**
     * A_WeaponReady
     * The player can fire the weapon
     * or change to another weapon at this time.
     * Follows after getting weapon up,
     * or after previous attack/fire sequence.
     */

    class A_WeaponReady implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
    {   
        statenum_t  newstate;
        int     angle;
        
        // get out of attack state
        if (player.mo.state == states[statenum_t.S_PLAY_ATK1.ordinal()]
        || player.mo.state == states[statenum_t.S_PLAY_ATK2.ordinal()] )
        {
        player.mo.SetMobjState (statenum_t.S_PLAY);
        }
        
        if (player.readyweapon == weapontype_t.wp_chainsaw
        && psp.state == states[statenum_t.S_SAW.ordinal()])
        {
      	  S.StartSound(player.mo, sfxenum_t.sfx_sawidl);
        }
        
        // check for change
        //  if player is dead, put the weapon away
        if (player.pendingweapon != weapontype_t.wp_nochange || !eval(player.health[0]))
        {
        // change weapon
        //  (pending weapon should allready be validated)
        newstate = weaponinfo[player.readyweapon.ordinal()].downstate;
        player.SetPsprite ( player_t.ps_weapon, newstate);
        return; 
        }
        
        // check for fire
        //  the missile launcher and bfg do not auto fire
        if (eval(player.cmd.buttons & BT_ATTACK))
        {
        if ( !player.attackdown
             || (player.readyweapon != weapontype_t.wp_missile
             && player.readyweapon != weapontype_t.wp_bfg) )
        {
            player.attackdown = true;
            EN.FireWeapon (player);      
            return;
        }
        }
        else
        player.attackdown = false;
        
        // bob the weapon based on movement speed
        angle = (128*DS.leveltime)&FINEMASK;
        psp.sx = FRACUNIT + FixedMul (player.bob, finecosine[angle]);
        angle &= FINEANGLES/2-1;
        psp.sy = player_t.WEAPONTOP + FixedMul (player.bob, finesine[angle]);
    	}
    }
    
    //
    // A_Raise
    //
    class A_Raise implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        statenum_t  newstate;
        
        //System.out.println("Trying to raise weapon");      
        //System.out.println(player.readyweapon + " height: "+psp.sy);
        psp.sy -= RAISESPEED;

        if (psp.sy > WEAPONTOP ) {
        //System.out.println("Not on top yet, exit and repeat.");
        return;
        }
        
        psp.sy = WEAPONTOP;
        
        // The weapon has been raised all the way,
        //  so change to the ready state.
        newstate = weaponinfo[player.readyweapon.ordinal()].readystate;
        //System.out.println("Weapon raised, setting new state.");
        
        player.SetPsprite (ps_weapon, newstate);
        }
    }


    //
    // A_ReFire
    // The player can re-fire the weapon
    // without lowering it entirely.
    //
    class A_ReFire implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        
        // check for fire
        //  (if a weaponchange is pending, let it go through instead)
        if ( eval(player.cmd.buttons & BT_ATTACK) 
         && player.pendingweapon == weapontype_t.wp_nochange
         && eval(player.health[0]))
        {
        player.refire++;
        EN.FireWeapon (player);
        }
        else
        {
        player.refire = 0;
        player.CheckAmmo ();
        }
        }
    }
    
    
    //
    // A_GunFlash
    //
    class A_GunFlash implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {    
        player.mo.SetMobjState (statenum_t.S_PLAY_ATK2);
        player.SetPsprite (ps_flash,weaponinfo[player.readyweapon.ordinal()].flashstate);
        }
    }

    //
    // A_Punch
    //
    class A_Punch implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        long angle; //angle_t
        int     damage;
        int     slope;
        
        damage = (RND.P_Random ()%10+1)<<1;

        if (eval(player.powers[pw_strength]))    
        damage *= 10;

        angle = player.mo.angle;
        //angle = (angle+(RND.P_Random()-RND.P_Random())<<18)/*&BITS32*/;
        // _D_: for some reason, punch didnt work until I change this
        // I think it's because of "+" VS "<<" prioritys...
        angle += (RND.P_Random()-RND.P_Random())<<18;
        slope = A.AimLineAttack (player.mo, angle, MELEERANGE);
        A.LineAttack (player.mo, angle, MELEERANGE, slope, damage);

        // turn to face target
        if (eval(A.linetarget))
        {
      	  S.StartSound(player.mo, sfxenum_t.sfx_punch);
        player.mo.angle = R.PointToAngle2 (player.mo.x,
                             player.mo.y,
                             A.linetarget.x,
                             A.linetarget.y)&BITS32;
        }
    }	
    }


    //
    // A_Saw
    //
    class A_Saw implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        long angle; // angle_t
        int     damage;
        int     slope;

        damage = 2*(RND.P_Random ()%10+1);
        angle = player.mo.angle;
        angle += (RND.P_Random()-RND.P_Random())<<18;
        angle&=BITS32;
        
        // use meleerange + 1 se the puff doesn't skip the flash
        slope = A.AimLineAttack (player.mo, angle, MELEERANGE+1);
        A.LineAttack (player.mo, angle, MELEERANGE+1, slope, damage);

        if (!eval(A.linetarget))
        {
      	  S.StartSound(player.mo, sfxenum_t.sfx_sawful);
        return;
        }
        S.StartSound(player.mo, sfxenum_t.sfx_sawhit);
        
        // turn to face target
        angle = R.PointToAngle2 (player.mo.x, player.mo.y,
                     A.linetarget.x, A.linetarget.y)&BITS32;
        /* FIXME: this comparison is going to fail.... or not?
         If e.g. angle = 359 degrees (which will be mapped to a small negative number),       
         and player.mo.angle = 160 degrees (a large, positive value), the result will be a 
         large negative value, which will still be "greater" than ANG180.
         
         It seems that *differences* between angles will always compare correctly, but
         not direct inequalities.
         
        */
        
        // Yet another screwy place where unsigned BAM angles are used as SIGNED comparisons.
        long dangle= (angle - player.mo.angle);
        dangle&=BITS32;
        if (dangle > ANG180)
        {
        if ((int)dangle < -ANG90/20)
            player.mo.angle = angle + ANG90/21;
        else
            player.mo.angle -= ANG90/20;
        }
        else
        {
        if (dangle > ANG90/20)
            player.mo.angle = angle - ANG90/21;
        else
            player.mo.angle += ANG90/20;
        }
        player.mo.angle&=BITS32;
        player.mo.flags |= MF_JUSTATTACKED;
        }
    }


    //
    // A_FireMissile
    //
    class A_FireMissile implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;
        A.SpawnPlayerMissile (player.mo, mobjtype_t.MT_ROCKET);
        }
    }


    //
    // A_FireBFG
    //
    class A_FireBFG implements ActionType2 {
    	 
    	
    	 // plasma cells for a bfg attack
    	 // IDEA: make action functions partially parametrizable?
    	private static final int BFGCELLS      =  40;     
    	
    	public void invoke( player_t player, pspdef_t psp )
        {   
        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()] -= BFGCELLS;
        A.SpawnPlayerMissile (player.mo, mobjtype_t.MT_BFG);
        }
    }


    //
    // A_FireCGun
    //
    class A_FireCGun implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        // For convenience.
        int readyweap=player.readyweapon.ordinal();
        int flashstate=weaponinfo[readyweap].flashstate.ordinal();
        int current_state=psp.state.id;
        
        S.StartSound (player.mo, sfxenum_t.sfx_pistol);      
        if (!eval(player.ammo[weaponinfo[readyweap].ammo.ordinal()]))
        return;
            
        player.mo.SetMobjState (statenum_t.S_PLAY_ATK2);
        player.ammo[weaponinfo[readyweap].ammo.ordinal()]--;
        
        // MAES: Code to alternate between two different gun flashes
        // needed a clear rewrite, as it was way too messy.
        // We know that the flash states are a certain amount away from 
        // the firing states. This amount is two frames.
        player.SetPsprite (ps_flash,statenum_t.values()[flashstate+current_state-statenum_t.S_CHAIN1.ordinal()]
              );

        A.P_BulletSlope (player.mo);
        
        A.P_GunShot (player.mo, !eval(player.refire));
        }
    }
    
    //
    // A_FirePlasma
    //
    class A_FirePlasma implements ActionType2 {
    	public void invoke( player_t player, pspdef_t psp )
        {   
        player.ammo[weaponinfo[player.readyweapon.ordinal()].ammo.ordinal()]--;

        player.SetPsprite (
              ps_flash,
              weaponinfo[player.readyweapon.ordinal()].flashstate );

        A.SpawnPlayerMissile (player.mo, mobjtype_t.MT_PLASMA);
        }
    }
    
    class A_XScream implements ActionType1 {
        public void invoke(mobj_t  actor){ 
        S.StartSound (actor, sfxenum_t.sfx_slop); 
        }
    }

    class A_Pain implements ActionType1 {
        public void invoke(mobj_t  actor){ 
        if (actor.info.painsound!=null)
        	S.StartSound(actor, actor.info.painsound);   
        }
    }



    class A_Fall implements ActionType1 {
        public void invoke(mobj_t  actor){ 
        // actor is on ground, it can be walked over
        actor.flags &= ~MF_SOLID;

        // So change this if corpse objects
        // are meant to be obstacles.
        }
    }


    //
    // A_Explode
    //
    class A_Explode implements ActionType1 {
        public void invoke(mobj_t  thingy){ 
        A.RadiusAttack ( thingy, thingy.target, 128 );
        }
    }


    /**
     * A_BossDeath
     * Possibly trigger special effects
     * if on first boss level
     *
     * TODO: find out how Plutonia/TNT does cope with this.
     * Special clauses?
     *
     */
    class A_BossDeath implements ActionType1 {
        public void invoke(mobj_t  mo){ 
        thinker_t  th;
        mobj_t  mo2;
        line_t  junk = new line_t();
        int     i;
            
        if ( DS.isCommercial())
        {
        if (DS.gamemap != 7)
            return;
            
        if ((mo.type != mobjtype_t.MT_FATSO)
            && (mo.type != mobjtype_t.MT_BABY))
            return;
        }
        else
        {
        switch(DS.gameepisode)
        {
          case 1:
            if (DS.gamemap != 8)
            return;

            if (mo.type != mobjtype_t.MT_BRUISER)
            return;
            break;
            
          case 2:
            if (DS.gamemap != 8)
            return;

            if (mo.type != mobjtype_t.MT_CYBORG)
            return;
            break;
            
          case 3:
            if (DS.gamemap != 8)
            return;
            
            if (mo.type != mobjtype_t.MT_SPIDER)
            return;
            
            break;
            
          case 4:
            switch(DS.gamemap)
            {
              case 6:
            if (mo.type != mobjtype_t.MT_CYBORG)
                return;
            break;
            
              case 8: 
            if (mo.type != mobjtype_t.MT_SPIDER)
                return;
            break;
            
              default:
            return;
            }
            break;
            
          default:
            if (DS.gamemap != 8)
            return;
            break;
        }
            
        }

        
        // make sure there is a player alive for victory
        for (i=0 ; i<MAXPLAYERS ; i++)
        if (DS.playeringame[i] && DS.players[i].health[0] > 0)
            break;
        
        if (i==MAXPLAYERS)
        return; // no one left alive, so do not end game
        
        // scan the remaining thinkers to see
        // if all bosses are dead
        for (th = A.thinkercap.next ; th != A.thinkercap ; th=th.next)
        {
        if (th.function != think_t.P_MobjThinker)
            continue;
        
        mo2 = (mobj_t)th;
        if (mo2 != mo
            && mo2.type == mo.type
            && mo2.health > 0)
        {
            // other boss not dead
            return;
        }
        }
        
        // victory!
        if ( DS.isCommercial())
        {
        if (DS.gamemap == 7)
        {
            if (mo.type == mobjtype_t.MT_FATSO)
            {
            junk.tag = 666;
            A.DoFloor(junk,floor_e.lowerFloorToLowest);
            return;
            }
            
            if (mo.type == mobjtype_t.MT_BABY)
            {
            junk.tag = 667;
            A.DoFloor(junk,floor_e.raiseToTexture);
            return;
            }
        }
        }
        else
        {
        switch(DS.gameepisode)
        {
          case 1:
            junk.tag = 666;
            A.DoFloor (junk, floor_e.lowerFloorToLowest);
            return;
            
          case 4:
            switch(DS.gamemap)
            {
              case 6:
            junk.tag = 666;
            A.DoDoor (junk, vldoor_e.blazeOpen);
            return;
            
              case 8:
            junk.tag = 666;
            A.DoFloor (junk, floor_e.lowerFloorToLowest);
            return;
            }
        }
        }
        
        A.DM.ExitLevel ();
        }
    }

    class A_Hoof implements ActionType1 {
        public void invoke(mobj_t  mo){ 
    	S.StartSound(mo, sfxenum_t.sfx_hoof);
    	 Chase.invoke(mo);
        }
    }
    
    class A_KeenDie implements ActionType1 {
        public void invoke(mobj_t  mo){ 
        thinker_t  th;
        mobj_t mo2;
        line_t  junk = new line_t(); // MAES: fixed null 21/5/2011

        Fall.invoke(mo);
        
        // scan the remaining thinkers
        // to see if all Keens are dead
        for (th = A.thinkercap.next ; th != A.thinkercap ; th=th.next)
        {
        if (th.function != think_t.P_MobjThinker)
            continue;

        mo2 = (mobj_t)th;
        if (mo2 != mo
            && mo2.type == mo.type
            && mo2.health > 0)
        {
            // other Keen not dead
            return;     
        }
        }

        junk.tag = 666;
        A.DoDoor(junk,vldoor_e.open);
        }
    }

    class A_Metal implements ActionType1 {
        public void invoke(mobj_t  mo){ 
    	S.StartSound(mo, sfxenum_t.sfx_metal);
        Chase.invoke(mo);
        }
    }

    class A_BabyMetal implements ActionType1 {
        public void invoke(mobj_t  mo){ 
    	S.StartSound(mo, sfxenum_t.sfx_bspwlk);
        Chase.invoke (mo);
        }
    }
    
    //
    // A_BFGsound
    //
    class A_BFGsound implements ActionType2{
    	public void invoke( player_t player, pspdef_t psp )	{
    		S.StartSound(player.mo, sfxenum_t.sfx_bfg);
    	}	
    }
    
    //
    // A_BFGSpray
    // Spawn a BFG explosion on every monster in view
    //
    class A_BFGSpray implements ActionType1 {
        public void invoke(mobj_t  mo){ 
        int         i;
        int         j;
        int         damage;
        long     an; // angle_t
        
        // offset angles from its attack angle
        for (i=0 ; i<40 ; i++)
        {
        an = (mo.angle - ANG90/2 + ANG90/40*i)&BITS32;

        // mo.target is the originator (player)
        //  of the missile
        A.AimLineAttack (mo.target, an, 16*64*FRACUNIT);

        if (!eval(A.linetarget))
            continue;

        A.SpawnMobj (A.linetarget.x,
            A.linetarget.y,
            A.linetarget.z + (A.linetarget.height>>2),
                 mobjtype_t.MT_EXTRABFG);
        
        damage = 0;
        for (j=0;j<15;j++)
            damage += (RND.P_Random()&7) + 1;

        A.DamageMobj (A.linetarget, mo.target,mo.target, damage);
        }
        }
    }


    class A_OpenShotgun2 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
    	S.StartSound(player.mo, sfxenum_t.sfx_dbopn);
		}
    }
    
    class A_LoadShotgun2 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
    	S.StartSound(player.mo, sfxenum_t.sfx_dbload);
		}
    }

    //
    // A_Look
    // Stay in state until a player is sighted.
    //
    class A_Look implements ActionType1 {
        public void invoke(mobj_t  actor){ 
        mobj_t targ;
        boolean seeyou=false; // to avoid the fugly goto
        
        actor.threshold = 0;   // any shot will wake up
        targ = actor.subsector.sector.soundtarget;

        if (targ!=null
        && eval(targ.flags& MF_SHOOTABLE) )
        {
        actor.target = targ;

        if ( eval(actor.flags&MF_AMBUSH ))
        {
            seeyou= (EN.CheckSight (actor, actor.target));              
        } else
            seeyou=true;
        }
        if (!seeyou){
        if (!EN.LookForPlayers (actor, false) )
        return;
        }
        
        // go into chase state
      seeyou:
        if (actor.info.seesound!=null && actor.info.seesound!=sfxenum_t.sfx_None)
        {
        int     sound;
            
        switch (actor.info.seesound)
        {
          case sfx_posit1:
          case sfx_posit2:
          case sfx_posit3:
            sound = sfxenum_t.sfx_posit1.ordinal()+RND.P_Random()%3;
            break;

          case sfx_bgsit1:
          case sfx_bgsit2:
            sound = sfxenum_t.sfx_bgsit1.ordinal()+RND.P_Random()%2;
            break;

          default:
            sound = actor.info.seesound.ordinal();
            break;
        }

        if (actor.type==mobjtype_t.MT_SPIDER
            || actor.type == mobjtype_t.MT_CYBORG)
        {
            // full volume
        	S.StartSound(null, sound);
        }
        else
        	S.StartSound(actor, sound);
        }

        actor.SetMobjState(actor.info.seestate);
        }
    }
    
    class A_CloseShotgun2 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
    	S.StartSound(player.mo, sfxenum_t.sfx_dbcls);
        ReFire.invoke(player,psp);
		}
    }

    //
    // ?
    //
    class A_Light0 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
        player.extralight = 0;
		}
    }

    class A_Light1 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
        player.extralight = 1;
		}
    }
    
    class A_Light2 implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
        player.extralight = 2;
		}
    }

    
    //
    // A_Lower
    // Lowers current weapon,
    //  and changes weapon at bottom.
    //
    class A_Lower implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
        psp.sy += LOWERSPEED;

        // Is already down.
        if (psp.sy < WEAPONBOTTOM )
        return;

        // Player is dead.
        if (player.playerstate == PST_DEAD)
        {
        psp.sy = WEAPONBOTTOM;

        // don't bring weapon back up
        return;     
        }
        
        // The old weapon has been lowered off the screen,
        // so change the weapon and start raising it
        if (!eval(player.health[0]))
        {
        // Player is dead, so keep the weapon off screen.
        player.SetPsprite (ps_weapon, statenum_t.S_NULL);
        return; 
        }
        
        player.readyweapon = player.pendingweapon; 

        player.BringUpWeapon ();
		}
    }
    
    class A_CheckReload implements ActionType2{
		public void invoke (player_t player,pspdef_t psp){
        player.CheckAmmo ();
    /*
        if (player.ammo[am_shell]<2)
        P_SetPsprite (player, ps_weapon, S_DSNR1);
    */}
    }
    

    // Brain status
    mobj_t[]      braintargets=new mobj_t[NUMBRAINTARGETS];
    int     numbraintargets;
    int     braintargeton;
    
    class A_BrainAwake implements ActionType1{
    	
        public void invoke(mobj_t  mo){ 
        thinker_t  thinker;
        mobj_t  m;
        
        // find all the target spots
        numbraintargets = 0;
        braintargeton = 0;
        
        thinker = A.thinkercap.next;
        for (thinker = A.thinkercap.next ;
         thinker != A.thinkercap ;
         thinker = thinker.next)
        {
        if (thinker.function != think_t.P_MobjThinker)
            continue;   // not a mobj

        m = (mobj_t)thinker;

        if (m.type == mobjtype_t.MT_BOSSTARGET )
        {
            braintargets[numbraintargets] = m;
            numbraintargets++;
        }
        }
        
        S.StartSound(null,sfxenum_t.sfx_bossit);
        }
    }


    class A_BrainPain implements ActionType1 {
        public void invoke(mobj_t  mo){ 
    	S.StartSound(null,sfxenum_t.sfx_bospn);
        }
    }


    class A_BrainScream implements ActionType1{
        public void invoke(mobj_t  mo){ 
        int     x;
        int     y;
        int     z;
        mobj_t  th;
        
        for (x=mo.x - 196*FRACUNIT ; x< mo.x + 320*FRACUNIT ; x+= FRACUNIT*8)
        {
        y = mo.y - 320*FRACUNIT;
        z = 128 + RND.P_Random()*2*FRACUNIT;
        th = A.SpawnMobj (x,y,z, mobjtype_t.MT_ROCKET);
        th.momz = RND.P_Random()*512;

        th.SetMobjState (statenum_t.S_BRAINEXPLODE1);

        th.tics -= RND.P_Random()&7;
        if (th.tics < 1)
            th.tics = 1;
        }
        
        S.StartSound(null,sfxenum_t.sfx_bosdth);
        }
    }



    class A_BrainExplode implements ActionType1{
        public void invoke(mobj_t  mo){ 
        int     x;
        int     y;
        int     z;
        mobj_t  th;
        
        x = mo.x + (RND.P_Random () - RND.P_Random ())*2048;
        y = mo.y;
        z = 128 + RND.P_Random()*2*FRACUNIT;
        th = A.SpawnMobj (x,y,z, mobjtype_t.MT_ROCKET);
        th.momz = RND.P_Random()*512;

        th.SetMobjState (statenum_t.S_BRAINEXPLODE1);

        th.tics -= RND.P_Random()&7;
        if (th.tics < 1)
        th.tics = 1;
        }
    }


    class A_BrainDie implements ActionType1{
        public void invoke(mobj_t  mo){ 
        DG.ExitLevel ();
        }
    }

    private int  easy = 0;
    
    class A_BrainSpit implements ActionType1{
        public void invoke(mobj_t  mo){ 
        mobj_t  targ;
        mobj_t  newmobj;
        
        easy ^= 1;
        if (DS.gameskill.ordinal() <= skill_t.sk_easy.ordinal() && (easy==0))
        return;
            
        // shoot a cube at current target
        targ = braintargets[braintargeton];
        
        // Load-time fix: awake on zero numbrain targets, if A_BrainSpit is called.
        if (numbraintargets==0) {BrainAwake.invoke(mo);
        						 return;
        						}
        braintargeton = (braintargeton+1)%numbraintargets;

        // spawn brain missile
        newmobj = A.SpawnMissile (mo, targ, mobjtype_t.MT_SPAWNSHOT);
        newmobj.target = targ;
        newmobj.reactiontime =
        (int) (((targ.y - mo.y)/newmobj.momy) / newmobj.state.tics);

         S.StartSound(null, sfxenum_t.sfx_bospit);
        }
    }


    // travelling cube sound
    class A_SpawnSound implements ActionType1{
        public void invoke(mobj_t  mo){ 
        S.StartSound (mo,sfxenum_t.sfx_boscub);
        SpawnFly.invoke(mo);
        }
    }

     
    class A_SpawnFly implements ActionType1{
        public void invoke(mobj_t  mo){ 
        mobj_t  newmobj;
        mobj_t  fog;
        mobj_t  targ;
        int     r;
        mobjtype_t  type;
        
        if (--mo.reactiontime!=0)
        return; // still flying
        
        targ = mo.target;

        // First spawn teleport fog.
        fog = A.SpawnMobj (targ.x, targ.y, targ.z, mobjtype_t.MT_SPAWNFIRE);
        S.StartSound (fog, sfxenum_t.sfx_telept);

        // Randomly select monster to spawn.
        r = RND.P_Random ();

        // Probability distribution (kind of :),
        // decreasing likelihood.
        if ( r<50 )
        type = mobjtype_t.MT_TROOP;
        else if (r<90)
        type = mobjtype_t.MT_SERGEANT;
        else if (r<120)
        type = mobjtype_t.MT_SHADOWS;
        else if (r<130)
        type = mobjtype_t.MT_PAIN;
        else if (r<160)
        type = mobjtype_t.MT_HEAD;
        else if (r<162)
        type = mobjtype_t.MT_VILE;
        else if (r<172)
        type = mobjtype_t.MT_UNDEAD;
        else if (r<192)
        type = mobjtype_t.MT_BABY;
        else if (r<222)
        type = mobjtype_t.MT_FATSO;
        else if (r<246)
        type = mobjtype_t.MT_KNIGHT;
        else
        type = mobjtype_t.MT_BRUISER;      

        newmobj = A.SpawnMobj (targ.x, targ.y, targ.z, type);
        if (EN.LookForPlayers (newmobj, true) )
        newmobj.SetMobjState (newmobj.info.seestate);
        
        // telefrag anything in this spot
        A.TeleportMove (newmobj, newmobj.x, newmobj.y);

        // remove self (i.e., cube).
        A.RemoveMobj (mo);
    }
    }
//
//P_MobjThinker
//
  
      class A_PlayerScream implements ActionType1{
          public void invoke(mobj_t  actor){ 
          // Default death sound.
          sfxenum_t     sound = sfxenum_t.sfx_pldeth;
          
          if ( DS.isCommercial()
          &&  (actor.health < -50))
          {
          // IF THE PLAYER DIES
          // LESS THAN -50% WITHOUT GIBBING
          sound =  sfxenum_t.sfx_pdiehi;
          }
          
          S.StartSound(actor, sound);
          }
      }

      //
      // A_FaceTarget. This is called by many other functions anyway, 
      // other than autonomously.
      //
      void A_FaceTarget (mobj_t  actor)
      {   
          if (actor.target==null)
          return;
          
          actor.flags &= ~MF_AMBUSH;
          
          actor.angle = R.PointToAngle2 (actor.x,
                          actor.y,
                          actor.target.x,
                          actor.target.y)&BITS32;
          
          if (eval(actor.target.flags & MF_SHADOW))
          actor.angle += (RND.P_Random()-RND.P_Random())<<21;
          actor.angle&=BITS32;
      }
      
      /**
       * A_Chase
       * Actor has a melee attack,
       * so it tries to close as fast as possible
       */
      
      class A_Chase implements ActionType1{
          public void invoke(mobj_t  actor){ 
          int     delta;
          boolean nomissile=false; // for the fugly goto

          if (actor.reactiontime!=0)
              actor.reactiontime--;


          // modify target threshold
          if  (actor.threshold!=0)
          {
              if (actor.target==null
                      || actor.target.health <= 0)
              {
                  actor.threshold = 0;
              }
              else
                  actor.threshold--;
          }

          // turn towards movement direction if not there yet
          if (actor.movedir < 8)
          {
              actor.angle &= (7<<29);
              actor.angle&=BITS32;
              // Nice problem, here!
              delta = (int) (actor.angle - (actor.movedir << 29));

              if (delta > 0)
                  actor.angle -= ANG45;
              else if (delta < 0)
                  actor.angle += ANG45;
              
              actor.angle&=BITS32;
          }

          
          
          if (actor.target==null
                  || !eval(actor.target.flags&MF_SHOOTABLE))
          {
              // look for a new target
              if (EN.LookForPlayers(actor,true))
                  return;     // got a new target

              actor.SetMobjState (actor.info.spawnstate);
              return;
          }

          // do not attack twice in a row
          if (eval(actor.flags & MF_JUSTATTACKED))
          {
              actor.flags &= ~MF_JUSTATTACKED;
              if (DS.gameskill != skill_t.sk_nightmare && !DS.fastparm)
                  A.NewChaseDir (actor);
              return;
          }

          // check for melee attack
          if (actor.info.meleestate!=statenum_t.S_NULL /*null*/
                  && EN.CheckMeleeRange (actor))
          {
              if (actor.info.attacksound!=null){
                  S.StartSound (actor, actor.info.attacksound);
              }
              actor.SetMobjState(actor.info.meleestate);
              return;
          }

          // check for missile attack
          if (actor.info.missilestate!=statenum_t.S_NULL /*!= null*/) //_D_: this caused a bug where Demon for example were disappearing
          {
              // Assume that a missile attack is possible
              if (DS.gameskill.ordinal() < skill_t.sk_nightmare.ordinal()
                      && !DS.fastparm && actor.movecount!=0)
              {
                  // Uhm....no.
                  nomissile=true;
              }
              else
                  if (!EN.CheckMissileRange (actor))
                      nomissile=true; // Out of range

              if (!nomissile){
                  // Perform the attack
                  actor.SetMobjState ( actor.info.missilestate);
                  actor.flags |= MF_JUSTATTACKED;
                  return;
              }
          }

          // This should be executed always, if not averted by returns.
          
              // possibly choose another target
              if (DS.netgame
                      && actor.threshold==0
                      && !EN.CheckSight (actor, actor.target) )
              {
                  if (EN.LookForPlayers(actor,true))
                      return; // got a new target
              }

          // chase towards player
          if (--actor.movecount<0
                  || !A.Move (actor))
          {
             A.NewChaseDir (actor);
          }

          // make active sound
          if (actor.info.activesound!=null
                  && RND.P_Random() < 3)
          {
              S.StartSound (actor, actor.info.activesound);
          }
          }
      }
      

      
}
