package com.viaversion.viaforge.hands;

import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.items.ServerEntityViews;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/** Local second hand; only server inventory/equipment packets confirm its contents. */
public final class Offhand {
    public static final KeyBinding SWAP = new KeyBinding("key.viaforge.swapHands",33,"key.categories.gameplay");
    private static EntityPlayer owner;
    private static InventoryBasic inventory = new InventoryBasic("Offhand",false,1);
    public static int context=-1, useHand, swingHand;
    private static boolean localUseSwing;
    public static ItemStack mainDuringUse;
    private static final java.util.Map<Integer,Integer> SWINGS=new java.util.HashMap<>();
    public static boolean active() { return ServerSession.has(com.viaversion.viaforge.common.compatibility.ClientFeature.TWO_HANDS) && Minecraft.getMinecraft().thePlayer!=null; }
    public static void register() { ClientRegistry.registerKeyBinding(SWAP); }
    public static boolean mainLeft(){return active()&&com.viaversion.viaforge.common.ViaForgeCommon.getManager().getConfig().isLeftMainHand();}
    public static boolean mainLeft(EntityLivingBase entity){
        if(entity==Minecraft.getMinecraft().thePlayer)return mainLeft();
        if(entity instanceof com.viaversion.viaforge.mobs.ServerMob)return ((com.viaversion.viaforge.mobs.ServerMob)entity).state.leftHanded();
        ServerEntityViews.View view=ServerEntityViews.get(entity.getEntityId());return view!=null&&view.leftHanded;
    }
    public static void clear() {
        if(owner!=null && owner.inventoryContainer.inventorySlots.size()==46 && owner.inventoryContainer.getSlot(45).inventory==inventory) {
            owner.inventoryContainer.inventorySlots.remove(45);owner.inventoryContainer.inventoryItemStacks.remove(45);
            crafting(owner.inventoryContainer,false);
        }
        owner=null;inventory=new InventoryBasic("Offhand",false,1);context=-1;mainDuringUse=null;useHand=swingHand=0;localUseSwing=false;SWINGS.clear();HandRenderer.clear();
    }
    public static void ensure() {
        if(!active()) return;
        EntityPlayer player=Minecraft.getMinecraft().thePlayer;
        if(owner!=player) {clear();owner=player;}
        Container container=player.inventoryContainer;
        if(container.inventorySlots.size()==45) {
            Slot slot=new Slot(inventory,0,77,62);slot.slotNumber=45;
            container.inventorySlots.add(slot);container.inventoryItemStacks.add(null);
            crafting(container,true);
        }
    }
    private static void crafting(Container container,boolean modern){
        container.getSlot(0).xDisplayPosition=modern?154:144;container.getSlot(0).yDisplayPosition=modern?28:36;
        for(int i=0;i<4;i++){Slot slot=container.getSlot(i+1);slot.xDisplayPosition=(modern?98:88)+(i%2)*18;slot.yDisplayPosition=(modern?18:26)+(i/2)*18;}
    }
    public static ItemStack get() { if(!active())return null;ensure();return inventory.getStackInSlot(0); }
    public static void set(ItemStack stack) { if(active()){ensure();inventory.setInventorySlotContents(0,stack!=null&&stack.stackSize>0?stack:null);} }
    public static ItemStack stack(int hand) { return hand==1?get():context==1?mainDuringUse:Minecraft.getMinecraft().thePlayer.getHeldItem(); }
    public static ItemStack of(EntityLivingBase entity) {
        if(entity==Minecraft.getMinecraft().thePlayer)return get();
        ServerEntityViews.View view=ServerEntityViews.get(entity.getEntityId());return view==null?null:view.offhand;
    }
    public static boolean local(EntityPlayer player) {return active()&&player==Minecraft.getMinecraft().thePlayer;}
    public static void accept(ByteBuf data) throws Exception {set(ServerEntityViews.item(Types.ITEM1_8.read(data)));}
    public static void swap() {
        if(!active() || Minecraft.getMinecraft().thePlayer.isSpectator())return;
        EntityPlayer p=Minecraft.getMinecraft().thePlayer;
        p.clearItemInUse();useHand=0;
        if(Minecraft.getMinecraft().playerController!=null)((com.viaversion.viaforge.mixin.impl.hands.HandControllerAccess)Minecraft.getMinecraft().playerController).viaForge$syncSlot();
        HandPackets.send(HandPackets.action(6));
        ItemStack held=p.getHeldItem();p.inventory.mainInventory[p.inventory.currentItem]=get();set(held);
    }
    public static void tick(EntityPlayer player) {
        if(!local(player))return;ensure();
        if(!player.isUsingItem())useHand=0;
        HandRenderer.tick();
        if(Minecraft.getMinecraft().currentScreen==null)while(SWAP.isPressed())swap();
        else while(SWAP.isPressed()) { }
    }
    public static void swing(int hand) {
        EntityPlayer p=Minecraft.getMinecraft().thePlayer;swingHand=hand;
        int old=context;context=hand;try{p.swingItem();}finally{context=old;}
    }
    public static boolean localUseSwing() { return localUseSwing; }
    public static void swingUse(int hand) {
        boolean previous=localUseSwing;
        // 26.3 predicts interaction swings locally; USE_ITEM/USE_ITEM_ON authorizes
        // the server animation. Its PUNCH packet is exclusively an attack action.
        localUseSwing=ServerSession.rule(com.viaversion.viaforge.common.compatibility.ClientRule.SERVER_OWNS_USE_SWING);
        try { swing(hand); } finally { localUseSwing=previous; }
    }
    public static int swingHand(EntityLivingBase entity){return entity==Minecraft.getMinecraft().thePlayer?swingHand:SWINGS.getOrDefault(entity.getEntityId(),0);}
    public static void remove(int id){SWINGS.remove(id);}
    public static void animation(ByteBuf data){
        int id=Types.VAR_INT.readPrimitive(data),animation=data.readUnsignedByte();
        net.minecraft.entity.Entity entity=Minecraft.getMinecraft().theWorld.getEntityByID(id);
        if(entity instanceof EntityLivingBase){SWINGS.put(id,animation==3?1:0);((EntityLivingBase)entity).swingItem();}
    }
    private Offhand() { }
}
