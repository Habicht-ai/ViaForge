package com.viaversion.viaforge.common.compatibility;

/** Original lookup arithmetic, including the 1.21.11 double-index boundary. */
public final class OriginalLookMath {
    private static final float[] LEGACY=new float[65536],MODERN=new float[65536];
    static {
        for(int i=0;i<65536;i++) {
            LEGACY[i]=(float)Math.sin((double)i*Math.PI*2/65536);
            MODERN[i]=(float)Math.sin(i/10430.378350470453);
        }
    }
    public static float sin(float angle,boolean modern) {
        return modern?MODERN[(int)((long)(angle*10430.378350470453)&65535L)]:LEGACY[(int)(angle*10430.378F)&65535];
    }
    public static float cos(float angle,boolean modern) {
        return modern?MODERN[(int)((long)(angle*10430.378350470453+16384D)&65535L)]:LEGACY[(int)(angle*10430.378F+16384F)&65535];
    }
    public static double[] look(float yaw,float pitch,VersionRules rules) {
        boolean modern=rules.enabled(ClientRule.DOUBLE_TRIG_LOOKUP);
        float p=pitch*((float)Math.PI/180F),y=-yaw*((float)Math.PI/180F);
        if(!rules.enabled(ClientRule.MODERN_LOOK_VECTOR)) {
            y-=(float)Math.PI;p=-p;float c=-cos(p,modern);
            return new double[]{sin(y,modern)*c,sin(p,modern),cos(y,modern)*c};
        }
        float c=cos(p,modern);
        return new double[]{sin(y,modern)*c,-sin(p,modern),cos(y,modern)*c};
    }
    private OriginalLookMath(){}
}
