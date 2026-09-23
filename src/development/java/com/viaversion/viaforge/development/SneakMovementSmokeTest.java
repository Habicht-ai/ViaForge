package com.viaversion.viaforge.development;

import com.viaversion.viaforge.compatibility.*;
import com.viaversion.viaforge.common.compatibility.ClientRule;
import com.viaversion.viaforge.items.ServerElytraFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import java.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Regression of the actual living/input/travel tick, using real keyboard sampling. */
final class SneakMovementSmokeTest {
    static void verify(WorldClient world,EntityPlayerSP p) {
        if(!ServerSession.rule(ClientRule.SWIMMING))return;
        Minecraft mc=Minecraft.getMinecraft();net.minecraft.entity.Entity camera=mc.getRenderViewEntity();
        MovementInput oldInput=p.movementInput;Map<BlockPos,net.minecraft.block.state.IBlockState> saved=new LinkedHashMap<>();
        net.minecraft.item.ItemStack oldLeggings=p.getCurrentArmor(1);
        boolean sneak=mc.gameSettings.keyBindSneak.isKeyDown(),sprint=mc.gameSettings.keyBindSprint.isKeyDown(),forward=mc.gameSettings.keyBindForward.isKeyDown();
        try {
            mc.setRenderViewEntity(p);ServerElytraFlight.clear();ServerSwimming.clear();
            for(int x=3;x<=8;x++)for(int z=3;z<=20;z++)for(int y=199;y<=202;y++) {
                BlockPos pos=new BlockPos(x,y,z);saved.put(pos,world.getBlockState(pos));world.setBlockState(pos,(y==199?Blocks.stone:Blocks.air).getDefaultState(),3);
            }
            p.inventory.armorInventory[2]=null;p.capabilities.isFlying=p.capabilities.allowFlying=false;p.setHealth(20);
            p.setPosition(5.5,200,5.5);p.motionX=p.motionY=p.motionZ=0;p.onGround=true;p.rotationYaw=0;p.setSprinting(false);
            p.movementInput=new net.minecraft.util.MovementInputFromOptions(mc.gameSettings);
            key(mc.gameSettings.keyBindForward,true);key(mc.gameSettings.keyBindSneak,false);key(mc.gameSettings.keyBindSprint,true);
            p.onLivingUpdate();require(p.isSprinting(),"Dry forward+sprint starts through target input tick");
            key(mc.gameSettings.keyBindSneak,true);p.onLivingUpdate();
            boolean poseInput=ServerSession.rule(ClientRule.CRAWLING_POSE),shiftInput=ServerSession.rule(ClientRule.SHIFT_SWIM_INPUT);
            require(p.isSprinting()==poseInput,"Sprint retention with sneak follows original 1.14 boundary");
            require(Math.abs(p.moveForward-((!poseInput||shiftInput)?.294F:.98F))<1e-6,"Press tick uses correct current/previous sneak phase");
            p.onLivingUpdate();require(Math.abs(p.moveForward-.294F)<1e-6,"Held sneak is slowed exactly once");
            key(mc.gameSettings.keyBindSneak,false);p.onLivingUpdate();
            require(Math.abs(p.moveForward-(poseInput?.294F:.98F))<1e-6,"Release tick retains pre-sample slowdown since 1.14");
            p.onLivingUpdate();require(Math.abs(p.moveForward-.98F)<1e-6,"Following tick restores full input");
            key(mc.gameSettings.keyBindForward,false);key(mc.gameSettings.keyBindSprint,false);p.onLivingUpdate();
            require(!p.isSprinting(),"Releasing forward terminates sprint");
            if(ServerSession.rule(ClientRule.SWIFT_SNEAK)&&!ServerSession.rule(ClientRule.SNEAK_SPEED_ATTRIBUTE)) {
                net.minecraft.item.ItemStack leggings=new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_leggings);
                try {
                    // Captured from the actual 1.19.4 dry failure: native dummy
                    // enchantment plus our exact original item snapshot.
                    leggings.setTagCompound(net.minecraft.nbt.JsonToNBT.getTagFromJson("{ench:[{lvl:0s,id:0s}],ViaForge|flattenedItem:{id:524,tag:{Enchantments:[{lvl:3s,id:\"minecraft:swift_sneak\"}],Damage:0}},HideFlags:1}"));
                }catch(net.minecraft.nbt.NBTException e){throw new AssertionError(e);}
                p.inventory.armorInventory[1]=leggings;
                key(mc.gameSettings.keyBindForward,true);key(mc.gameSettings.keyBindSneak,true);
                p.onLivingUpdate();p.onLivingUpdate();
                require(Math.abs(p.moveForward-.735F)<1e-6,"Actual original Swift Sneak NBT supplies held input before the attribute boundary");
            }
        }finally {
            key(mc.gameSettings.keyBindSneak,sneak);key(mc.gameSettings.keyBindSprint,sprint);key(mc.gameSettings.keyBindForward,forward);
            p.movementInput=oldInput;mc.setRenderViewEntity(camera);ServerElytraFlight.clear();ServerSwimming.clear();
            p.inventory.armorInventory[1]=oldLeggings;
            for(Map.Entry<BlockPos,net.minecraft.block.state.IBlockState> e:saved.entrySet())world.setBlockState(e.getKey(),e.getValue(),3);
        }
    }
    private static void key(KeyBinding key,boolean down){KeyBinding.setKeyBindState(key.getKeyCode(),down);}
}
