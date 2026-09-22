package com.viaversion.viaforge.compatibility;
import com.viaversion.viaforge.common.compatibility.ClientTickPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
public final class NativeClientTicks {
    private static EntityPlayerSP pending;
    public static void install() {
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new NativeClientTicks());
    }
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent(priority=net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public void end(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if(event.phase!=net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)return;
        EntityPlayerSP player=pending;pending=null;
        if(player!=null && player==Minecraft.getMinecraft().thePlayer)send(player,true);
    }
    public static void send(EntityPlayerSP player,boolean end) {
        if(ServerSession.profile().serverProtocol()<768 || ServerSession.awaitingWorld(player.sendQueue))return;
        if(!end)pending=player;
        PacketBuffer data=new PacketBuffer(Unpooled.buffer(2));data.writeByte(end?1:0);
        if(!end) {
            net.minecraft.util.MovementInput input=player.movementInput;
            int flags=(input.moveForward>0?1:0)|(input.moveForward<0?2:0)|(input.moveStrafe>0?4:0)|(input.moveStrafe<0?8:0)
                |(input.jump?16:0)|(input.sneak?32:0)|(Minecraft.getMinecraft().gameSettings.keyBindSprint.isKeyDown()?64:0);
            data.writeByte(flags);
        }
        player.sendQueue.addToSendQueue(new C17PacketCustomPayload(ClientTickPackets.CHANNEL,data));
    }
    /** Riding bypasses onUpdateWalkingPlayer, but still needs the real tick boundary.
     * Its passenger-input packet already supplies input through Via. */
    public static void scheduleEnd(EntityPlayerSP player) {
        if(ServerSession.profile().serverProtocol()>=768 && !ServerSession.awaitingWorld(player.sendQueue))pending=player;
    }
    private NativeClientTicks(){}
}
