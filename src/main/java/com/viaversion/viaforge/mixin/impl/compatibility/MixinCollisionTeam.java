package com.viaversion.viaforge.mixin.impl.compatibility;
import com.viaversion.viaforge.compatibility.CollisionTeam;
import com.viaversion.viaforge.common.compatibility.EntityPushRules.TeamRule;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ScorePlayerTeam.class)
public abstract class MixinCollisionTeam implements CollisionTeam {
    @Unique private TeamRule viaForge$collisionRule=TeamRule.ALWAYS;
    public TeamRule viaForge$collisionRule(){return viaForge$collisionRule;}
    public void viaForge$collisionRule(TeamRule rule){viaForge$collisionRule=rule;}
}
