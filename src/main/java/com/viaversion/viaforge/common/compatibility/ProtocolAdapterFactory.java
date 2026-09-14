package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Registers both directions. A target cannot enable features its codec cannot preserve. */
public interface ProtocolAdapterFactory {
    String id();
    Set<ClientFeature> capabilities();
    PacketAdapter create(CompatibilityProfile target,IntUnaryOperator clientBlockState);
    ItemDataAdapter items();
    void serverbound(String channel,PacketWrapper packet);
}
