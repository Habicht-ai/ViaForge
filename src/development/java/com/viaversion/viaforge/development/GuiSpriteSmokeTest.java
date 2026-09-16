package com.viaversion.viaforge.development;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import static com.viaversion.viaforge.development.ServerEntitySmokeTest.require;

final class GuiSpriteSmokeTest {
    static void verify(Map<String,byte[]> original,Map<String,byte[]> normalized)throws Exception {
        // Coordinates used by the actual offhand HUD and combat indicator.
        Object[][] regions={{"widgets","hud/hotbar_offhand_left",24,22},{"widgets","hud/hotbar_offhand_right",53,22},
                {"icons","hud/crosshair_attack_indicator_background",36,94},{"icons","hud/crosshair_attack_indicator_progress",52,94},{"icons","hud/crosshair_attack_indicator_full",68,94}};
        for(Object[] region:regions) {
            BufferedImage sheet=ImageIO.read(new ByteArrayInputStream(normalized.get("textures/gui/"+region[0]+".png")));
            BufferedImage sprite=ImageIO.read(new ByteArrayInputStream(original.get("textures/gui/sprites/"+region[1]+".png")));
            for(int y=0;y<sprite.getHeight();y++)for(int x=0;x<sprite.getWidth();x++)
                require(sheet.getRGB((Integer)region[2]+x,(Integer)region[3]+y)==sprite.getRGB(x,y),"Original target HUD texel "+region[1]);
        }
    }
}
