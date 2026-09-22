package com.viaversion.viaforge.blocks;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.common.compatibility.EntityPushRules.TeamRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityPushRulesTest {
    @Test public void originalPositionCodecAndZeroDeltaBoundaries() {
        assertEquals(3687/4096D,EntityPositionRules.relative(.9,1,false,false),0);
        assertEquals(.9,EntityPositionRules.relative(.9,0,true,false),0);
        assertEquals(3686/4096D,EntityPositionRules.relative(.9,0,false,false),0);
        assertEquals(3688/4096D,EntityPositionRules.relative(.9001,1,true,true),0);
        assertEquals(-3687/4096D,EntityPositionRules.relative(-.9,0,false,false),0);
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(498).rules().enabled(ClientRule.ROTATION_ONLY_ENTITY_UPDATE));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(573).rules().enabled(ClientRule.ROTATION_ONLY_ENTITY_UPDATE));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(736).rules().enabled(ClientRule.KEEP_ZERO_ENTITY_DELTA));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(751).rules().enabled(ClientRule.KEEP_ZERO_ENTITY_DELTA));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(760).rules().enabled(ClientRule.ROUND_ENTITY_DELTA));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(761).rules().enabled(ClientRule.ROUND_ENTITY_DELTA));
    }
    @Test public void originalFloatAndDoubleBranchesRemainDistinct() {
        assertArrayEquals(new double[]{.03162277728546167,.015811388642730836},EntityPushRules.impulse(.4,.2,0,false),0);
        assertArrayEquals(new double[]{.031622777072899885,.015811388536449943},EntityPushRules.impulse(.4,.2,0,true),0);
        assertArrayEquals(new double[]{-.03162277728546167,.015811388642730836},EntityPushRules.impulse(-.4,.2,0,false),0);
    }
    @Test public void originalCutoffAndLegacyReduction() {
        assertNull(EntityPushRules.impulse(0,0,0,false));
        assertNull(EntityPushRules.impulse(.009,0,0,true));
        assertNotNull(EntityPushRules.impulse((double).01F,0,0,false));
        assertArrayEquals(new double[]{0,0},EntityPushRules.impulse(.4,.2,1,false),0);
        assertArrayEquals(EntityPushRules.impulse(.4,.2,0,true),EntityPushRules.impulse(.4,.2,1,true),0);
    }
    @Test public void bothTeamsParticipateInTheOriginalPredicate() {
        for(TeamRule rule:TeamRule.values())for(boolean same:new boolean[]{true,false}) {
            assertFalse(EntityPushRules.collides(TeamRule.NEVER,rule,same));
            assertFalse(EntityPushRules.collides(rule,TeamRule.NEVER,same));
        }
        assertTrue(EntityPushRules.collides(TeamRule.ALWAYS,TeamRule.ALWAYS,false));
        assertFalse(EntityPushRules.collides(TeamRule.PUSH_OWN_TEAM,TeamRule.ALWAYS,true));
        assertTrue(EntityPushRules.collides(TeamRule.PUSH_OWN_TEAM,TeamRule.ALWAYS,false));
        assertTrue(EntityPushRules.collides(TeamRule.ALWAYS,TeamRule.PUSH_OTHER_TEAMS,true));
        assertFalse(EntityPushRules.collides(TeamRule.ALWAYS,TeamRule.PUSH_OTHER_TEAMS,false));
        assertFalse(EntityPushRules.collides(TeamRule.PUSH_OWN_TEAM,TeamRule.PUSH_OTHER_TEAMS,true));
        assertFalse(EntityPushRules.collides(TeamRule.PUSH_OWN_TEAM,TeamRule.PUSH_OTHER_TEAMS,false));
    }
    @Test public void exactBehaviorBoundariesAndNativeFallback() {
        for(int protocol:new int[]{47,-1,99999})assertFalse(CompatibilityRegistry.DEFAULT.resolve(protocol).rules().enabled(ClientRule.CLIENT_ENTITY_PUSH));
        for(int protocol:new int[]{107,110,340,404,477,754,755,758,759,776,777})
            assertTrue(CompatibilityRegistry.DEFAULT.resolve(protocol).rules().enabled(ClientRule.CLIENT_ENTITY_PUSH));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(404).rules().enabled(ClientRule.SLEEPING_PUSH_IMMUNITY));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(477).rules().enabled(ClientRule.SLEEPING_PUSH_IMMUNITY));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(754).rules().enabled(ClientRule.PRECISE_ENTITY_PUSH));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(755).rules().enabled(ClientRule.PRECISE_ENTITY_PUSH));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(758).rules().enabled(ClientRule.PUSHABLE_IMPULSE_TARGETS));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(759).rules().enabled(ClientRule.PUSHABLE_IMPULSE_TARGETS));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(769).rules().enabled(ClientRule.HORIZONTAL_MOTION_THRESHOLD));
        assertTrue(CompatibilityRegistry.DEFAULT.resolve(770).rules().enabled(ClientRule.HORIZONTAL_MOTION_THRESHOLD));
    }
}
