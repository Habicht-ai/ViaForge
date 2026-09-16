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
        VersionRules v113=v112.derive().rule(BIOME_WATER_COLORS,true).build();
        VersionRules v1131=v113.derive().rule(PREDICT_HOTBAR_DROPS,true).build();
        for(int i=0;i<flattened.length;i++)profiles.add(new CompatibilityProfile(flattened[i],i==0?v113:v1131,
                new ResourceProfile(resources[i],com.viaversion.viaforge.blocks.resources.FlattenedResourceConverter::convert),new FlattenedProtocolAdapter.Factory(flattened[i])));
        int[] village={477,480,485,490,498};String[] villageResources={"1.14","1.14.1","1.14.2","1.14.3","1.14.4"};
        VersionRules v114=v1131.derive().build();
        for(int i=0;i<village.length;i++)profiles.add(new CompatibilityProfile(village[i],v114,
                new ResourceProfile(villageResources[i],com.viaversion.viaforge.blocks.resources.VillageResourceConverter::convert),new FlattenedProtocolAdapter.Factory(village[i])));
        int[] buzzy={573,575,578};String[] buzzyResources={"1.15","1.15.1","1.15.2"};
        VersionRules v115=v114.derive().build();
        for(int i=0;i<buzzy.length;i++)profiles.add(new CompatibilityProfile(buzzy[i],v115,
                new ResourceProfile(buzzyResources[i],com.viaversion.viaforge.blocks.resources.BuzzyResourceConverter::convert),new FlattenedProtocolAdapter.Factory(buzzy[i])));
        int[] nether={735,736,751,753,754};String[] netherResources={"1.16","1.16.1","1.16.2","1.16.3","1.16.5"};
        VersionRules v116=v115.derive().build();
        for(int i=0;i<nether.length;i++)profiles.add(new CompatibilityProfile(nether[i],v116,
                new ResourceProfile(netherResources[i],com.viaversion.viaforge.blocks.resources.NetherResourceConverter::convert),new FlattenedProtocolAdapter.Factory(nether[i])));
        int[] caves={755,756};String[] cavesResources={"1.17","1.17.1"};
        VersionRules v117=v116.derive().build();
        for(int i=0;i<caves.length;i++)profiles.add(new CompatibilityProfile(caves[i],v117,
                new ResourceProfile(cavesResources[i],com.viaversion.viaforge.blocks.resources.CavesResourceConverter::convert),new FlattenedProtocolAdapter.Factory(caves[i])));
        int[] cliffs={757,758};String[] cliffsResources={"1.18.1","1.18.2"};
        VersionRules v118=v117.derive().build();
        for(int i=0;i<cliffs.length;i++)profiles.add(new CompatibilityProfile(cliffs[i],v118,
                new ResourceProfile(cliffsResources[i],com.viaversion.viaforge.blocks.resources.CavesResourceConverter::convert),new FlattenedProtocolAdapter.Factory(cliffs[i])));
        int[] wild={759,760};String[] wildResources={"1.19","1.19.2"};
        VersionRules v119=v118.derive().build();
        for(int i=0;i<wild.length;i++)profiles.add(new CompatibilityProfile(wild[i],v119,
                new ResourceProfile(wildResources[i],com.viaversion.viaforge.blocks.resources.CavesResourceConverter::convert),new FlattenedProtocolAdapter.Factory(wild[i])));
        int[] trails={761,762,763};String[] trailsResources={"1.19.3","1.19.4","1.20.1"};
        VersionRules v120=v119.derive().rule(SMALL_VEX_MODEL,true).build();
        for(int i=0;i<trails.length;i++)profiles.add(new CompatibilityProfile(trails[i],v120,
                new ResourceProfile(trailsResources[i],com.viaversion.viaforge.blocks.resources.CavesResourceConverter::convert),new FlattenedProtocolAdapter.Factory(trails[i])));
        profiles.add(new CompatibilityProfile(764,v120,new ResourceProfile("1.20.2",com.viaversion.viaforge.blocks.resources.GuiSpriteResourceConverter::convert),new FlattenedProtocolAdapter.Factory(764)));
        profiles.add(new CompatibilityProfile(765,v120,new ResourceProfile("1.20.4",com.viaversion.viaforge.blocks.resources.GuiSpriteResourceConverter::convert),new FlattenedProtocolAdapter.Factory(765)));
        profiles.add(new CompatibilityProfile(766,v120,new ResourceProfile("1.20.6",com.viaversion.viaforge.blocks.resources.GuiSpriteResourceConverter::convert),new FlattenedProtocolAdapter.Factory(766)));
        profiles.add(new CompatibilityProfile(767,v120,new ResourceProfile("1.21.1",com.viaversion.viaforge.blocks.resources.GuiSpriteResourceConverter::convert),new FlattenedProtocolAdapter.Factory(767)));
        profiles.add(new CompatibilityProfile(768,v120,new ResourceProfile("1.21.3",com.viaversion.viaforge.blocks.resources.EquipmentResourceConverter::convert),new FlattenedProtocolAdapter.Factory(768)));
        profiles.add(new CompatibilityProfile(769,v120,new ResourceProfile("1.21.4",com.viaversion.viaforge.blocks.resources.EquipmentResourceConverter::convert),new FlattenedProtocolAdapter.Factory(769)));
        VersionRules v1215=v120.derive().rule(INDIVIDUAL_EGG_MODELS,true).build();
        profiles.add(new CompatibilityProfile(770,v1215,new ResourceProfile("1.21.5",com.viaversion.viaforge.blocks.resources.SpringResourceConverter::convert),new FlattenedProtocolAdapter.Factory(770)));
        profiles.add(new CompatibilityProfile(771,v1215,new ResourceProfile("1.21.6",com.viaversion.viaforge.blocks.resources.SpringResourceConverter::convert),new FlattenedProtocolAdapter.Factory(771)));
        profiles.add(new CompatibilityProfile(772,v1215,new ResourceProfile("1.21.8",com.viaversion.viaforge.blocks.resources.SpringResourceConverter::convert),new FlattenedProtocolAdapter.Factory(772)));
        profiles.add(new CompatibilityProfile(773,v1215,new ResourceProfile("1.21.10",com.viaversion.viaforge.blocks.resources.SpringResourceConverter::convert),new FlattenedProtocolAdapter.Factory(773)));
        profiles.add(new CompatibilityProfile(774,v1215,new ResourceProfile("1.21.11",com.viaversion.viaforge.blocks.resources.SpringResourceConverter::convert),new FlattenedProtocolAdapter.Factory(774)));
        profiles.add(new CompatibilityProfile(775,v1215,new ResourceProfile("26.1.2",com.viaversion.viaforge.blocks.resources.Year26ResourceConverter::convert),new FlattenedProtocolAdapter.Factory(775)));
        profiles.add(new CompatibilityProfile(776,v1215,new ResourceProfile("26.2",com.viaversion.viaforge.blocks.resources.Year26ResourceConverter::convert),new FlattenedProtocolAdapter.Factory(776)));
        return new CompatibilityRegistry(profiles);
    }
    private LegacyCompatibility() { }
}
