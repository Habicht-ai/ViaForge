package com.viaversion.viaforge.blocks.resources;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** 1.14 resource changes layered on the flattened importer. New villager biome
 * and hat geometry still require a separate native model implementation. */
public final class VillageResourceConverter {
    public static Map<String,byte[]> convert(Map<String,byte[]> original) {
        Map<String,byte[]> source=new HashMap<>(original);
        String[][] aliases={{"block/stone_slab_side","block/smooth_stone_slab_side"},{"block/stone_slab_top","block/smooth_stone"},
                {"item/cactus_green","item/green_dye"},{"item/dandelion_yellow","item/yellow_dye"},{"item/rose_red","item/red_dye"},{"item/sign","item/oak_sign"},
                {"entity/sign","entity/signs/oak"},{"entity/cow/mooshroom","entity/cow/red_mooshroom"}};
        for(String[] alias:aliases) {
            String target="textures/"+alias[1]+".png";
            if(!original.containsKey(target))throw new IllegalArgumentException("Missing 1.14 texture: "+target);
            source.put("textures/"+alias[0]+".png",original.get(target));
        }
        Map<String,byte[]> result=FlattenedResourceConverter.convert(source);
        ParticleAtlasConverter.from1_14(original,result);
        String[] professions={"farmer","librarian","cleric","weaponsmith","butcher","nitwit"};
        String[] old={"farmer","librarian","priest","smith","butcher","villager"};
        for(String entity:new String[]{"villager","zombie_villager"}) {
            for(int i=0;i<old.length;i++) {
                String path="textures/entity/"+entity+"/";
                BufferedImage base=ParticleAtlasConverter.read(original,path+entity+".png");
                BufferedImage image=new BufferedImage(base.getWidth(),base.getHeight(),BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=image.createGraphics();
                try {
                    graphics.drawImage(base,0,0,null);
                    graphics.drawImage(ParticleAtlasConverter.read(original,path+"type/plains.png"),0,0,null);
                    graphics.drawImage(ParticleAtlasConverter.read(original,path+"profession/"+professions[i]+".png"),0,0,null);
                }finally{graphics.dispose();}
                String file=entity.equals("zombie_villager")?(i==5?entity:"zombie_"+old[i]):old[i];
                result.put(path+file+".png",ParticleAtlasConverter.encode(image));
            }
        }
        return result;
    }
    private VillageResourceConverter(){}
}
