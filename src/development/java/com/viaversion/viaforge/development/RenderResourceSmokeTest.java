package com.viaversion.viaforge.development;

import com.google.gson.*;
import com.viaversion.viaforge.blocks.resources.*;
import com.viaversion.viaforge.common.blocks.LegacyBlockCatalog;
import com.viaversion.viaforge.common.compatibility.CompatibilityProfile;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

/** Regressions from real 1.13/26.2 screenshots. Pixel hashes were independently
 * decoded with Pillow from the hash-pinned original 26.2 client, not ImageIO. */
final class RenderResourceSmokeTest {
    static void restored() {
        Minecraft mc=Minecraft.getMinecraft();
        net.minecraft.client.multiplayer.WorldClient world=new net.minecraft.client.multiplayer.WorldClient(null,
                new net.minecraft.world.WorldSettings(0,net.minecraft.world.WorldSettings.GameType.CREATIVE,false,false,net.minecraft.world.WorldType.DEFAULT),0,net.minecraft.world.EnumDifficulty.PEACEFUL,new net.minecraft.profiler.Profiler());
        world.doPreChunk(0,0,true);Arrays.fill(world.getChunkFromChunkCoords(0,0).getBiomeArray(),(byte)1);
        require(net.minecraft.world.biome.BiomeColorHelper.getWaterColorAtPos(world,new BlockPos(8,64,8))==0xFFFFFF,"Native 1.8 water restored after disconnect");
        for(ITextureObject texture:((com.viaversion.viaforge.mixin.impl.blocks.VersionTextureCache)mc.getTextureManager()).viaForge$textures().values())
            require(!(texture instanceof LayeredColorMaskTexture),"All previous-session banner/shield composites evicted");
    }
    static void verify(CompatibilityProfile profile,Map<String,byte[]> original,Map<String,byte[]> converted,Path directory)throws Exception {
        for(String texture:new String[]{"shield_base","shield_base_nopattern","banner_base"})
            require(converted.containsKey("textures/entity/"+texture+".png"),"Required standalone texture "+texture);
        for(String family:new String[]{"banner","shield"})for(String mask:new String[]{"base","stripe_center","cross"}) {
            BufferedImage image=read(converted.get("textures/entity/"+family+"/"+mask+".png"));
            require(image.getType()==BufferedImage.TYPE_4BYTE_ABGR,"Native dye renderer accepts "+family+"/"+mask);
        }
        net.minecraft.client.multiplayer.WorldClient world=new net.minecraft.client.multiplayer.WorldClient(null,
                new net.minecraft.world.WorldSettings(0,net.minecraft.world.WorldSettings.GameType.CREATIVE,false,false,net.minecraft.world.WorldType.DEFAULT),0,net.minecraft.world.EnumDifficulty.PEACEFUL,new net.minecraft.profiler.Profiler());
        world.doPreChunk(0,0,true);Arrays.fill(world.getChunkFromChunkCoords(0,0).getBiomeArray(),(byte)1);
        require(net.minecraft.world.biome.BiomeColorHelper.getWaterColorAtPos(world,new BlockPos(8,64,8))==0x3F76E4,"Actual native renderer uses modern water tint");
        if(profile.serverProtocol()!=776)return;
        require(!com.viaversion.viaforge.compatibility.ServerSession.awaitingResources(),"Completed pack releases the loading gate");
        String[][] pixels={
            {"block/grass_block_side_overlay","acc365a4695b2b4973f49f13bc846da4a46b4e26d89c56ed6bbd9973971c096d"},
            {"item/tipped_arrow_head","6160493705b797698be6b09d6fb3cd60877978cdeb1e6ce421721df52808378f"},
            {"item/iron_nugget","b5f5fe3eff5b8d102bb65e9d8e42830a6087cb8f46572124388f240f48d9d352"},
            {"item/iron_ingot","5b855a75c0de56b154b73a05cd2785e3fdd2916e2265f5398678b497218c26d7"},
            {"item/gunpowder","621e43198b27525c374f286177adb50d8da86cf08b21762070747c155e553138"},
            {"entity/shield/shield_base_nopattern","3ff99d9e39753b493bd4ea2f9fc425e69186f0257c744024c00657cdd1d1e3bd"},
            {"entity/banner/banner_base","69301d5f3c24435cad1c0208c1e998fe139ca29e9d2f2a60f93939cdbfc459db"},
            {"entity/banner/base","bc46d8d547fb4f9609a089e570675d4fba38b9e762dc80d3fd388d78cb4294a4"},
            {"entity/banner/stripe_center","f9934d32b67cd060799076451c476a5cff07aea7489be098e186ac9aaad2898f"},
            {"block/blue_bed_head_up","cf1180f9053edef65301190e235c24557b0b63775676b6f041817666fc76615a"},
            {"block/red_bed_foot_up","b9b743695f29723c8596ec7dac24f081d3258cd538a678f3c43807a52e6341a8"}
        };
        for(String[] fixture:pixels)require(hash(read(converted.get("textures/"+fixture[0]+".png"))).equals(fixture[1]),"Original RGBA samples including transparency "+fixture[0]);
        for(String texture:new String[]{"grass_side_overlay","iron_ingot","gunpowder"}) {
            boolean block=texture.startsWith("grass");
            TextureAtlasSprite sprite=Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:"+(block?"blocks/":"items/")+texture);
            require(sprite.getIconWidth()==16&&sprite.getIconHeight()==16,"Actual atlas sprite "+texture);
            int[] data=sprite.getFrameTextureData(0)[0];
            BufferedImage expected=read(converted.get("textures/"+(block?"blocks/":"items/")+texture+".png"));
            for(int i=0;i<256;i++)require(data[i]>>>24==expected.getRGB(i&15,i>>4)>>>24,"Baked atlas alpha "+texture+"/"+i);
        }
        verifyBeds(original,converted);
        textureUpload("viaforge:textures/entity/shield_base_nopattern.png",read(converted.get("textures/entity/shield_base_nopattern.png")));
        preview(directory);
    }
    private static void verifyBeds(Map<String,byte[]> original,Map<String,byte[]> converted)throws Exception {
        Map<String,byte[]> models=LegacyBlockModels.generate(converted);
        for(String color:LegacyBlockCatalog.COLORS) {
            String modern=color.replace("silver","light_gray");
            for(String half:new String[]{"head","foot"}) {
                JsonObject actual=json(models.get("models/block/"+color+"_bed_"+half+".json"));
                JsonObject template=json(original.get("models/block/template_bed_"+half+".json"));
                require(actual.get("elements").equals(template.get("elements")),"Original 26.2 bed geometry and every face UV "+color+"/"+half);
                String up=actual.getAsJsonObject("textures").get("up").getAsString();
                require(up.equals("viaforge:blocks/"+modern+"_bed_"+half+"_up"),"Bed uses its own target color "+color);
                require(converted.containsKey("textures/blocks/"+modern+"_bed_"+half+"_up.png"),"Bed color texture exists "+color);
            }
            JsonArray elements=json(models.get("models/item/"+color+"_bed.json")).getAsJsonArray("elements");
            require(elements.size()==6,"Composite bed has both mattresses and all four legs");
            require(elements.get(3).getAsJsonObject().getAsJsonArray("from").get(2).getAsInt()==16,"Original composite foot translation");
        }
    }
    private static void textureUpload(String path,BufferedImage expected) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(path));
        require(GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)==expected.getWidth(),"Shield uploaded its own texture");
        ByteBuffer rgba=BufferUtils.createByteBuffer(expected.getWidth()*expected.getHeight()*4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,rgba);
        for(int y=0;y<expected.getHeight();y++)for(int x=0;x<expected.getWidth();x++) {
            int i=(y*expected.getWidth()+x)*4,actual=(rgba.get(i+3)&255)<<24|(rgba.get(i)&255)<<16|(rgba.get(i+1)&255)<<8|rgba.get(i+2)&255;
            require(actual==expected.getRGB(x,y),"Actual shield GL texel "+x+","+y);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
    }
    private static String hash(BufferedImage image)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int rgb=image.getRGB(x,y);digest.update((byte)(rgb>>16));digest.update((byte)(rgb>>8));digest.update((byte)rgb);digest.update((byte)(rgb>>24));
        }
        StringBuilder hex=new StringBuilder();for(byte value:digest.digest())hex.append(String.format("%02x",value&255));return hex.toString();
    }
    private static BufferedImage read(byte[] png)throws Exception { require(png!=null,"Required PNG is present");return ImageIO.read(new ByteArrayInputStream(png)); }
    private static JsonObject json(byte[] data){return new JsonParser().parse(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();}
    private static void preview(Path directory)throws Exception {
        Minecraft mc=Minecraft.getMinecraft();int width=1080,height=360;
        Framebuffer target=new Framebuffer(width,height,true);target.setFramebufferColor(.18F,.22F,.29F,1);target.framebufferClear();target.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.loadIdentity();GlStateManager.ortho(0,width,height,0,-3000,3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        try {
            GlStateManager.disableLighting();mc.fontRendererObj.drawString("26.2 | target textures, transparency, composite beds, dyed banners and shield",16,12,0xFFFFFF);
            Set<Integer> bannerColors=new HashSet<>();
            for(int color=0;color<16;color++) {
                draw(ServerEntitySmokeTest.stack(355,color),12+color*66,45);
                ItemStack banner=new ItemStack(Items.banner,1,15-color);draw(banner,12+color*66,120);
                TileEntityBanner tile=new TileEntityBanner();tile.setItemValues(banner);
                mc.getTextureManager().bindTexture(new ResourceLocation(tile.getPatternResourceLocation()));
                ByteBuffer data=BufferUtils.createByteBuffer(64*64*4);GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
                int i=(8*64+8)*4;bannerColors.add((data.get(i)&255)<<16|(data.get(i+1)&255)<<8|data.get(i+2)&255);
            }
            require(bannerColors.size()==16,"All 16 rendered banner dyes are distinct (no skipped masks/palette quantization)");
            draw(ServerEntitySmokeTest.stack(442,0),12,235);
            draw(ServerEntitySmokeTest.stack(440,0),90,235);
            draw(new ItemStack(Items.iron_ingot),168,235);draw(new ItemStack(Items.gunpowder),246,235);
            draw(new ItemStack(Items.ghast_tear),324,235);draw(new ItemStack(Items.nether_star),402,235);
            ScreenShotHelper.saveScreenshot(directory.toFile(),"render-fixes-26.2.png",width,height,target);
        }finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            target.deleteFramebuffer();mc.getFramebuffer().bindFramebuffer(true);RenderHelper.disableStandardItemLighting();GlStateManager.color(1,1,1,1);
        }
    }
    private static void draw(ItemStack stack,int x,int y) {
        RenderHelper.enableGUIStandardItemLighting();GlStateManager.enableDepth();GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();GlStateManager.translate(x,y,0);GlStateManager.scale(3,3,3);
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack,0,0);GlStateManager.popMatrix();
    }
}
