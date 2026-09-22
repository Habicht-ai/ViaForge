package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.viaversion.viaforge.compatibility.ServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Read-only, bounded inventory evidence for ordinary local development sessions.
 * Never sends/cancels packets, changes a slot, or records chat/account credentials. */
public final class HotbarTrace {
    private Channel channel;
    private Capture capture;
    private String previous;

    static void install() { FMLCommonHandler.instance().bus().register(new HotbarTrace()); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(capture!=null)capture.flush();
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.thePlayer==null||mc.getNetHandler()==null)return;
        Channel next=mc.getNetHandler().getNetworkManager().channel();
        if(!(next.remoteAddress() instanceof InetSocketAddress)
                ||!((InetSocketAddress)next.remoteAddress()).getAddress().isLoopbackAddress())return;
        if(next!=channel) {
            channel=next;previous=null;
            Path path=mc.mcDataDir.toPath().resolve("ViaForge/diagnostics/hotbar-"+System.currentTimeMillis()+".jsonl");
            capture=new Capture(path);
            final Capture connectionCapture=capture;
            next.pipeline().addBefore("packet_handler","viaforge_hotbar_trace",new ChannelDuplexHandler() {
                @Override public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise)throws Exception {
                    connectionCapture.packet("send",msg);super.write(ctx,msg,promise);
                }
                @Override public void channelRead(ChannelHandlerContext ctx,Object msg)throws Exception {
                    connectionCapture.packet("receive",msg);super.channelRead(ctx,msg);
                }
                @Override public void channelInactive(ChannelHandlerContext ctx)throws Exception {
                    JsonObject end=new JsonObject();end.addProperty("event","disconnect");connectionCapture.log(end);
                    super.channelInactive(ctx);
                }
            });
        }
        JsonObject state=new JsonObject();
        state.addProperty("event","inventory");state.addProperty("protocol",ServerSession.profile().serverProtocol());
        state.addProperty("selected_slot",mc.thePlayer.inventory.currentItem);
        JsonArray items=new JsonArray();for(int i=0;i<9;i++)items.add(LiveHotbarActions.item(mc.thePlayer.inventory.mainInventory[i]));state.add("hotbar",items);
        // Observe an already installed offhand slot; do not call Offhand.ensure().
        if(mc.thePlayer.inventoryContainer.inventorySlots.size()>45)
            state.add("offhand",LiveHotbarActions.item(mc.thePlayer.inventoryContainer.getSlot(45).getStack()));
        state.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName());
        state.addProperty("container",mc.thePlayer.openContainer.getClass().getSimpleName());
        state.addProperty("container_slots",mc.thePlayer.openContainer.inventorySlots.size());
        state.addProperty("gamemode",mc.playerController.getCurrentGameType().toString());
        String current=state.toString();
        if(!current.equals(previous)) {previous=current;capture.log(state);}
    }

    private static final class Capture {
        private final Path path;
        private final java.util.concurrent.ConcurrentLinkedQueue<String> pending=new java.util.concurrent.ConcurrentLinkedQueue<>();
        private int bytes,events;
        private volatile boolean failed;
        private Capture(Path path) {this.path=path;}
        synchronized void log(JsonObject value) {
            if(failed||events>=4096||bytes>=4*1024*1024)return;
            value.addProperty("time_ms",System.currentTimeMillis());value.addProperty("sequence",events++);
            String line=value.toString()+"\n";
            int length=line.getBytes(StandardCharsets.UTF_8).length;
            if(bytes+length>4*1024*1024){bytes=4*1024*1024;return;}
            bytes+=length;pending.add(line);
        }
        void flush() {
            if(pending.isEmpty()||failed)return;
            StringBuilder lines=new StringBuilder();String line;
            while((line=pending.poll())!=null)lines.append(line);
            try {
                Files.createDirectories(path.getParent());
                Files.write(path,lines.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }catch(Exception failure) {
                failed=true;java.util.logging.Logger.getLogger("ViaForge/HotbarTrace").warning("Inventory trace unavailable: "+failure);
            }
        }
        void packet(String direction,Object packet) {
            if(failed)return;
            try {observePacket(direction,packet);}
            catch(RuntimeException failure) {
                failed=true;java.util.logging.Logger.getLogger("ViaForge/HotbarTrace").warning("Inventory observation unavailable: "+failure);
            }
        }
        private void observePacket(String direction,Object packet) {
            JsonObject j=new JsonObject();
            if(packet instanceof C09PacketHeldItemChange)j.addProperty("slot",((C09PacketHeldItemChange)packet).getSlotId());
            else if(packet instanceof C10PacketCreativeInventoryAction) {
                C10PacketCreativeInventoryAction p=(C10PacketCreativeInventoryAction)packet;j.addProperty("slot",p.getSlotId());j.add("item",LiveHotbarActions.item(p.getStack()));
            }else if(packet instanceof S09PacketHeldItemChange)j.addProperty("slot",((S09PacketHeldItemChange)packet).getHeldItemHotbarIndex());
            else if(packet instanceof S2FPacketSetSlot) {
                S2FPacketSetSlot p=(S2FPacketSetSlot)packet;j.addProperty("window",p.func_149175_c());j.addProperty("slot",p.func_149173_d());j.add("item",LiveHotbarActions.item(p.func_149174_e()));
            }else if(packet instanceof S30PacketWindowItems) {
                S30PacketWindowItems p=(S30PacketWindowItems)packet;j.addProperty("window",p.func_148911_c());
                JsonArray items=new JsonArray();for(net.minecraft.item.ItemStack stack:p.getItemStacks())items.add(LiveHotbarActions.item(stack));j.add("items",items);
            }else if(packet instanceof C17PacketCustomPayload) {
                C17PacketCustomPayload p=(C17PacketCustomPayload)packet;if(!p.getChannelName().equals("VF|hands"))return;
                ByteBuf data=p.getBufferData();j.addProperty("hand_operation_hex",ByteBufUtil.hexDump(data,data.readerIndex(),Math.min(96,data.readableBytes())));
            }else if(!(packet instanceof C0DPacketCloseWindow||packet instanceof C0EPacketClickWindow||packet instanceof C08PacketPlayerBlockPlacement))return;
            j.addProperty("event",direction);j.addProperty("packet",packet.getClass().getSimpleName());log(j);
        }
    }
}
