package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InteractionTest {
    @Test public void sharedPredictionSequenceSurvivesSameWorldAndResetsOnWorldChange() {
        InteractionSequence sequence=new InteractionSequence();
        assertEquals(1,sequence.next("minecraft:overworld"));
        sequence.worldChanged("minecraft:the_nether");
        sequence.worldChanged("minecraft:overworld");
        assertEquals(1,sequence.next("minecraft:overworld"));
        assertEquals(2,sequence.next("minecraft:overworld"));
        assertEquals(1,sequence.next("minecraft:the_nether"));
        assertEquals(2,sequence.next("minecraft:the_nether"));
        assertEquals(1,sequence.next("minecraft:overworld"));
    }
    @Test public void behaviorRulesDoNotUseInheritedContentRevision() {
        for(int protocol:new int[]{47,107,110,340,404,477,498,759,769,770,775,776,777,778}) {
            VersionRules rules=CompatibilityRegistry.DEFAULT.resolve(protocol).rules();
            assertEquals(protocol>=107&&protocol<=777,rules.enabled(ClientRule.MODERN_THIN_COLLISIONS));
            assertEquals(protocol>=477&&protocol<=777,rules.enabled(ClientRule.JUMP_CLIMBING));
            assertEquals(protocol>=770&&protocol<=777,rules.enabled(ClientRule.COMPONENT_BLOCKING));
            assertEquals(protocol>=393&&protocol<=777,rules.enabled(ClientRule.MODERN_BLOCK_USE_FAILURE));
        }
    }
}
