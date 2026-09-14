package com.viaversion.viaforge.hands;

import com.viaversion.viaforge.items.ServerItemCooldowns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.*;

/** Ordered main/offhand interactions; native item implementations supply local prediction. */
public final class HandActions {
    public static boolean bucketUsed;
    public static void rightClick() {
        Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP player=mc.thePlayer;
        if(mc.playerController.getIsHittingBlock())return;
        Offhand.ensure();
        for(int hand=0;hand<2;hand++) {
            int old=Offhand.context;Offhand.context=hand;
            ItemStack main=player.getHeldItem(), previousMain=Offhand.mainDuringUse;
            Offhand.mainDuringUse=main;
            if(hand==1)player.inventory.mainInventory[player.inventory.currentItem]=Offhand.get();
            try { if(use(hand))return; }
            finally {
                if(hand==1){Offhand.set(player.getHeldItem());player.inventory.mainInventory[player.inventory.currentItem]=main;}
                Offhand.context=old;
                Offhand.mainDuringUse=previousMain;
            }
        }
    }
    private static boolean use(int hand) {
        Minecraft mc=Minecraft.getMinecraft();EntityPlayerSP p=mc.thePlayer;ItemStack held=p.getHeldItem();MovingObjectPosition hit=mc.objectMouseOver;
        if(hit!=null) {
            if(hit.typeOfHit==MovingObjectPosition.MovingObjectType.ENTITY) {
                if(mc.playerController.isPlayerRightClickingOnEntity(p,hit.entityHit,hit) || mc.playerController.interactWithEntitySendPacket(p,hit.entityHit))return true;
            }else if(hit.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK && !mc.theWorld.isAirBlock(hit.getBlockPos())) {
                int count=held==null?0:held.stackSize;
                if(net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p,net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,mc.theWorld,hit.getBlockPos(),hit.sideHit,hit.hitVec).isCanceled())return true;
                if(mc.playerController.onPlayerRightClick(p,mc.theWorld,held,hit.getBlockPos(),hit.sideHit,hit.hitVec)) {
                    Offhand.swing(hand);
                    if(held!=null&&held.stackSize<=0)p.inventory.mainInventory[p.inventory.currentItem]=null;
                    if(held!=null&&(held.stackSize!=count||p.capabilities.isCreativeMode))HandRenderer.reset(hand);
                    return true;
                }
            }
        }
        if(held==null)return false;
        if(ServerItemCooldowns.cooling(held))return false;
        if(net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p,net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR,mc.theWorld,null,null,null).isCanceled())return true;
        bucketUsed=false;
        boolean changed=mc.playerController.sendUseItem(p,mc.theWorld,held);
        if(p.isUsingItem()){Offhand.useHand=hand;return true;}
        if(changed||bucketUsed){HandRenderer.reset(hand);return true;}
        Item item=held.getItem();
        // These native/ported actions can succeed without changing a creative
        // stack. They still consume the right click, unlike tools or materials.
        return item instanceof ItemSnowball || item instanceof ItemEgg
                || item==Items.ender_pearl || item==Items.experience_bottle
                || com.viaversion.viaforge.items.ClientItems.is(held,com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.SPLASH)
                || com.viaversion.viaforge.items.ClientItems.is(held,com.viaversion.viaforge.common.blocks.LegacyItemCatalog.Kind.LINGERING);
    }
    private HandActions() { }
}
