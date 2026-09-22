package com.viaversion.viaforge.common.compatibility;

/** Original proximity impulse and scoreboard filter; independent of the native world. */
public final class EntityPushRules {
    public enum TeamRule {
        ALWAYS("always"), NEVER("never"), PUSH_OTHER_TEAMS("pushOtherTeams"), PUSH_OWN_TEAM("pushOwnTeam");
        public final String wire;
        TeamRule(String wire){this.wire=wire;}
        public static TeamRule read(String name) {
            for(TeamRule rule:values())if(rule.wire.equals(name))return rule;
            throw new IllegalArgumentException("Unknown team collision rule "+name);
        }
    }
    public static boolean collides(TeamRule own,TeamRule other,boolean sameTeam) {
        if(own==TeamRule.NEVER||other==TeamRule.NEVER)return false;
        if((own==TeamRule.PUSH_OWN_TEAM||other==TeamRule.PUSH_OWN_TEAM)&&sameTeam)return false;
        return own!=TeamRule.PUSH_OTHER_TEAMS&&other!=TeamRule.PUSH_OTHER_TEAMS||sameTeam;
    }
    public static double[] impulse(double x,double z,float reduction,boolean precise) {
        double distance=Math.max(Math.abs(x),Math.abs(z));
        if(distance<(double).01F)return null;
        distance=precise?Math.sqrt(distance):(double)(float)Math.sqrt(distance);
        x/=distance;z/=distance;
        double inverse=1.0/distance;if(inverse>1.0)inverse=1.0;
        x*=inverse;z*=inverse;x*=(double).05F;z*=(double).05F;
        if(!precise){x*=(double)(1F-reduction);z*=(double)(1F-reduction);}
        return new double[]{x,z};
    }
    private EntityPushRules(){}
}
