package com.viaversion.viaforge.compatibility;
import com.viaversion.viaforge.common.compatibility.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ElytraPhysicsTest {
    @Test public void originalFlightArithmeticBoundaries() {
        for(int p:new int[]{47,107,340,393,404,754,755,756,757,758,767,768,772,773,774,775,776,777,778}) {
            VersionRules r=CompatibilityRegistry.DEFAULT.resolve(p).rules();
            assertEquals(p>=393&&p<=777,r.enabled(ClientRule.MODERN_LOOK_VECTOR));
            assertEquals(p>=758&&p<=777,r.enabled(ClientRule.JAVA_ELYTRA_LIFT));
            assertEquals(p>=774&&p<=777,r.enabled(ClientRule.DOUBLE_TRIG_LOOKUP));
            assertEquals(p>=755&&p<=777,r.enabled(ClientRule.SHULKER_DELTA_PUSH));
            assertEquals(p>=776&&p<=777,r.enabled(ClientRule.COLLISION_PORTION_BOUNCE));
            assertEquals(p==777,r.enabled(ClientRule.SUPPRESS_GRAVITY_EQUAL_BOUNCE));
            assertEquals(p>=393&&p<=777,r.enabled(ClientRule.EXPANDED_BLOCK_RAY));
            assertEquals(p>=773&&p<=777,r.enabled(ClientRule.PRECISE_BLOCK_EFFECTS));
        }
    }
    @Test public void straightUpHasNoHorizontalLookSinceOriginalVectorConvention() {
        for(int p:new int[]{393,404,754,758,773,774,775,776}) {
            VersionRules rules=CompatibilityRegistry.DEFAULT.resolve(p).rules();
            double[] look=OriginalLookMath.look(0,-90,rules);
            assertArrayEquals(new double[]{0,1,0},look,0);
            double[] velocity=ElytraPhysics.step(rules,0,-.2,1,look[0],look[1],look[2],-90,.08);
            assertEquals(-.2744000053405757,velocity[1],1e-14);
            assertEquals((double).99F,velocity[2],0);
            double[] near=OriginalLookMath.look(0,-89.9F,rules);
            assertTrue(near[2]>.001);
            assertTrue(ElytraPhysics.step(rules,0,-.2,1,near[0],near[1],near[2],-89.9F,.08)[1]>velocity[1]+.12);
        }
        for(int p:new int[]{107,340}) {
            double[] look=OriginalLookMath.look(0,-90,CompatibilityRegistry.DEFAULT.resolve(p).rules());
            assertTrue("Original float table retains a horizontal remainder: "+p,Math.hypot(look[0],look[2])>0);
        }
    }
    @Test public void flightStartAndCrawlingFollowOriginalBoundaries() {
        for(int protocol:new int[]{47,107,340,393,404,477,498,573,578,763,767,768,777,778}) {
            VersionRules rules=CompatibilityRegistry.DEFAULT.resolve(protocol).rules();
            assertEquals(protocol>=477&&protocol<=777,rules.enabled(ClientRule.CRAWLING_POSE));
            assertEquals(protocol>=573&&protocol<=777,rules.enabled(ClientRule.PREDICT_ELYTRA_START));
            assertEquals(protocol>=768&&protocol<=777,rules.enabled(ClientRule.FIXED_CRAWLING_HEAD));
        }
    }
    @Test public void inventoryPreviewKeepsOriginalVersionBoundaries() {
        for(int protocol:new int[]{47,107,340,763,764,765,767,768,769,770,773,774,775,776,777,778}) {
            VersionRules rules=CompatibilityRegistry.DEFAULT.resolve(protocol).rules();
            assertEquals(protocol>=764&&protocol<=777,rules.enabled(ClientRule.BOUNDED_INVENTORY_PREVIEW));
            assertEquals(protocol>=770&&protocol<=777,rules.enabled(ClientRule.LIMITED_INVENTORY_FLIGHT_YAW));
            assertEquals(protocol>=774&&protocol<=777,rules.enabled(ClientRule.CAPTURED_INVENTORY_FLIGHT));
        }
    }
    @Test public void handAnimationsUseOriginalVersionBoundaries() {
        for(int protocol:new int[]{47,107,315,316,340,498,573,578,763,764,765,766,776,777,778}) {
            VersionRules rules=CompatibilityRegistry.DEFAULT.resolve(protocol).rules();
            assertEquals(protocol>=573&&protocol<=777,rules.enabled(ClientRule.ROCKET_USE_SWING));
            assertEquals(protocol>=765&&protocol<=777,rules.enabled(ClientRule.SHIELD_FOLLOWS_LOOK));
            assertEquals(protocol==777,rules.enabled(ClientRule.SERVER_OWNS_USE_SWING));
        }
    }
    @Test public void flightPresentationUsesOriginalVersionBoundaries() {
        for(int protocol:new int[]{107,315,316,340,757,758,759,767,768,769,777}) {
            CompatibilityProfile profile=CompatibilityRegistry.DEFAULT.resolve(protocol);
            assertEquals(profile.serverProtocol()>=758,profile.rules().enabled(ClientRule.FIREWORK_HAND_TRAIL));
            assertEquals(profile.serverProtocol()>=768,profile.rules().enabled(ClientRule.TICKED_ELYTRA_WINGS));
        }
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(47).rules().enabled(ClientRule.FIREWORK_HAND_TRAIL));
    }
    @Test public void horizontalGlideTradesDescentForForwardSpeed() {
        double[] v=ElytraPhysics.step(0,-.2,1,0,0,1,0);
        assertEquals(-.19404,v[1],1e-8); assertEquals(1.009602,v[2],1e-8);
    }
    @Test public void steeringTurnsWithoutAddingStrafeThrust() {
        double[] v=ElytraPhysics.step(0,0,1,1,0,0,0);
        assertTrue(v[0]>0);assertTrue(v[2]<1);assertTrue(v[1]<0);
    }
    @Test public void climbingSpendsHorizontalMomentumAndVerticalLookIsFinite() {
        double[] v=ElytraPhysics.step(0,0,1,0,Math.sqrt(.5),Math.sqrt(.5),-45);
        assertTrue(v[1]>0);assertTrue(v[2]<1);
        for(double n:ElytraPhysics.step(0,-.2,0,0,-1,0,90))assertTrue(Double.isFinite(n));
    }
    @Test public void durabilityAndBoostRulesUseRealIntroductionBoundary() {
        assertTrue(ElytraPhysics.usable(430,432));assertFalse(ElytraPhysics.usable(431,432));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(47).has(ClientFeature.ELYTRA));
        assertFalse(CompatibilityRegistry.DEFAULT.resolve(778).has(ClientFeature.ELYTRA));
        for(int p:new int[]{107,315,316,340,736,776,777}) {
            CompatibilityProfile profile=CompatibilityRegistry.DEFAULT.resolve(p);
            assertTrue(profile.has(ClientFeature.ELYTRA));
            assertEquals(p>=316,profile.rules().enabled(ClientRule.ELYTRA_FIREWORKS));
        }
        assertEquals(.85,ElytraPhysics.boost(0,1),1e-10);
    }
}
