package com.viaversion.viaforge.common.blocks;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Subclassing keeps capture after decompression when Via reorders its decoder. */
public final class BlockPreservingDecodeHandler extends ViaDecodeHandler {
    private static final Logger LOGGER = Logger.getLogger("ViaForge/Blocks");
    private final LegacyBlockPackets blocks;
    private final LegacyEntityPackets entities;
    private final IntUnaryOperator mapper;
    private final Runnable onJoin;
    private final Runnable onClose;
    private boolean disabled;

    public BlockPreservingDecodeHandler(UserConnection connection, BlockVersionProfile profile,
            IntUnaryOperator mapper, Runnable onJoin, Runnable onClose) {
        super(connection);
        this.blocks = new LegacyBlockPackets(profile);
        this.entities = new LegacyEntityPackets(profile);
        this.mapper = mapper;
        this.onJoin = onJoin;
        this.onClose = onClose;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // Keep ViaDecodeHandler's sharable contract: enabling compression removes
        // and reinserts this instance. Its mutable state still belongs to one channel.
        if (ctx.channel() != connection.getChannel()) {
            throw new IllegalArgumentException("Block decoder belongs to a different connection");
        }
        super.handlerAdded(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> output) throws Exception {
        boolean play = !disabled && connection.shouldTransformPacket()
                && connection.getProtocolInfo().getServerState() == State.PLAY;
        boolean joined = false;
        if (play) {
            try {
                ByteBuf visualEvent = entities.replacement(input);
                if (visualEvent != null) {
                    if (connection.checkIncomingPacket(input.readableBytes())) output.add(visualEvent);
                    else visualEvent.release();
                    return;
                }
                ByteBuf editor = blocks.editorUpdate(input);
                if (editor != null) {
                    if (connection.checkIncomingPacket()) output.add(editor);
                    else editor.release();
                    return;
                }
                List<ByteBuf> bedUpdates = blocks.bedUpdate(input, mapper);
                if (bedUpdates != null) {
                    boolean allowed = connection.checkIncomingPacket();
                    for (ByteBuf update : bedUpdates) { if (allowed) output.add(update); else update.release(); }
                    return;
                }
                ByteBuf event = blocks.blockEvent(input, mapper);
                if (event != null) {
                    if (connection.checkIncomingPacket()) output.add(event);
                    else event.release();
                    return;
                }
                joined = blocks.capture(input);
            } catch (Exception error) {
                disable(error);
            }
        }
        int start = output.size();
        ByteBuf visual = play && !disabled ? entities.capture(input) : null;
        try {
            if (visual == null) super.decode(ctx, input, output);
            else {
                // A cloud or off-hand update is intentionally cancelled by Via.
                // Validate the incoming packet once, then retain its visual message
                // even when the translation has no corresponding 1.8 packet.
                if (!connection.checkIncomingPacket(input.readableBytes())) throw com.viaversion.viaversion.exception.CancelDecoderException.generate(null);
                ByteBuf translated = ctx.alloc().buffer(input.readableBytes());
                try {
                    translated.writeBytes(input, input.readerIndex(), input.readableBytes());
                    try {
                        connection.transformIncoming(translated, com.viaversion.viaversion.exception.CancelDecoderException::generate);
                        output.add(translated.retain());
                    } catch (com.viaversion.viaversion.exception.CancelDecoderException expected) { }
                } finally { translated.release(); }
            }
        }
        catch (Exception failure) { if (visual != null) visual.release(); throw failure; }
        if (visual != null) output.add(visual);
        if (!play || disabled) return;
        if (joined) onJoin.run();
        for (int i = start; i < output.size(); i++) {
            ByteBuf translated = (ByteBuf) output.get(i);
            try {
                ByteBuf restored = blocks.restore(translated, mapper);
                if (restored != null) {
                    output.set(i, restored);
                    translated.release();
                }
            } catch (Exception error) {
                disable(error);
                break;
            }
        }
        if (!disabled && output.size() > start) output.addAll(blocks.editorChunkUpdates(input));
    }

    private void disable(Exception error) {
        disabled = true;
        blocks.world().clear();
        LOGGER.log(Level.WARNING, "Original block capture failed; keeping Via's block replacements for this connection", error);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        blocks.world().clear();
        onClose.run();
        super.channelInactive(ctx);
    }
}
