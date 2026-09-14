package com.viaversion.viaforge.hands;
import com.google.gson.*;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.mixin.impl.hands.ItemShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.*;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import java.io.*;
import java.util.*;

/** Selects the target JSON's separate left/right display transforms. */
public final class HandModels {
    public static ItemStack current;
    public static boolean left;
    private static final Map<String,JsonObject> CACHE=new HashMap<>();
    private static String version;
    public static void apply(IBakedModel model,ItemCameraTransforms.TransformType type) {
        Minecraft mc=Minecraft.getMinecraft();String now=ServerSession.getLoadedResourceVersion();
        if(!Objects.equals(now,version)){CACHE.clear();version=now;}
        ItemShapes shapes=(ItemShapes)mc.getRenderItem().getItemModelMesher();
        int meta=current.isItemStackDamageable()?0:current.getMetadata();
        ModelResourceLocation location=shapes.viaForge$shapes().get(Item.getIdFromItem(current.getItem())<<16|meta);
        ItemMeshDefinition mesh=shapes.viaForge$meshes().get(current.getItem());if(mesh!=null)location=mesh.getModelLocation(current);
        String name=location==null?Item.itemRegistry.getNameForObject(current.getItem()).getResourcePath():location.getResourcePath();
        if(current.getItem()==net.minecraft.init.Items.bow)name="bow";
        if(name.endsWith("_left"))name=name.substring(0,name.length()-5);
        String path="models/item/"+name+".json";
        JsonObject display=CACHE.get(path);
        if(display==null){
            try(InputStream in=mc.getResourceManager().getResource(new ResourceLocation("viaforge",path)).getInputStream()) {
                JsonObject json=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                display=json.has("display")?json.getAsJsonObject("display"):new JsonObject();
            }catch(IOException e){display=new JsonObject();}
            CACHE.put(path,display);
        }
        String prefix=type==ItemCameraTransforms.TransformType.THIRD_PERSON?"thirdperson":"firstperson";
        String key=prefix+(left?"_lefthand":"_righthand");
        if(!display.has(key))key=prefix+"_righthand";
        if(!display.has(key))key=prefix;
        if(!display.has(key)){model.getItemCameraTransforms().applyTransform(type);return;}
        JsonObject transform=display.getAsJsonObject(key);float sign=left?-1:1;
        float[] position=vector(transform,"translation",0),rotation=vector(transform,"rotation",0),scale=vector(transform,"scale",1);
        GlStateManager.translate(sign*position[0]/16,position[1]/16,position[2]/16);
        GlStateManager.rotate(sign*rotation[1],0,1,0);GlStateManager.rotate(rotation[0],1,0,0);GlStateManager.rotate(sign*rotation[2],0,0,1);
        GlStateManager.scale(scale[0],scale[1],scale[2]);
    }
    private static float[] vector(JsonObject t,String key,float value){if(!t.has(key))return new float[]{value,value,value};JsonArray a=t.getAsJsonArray(key);return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private HandModels(){ }
}
