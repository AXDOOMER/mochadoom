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

import doom.SourceCode.D_Think;
import doom.SourceCode.D_Think.actionf_t;
import doom.SourceCode.actionf_p1;
import doom.SourceCode.actionf_p2;
import doom.SourceCode.actionf_v;
import doom.player_t;
import doom.thinker_t;
import java.util.logging.Level;
import java.util.logging.Logger;
import mochadoom.Loggers;

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
 * an Command pattern is implemented, requiring the function caller
 * to provide himself or any sufficient class that implements the Client
 * contract to provide the information needed for holding the state
 * of action functions.
 * 
 * - Good Sign 2017/04/28
 * 
 * Thinkers can either have one parameter of type (mobj_t),
 * Or otherwise be sector specials, flickering lights etc.
 * Those are atypical and need special handling.
 */
public enum ActiveStates {
    NOP(ActiveStates::nop, ThinkerConsumer.class),
    A_Light0(Actions.Registry::A_Light0, PlayerSpriteConsumer.class),
    A_WeaponReady(Actions.Registry::A_WeaponReady, PlayerSpriteConsumer.class),
    A_Lower(Actions.Registry::A_Lower, PlayerSpriteConsumer.class),
    A_Raise(Actions.Registry::A_Raise, PlayerSpriteConsumer.class),
    A_Punch(Actions.Registry::A_Punch, PlayerSpriteConsumer.class),
    A_ReFire(Actions.Registry::A_ReFire, PlayerSpriteConsumer.class),
    A_FirePistol(Actions.Registry::A_FirePistol, PlayerSpriteConsumer.class),
    A_Light1(Actions.Registry::A_Light1, PlayerSpriteConsumer.class),
    A_FireShotgun(Actions.Registry::A_FireShotgun, PlayerSpriteConsumer.class),
    A_Light2(Actions.Registry::A_Light2, PlayerSpriteConsumer.class),
    A_FireShotgun2(Actions.Registry::A_FireShotgun2, PlayerSpriteConsumer.class),
    A_CheckReload(Actions.Registry::A_CheckReload, PlayerSpriteConsumer.class),
    A_OpenShotgun2(Actions.Registry::A_OpenShotgun2, PlayerSpriteConsumer.class),
    A_LoadShotgun2(Actions.Registry::A_LoadShotgun2, PlayerSpriteConsumer.class),
    A_CloseShotgun2(Actions.Registry::A_CloseShotgun2, PlayerSpriteConsumer.class),
    A_FireCGun(Actions.Registry::A_FireCGun, PlayerSpriteConsumer.class),
    A_GunFlash(Actions.Registry::A_GunFlash, PlayerSpriteConsumer.class),
    A_FireMissile(Actions.Registry::A_FireMissile, PlayerSpriteConsumer.class),
    A_Saw(Actions.Registry::A_Saw, PlayerSpriteConsumer.class),
    A_FirePlasma(Actions.Registry::A_FirePlasma, PlayerSpriteConsumer.class),
    A_BFGsound(Actions.Registry::A_BFGsound, PlayerSpriteConsumer.class),
    A_FireBFG(Actions.Registry::A_FireBFG, PlayerSpriteConsumer.class),
    A_BFGSpray(Actions.Registry::A_BFGSpray, MobjConsumer.class),
    A_Explode(Actions.Registry::A_Explode, MobjConsumer.class),
    A_Pain(Actions.Registry::A_Pain, MobjConsumer.class),
    A_PlayerScream(Actions.Registry::A_PlayerScream, MobjConsumer.class),
    A_Fall(Actions.Registry::A_Fall, MobjConsumer.class),
    A_XScream(Actions.Registry::A_XScream, MobjConsumer.class),
    A_Look(Actions.Registry::A_Look, MobjConsumer.class),
    A_Chase(Actions.Registry::A_Chase, MobjConsumer.class),
    A_FaceTarget(Actions.Registry::A_FaceTarget, MobjConsumer.class),
    A_PosAttack(Actions.Registry::A_PosAttack, MobjConsumer.class),
    A_Scream(Actions.Registry::A_Scream, MobjConsumer.class),
    A_SPosAttack(Actions.Registry::A_SPosAttack, MobjConsumer.class),
    A_VileChase(Actions.Registry::A_VileChase, MobjConsumer.class),
    A_VileStart(Actions.Registry::A_VileStart, MobjConsumer.class),
    A_VileTarget(Actions.Registry::A_VileTarget, MobjConsumer.class),
    A_VileAttack(Actions.Registry::A_VileAttack, MobjConsumer.class),
    A_StartFire(Actions.Registry::A_StartFire, MobjConsumer.class),
    A_Fire(Actions.Registry::A_Fire, MobjConsumer.class),
    A_FireCrackle(Actions.Registry::A_FireCrackle, MobjConsumer.class),
    A_Tracer(Actions.Registry::A_Tracer, MobjConsumer.class),
    A_SkelWhoosh(Actions.Registry::A_SkelWhoosh, MobjConsumer.class),
    A_SkelFist(Actions.Registry::A_SkelFist, MobjConsumer.class),
    A_SkelMissile(Actions.Registry::A_SkelMissile, MobjConsumer.class),
    A_FatRaise(Actions.Registry::A_FatRaise, MobjConsumer.class),
    A_FatAttack1(Actions.Registry::A_FatAttack1, MobjConsumer.class),
    A_FatAttack2(Actions.Registry::A_FatAttack2, MobjConsumer.class),
    A_FatAttack3(Actions.Registry::A_FatAttack3, MobjConsumer.class),
    A_BossDeath(Actions.Registry::A_BossDeath, MobjConsumer.class),
    A_CPosAttack(Actions.Registry::A_CPosAttack, MobjConsumer.class),
    A_CPosRefire(Actions.Registry::A_CPosRefire, MobjConsumer.class),
    A_TroopAttack(Actions.Registry::A_TroopAttack, MobjConsumer.class),
    A_SargAttack(Actions.Registry::A_SargAttack, MobjConsumer.class),
    A_HeadAttack(Actions.Registry::A_HeadAttack, MobjConsumer.class),
    A_BruisAttack(Actions.Registry::A_BruisAttack, MobjConsumer.class),
    A_SkullAttack(Actions.Registry::A_SkullAttack, MobjConsumer.class),
    A_Metal(Actions.Registry::A_Metal, MobjConsumer.class),
    A_SpidRefire(Actions.Registry::A_SpidRefire, MobjConsumer.class),
    A_BabyMetal(Actions.Registry::A_BabyMetal, MobjConsumer.class),
    A_BspiAttack(Actions.Registry::A_BspiAttack, MobjConsumer.class),
    A_Hoof(Actions.Registry::A_Hoof, MobjConsumer.class),
    A_CyberAttack(Actions.Registry::A_CyberAttack, MobjConsumer.class),
    A_PainAttack(Actions.Registry::A_PainAttack, MobjConsumer.class),
    A_PainDie(Actions.Registry::A_PainDie, MobjConsumer.class),
    A_KeenDie(Actions.Registry::A_KeenDie, MobjConsumer.class),
    A_BrainPain(Actions.Registry::A_BrainPain, MobjConsumer.class),
    A_BrainScream(Actions.Registry::A_BrainScream, MobjConsumer.class),
    A_BrainDie(Actions.Registry::A_BrainDie, MobjConsumer.class),
    A_BrainAwake(Actions.Registry::A_BrainAwake, MobjConsumer.class),
    A_BrainSpit(Actions.Registry::A_BrainSpit, MobjConsumer.class),
    A_SpawnSound(Actions.Registry::A_SpawnSound, MobjConsumer.class),
    A_SpawnFly(Actions.Registry::A_SpawnFly, MobjConsumer.class),
    A_BrainExplode(Actions.Registry::A_BrainExplode, MobjConsumer.class),
    P_MobjThinker(Actions.Registry::P_MobjThinker, MobjConsumer.class),
    T_FireFlicker(Actions.Registry::T_FireFlicker, ThinkerConsumer.class),
    T_LightFlash(Actions.Registry::T_LightFlash, ThinkerConsumer.class),
    T_StrobeFlash(Actions.Registry::T_StrobeFlash, ThinkerConsumer.class),
    T_Glow(Actions.Registry::T_Glow, ThinkerConsumer.class),
    T_MoveCeiling(Actions.Registry::T_MoveCeiling, ThinkerConsumer.class),
    T_MoveFloor(Actions.Registry::T_MoveFloor, ThinkerConsumer.class),
    T_VerticalDoor(Actions.Registry::T_VerticalDoor, ThinkerConsumer.class),
    T_PlatRaise(Actions.Registry::T_PlatRaise, ThinkerConsumer.class),
    T_SlidingDoor(Actions.Registry::T_SlidingDoor, ThinkerConsumer.class);
    
    private final static Logger LOGGER = Loggers.getLogger(ActiveStates.class.getName());
    
    private final ParamClass actionFunction;
    private final Class<? extends ParamClass> paramType;

    private <T extends ParamClass> ActiveStates(final T actionFunction, final Class<T> paramType) {
        this.actionFunction = actionFunction;
        this.paramType = paramType;
    }
    
    private static void nop(Object... o) {}

    @actionf_p1
    @D_Think.C(actionf_t.acp1)
    public interface MobjConsumer extends ParamClass<MobjConsumer> {
        void accept(Actions.Registry a, mobj_t m);
    }
    
    @actionf_v
    @D_Think.C(actionf_t.acv)
    public interface ThinkerConsumer extends ParamClass<ThinkerConsumer> {
        void accept(Actions.Registry a, thinker_t t);
    }
    
    @actionf_p2
    @D_Think.C(actionf_t.acp2)
    public interface PlayerSpriteConsumer extends ParamClass<PlayerSpriteConsumer> {
        void accept(Actions.Registry a, player_t p, pspdef_t s);
    }
    
    private interface ParamClass<T extends ParamClass<T>> {}
    
    public boolean isParamType(final Class<?> paramType) {
        return this.paramType == paramType;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ParamClass<T>> T fun(final Class<T> paramType) {
        if (this.paramType != paramType) {
            LOGGER.log(Level.WARNING, "Wrong paramType for state: {0}", this);
            return null;
        }
        
        // don't believe, it's checked
        return (T) this.actionFunction;
    }
}
