package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import java.util.*;
import static com.viaversion.viaforge.common.compatibility.ClientRule.*;

/** Existing releases composed from a baseline and only the changes at each boundary. */
final class LegacyCompatibility {
    static CompatibilityRegistry create() {
        VersionRules v19=VersionRules.NATIVE.derive().content(107).enable(ClientFeature.BLOCKS,ClientFeature.ITEMS,ClientFeature.ENTITY_VISUALS,ClientFeature.MOBS,ClientFeature.TWO_HANDS,ClientFeature.BOATS,ClientFeature.COMBAT,ClientFeature.COOLDOWNS).build();
        VersionRules v110=v19.derive().content(210).rule(CRYSTAL_BEAM_OFFSET,true).build();
        VersionRules v111=v110.derive().content(315).enable(ClientFeature.TOTEM).rule(EXTENDED_GATEWAY_BEAM,true).rule(INTERPOLATED_GATEWAY_COOLDOWN,true).rule(COMBAT_CURSE_BOOKS,true).rule(NAMESPACED_ENTITY_IDS,true).rule(EXCLUSIVE_TURN_PADDLES,true).rule(COLORED_SHULKERS,true).build();
        VersionRules v1111=v111.derive().content(316).rule(REVERSED_DRAGON_HEAD_ITEM,true).rule(MODERN_ATTACK_ICON,true).build();
        VersionRules v112=v1111.derive().content(335).rule(UPDATED_DYE_COLORS,true).rule(MERGED_MATERIALS_TAB,true).rule(FAST_PADDLE_CYCLE,true).rule(UPDATED_ILLAGER_ARMS,true).build();
        List<CompatibilityProfile> profiles=new ArrayList<>();
        for(BlockVersionProfile wire:BlockVersionProfile.values()) {
            VersionRules rules=wire.protocol()>=335?v112:wire.protocol()>=316?v1111:wire.protocol()>=315?v111:wire.protocol()>=210?v110:v19;
            profiles.add(new CompatibilityProfile(wire.protocol(),rules,ResourceProfile.legacy(wire.resourceVersion()),new LegacyProtocolAdapter.Factory(wire)));
        }
        int[] flattened={393,401,404};String[] resources={"1.13","1.13.1","1.13.2"};
        VersionRules v113=v112.derive().build();
        for(int i=0;i<flattened.length;i++)profiles.add(new CompatibilityProfile(flattened[i],v113,
                new ResourceProfile(resources[i],com.viaversion.viaforge.blocks.resources.FlattenedResourceConverter::convert),new FlattenedProtocolAdapter.Factory(flattened[i])));
        int[] village={477,480,485,490,498};String[] villageResources={"1.14","1.14.1","1.14.2","1.14.3","1.14.4"};
        VersionRules v114=v113.derive().build();
        for(int i=0;i<village.length;i++)profiles.add(new CompatibilityProfile(village[i],v114,
                new ResourceProfile(villageResources[i],com.viaversion.viaforge.blocks.resources.VillageResourceConverter::convert),new FlattenedProtocolAdapter.Factory(village[i])));
        return new CompatibilityRegistry(profiles);
    }
    private LegacyCompatibility() { }
}
