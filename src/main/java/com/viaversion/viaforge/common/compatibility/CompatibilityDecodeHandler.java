package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.exception.CancelDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import java.util.logging.*;

/** Transport pipeline shared by every codec: preserve -> Via -> restore -> client events. */
public class CompatibilityDecodeHandler extends ViaDecodeHandler {
    private static final Logger LOGGER=Logger.getLogger("ViaForge/Compatibility");
    private final PacketAdapter adapter;
    private final Runnable onJoin,onClose;
    private boolean disabled;
    public CompatibilityDecodeHandler(UserConnection user,PacketAdapter adapter,Runnable onJoin,Runnable onClose) {
        super(user);this.adapter=adapter;this.onJoin=onJoin;this.onClose=onClose;
        if (adapter instanceof FlattenedProtocolAdapter) user.put((FlattenedProtocolAdapter) adapter);
    }
    @Override public void handlerAdded(ChannelHandlerContext ctx)throws Exception {
        if(ctx.channel()!=connection.getChannel())throw new IllegalArgumentException("Compatibility decoder belongs to a different connection");
        super.handlerAdded(ctx);
    }
    @Override protected void decode(ChannelHandlerContext ctx,ByteBuf input,List<Object> output)throws Exception {
        if (!disabled && connection.shouldTransformPacket() && adapter instanceof FlattenedProtocolAdapter)
            ((FlattenedProtocolAdapter)adapter).waterColors.observe(input,connection);
        boolean play=!disabled&&connection.shouldTransformPacket()&&connection.getProtocolInfo().getServerState()==State.PLAY;
        boolean joined=false;ByteBuf event=null;
        if(play)try {
            List<ByteBuf> replacements=adapter.replace(input);
            if(replacements!=null) {
                boolean allowed=false;
                try { allowed=connection.checkIncomingPacket(input.readableBytes());if(allowed)output.addAll(replacements); }
                finally { if(!allowed)for(ByteBuf replacement:replacements)replacement.release(); }
                return;
            }
            joined=adapter.capture(input);
            event=adapter.event(input);
        }catch(Exception error){disable(error);}
        int start=output.size();
        try {
            if(event==null && !(play && adapter instanceof FlattenedProtocolAdapter))super.decode(ctx,input,output);
            else {
                if(!connection.checkIncomingPacket(input.readableBytes()))throw CancelDecoderException.generate(null);
                ByteBuf translated=ctx.alloc().buffer(input.readableBytes());
                try {
                    translated.writeBytes(input,input.readerIndex(),input.readableBytes());
                    try { connection.transformIncoming(translated,CancelDecoderException::generate);output.add(translated.retain()); }
                    catch(CancelDecoderException cancelled) { /* The retained event survives Via cancellation. */ }
                }finally{translated.release();}
            }
        }catch(CancelDecoderException cancelled){
            // Recipe books, advancements and other unrepresented packets are
            // routinely cancelled by Via. They must not discard the loaded
            // columns or boat tracking needed by later placement confirmations.
            if(event!=null)event.release();
            return;
        }catch(Exception failure){if(event!=null)event.release();disable(failure);throw failure;}
        if(event!=null)output.add(event);
        if(!play||disabled)return;
        if(adapter instanceof FlattenedProtocolAdapter&&((FlattenedProtocolAdapter)adapter).observedJoin())joined=true;
        if(adapter instanceof FlattenedProtocolAdapter)for(int i=start;i<output.size();i++) {
            if(com.viaversion.viaversion.api.type.Types.VAR_INT.readPrimitive(((ByteBuf)output.get(i)).duplicate())==1)joined=true;
        }
        if(joined)onJoin.run();
        try {
            for(int i=start;i<output.size();i++) {
                ByteBuf translated=(ByteBuf)output.get(i),restored=adapter.restore(translated);
                if(restored!=null){output.set(i,restored);translated.release();}
            }
            if(output.size()>start || adapter instanceof FlattenedProtocolAdapter)output.addAll(adapter.afterTranslation(input));
        }catch(Exception error){disable(error);}
    }
    private void disable(Exception error){disabled=true;adapter.clear();LOGGER.log(Level.WARNING,"Original-data adapter failed; retaining Via output for this connection",error);}
    @Override public void channelInactive(ChannelHandlerContext ctx)throws Exception {
        adapter.clear();onClose.run();super.channelInactive(ctx);
    }
}
