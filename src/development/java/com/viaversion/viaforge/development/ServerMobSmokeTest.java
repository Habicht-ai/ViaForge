package com.viaversion.viaforge.development;
import com.viaversion.viaforge.common.blocks.*;
import com.viaversion.viaforge.mobs.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import java.util.*;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.*;

final class ServerMobSmokeTest {
    static void pipeline(BlockVersionProfile profile,EmbeddedChannel client,EmbeddedChannel server,NetHandlerPlayClient handler,WorldClient world) throws Exception {
        int first = profile.protocol() >= 210 ? 12 : 11, id = 800;
        List<Integer> spawned = new ArrayList<>();
        try {
            for (MobKind kind : MobKind.values()) {
                if (kind.protocol > profile.protocol()) continue;
                int mobId = ++id; spawned.add(mobId);
                ByteBuf add = packet(profile,"ADD_MOB"); Types.VAR_INT.writePrimitive(add,mobId); add.writeLong(0).writeLong(mobId);
                if (profile.protocol() >= 315) Types.VAR_INT.writePrimitive(add,kind.id); else add.writeByte(kind.id);
                add.writeDouble(4).writeDouble(65).writeDouble(4).writeByte(32).writeByte(16).writeByte(48).writeShort(0).writeShort(0).writeShort(0);
                add.writeByte(0).writeByte(0).writeByte(0);
                add.writeByte(first-5).writeByte(2).writeFloat(20); add.writeByte(255);
                send(client,server,handler,add);
                Entity entity = world.getEntityByID(mobId);
                require(entity != null,"Mob spawn "+kind+" "+profile);
                require(ServerMobs.get(entity) != null && ServerMobs.get(entity).kind == kind,"Original mob identity "+kind);
                if (kind == MobKind.ENDER_DRAGON) {
                    net.minecraft.entity.boss.EntityDragon dragon=(net.minecraft.entity.boss.EntityDragon)entity;
                    require(dragon.getParts().length == 8,"1.9 dragon has the additional neck hitbox");
                    for(int i=0;i<8;i++) require(dragon.getParts()[i].getEntityId() == mobId+i+1,"Server dragon part ID "+i);
                    ByteBuf phaseUpdate=packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(phaseUpdate,mobId);
                    phaseUpdate.writeByte(first).writeByte(1); Types.VAR_INT.writePrimitive(phaseUpdate,5); phaseUpdate.writeByte(255); send(client,server,handler,phaseUpdate);
                    float before=dragon.animTime; dragon.onLivingUpdate();
                    require(Math.abs(dragon.animTime-before-.1)<.0001 && dragon.dragonPartHead.width == 1,"Perched dragon wings and head hitbox");
                }
                if (!kind.custom()) continue;
                require(entity instanceof ServerMob,"Native mob replaces Via model "+kind);
                ServerMob mob = (ServerMob)entity;
                net.minecraft.item.ItemStack picked = mob.getPickedResult(new net.minecraft.util.MovingObjectPosition(mob));
                if (kind == MobKind.ILLUSIONER) require(picked == null, "No invented illusioner spawn egg");
                else require(picked != null && com.viaversion.viaforge.items.ItemVariants.eggType(picked) != null, "Target spawn egg when picking mob " + kind);
                require(Math.abs(mob.getHealth()-20) < .001,"Original mob health "+kind);
                ServerMobRenderer renderer = (ServerMobRenderer)Minecraft.getMinecraft().getRenderManager().<ServerMob>getEntityRenderObject(mob);
                require(renderer.model(kind) != null,"Mob model "+kind);
                try (java.io.InputStream texture = Minecraft.getMinecraft().getResourceManager().getResource(renderer.texture(mob)).getInputStream()) {
                    require(texture.read() == 137,"Original PNG "+renderer.texture(mob));
                }
                ByteBuf update = packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(update,mobId);
                update.writeByte(2).writeByte(3); Types.STRING.write(update,"MobFixture"); update.writeByte(3).writeByte(6).writeBoolean(true).writeByte(255);
                send(client,server,handler,update);
                require(mob.getCustomNameTag().equals("MobFixture") && mob.getHealth() == 20,"Partial mob metadata "+kind);
                ByteBuf movement=packet(profile,"MOVE_ENTITY_POS"); Types.VAR_INT.writePrimitive(movement,mobId); movement.writeShort(1024).writeShort(0).writeShort(0).writeBoolean(true);
                send(client,server,handler,movement);
                require(world.getEntityByID(mobId) == mob && mob.serverPosX == 136,"Movement retains original mob entity "+kind);
                ByteBuf status=packet(profile,"ENTITY_EVENT"); status.writeInt(mobId).writeByte(2); send(client,server,handler,status);
                require(mob.hurtTime == 10,"Mob hurt animation "+kind);
                for (int damage : new int[]{33,36,37}) {
                    if (damage != 33 && profile.protocol() < 335) continue;
                    mob.hurtTime = 0;
                    status = packet(profile,"ENTITY_EVENT"); status.writeInt(mobId).writeByte(damage); send(client,server,handler,status);
                    require(mob.hurtTime == 10, "Later damage status retains hurt animation " + damage);
                }
                for (int slot=0;slot<=5;slot++) {
                    ByteBuf equip = packet(profile,"SET_EQUIPPED_ITEM"); Types.VAR_INT.writePrimitive(equip,mobId); Types.VAR_INT.writePrimitive(equip,slot);
                    Types.ITEM1_8.write(equip,new com.viaversion.viaversion.api.minecraft.item.DataItem(267,(byte)1,(short)0,null)); send(client,server,handler,equip);
                }
                require(mob.offhand != null && mob.getHeldItem() != null && mob.getCurrentArmor(3) != null,"Mob equipment and offhand "+kind);
                if (kind == MobKind.SHULKER) {
                    update = packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(update,mobId);
                    update.writeByte(first).writeByte(10); Types.VAR_INT.writePrimitive(update,0);
                    update.writeByte(first+2).writeByte(0).writeByte(100).writeByte(255); send(client,server,handler,update);
                    for (int tick=0;tick<20;tick++) mob.onUpdate();
                    require(Math.abs(mob.peek-1) < .0001 && Math.abs(mob.getEntityBoundingBox().maxY-mob.getEntityBoundingBox().minY-2)<.001,"Opening shulker bounds "+profile);
                    update = packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(update,mobId);
                    update.writeByte(first).writeByte(10); Types.VAR_INT.writePrimitive(update,5); update.writeByte(255); send(client,server,handler,update);
                    require(Math.abs(mob.getEntityBoundingBox().maxX-mob.getEntityBoundingBox().minX-2)<.001,"Wall shulker bounds");
                }
                if (kind == MobKind.POLAR_BEAR || kind == MobKind.LLAMA || kind.zombie()) {
                    float width = mob.width;
                    update = packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(update,mobId); update.writeByte(first).writeByte(6).writeBoolean(true).writeByte(255); send(client,server,handler,update);
                    require(mob.isChild() && Math.abs(mob.width-width*.5)<.0001,"Baby mob hitbox "+kind);
                }
                if (kind == MobKind.LLAMA) llamaInventory(profile, client, server, handler, mob);
                int variants = kind == MobKind.SHULKER && profile.protocol() >= 315 ? 16 : kind == MobKind.LLAMA ? 4 : kind == MobKind.PARROT ? 5 : kind == MobKind.ZOMBIE_VILLAGER ? 6 : 0;
                for (int variant = 0; variant < variants; variant++) {
                    int slot = mob.state.first + (kind == MobKind.LLAMA ? 6 : kind == MobKind.ZOMBIE_VILLAGER ? 4 : 3);
                    mob.state.data.put(slot, variant);
                    try (java.io.InputStream texture = Minecraft.getMinecraft().getResourceManager().getResource(renderer.texture(mob)).getInputStream()) {
                        require(texture.read() == 137, "Original variant texture " + kind + "/" + variant);
                    }
                }
            }
            if (profile.protocol() < 315) {
                // 1.9/1.10 encode variants inside skeleton/zombie metadata, not separate spawn IDs.
                for (int[] variant : new int[][]{{51,1,MobKind.WITHER_SKELETON.ordinal()},{54,3,MobKind.ZOMBIE_VILLAGER.ordinal()},{51,2,MobKind.STRAY.ordinal()},{54,6,MobKind.HUSK.ordinal()}}) {
                    if (variant[1] == 2 || variant[1] == 6) if (profile.protocol() < 210) continue;
                    int mobId=++id; spawned.add(mobId); ByteBuf add=packet(profile,"ADD_MOB"); Types.VAR_INT.writePrimitive(add,mobId);
                    add.writeLong(0).writeLong(mobId).writeByte(variant[0]).writeDouble(4).writeDouble(65).writeDouble(4).writeByte(0).writeByte(0).writeByte(0).writeShort(0).writeShort(0).writeShort(0);
                    add.writeByte(first+(variant[0]==54?1:0)).writeByte(1); Types.VAR_INT.writePrimitive(add,variant[1]); add.writeByte(255); send(client,server,handler,add);
                    require(world.getEntityByID(mobId) instanceof ServerMob && ((ServerMob)world.getEntityByID(mobId)).kind() == MobKind.values()[variant[2]],"Pre-1.11 variant "+Arrays.toString(variant));
                }
            }
            for(int type : new int[]{67,93,68,79}) {
                if ((type==68 || type==79) && profile.protocol()<315) continue;
                int projectileId=++id; spawned.add(projectileId);
                ByteBuf add=packet(profile,"ADD_ENTITY"); Types.VAR_INT.writePrimitive(add,projectileId); add.writeLong(0).writeLong(projectileId).writeByte(type);
                add.writeDouble(4).writeDouble(65).writeDouble(4).writeByte(0).writeByte(0).writeInt(0).writeShort(400).writeShort(100).writeShort(200); send(client,server,handler,add);
                require(world.getEntityByID(projectileId) instanceof ServerMobProjectile,"Original mob projectile "+type);
                ServerMobProjectile entity=(ServerMobProjectile)world.getEntityByID(projectileId);
                ByteBuf original = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(original, projectileId);
                original.writeByte(0).writeByte(0).writeByte(0).writeByte(255); send(client, server, handler, original);
                require(!entity.hasCustomName() && !entity.getAlwaysRenderNameTag(), "No Via replacement name above mob projectile " + type);
                original = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(original, projectileId);
                original.writeByte(2).writeByte(3); Types.STRING.write(original, "Server projectile"); original.writeByte(3).writeByte(6).writeBoolean(true).writeByte(255);
                send(client, server, handler, original);
                require(entity.getCustomNameTag().equals("Server projectile") && entity.getAlwaysRenderNameTag(), "Real server projectile name retained");
                original = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(original, projectileId); original.writeByte(0).writeByte(0).writeByte(0).writeByte(255);
                send(client, server, handler, original); require(entity.getCustomNameTag().equals("Server projectile"), "Partial metadata retains original projectile name");
                original = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(original, projectileId);
                original.writeByte(2).writeByte(3); Types.STRING.write(original, ""); original.writeByte(3).writeByte(6).writeBoolean(false).writeByte(255);
                send(client, server, handler, original); require(!entity.hasCustomName() && !entity.getAlwaysRenderNameTag(), "Server clears projectile name");
                if(type==79) { ByteBuf event=packet(profile,"ENTITY_EVENT"); event.writeInt(projectileId).writeByte(4); send(client,server,handler,event); require(entity.fangsStarted,"Fangs animation trigger"); }
            }
            if(profile.protocol()>=335) {
                com.viaversion.nbt.tag.CompoundTag parrot=new com.viaversion.nbt.tag.CompoundTag(); parrot.putString("id","minecraft:parrot"); parrot.putInt("Variant",4);
                ByteBuf shoulder=packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(shoulder,612); shoulder.writeByte(15).writeByte(13); Types.NAMED_COMPOUND_TAG.write(shoulder,parrot); shoulder.writeByte(255);
                send(client,server,handler,shoulder);
                require(com.viaversion.viaforge.items.ServerEntityViews.get(612).leftShoulder != null && com.viaversion.viaforge.items.ServerEntityViews.get(612).leftShoulder.state.number(15,0)==4,"Shoulder parrot original variant NBT");
                shoulder=packet(profile,"SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(shoulder,612); shoulder.writeByte(15).writeByte(13); Types.NAMED_COMPOUND_TAG.write(shoulder,new com.viaversion.nbt.tag.CompoundTag()); shoulder.writeByte(255); send(client,server,handler,shoulder);
                require(com.viaversion.viaforge.items.ServerEntityViews.get(612).leftShoulder == null,"Shoulder cleared by server");
            }
            MobRenderSmokeTest.verify(profile,world,java.nio.file.Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent());
            require(ServerMobSounds.has("entity.shulker.ambient"),"Shulker original sounds loaded");
            String sound=ServerMobSounds.key("entity.shulker.ambient",5);
            require(Minecraft.getMinecraft().getSoundHandler().getSound(new net.minecraft.util.ResourceLocation(sound)) != null,"Shulker sound event registered");
            for(int soundId=0;soundId<600;soundId++) {
                String name=MobSoundCatalog.name(profile.resourceVersion(),soundId);
                if (!"entity.shulker.ambient".equals(name)) continue;
                ByteBuf packet=packet(profile,"SOUND"); Types.VAR_INT.writePrimitive(packet,soundId); Types.VAR_INT.writePrimitive(packet,5); packet.writeInt(32).writeInt(520).writeInt(32).writeFloat(.1F);
                if(profile.protocol()>=210) packet.writeFloat(1); else packet.writeByte(63);
                send(client,server,handler,packet);
            }
        } finally { for(int mobId:spawned) { world.removeEntityFromWorld(mobId); ServerMobs.remove(mobId); } }
    }
    private static void llamaInventory(BlockVersionProfile profile, EmbeddedChannel client, EmbeddedChannel server, NetHandlerPlayClient handler, ServerMob mob) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.client.gui.GuiScreen previousScreen = mc.currentScreen;
        net.minecraft.inventory.Container previousContainer = mc.thePlayer.openContainer;
        try {
            for (int strength = 0; strength <= 5; strength++) {
                int size = 2 + strength * 3, window = 21 + strength;
                ByteBuf metadata = packet(profile, "SET_ENTITY_DATA"); Types.VAR_INT.writePrimitive(metadata, mob.getEntityId());
                metadata.writeByte(mob.state.first).writeByte(6).writeBoolean(false);
                metadata.writeByte(mob.state.first + 3).writeByte(6).writeBoolean(strength != 0);
                metadata.writeByte(mob.state.first + 5).writeByte(1); Types.VAR_INT.writePrimitive(metadata, 14);
                metadata.writeByte(mob.state.first + 4).writeByte(1); Types.VAR_INT.writePrimitive(metadata, Math.max(1, strength)); metadata.writeByte(255);
                send(client, server, handler, metadata);
                ByteBuf open = packet(profile, "OPEN_SCREEN"); open.writeByte(window); Types.STRING.write(open, "EntityHorse");
                Types.STRING.write(open, "{\"text\":\"Llama\"}"); open.writeByte(size).writeInt(mob.getEntityId()); send(client, server, handler, open);
                require(mc.currentScreen instanceof LlamaInventoryScreen && mc.thePlayer.openContainer instanceof LlamaContainer, "Native llama inventory opens");
                LlamaContainer menu = (LlamaContainer)mc.thePlayer.openContainer;
                require(menu.columns == strength && menu.inventorySlots.size() == size + 36 && menu.windowId == window, "Exact llama server slot count " + strength);
                com.viaversion.viaversion.api.minecraft.item.Item[] content = new com.viaversion.viaversion.api.minecraft.item.Item[size + 36];
                content[1] = new com.viaversion.viaversion.api.minecraft.item.DataItem(171, (byte)1, (short)14, null);
                content[size + 8] = new com.viaversion.viaversion.api.minecraft.item.DataItem(201, (byte)7, (short)0, null);
                ByteBuf items = packet(profile, "CONTAINER_SET_CONTENT"); items.writeByte(window); Types.ITEM1_8_SHORT_ARRAY.write(items, content); send(client, server, handler, items);
                require(menu.getSlot(1).getStack().getMetadata() == 14 && menu.getSlot(size + 8).getStack().stackSize == 7, "Original llama inventory slots survive Via");
                require(!menu.getSlot(0).isItemValid(new net.minecraft.item.ItemStack(net.minecraft.init.Items.saddle)) && menu.getSlot(1).isItemValid(menu.getSlot(1).getStack()), "Llama carpet and disabled saddle slot");
                if (strength == 3 && profile == BlockVersionProfile.V1_12_2) {
                    mc.getFramebuffer().framebufferClear(); mc.getFramebuffer().bindFramebuffer(true);
                    mc.entityRenderer.setupOverlayRendering();
                    mc.currentScreen.drawScreen(0, 0, 0);
                    net.minecraft.util.ScreenShotHelper.saveScreenshot(java.nio.file.Paths.get(System.getenv("VIAFORGE_BLOCK_SMOKE_TEST")).toAbsolutePath().getParent().toFile(), "llama-inventory-1.12.2.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                }
                ByteBuf click = BlockPipelineSmokeTest.packet(0x0e); click.writeByte(window).writeShort(1).writeByte(0).writeShort(1).writeByte(0); Types.ITEM1_8.write(click, content[1]);
                BlockItemPipelineSmokeTest.send(client, server, click);
                ByteBuf sent = BlockPipelineSmokeTest.take(server, BlockItemPipelineSmokeTest.serverbound(profile, "CONTAINER_CLICK"));
                try { require(sent.readUnsignedByte() == window && sent.readShort() == 1, "Carpet click retains original slot"); } finally { sent.release(); }
                ByteBuf close = BlockPipelineSmokeTest.packet(0x0d); close.writeByte(window); BlockItemPipelineSmokeTest.send(client, server, close);
                BlockPipelineSmokeTest.take(server, BlockItemPipelineSmokeTest.serverbound(profile, "CONTAINER_CLOSE")).release();
            }
        } finally { mc.displayGuiScreen(previousScreen); mc.thePlayer.openContainer = previousContainer; }
    }
}
