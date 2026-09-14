package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.function.IntUnaryOperator;

/** Compatibility constructor for legacy fixtures. Production selects a registered adapter. */
@Deprecated
public final class BlockPreservingDecodeHandler extends CompatibilityDecodeHandler {
    public BlockPreservingDecodeHandler(UserConnection user,BlockVersionProfile wire,IntUnaryOperator mapper,Runnable onJoin,Runnable onClose) {
        super(user,new LegacyProtocolAdapter(wire,mapper),onJoin,onClose);
    }
}
