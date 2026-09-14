package com.viaversion.viaforge.common.compatibility;

import io.netty.buffer.ByteBuf;
import java.util.List;

/** Per-connection codec/state before and after Via translation. Never reads Minecraft globals.
 * Input buffers are borrowed and must not be consumed. Returned buffers transfer ownership
 * to the decoder. Replacement packets and restored packets use the native client format;
 * retained events use a declared client data format, never an implicit server format.
 */
public interface PacketAdapter {
    List<ByteBuf> replace(ByteBuf original) throws Exception;
    boolean capture(ByteBuf original) throws Exception;
    ByteBuf event(ByteBuf original) throws Exception;
    ByteBuf restore(ByteBuf translated) throws Exception;
    List<ByteBuf> afterTranslation(ByteBuf original) throws Exception;
    void clear();
}
