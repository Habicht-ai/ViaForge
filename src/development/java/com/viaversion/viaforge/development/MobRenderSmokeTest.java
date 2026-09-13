package com.viaversion.viaforge.development;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.mobs.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.lwjgl.util.glu.GLU;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class MobRenderSmokeTest {
    static void verify(BlockVersionProfile profile,WorldClient world,Path directory) throws Exception {
        if (profile != BlockVersionProfile.V1_9 && profile != BlockVersionProfile.V1_10_2 && profile != BlockVersionProfile.V1_11 && profile != BlockVersionProfile.V1_12_2) return;
        List<ServerMob> mobs=new ArrayList<>();
        for (MobKind kind:MobKind.values()) if(kind.custom() && kind.protocol<=profile.protocol()) {
            MobState state=new MobState(profile.protocol(),kind.id); ServerMob mob=new ServerMob(world,state,new UUID(0,kind.id)); mobs.add(mob);
            if(kind.skeleton()) { mob.setCurrentItemOrArmor(0,new ItemStack(Items.bow)); state.data.put(state.first+(profile.protocol()<315?1:0),true); }
            if(kind == MobKind.VINDICATOR) { mob.setCurrentItemOrArmor(0,new ItemStack(Items.iron_axe)); state.data.put(state.first,(byte)1); }
            if(kind == MobKind.VEX) { mob.setCurrentItemOrArmor(0,new ItemStack(Items.iron_sword)); state.data.put(state.first,(byte)1); }
            if(kind == MobKind.EVOKER) state.data.put(state.first+(profile.protocol()>=335?1:0),(byte)1);
            if(kind == MobKind.SHULKER) mob.peek=mob.prevPeek=.7F;
            if(kind == MobKind.LLAMA) { state.data.put(state.first+3,true); state.data.put(state.first+5,14); }
        }
        List<String> labels = new ArrayList<>();
        for (ServerMob mob : mobs) labels.add(mob.kind().name());
        gallery(profile, directory, new ArrayList<Entity>(mobs), labels, "mobs");
        List<Entity> details = new ArrayList<>(); labels.clear();
        for (int face = 0; face < 6; face++) {
            MobState state = new MobState(profile.protocol(), 69); state.data.put(state.first, face);
            ServerMob mob = new ServerMob(world, state, new UUID(0, face)); mob.peek = mob.prevPeek = 1;
            details.add(mob); labels.add("SHULKER " + EnumFacing.getFront(face));
        }
        for (int type : new int[]{67, 93, 68, 79}) {
            if (profile.protocol() < 315 && (type == 68 || type == 79)) continue;
            ServerMobProjectile projectile = new ServerMobProjectile(world, type);
            projectile.fangsStarted = true; projectile.fangLife = 16;
            details.add(projectile); labels.add(type == 67 ? "SHULKER BULLET" : type == 93 ? "DRAGON FIREBALL" : type == 68 ? "LLAMA SPIT" : "EVOKER FANGS");
        }
        for (Entity entity : world.loadedEntityList) if (entity instanceof net.minecraft.entity.boss.EntityDragon && ServerMobs.get(entity) != null) {
            details.add(entity); labels.add("PERCHED DRAGON (1:6.7)"); break;
        }
        if (profile.protocol() >= 210) {
            ServerMob bear = new ServerMob(world, new MobState(profile.protocol(), 102), new UUID(0, 102));
            bear.standing = bear.prevStanding = 6; details.add(bear); labels.add("POLAR BEAR STANDING");
        }
        for (Entity entity : world.loadedEntityList) if (entity instanceof net.minecraft.entity.passive.EntityRabbit && ServerMobs.get(entity) != null) {
            require(Math.abs(entity.width - .4F) < .0001 && Math.abs(entity.height - .5F) < .0001, "Native 1.9 rabbit dimensions");
            details.add(entity); labels.add("RABBIT 1.9+ SIZE"); break;
        }
        gallery(profile, directory, details, labels, "mob-details");
    }
    private static void gallery(BlockVersionProfile profile, Path directory, List<Entity> mobs, List<String> labels, String filename) throws Exception {
        int columns=4, rows=(mobs.size()+3)/4, width=1200,height=rows*320+40;
        Minecraft mc=Minecraft.getMinecraft(); Framebuffer frame=new Framebuffer(width,height,true); frame.setFramebufferColor(.12F,.14F,.18F,1);
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.matrixMode(5888); GlStateManager.pushMatrix();
        try {
            frame.framebufferClear(); frame.bindFramebuffer(true);
            for(int i=0;i<mobs.size();i++) {
                Entity entity=mobs.get(i); int col=i%columns,row=i/columns;
                GlStateManager.viewport(col*300,height-40-(row+1)*320,300,300);
                GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GLU.gluPerspective(40,1,.05F,100);
                GlStateManager.matrixMode(5888); GlStateManager.loadIdentity();
                if (filename.equals("mob-details")) GLU.gluLookAt(4,2.8F,5,0,1,0,0,1,0);
                else GLU.gluLookAt(3,2.1F,4,0,1,0,0,1,0);
                GlStateManager.enableDepth(); GlStateManager.depthMask(true); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D(); GlStateManager.enableCull(); GlStateManager.disableFog();
                RenderHelper.enableStandardItemLighting(); OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240); GlStateManager.color(1,1,1,1);
                entity.ticksExisted = 100;
                if (entity instanceof ServerMob) {
                    ServerMob mob = (ServerMob)entity;
                    mob.limbSwing=1.5F; mob.limbSwingAmount=mob.prevLimbSwingAmount=.25F;
                    if(mob.kind() == MobKind.SHULKER) mob.renderYawOffset=mob.prevRenderYawOffset=mob.rotationYaw=180;
                }
                if (entity instanceof net.minecraft.entity.boss.EntityDragon) { GlStateManager.translate(0, .6F, 0); GlStateManager.scale(.15F, .15F, .15F); }
                mc.getRenderManager().options = mc.gameSettings;
                mc.getRenderManager().<Entity>getEntityRenderObject(entity).doRender(entity,0,0,0,0,.5F);
                require(org.lwjgl.opengl.GL11.glGetError()==0,"Mob rendering GL state "+labels.get(i)+" "+profile);
            }
            GlStateManager.viewport(0,0,width,height); GlStateManager.matrixMode(5889); GlStateManager.loadIdentity(); GlStateManager.ortho(0,width,height,0,-1000,1000);
            GlStateManager.matrixMode(5888); GlStateManager.loadIdentity(); GlStateManager.disableDepth(); GlStateManager.disableLighting(); GlStateManager.color(1,1,1,1);
            mc.fontRendererObj.drawString("Forge 1.8.9 | Original mob models and textures | "+profile.resourceVersion(),16,12,0xffffff);
            for(int i=0;i<mobs.size();i++) mc.fontRendererObj.drawString(labels.get(i),i%columns*300+12,i/columns*320+48,0xffffff);
            ScreenShotHelper.saveScreenshot(directory.toFile(),filename+"-"+profile.resourceVersion()+".png",width,height,frame);
        } finally {
            GlStateManager.matrixMode(5888); GlStateManager.popMatrix(); GlStateManager.matrixMode(5889); GlStateManager.popMatrix(); GlStateManager.matrixMode(5888);
            frame.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true); GlStateManager.color(1,1,1,1);
        }
    }
}
