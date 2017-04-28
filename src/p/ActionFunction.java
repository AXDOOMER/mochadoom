/*
 * Copyright (C) 1993-1996 Id Software, Inc.
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package p;

import doom.player_t;
import doom.thinker_t;
import static p.ActionFunction.Param.*;

/**
 * In vanilla doom there is union called actionf_t that can hold
 * one of the three types: actionf_p1, actionf_v and actionf_p2
 * 
 * typedef union
 * {
 *   actionf_p1	acp1;
 *   actionf_v	acv;
 *   actionf_p2	acp2;
 *
 * } actionf_t;
 * 
 * For those unfamiliar with C, the union can have only one value
 * assigned with all the values combined solving the behavior of
 * logical and of all of them)
 * 
 * actionf_p1, actionf_v and actionf_p2 are defined as these:
 * 
 * typedef  void (*actionf_v)();
 * typedef  void (*actionf_p1)( void* );
 * typedef  void (*actionf_p2)( void*, void* );
 * 
 * As you can see, they are pointers, so they all occupy the same space
 * in the union: the length of the memory pointer.
 * 
 * Effectively, this means that you can write to any of the three fields
 * the pointer to the function correspoding to the field, and
 * it will completely overwrite any other function assigned in other
 * two fields. Even more: the other fields will have the same pointer,
 * just with wrong type.
 * 
 * In Mocha Doom, this were addressed differently. A special helper enum
 * was created to hold possible names of the functions, and they were checked
 * by name, not by equality of the objects (object == object if point the same)
 * assigned to one of three fields. But, not understanding the true nature
 * of C's unions, in Mocha Doom all three fields were preserved and threated
 * like they can hold some different information at the same time.
 * 
 * I present hereby the solution that will both simplify the definition
 * and usage of the action functions, and provide a way to achieve the
 * exact same behavior as would be in C: if you assign the function,
 * you will replace the old one (virtually, "all the three fields")
 * and you can call any function with 0 to 2 arguments.
 * 
 * Also to store the functions in the same place where we declare them,
 * an Observer pattern is implemented, requiring the function caller
 * to provide himself or any sufficient class that implements the Observer
 * contract to provide the information needed for holding the state
 * of action functions.
 * 
 * - Good Sign 2017/04/28
 * 
 * Thinkers can either have one parameter of type (mobj_t),
 * Or otherwise be sector specials, flickering lights etc.
 * Those are atypical and need special handling.
 */
public interface ActionFunction {
    enum Param {
        Mobj,
        Thinker,
        PlayerSprite
    }
    
    enum think_t {
        NOP(Thinker),
        A_Light0(PlayerSprite),
        A_WeaponReady(PlayerSprite),
        A_Lower(PlayerSprite),
        A_Raise(PlayerSprite),
        A_Punch(PlayerSprite),
        A_ReFire(PlayerSprite),
        A_FirePistol(PlayerSprite),
        A_Light1(PlayerSprite),
        A_FireShotgun(PlayerSprite),
        A_Light2(PlayerSprite),
        A_FireShotgun2(PlayerSprite),
        A_CheckReload(PlayerSprite),
        A_OpenShotgun2(PlayerSprite),
        A_LoadShotgun2(PlayerSprite),
        A_CloseShotgun2(PlayerSprite),
        A_FireCGun(PlayerSprite),
        A_GunFlash(PlayerSprite),
        A_FireMissile(PlayerSprite),
        A_Saw(PlayerSprite),
        A_FirePlasma(PlayerSprite),
        A_BFGsound(PlayerSprite),
        A_FireBFG(PlayerSprite),
        A_BFGSpray(Mobj),
        A_Explode(Mobj),
        A_Pain(Mobj),
        A_PlayerScream(Mobj),
        A_Fall(Mobj),
        A_XScream(Mobj),
        A_Look(Mobj),
        A_Chase(Mobj),
        A_FaceTarget(Mobj),
        A_PosAttack(Mobj),
        A_Scream(Mobj),
        A_SPosAttack(Mobj),
        A_VileChase(Mobj),
        A_VileStart(Mobj),
        A_VileTarget(Mobj),
        A_VileAttack(Mobj),
        A_StartFire(Mobj),
        A_Fire(Mobj),
        A_FireCrackle(Mobj),
        A_Tracer(Mobj),
        A_SkelWhoosh(Mobj),
        A_SkelFist(Mobj),
        A_SkelMissile(Mobj),
        A_FatRaise(Mobj),
        A_FatAttack1(Mobj),
        A_FatAttack2(Mobj),
        A_FatAttack3(Mobj),
        A_BossDeath(Mobj),
        A_CPosAttack(Mobj),
        A_CPosRefire(Mobj),
        A_TroopAttack(Mobj),
        A_SargAttack(Mobj),
        A_HeadAttack(Mobj),
        A_BruisAttack(Mobj),
        A_SkullAttack(Mobj),
        A_Metal(Mobj),
        A_SpidRefire(Mobj),
        A_BabyMetal(Mobj),
        A_BspiAttack(Mobj),
        A_Hoof(Mobj),
        A_CyberAttack(Mobj),
        A_PainAttack(Mobj),
        A_PainDie(Mobj),
        A_KeenDie(Mobj),
        A_BrainPain(Mobj),
        A_BrainScream(Mobj),
        A_BrainDie(Mobj),
        A_BrainAwake(Mobj),
        A_BrainSpit(Mobj),
        A_SpawnSound(Mobj),
        A_SpawnFly(Mobj),
        A_BrainExplode(Mobj),
        P_MobjThinker(Mobj),
        T_FireFlicker(Thinker),
        T_LightFlash(Thinker),
        T_StrobeFlash(Thinker),
        T_Glow(Thinker),
        T_MoveCeiling(Thinker),
        T_MoveFloor(Thinker),
        T_VerticalDoor(Thinker),
        T_PlatRaise(Thinker),
        T_SlidingDoor(Thinker);
        
        private final Param actionf_t;

        private think_t(Param actionf_t) {
            this.actionf_t = actionf_t;
        }
        
        public boolean ac(Param a) {
            return actionf_t == a;
        }
        
        public void acp1(actionf_t f, mobj_t mobj) {
            f.acp1(this, mobj);
        }
        
        public void acp2(actionf_t f, player_t player, pspdef_t pspdef) {
            f.acp2(this, player, pspdef);
        }
        
        public void acv(actionf_t f, thinker_t thinker) {
            f.acv(this, thinker);
        }
    }
    
    interface actionf_t {
        void acp1(think_t think_t, mobj_t mobj);        
        void acp2(think_t think_t, player_t player, pspdef_t pspdef);
        void acv(think_t think_t, thinker_t thinker);
    }
}
