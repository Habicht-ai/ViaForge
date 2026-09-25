package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** 1.21.9 motion values, retained before Via's lossy conversion to velocity shorts. */
public final class OriginalVelocity {
    public final int entityId;
    public final Vector3d movement;

    private OriginalVelocity(int entityId, Vector3d movement) {
        this.entityId = entityId;
        this.movement = movement;
    }

    public static OriginalVelocity capture(ByteBuf original) {
        ByteBuf input = original.duplicate();
        if (Types.VAR_INT.readPrimitive(input) != ClientboundPackets1_21_9.SET_ENTITY_MOTION.getId()) return null;
        return new OriginalVelocity(Types.VAR_INT.readPrimitive(input), Types.LOW_PRECISION_VECTOR.read(input));
    }

    public boolean replaces(ByteBuf translated) {
        ByteBuf input = translated.duplicate();
        return Types.VAR_INT.readPrimitive(input) == 0x12 && Types.VAR_INT.readPrimitive(input) == entityId;
    }

    public ByteBuf event() {
        ByteBuf output = Unpooled.buffer();
        try {
            Types.VAR_INT.writePrimitive(output, 0x3f);
            Types.STRING.write(output, "VF|entity");
            ClientEventEnvelope.write(output, ClientEventFormat.legacy(340), 37);
            Types.VAR_INT.writePrimitive(output, entityId);
            output.writeDouble(movement.x()).writeDouble(movement.y()).writeDouble(movement.z());
            return output;
        } catch (Throwable error) {
            output.release();
            throw error;
        }
    }
}
