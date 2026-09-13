package com.viaversion.viaforge.development;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.items.*;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class DragonHeadSmokeTest {
    static void verify(BlockVersionProfile profile, WorldClient world) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Field jawField = ServerItemRenderer.class.getDeclaredField("JAW"); jawField.setAccessible(true);
        ModelRenderer jaw = (ModelRenderer)jawField.get(null);
        LayerCustomHead layer = new LayerCustomHead(new ModelRenderer(new ModelBase() { }));
        net.minecraft.item.ItemStack previous = mc.thePlayer.getCurrentArmor(3);
        TextureManager engine = TileEntityRendererDispatcher.instance.renderEngine;
        TileEntityRendererDispatcher.instance.renderEngine = mc.getTextureManager();
        BlockPos pos = new BlockPos(9, 75, 9);
        net.minecraft.block.state.IBlockState oldBlock = world.getBlockState(pos), oldBelow = world.getBlockState(pos.down());
        int oldAge = mc.thePlayer.ticksExisted;
        try {
            for (EntityLivingBase entity : new EntityLivingBase[]{mc.thePlayer, new EntityZombie(world), new EntityArmorStand(world)}) {
                entity.setCurrentItemOrArmor(4, stack(397, 5));
                entity.ticksExisted = 10;
                layer.doRenderLayer(entity, 0, 0, .2F, 10.2F, 0, 0, .0625F);
                float resting = jaw.rotateAngleX;
                entity.ticksExisted = 300;
                layer.doRenderLayer(entity, 0, 0, .9F, 300.9F, 0, 0, .0625F);
                require(jaw.rotateAngleX == resting, "Idle worn dragon head ignores elapsed time: " + entity.getClass().getSimpleName());
                layer.doRenderLayer(entity, 2.5F, 1, .5F, 301.5F, 0, 0, .0625F);
                require(Math.abs(jaw.rotateAngleX - .4F) < .00001F, "Walking head follows limb-swing phase");
                layer.doRenderLayer(entity, 2.5F, 0, .8F, 400.8F, 0, 0, .0625F);
                require(Math.abs(jaw.rotateAngleX - .4F) < .00001F, "Stopping freezes head pose instead of advancing by time");
            }
            ServerItemRenderer.render(stack(397, 5));
            require(Math.abs(jaw.rotateAngleX - .2F) < .00001F, "Inventory/held/dropped head uses fixed phase after worn render");
            world.setBlockState(pos, Blocks.skull.getDefaultState(), 0);
            TileEntitySkull skull = (TileEntitySkull)world.getTileEntity(pos); skull.setType(5);
            require(skull instanceof ITickable && skull instanceof DragonHeadAnimation, "Placed skull participates in native tile ticking");
            world.setBlockState(pos.down(), Blocks.stone.getDefaultState(), 0);
            for (int tick = 0; tick < 4; tick++) ((ITickable)skull).update();
            DragonHeadAnimation animation = (DragonHeadAnimation)skull;
            require(animation.viaForge$jawTime(.5F) == 0, "Unpowered placed head stays still");
            world.setBlockState(pos.down(), Blocks.redstone_block.getDefaultState(), 0);
            for (int tick = 0; tick < 3; tick++) ((ITickable)skull).update();
            require(animation.viaForge$jawTime(.5F) == 3.5F, "Powered head increments and interpolates original animation clock");
            for (EnumFacing facing : new EnumFacing[]{EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}) {
                world.setBlockState(pos, Blocks.skull.getStateFromMeta(facing.getIndex()), 0);
                TileEntityRendererDispatcher.instance.<TileEntitySkull>getSpecialRenderer(skull).renderTileEntityAt(skull, 0, 0, 0, .5F, -1);
                require(Math.abs(jaw.rotateAngleX - ((float)Math.sin(3.5F * (float)Math.PI * .2F) + 1) * .2F) < .00001F, "Placed renderer uses powered clock for " + facing);
            }
            world.setBlockState(pos.down(), Blocks.stone.getDefaultState(), 0);
            for (int tick = 0; tick < 5; tick++) ((ITickable)skull).update();
            require(animation.viaForge$jawTime(.1F) == 3 && animation.viaForge$jawTime(.9F) == 3, "Power removal freezes pose without partial-tick motion");
            world.setBlockState(pos.down(), Blocks.redstone_block.getDefaultState(), 0); ((ITickable)skull).update();
            require(animation.viaForge$jawTime(0) == 4, "Power resumes from previous pose");
            skull.setType(0); ((ITickable)skull).update();
            require(animation.viaForge$jawTime(.5F) == 4, "Other skull types do not animate");
        } finally {
            mc.thePlayer.ticksExisted = oldAge; mc.thePlayer.setCurrentItemOrArmor(4, previous);
            TileEntityRendererDispatcher.instance.renderEngine = engine;
            world.setBlockState(pos, oldBlock, 0); world.setBlockState(pos.down(), oldBelow, 0);
        }
    }
}
