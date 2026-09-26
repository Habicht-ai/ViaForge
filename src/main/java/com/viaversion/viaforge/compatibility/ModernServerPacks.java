package com.viaversion.viaforge.compatibility;

import com.viaversion.nbt.tag.Tag;
import static com.viaversion.viaforge.common.compatibility.ServerPackStatus.*;
import com.viaversion.viaforge.common.compatibility.*;
import com.viaversion.viaforge.mixin.impl.connect.ServerPackRepositoryAccess;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.*;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.*;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.util.ComponentUtil;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.*;
import net.minecraft.util.IChatComponent;

/** Retain UUID, push/pop and configuration requests before the lossy single-pack translator. */
public final class ModernServerPacks {
    private static final ExecutorService DOWNLOADS=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"ViaForge modern server pack");t.setDaemon(true);return t;});
    private static Session current;
    private static boolean installing;
    private static final org.apache.logging.log4j.Logger LOGGER=org.apache.logging.log4j.LogManager.getLogger("ViaForge/ServerPacks");

    public static boolean capture(Object boundary,State state,PacketWrapper packet) {
        int version=CompatibilityRegistry.forUser(packet.user()).serverProtocol();
        boolean single=version==764&&boundary instanceof Protocol1_20_2To1_20;
        if(!single&&(!(boundary instanceof Protocol1_20_3To1_20_2)||version<765))return false;
        if(state!=State.PLAY&&state!=State.CONFIGURATION)return false;
        String name=packet.getPacketType()==null?"":packet.getPacketType().getName();
        boolean push=name.equals("RESOURCE_PACK_PUSH")||single&&name.equals("RESOURCE_PACK"),pop=name.equals("RESOURCE_PACK_POP");
        if(!push&&!pop)return false;
        UserConnection user=packet.user();
        if(push) {
            UUID id=single?new UUID(0,0):packet.read(Types.UUID);String url=packet.read(Types.STRING),hash=packet.read(Types.STRING);
            boolean required=packet.read(Types.BOOLEAN);String text;
            if(single){com.viaversion.viaversion.libs.gson.JsonElement prompt=packet.read(Types.OPTIONAL_COMPONENT);text=prompt==null?null:prompt.toString();}
            else {Tag prompt=packet.read(Types.TRUSTED_OPTIONAL_TAG);text=prompt==null?null:ComponentUtil.tagToJson(prompt).toString();}
            ClientPacketTasks.connectionEvent(user.getChannel(),()->session(user).push(id,url,hash,required,text));
        }else {
            UUID id=packet.read(Types.OPTIONAL_UUID);
            ClientPacketTasks.connectionEvent(user.getChannel(),()->session(user).pop(id));
        }
        packet.cancel();return true;
    }
    private static Session session(UserConnection user) {
        if(current==null||current.user!=user) {
            if(current!=null)current.close();current=new Session(user);
            Session owner=current;
            user.getChannel().closeFuture().addListener(ignored->ClientPacketTasks.connectionEvent(user.getChannel(),()->{
                owner.close();if(current==owner){current=null;install(null);}
            }));
        }
        return current;
    }
    /** Native disconnect/clear cancels download ownership, not just the visible pack. */
    public static void clear() {
        if(installing)return;
        if(current!=null){current.close();current=null;}
    }
    private static void install(IResourcePack pack) {
        installing=true;try{ServerPackReload.install(pack);}finally{installing=false;}
    }
    private static final class Entry {
        final UUID id;final String url,hash,prompt;final boolean required;
        Future<?> worker;File file;boolean accepted,loaded,finished;
        Entry(UUID id,String url,String hash,boolean required,String prompt){this.id=id;this.url=url;this.hash=hash;this.required=required;this.prompt=prompt;}
    }
    private static final class Session {
        final UserConnection user;
        final LinkedHashMap<UUID,Entry> entries=new LinkedHashMap<>();
        List<Entry> installed=Collections.emptyList();
        Boolean consent;boolean closed;
        GuiYesNo dialog;
        GuiScreen dialogParent;
        Session(UserConnection user){this.user=user;}
        boolean owns(Entry e){return current==this&&!closed&&user.getChannel().isActive()&&entries.get(e.id)==e;}
        void status(Entry entry,ServerPackStatus action) {
            if(closed||!user.getChannel().isActive())return;
            user.getChannel().eventLoop().execute(()->{
                if(!user.getChannel().isActive())return;
                State state=user.getProtocolInfo().getServerState();
                if(state!=State.PLAY&&state!=State.CONFIGURATION)return;
                LOGGER.debug("Server pack {} status {} in {}",entry.id,action,state);
                boolean single=CompatibilityRegistry.forUser(user).serverProtocol()==764;
                if(single&&action==DOWNLOADED)return;
                PacketWrapper response=PacketWrapper.create(state==State.PLAY?(single?com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2.RESOURCE_PACK:ServerboundPackets1_20_3.RESOURCE_PACK):ServerboundConfigurationPackets1_20_2.RESOURCE_PACK,user);
                if(!single)response.write(Types.UUID,entry.id);response.write(Types.VAR_INT,action.wireId(single));
                if(single)response.sendToServer(Protocol1_20_2To1_20.class,true);else response.sendToServer(Protocol1_20_3To1_20_2.class,true);
            });
        }
        void push(UUID id,String url,String hash,boolean required,String prompt) {
            Entry entry=new Entry(id,url,hash,required,prompt);
            try {URL parsed=new URL(url);if(!parsed.getProtocol().equals("http")&&!parsed.getProtocol().equals("https"))throw new IllegalArgumentException("URL scheme");}
            catch(Exception invalid){status(entry,INVALID_URL);return;}
            Entry previous=entries.remove(id);if(previous!=null&&previous.worker!=null)previous.worker.cancel(true);
            entries.put(id,entry);
            Minecraft mc=Minecraft.getMinecraft();ServerData data=mc.getCurrentServerData();
            if(consent==null&&data!=null&&data.getResourceMode()!=ServerData.ServerResourceMode.PROMPT)consent=data.getResourceMode()==ServerData.ServerResourceMode.ENABLED;
            LOGGER.debug("Server pack {} required={} preference={} in {}",id,required,data==null?"PROMPT":data.getResourceMode(),user.getProtocolInfo().getServerState());
            // Original clients ask again for a required pack even when optional
            // packs were disabled. A saved preference is not a required-pack refusal.
            if(required&&Boolean.FALSE.equals(consent))consent=null;
            if(consent!=null){if(consent)accept(entry);else decline(entry);return;}
            if(dialog==null)dialogParent=mc.currentScreen;
            boolean mandatory=entries.values().stream().anyMatch(e->!e.finished&&!e.accepted&&e.required);
            String message=mandatory?"This server requires a custom resource pack.":"This server recommends a custom resource pack.";
            String detail=mandatory?"Accept it to join this server.":"Would you like to download and install it?";
            if(prompt!=null)try{detail+=" "+IChatComponent.Serializer.jsonToComponent(prompt).getFormattedText();}catch(Exception ignored){}
            dialog=new GuiYesNo((accepted,button)->{
                if(current!=this||closed)return;
                consent=accepted;GuiYesNo old=dialog;dialog=null;
                if(data!=null){
                    if(accepted)data.setResourceMode(ServerData.ServerResourceMode.ENABLED);
                    else if(!mandatory)data.setResourceMode(ServerData.ServerResourceMode.DISABLED);
                    net.minecraft.client.multiplayer.ServerList.func_147414_b(data);
                }
                if(mc.currentScreen==old)mc.displayGuiScreen(mc.theWorld==null?dialogParent:null);
                for(Entry pending:new ArrayList<>(entries.values()))if(!pending.accepted&&!pending.finished){if(accepted)accept(pending);else decline(pending);}
            },message,detail,mandatory?"Proceed":"Yes",mandatory?"Disconnect":"No",0);
            mc.displayGuiScreen(dialog);
        }
        void decline(Entry entry) {
            entry.finished=true;status(entry,DECLINED);entries.remove(entry.id);
            if(entry.required) {
                net.minecraft.network.NetworkManager connection=user.getChannel().pipeline().get(net.minecraft.network.NetworkManager.class);
                if(connection!=null)connection.closeChannel(new net.minecraft.util.ChatComponentText("You must accept the server resource pack to join this server."));
                else user.getChannel().close();
            }
        }
        void accept(Entry entry) {
            entry.accepted=true;status(entry,ACCEPTED);Minecraft mc=Minecraft.getMinecraft();
            File directory=((ServerPackRepositoryAccess)mc.getResourcePackRepository()).viaForge$cache();
            Map<String,String> headers=new HashMap<>(Minecraft.getSessionInfo());
            String version=CompatibilityRegistry.forUser(user).resources().version();
            headers.put("X-Minecraft-Version",version);headers.put("X-Minecraft-Version-ID",version);headers.put("User-Agent","Minecraft Java/"+version);
            entry.worker=DOWNLOADS.submit(()->{
                try {
                    headers.put("X-Minecraft-Pack-Format",packFormat(version));
                    File file=ServerPackDownload.fetch(directory.toPath(),entry.url,entry.hash,mc.getProxy(),headers,250*1024*1024,false).toFile();
                    mc.addScheduledTask(()->{
                        if(!owns(entry))return;status(entry,DOWNLOADED);entry.finished=true;
                        boolean failedReload=false;
                        try{ServerPackDownload.validate(file.toPath());entry.file=file;}
                        catch(Exception invalid){status(entry,FAILED_RELOAD);entries.remove(entry.id);failedReload=true;}
                        reload(failedReload);
                    });
                }catch(Exception failure){mc.addScheduledTask(()->{
                    if(!owns(entry))return;entry.finished=true;status(entry,FAILED_DOWNLOAD);entries.remove(entry.id);reload();
                });}
            });
        }
        void pop(UUID id) {
            if(id==null){for(Entry e:entries.values())if(e.worker!=null)e.worker.cancel(true);entries.clear();}
            else {Entry entry=entries.remove(id);if(entry!=null&&entry.worker!=null)entry.worker.cancel(true);}
            reload();
        }
        void reload() { reload(false); }
        void reload(boolean restore) {
            if(closed||current!=this)return;
            for(Entry e:entries.values())if(e.accepted&&!e.finished)return;
            List<IResourcePack> packs=new ArrayList<>();List<Entry> next=new ArrayList<>();
            for(Entry e:entries.values())if(e.file!=null){packs.add(new FileResourcePack(e.file));next.add(e);}
            if(!restore&&installed.equals(next))return;
            try {
                install(packs.isEmpty()?null:new ServerPackStack(packs));installed=next;
                // Original ServerPackManager acknowledges every member of a completed
                // reload, including an already-active lower-priority pack after pop.
                for(Entry e:next){e.loaded=true;status(e,SUCCESSFULLY_LOADED);}
            }catch(RuntimeException failure){for(Entry e:next)if(!e.loaded){entries.remove(e.id);status(e,FAILED_RELOAD);}}
        }
        void close(){closed=true;for(Entry e:entries.values())if(e.worker!=null)e.worker.cancel(true);entries.clear();}
    }
    private static String packFormat(String version)throws java.io.IOException {
        try(java.io.InputStream stream=ModernServerPacks.class.getResourceAsStream("/assets/viaforge/block-versions.json")) {
            com.google.gson.JsonObject catalog=new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            com.google.gson.JsonObject entry=catalog.getAsJsonObject(version);
            String major=entry.get("pack_format").getAsString();
            return entry.has("pack_format_minor")?major+"."+entry.get("pack_format_minor").getAsString():major;
        }
    }
    private ModernServerPacks(){}
}
