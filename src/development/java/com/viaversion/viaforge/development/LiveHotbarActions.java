package com.viaversion.viaforge.development;

import com.google.gson.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;

/** Drives the real Creative mouse handler and hotbar keys; no packet replacement. */
final class LiveHotbarActions {
    private static GuiContainerCreative pacedGui;
    private static Slot pacedSource,pacedTarget;
    static void act(String action,int tick) throws Exception {
        if(!action.startsWith("hotbar:"))return;
        if(action.startsWith("hotbar:creative-paced:")&&tick>0) {
            if(tick==8)mouse(pacedGui,"mouseClicked",pacedSource,0);
            if(tick==9)mouse(pacedGui,"mouseReleased",pacedSource,0);
            if(tick==18)mouse(pacedGui,"mouseClicked",pacedTarget,0);
            if(tick==19)mouse(pacedGui,"mouseReleased",pacedTarget,0);
            if(tick==30)Minecraft.getMinecraft().thePlayer.closeScreen();
            return;
        }
        if(tick!=0)return;
        Minecraft mc=Minecraft.getMinecraft();String[] a=action.split(":",4);
        if(a[1].startsWith("creative")) {
            GuiContainerCreative gui=new GuiContainerCreative(mc.thePlayer);mc.displayGuiScreen(gui);
            Method tab=GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab",CreativeTabs.class);tab.setAccessible(true);tab.invoke(gui,CreativeTabs.tabBlock);
            Method click=GuiContainerCreative.class.getDeclaredMethod("handleMouseClick",Slot.class,int.class,int.class,int.class);click.setAccessible(true);
            Item wanted=Item.getByNameOrId(a[3]);Slot source=null;
            com.viaversion.viaforge.mixin.impl.items.CreativeContainerAccess catalog=(com.viaversion.viaforge.mixin.impl.items.CreativeContainerAccess)gui.inventorySlots;
            java.util.List<ItemStack> entries=catalog.viaForge$items();
            for(int i=0;i<entries.size();i++)if(entries.get(i).getItem()==wanted&&entries.get(i).getMetadata()==0){
                int rows=(entries.size()+8)/9-5;
                catalog.viaForge$scrollTo(rows>0?Math.min(i/9,rows)/(float)rows:0);break;
            }
            for(int i=0;i<45;i++){Slot s=gui.inventorySlots.getSlot(i);if(s.getHasStack()&&s.getStack().getItem()==wanted&&s.getStack().getMetadata()==0){source=s;break;}}
            if(source==null)throw new IllegalStateException("Creative item unavailable: "+a[3]);
            Slot target=gui.inventorySlots.getSlot(45+Integer.parseInt(a[2]));
            if(a[1].equals("creative-paced")) {pacedGui=gui;pacedSource=source;pacedTarget=target;return;}
            if(a[1].equals("creative-key")) {
                click.invoke(gui,source,source.slotNumber,Integer.parseInt(a[2]),2);
            }else if(a[1].equals("creative-mouse")||a[1].equals("creative-drag")) {
                mouse(gui,"mouseClicked",source,0);
                mouse(gui,"mouseReleased",source,0);
                if(a[1].equals("creative-drag")) {
                    // Native quick-craft begin, add hovered slot, finish.
                    click.invoke(gui,null,-999,0,5);
                    click.invoke(gui,target,target.slotNumber,1,5);
                    click.invoke(gui,null,-999,2,5);
                }else {
                    mouse(gui,"mouseClicked",target,0);mouse(gui,"mouseReleased",target,0);
                }
            }else {
                click.invoke(gui,source,source.slotNumber,0,0);
                click.invoke(gui,target,target.slotNumber,0,0);
            }
            if(mc.thePlayer.inventory.getItemStack()!=null)throw new IllegalStateException("Creative cursor not empty");
            mc.thePlayer.closeScreen();
        }else if(a[1].equals("select")) {
            KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[Integer.parseInt(a[2])].getKeyCode());
        }else if(a[1].equals("pick")) {
            mc.entityRenderer.getMouseOver(1);
            KeyBinding.onTick(mc.gameSettings.keyBindPickBlock.getKeyCode());
        }else if(a[1].equals("scroll")) {
            mc.thePlayer.inventory.changeCurrentItem(Integer.parseInt(a[2]));
        }else if(a[1].equals("select-click")) {
            KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[Integer.parseInt(a[2])].getKeyCode());
            mc.entityRenderer.getMouseOver(1);
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
        }else if(a[1].equals("click")) {
            mc.entityRenderer.getMouseOver(1);
            Method click=Minecraft.class.getDeclaredMethod("rightClickMouse");click.setAccessible(true);click.invoke(mc);
        }
    }
    private static void mouse(GuiContainerCreative gui,String method,Slot slot,int button)throws Exception {
        Field left=GuiContainer.class.getDeclaredField("guiLeft"),top=GuiContainer.class.getDeclaredField("guiTop");
        left.setAccessible(true);top.setAccessible(true);
        Method event=GuiContainerCreative.class.getDeclaredMethod(method,int.class,int.class,int.class);event.setAccessible(true);
        event.invoke(gui,left.getInt(gui)+slot.xDisplayPosition+8,top.getInt(gui)+slot.yDisplayPosition+8,button);
    }
    static void observe(JsonObject out) {
        Minecraft mc=Minecraft.getMinecraft();out.addProperty("selected_slot",mc.thePlayer.inventory.currentItem);
        JsonArray items=new JsonArray();for(int i=0;i<9;i++)items.add(item(mc.thePlayer.inventory.mainInventory[i]));out.add("hotbar",items);
        out.add("offhand",item(com.viaversion.viaforge.hands.Offhand.get()));
        if(mc.objectMouseOver!=null)out.addProperty("hit",mc.objectMouseOver.toString());
    }
    static JsonElement item(ItemStack stack) {
        if(stack==null)return JsonNull.INSTANCE;JsonObject j=new JsonObject();j.addProperty("item",Item.itemRegistry.getNameForObject(stack.getItem()).toString());j.addProperty("count",stack.stackSize);j.addProperty("meta",stack.getMetadata());return j;
    }
}
