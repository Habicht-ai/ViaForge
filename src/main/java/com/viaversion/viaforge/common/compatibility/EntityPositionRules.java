package com.viaversion.viaforge.common.compatibility;

/** Target client's relative-position codec, before 1.8's 1/32 conversion. */
public final class EntityPositionRules {
    public static double relative(double base,int delta,boolean keepZero,boolean round) {
        if(keepZero&&delta==0)return base;
        long encoded=round?Math.round(base*4096D):(long)Math.floor(base*4096D);
        return (encoded+delta)/4096D;
    }
    private EntityPositionRules(){}
}
