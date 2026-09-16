package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_15;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.*;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_4to1_20.Protocol1_19_4To1_20;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import java.util.function.IntUnaryOperator;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;

/** Explicit wire boundaries, with inverse forward mappings instead of lossy fallbacks. */
final class ModernBlockFamilies {
    static VillageBlockData create(Object protocol, FlattenedBlockData legacy, UserConnection user) {
        MappingData bee=Protocol1_14_4To1_15.MAPPINGS;
        MappingData nether=Protocol1_15_2To1_16.MAPPINGS;
        MappingData patch=Protocol1_16_1To1_16_2.MAPPINGS;
        MappingData wild=Protocol1_18_2To1_19.MAPPINGS,wild3=Protocol1_19_1To1_19_3.MAPPINGS,wild4=Protocol1_19_3To1_19_4.MAPPINGS;
        if(protocol instanceof com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1.values(),com.viaversion.viaversion.protocols.v26_1to26_2.Protocol26_1To26_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_9to1_21_11.Protocol1_21_9To1_21_11.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag().lowPrecisionMovement();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1.values(),com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_9to1_21_11.Protocol1_21_9To1_21_11.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag().lowPrecisionMovement();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11.values(),com.viaversion.viaversion.protocols.v1_21_9to1_21_11.Protocol1_21_9To1_21_11.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag().lowPrecisionMovement();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9.values(),com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag().lowPrecisionMovement();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.Protocol1_21_7To1_21_6)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6.values(),com.viaversion.viaversion.protocols.v1_21_6to1_21_7.Protocol1_21_6To1_21_7.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6.values(),com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5.values(),com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5.MAPPINGS,com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.Protocol1_21_4To1_21_2)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2.values(),com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4.MAPPINGS,com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2.values(),com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21.values(),com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21.MAPPINGS,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5.values(),com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5.MAPPINGS,Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_20_3.values(),Protocol1_20_2To1_20_3.MAPPINGS,Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_20_2.values(),Protocol1_20To1_20_2.MAPPINGS,Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_20to1_19_4.Protocol1_20To1_19_4)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_19_4.values(),Protocol1_19_4To1_20.MAPPINGS,wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee).withoutSectionLightFlag();
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_19_4.values(),wild4,wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee);
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.Protocol1_19_3To1_19_1)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_19_3.values(),wild3,wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee);
        if(protocol instanceof Protocol1_19To1_18_2)
            return sectioned((Protocol<?,?,?,?>)protocol,user,legacy,ClientboundPackets1_19.values(),wild,Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee);
        if(protocol instanceof Protocol1_17To1_16_4)
            return family(legacy,ClientboundPackets1_17.values(),
                    () -> new ChunkType1_17(user.getEntityTracker(Protocol1_17To1_16_4.class).currentWorldSectionHeight()),
                    () -> user.getEntityTracker(Protocol1_17To1_16_4.class).currentMinY(),
                    EntityTypes1_17.FALLING_BLOCK.getId(),Protocol1_16_4To1_17.MAPPINGS,patch,nether,bee);
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_15to1_14_4.Protocol1_15To1_14_4)
            return family(legacy,ClientboundPackets1_15.values(),ChunkType1_15.TYPE,EntityTypes1_15.FALLING_BLOCK.getId(),bee);
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2)
            return family(legacy,ClientboundPackets1_16.values(),ChunkType1_16.TYPE,EntityTypes1_16.FALLING_BLOCK.getId(),nether,bee);
        if(protocol instanceof com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.Protocol1_16_2To1_16_1)
            return family(legacy,ClientboundPackets1_16_2.values(),ChunkType1_16_2.TYPE,EntityTypes1_16_2.FALLING_BLOCK.getId(),patch,nether,bee);
        return null;
    }
    private static VillageBlockData sectioned(Protocol<?,?,?,?> protocol,UserConnection user,FlattenedBlockData legacy,ClientboundPacketType[] packets,MappingData... mappings) {
        MappingData source=mappings[0];
        return family(legacy,packets,
                () -> {
                    int height=protocol.getEntityRewriter().tracker(user).currentWorldSectionHeight();
                    int blocks=com.viaversion.viaversion.util.MathUtil.ceilLog2(source.getBlockStateMappings().mappedSize());
                    int biomes=com.viaversion.viaversion.util.MathUtil.ceilLog2(protocol.getEntityRewriter().tracker(user).biomesSent());
                    if(protocol instanceof com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11||protocol instanceof com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1)return new ChunkType26_1(height,blocks,biomes);
                    if(protocol instanceof com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9||protocol instanceof com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4||protocol instanceof com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5||protocol instanceof com.viaversion.viabackwards.protocol.v1_21_7to1_21_6.Protocol1_21_7To1_21_6||protocol instanceof com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7)return new ChunkType1_21_5(height,blocks,biomes);
                    return protocol instanceof com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.Protocol1_21_4To1_21_2||protocol instanceof com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21||protocol instanceof com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5||protocol instanceof com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3||protocol instanceof com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20||protocol instanceof com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2?new ChunkType1_20_2(height,blocks,biomes):new ChunkType1_18(height,blocks,biomes);
                },
                () -> protocol.getEntityRewriter().tracker(user).currentMinY(),source.getEntityMappings().mappedId("minecraft:falling_block"),mappings)
                .sectionedEntities(id -> source.getBlockEntityMappings().mappedIdentifier(id));
    }
    private static VillageBlockData family(FlattenedBlockData legacy,ClientboundPacketType[] packets,
            Type<Chunk> chunk,int falling,MappingData... data) {
        return family(legacy,packets,() -> chunk,() -> 0,falling,data);
    }
    private static VillageBlockData family(FlattenedBlockData legacy,ClientboundPacketType[] packets,
            Supplier<Type<Chunk>> chunk,IntSupplier minY,int falling,MappingData... data) {
        Mappings[] states=new Mappings[data.length],blocks=new Mappings[data.length];
        for(int i=0;i<data.length;i++) {
            states[i]=data[i].getBlockStateMappings()==null?null:data[i].getBlockStateMappings().inverse();
            blocks[i]=data[i].getBlockMappings()==null?null:data[i].getBlockMappings().inverse();
        }
        return new VillageBlockData(legacy,packets,chunk,minY,inverse(states),inverse(blocks),falling);
    }
    private static IntUnaryOperator inverse(Mappings[] mappings) {
        return id->{for(Mappings mapping:mappings){if(id<0)return -1;if(mapping!=null)id=mapping.getNewId(id);}return id;};
    }
    private ModernBlockFamilies() { }
}
