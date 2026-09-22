package com.viaversion.viaforge.compatibility;
import com.viaversion.viaforge.common.compatibility.EntityPushRules.TeamRule;
/** Attached to the real scoreboard team, so removal and reconnect discard its rule. */
public interface CollisionTeam {
    TeamRule viaForge$collisionRule();
    void viaForge$collisionRule(TeamRule rule);
}
