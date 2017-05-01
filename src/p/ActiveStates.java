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
import p.ActionSystem.AbstractCommand;
import p.Actions.Registry;

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
    A_Light0(AbstractCommand::A_Light0, PlayerSpriteConsumer.class),
    A_WeaponReady(AbstractCommand::A_WeaponReady, PlayerSpriteConsumer.class),
    A_Lower(AbstractCommand::A_Lower, PlayerSpriteConsumer.class),
    A_Raise(AbstractCommand::A_Raise, PlayerSpriteConsumer.class),
    A_Punch(AbstractCommand::A_Punch, PlayerSpriteConsumer.class),
    A_ReFire(AbstractCommand::A_ReFire, PlayerSpriteConsumer.class),
    A_FirePistol(AbstractCommand::A_FirePistol, PlayerSpriteConsumer.class),
    A_Light1(AbstractCommand::A_Light1, PlayerSpriteConsumer.class),
    A_FireShotgun(AbstractCommand::A_FireShotgun, PlayerSpriteConsumer.class),
    A_Light2(AbstractCommand::A_Light2, PlayerSpriteConsumer.class),
    A_FireShotgun2(AbstractCommand::A_FireShotgun2, PlayerSpriteConsumer.class),
    A_CheckReload(AbstractCommand::A_CheckReload, PlayerSpriteConsumer.class),
    A_OpenShotgun2(AbstractCommand::A_OpenShotgun2, PlayerSpriteConsumer.class),
    A_LoadShotgun2(AbstractCommand::A_LoadShotgun2, PlayerSpriteConsumer.class),
    A_CloseShotgun2(AbstractCommand::A_CloseShotgun2, PlayerSpriteConsumer.class),
    A_FireCGun(AbstractCommand::A_FireCGun, PlayerSpriteConsumer.class),
    A_GunFlash(AbstractCommand::A_GunFlash, PlayerSpriteConsumer.class),
    A_FireMissile(AbstractCommand::A_FireMissile, PlayerSpriteConsumer.class),
    A_Saw(AbstractCommand::A_Saw, PlayerSpriteConsumer.class),
    A_FirePlasma(AbstractCommand::A_FirePlasma, PlayerSpriteConsumer.class),
    A_BFGsound(AbstractCommand::A_BFGsound, PlayerSpriteConsumer.class),
    A_FireBFG(AbstractCommand::A_FireBFG, PlayerSpriteConsumer.class),
    A_BFGSpray(AbstractCommand::A_BFGSpray, MobjConsumer.class),
    A_Explode(AbstractCommand::A_Explode, MobjConsumer.class),
    A_Pain(AbstractCommand::A_Pain, MobjConsumer.class),
    A_PlayerScream(AbstractCommand::A_PlayerScream, MobjConsumer.class),
    A_Fall(AbstractCommand::A_Fall, MobjConsumer.class),
    A_XScream(AbstractCommand::A_XScream, MobjConsumer.class),
    A_Look(AbstractCommand::A_Look, MobjConsumer.class),
    A_Chase(AbstractCommand::A_Chase, MobjConsumer.class),
    A_FaceTarget(AbstractCommand::A_FaceTarget, MobjConsumer.class),
    A_PosAttack(AbstractCommand::A_PosAttack, MobjConsumer.class),
    A_Scream(AbstractCommand::A_Scream, MobjConsumer.class),
    A_SPosAttack(AbstractCommand::A_SPosAttack, MobjConsumer.class),
    A_VileChase(AbstractCommand::A_VileChase, MobjConsumer.class),
    A_VileStart(AbstractCommand::A_VileStart, MobjConsumer.class),
    A_VileTarget(AbstractCommand::A_VileTarget, MobjConsumer.class),
    A_VileAttack(AbstractCommand::A_VileAttack, MobjConsumer.class),
    A_StartFire(AbstractCommand::A_StartFire, MobjConsumer.class),
    A_Fire(AbstractCommand::A_Fire, MobjConsumer.class),
    A_FireCrackle(AbstractCommand::A_FireCrackle, MobjConsumer.class),
    A_Tracer(AbstractCommand::A_Tracer, MobjConsumer.class),
    A_SkelWhoosh(AbstractCommand::A_SkelWhoosh, MobjConsumer.class),
    A_SkelFist(AbstractCommand::A_SkelFist, MobjConsumer.class),
    A_SkelMissile(AbstractCommand::A_SkelMissile, MobjConsumer.class),
    A_FatRaise(AbstractCommand::A_FatRaise, MobjConsumer.class),
    A_FatAttack1(AbstractCommand::A_FatAttack1, MobjConsumer.class),
    A_FatAttack2(AbstractCommand::A_FatAttack2, MobjConsumer.class),
    A_FatAttack3(AbstractCommand::A_FatAttack3, MobjConsumer.class),
    A_BossDeath(AbstractCommand::A_BossDeath, MobjConsumer.class),
    A_CPosAttack(AbstractCommand::A_CPosAttack, MobjConsumer.class),
    A_CPosRefire(AbstractCommand::A_CPosRefire, MobjConsumer.class),
    A_TroopAttack(AbstractCommand::A_TroopAttack, MobjConsumer.class),
    A_SargAttack(AbstractCommand::A_SargAttack, MobjConsumer.class),
    A_HeadAttack(AbstractCommand::A_HeadAttack, MobjConsumer.class),
    A_BruisAttack(AbstractCommand::A_BruisAttack, MobjConsumer.class),
    A_SkullAttack(AbstractCommand::A_SkullAttack, MobjConsumer.class),
    A_Metal(AbstractCommand::A_Metal, MobjConsumer.class),
    A_SpidRefire(AbstractCommand::A_SpidRefire, MobjConsumer.class),
    A_BabyMetal(AbstractCommand::A_BabyMetal, MobjConsumer.class),
    A_BspiAttack(AbstractCommand::A_BspiAttack, MobjConsumer.class),
    A_Hoof(AbstractCommand::A_Hoof, MobjConsumer.class),
    A_CyberAttack(AbstractCommand::A_CyberAttack, MobjConsumer.class),
    A_PainAttack(AbstractCommand::A_PainAttack, MobjConsumer.class),
    A_PainDie(AbstractCommand::A_PainDie, MobjConsumer.class),
    A_KeenDie(AbstractCommand::A_KeenDie, MobjConsumer.class),
    A_BrainPain(AbstractCommand::A_BrainPain, MobjConsumer.class),
    A_BrainScream(AbstractCommand::A_BrainScream, MobjConsumer.class),
    A_BrainDie(AbstractCommand::A_BrainDie, MobjConsumer.class),
    A_BrainAwake(AbstractCommand::A_BrainAwake, MobjConsumer.class),
    A_BrainSpit(AbstractCommand::A_BrainSpit, MobjConsumer.class),
    A_SpawnSound(AbstractCommand::A_SpawnSound, MobjConsumer.class),
    A_SpawnFly(AbstractCommand::A_SpawnFly, MobjConsumer.class),
    A_BrainExplode(AbstractCommand::A_BrainExplode, MobjConsumer.class),
    P_MobjThinker(AbstractCommand::P_MobjThinker, MobjConsumer.class),
    T_FireFlicker(AbstractCommand::T_FireFlicker, ThinkerConsumer.class),
    T_LightFlash(AbstractCommand::T_LightFlash, ThinkerConsumer.class),
    T_StrobeFlash(AbstractCommand::T_StrobeFlash, ThinkerConsumer.class),
    T_Glow(AbstractCommand::T_Glow, ThinkerConsumer.class),
    T_MoveCeiling(AbstractCommand::T_MoveCeiling, ThinkerConsumer.class),
    T_MoveFloor(AbstractCommand::T_MoveFloor, ThinkerConsumer.class),
    T_VerticalDoor(AbstractCommand::T_VerticalDoor, ThinkerConsumer.class),
    T_PlatRaise(AbstractCommand::T_PlatRaise, ThinkerConsumer.class),
    T_SlidingDoor(AbstractCommand::T_SlidingDoor, ThinkerConsumer.class);
    
    private final static Logger LOGGER = Loggers.getLogger(ActiveStates.class.getName());
    
    private final ParamClass<?> actionFunction;
    private final Class<? extends ParamClass<?>> paramType;

    private <T extends ParamClass<?>> ActiveStates(final T actionFunction, final Class<T> paramType) {
        this.actionFunction = actionFunction;
        this.paramType = paramType;
    }
    
    private static void nop(Object... o) {}

    @actionf_p1
    @D_Think.C(actionf_t.acp1)
    public interface MobjConsumer<R extends Registry & AbstractCommand<R>> extends ParamClass<MobjConsumer<R>> {
    	void accept(AbstractCommand<R> a, mobj_t m);
    }
    
    @actionf_v
    @D_Think.C(actionf_t.acv)
    public interface ThinkerConsumer<R extends Registry & AbstractCommand<R>> extends ParamClass<ThinkerConsumer<R>> {
    	void accept(AbstractCommand<R> a, thinker_t t);
    }
    
    @actionf_p2
    @D_Think.C(actionf_t.acp2)
    public interface PlayerSpriteConsumer<R extends Registry & AbstractCommand<R>> extends ParamClass<PlayerSpriteConsumer<R>> {
    	void accept(AbstractCommand<R> a, player_t p, pspdef_t s);
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
