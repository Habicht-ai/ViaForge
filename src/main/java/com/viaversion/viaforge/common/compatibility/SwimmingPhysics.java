package com.viaversion.viaforge.common.compatibility;

/** Vanilla constants and arithmetic; no anticheat-dependent tolerances. */
public final class SwimmingPhysics {
    public static double lookY(double velocity,double look,boolean jump,boolean submergedAbove) {
        return look<=0||jump||submergedAbove?velocity+(look-velocity)*(look<-.2?.085:.06):velocity;
    }
    public static float[] waterFactors(boolean sprint,float depth,boolean ground,float movementSpeed,boolean dolphin) {
        float drag=sprint?.9F:.8F,speed=.02F;
        depth=Math.min(3,depth);if(!ground)depth*=.5F;
        if(depth>0){drag+=(.54600006F-drag)*depth/3F;speed+=(movementSpeed-speed)*depth/3F;}
        return new float[]{dolphin?.96F:drag,speed};
    }
    public static double falling(double velocity,double gravity,boolean falling,boolean sprint) {
        if(gravity==0||sprint)return velocity;
        return falling&&Math.abs(velocity-.005)>=.003&&Math.abs(velocity-gravity/16)<.003?-.003:velocity-gravity/16;
    }
    /** KeyboardInput + LocalPlayer square movement introduced in 1.21.5. */
    public static float[] squareInput(float strafe,float forward,float itemFactor,float sneakFactor) {
        float length=(float)Math.sqrt(strafe*strafe+forward*forward);
        if(length<1.0E-4F)return new float[]{0,0};
        strafe=strafe/length*.98F*itemFactor*sneakFactor;forward=forward/length*.98F*itemFactor*sneakFactor;
        length=(float)Math.sqrt(strafe*strafe+forward*forward);
        // Vanilla returns the scaled zero vector before normalizing it. A valid
        // synchronized sneaking_speed of zero must not produce NaN movement.
        if(length<=0)return new float[]{strafe,forward};
        float x=strafe*(1/length),z=forward*(1/length),ax=Math.abs(x),az=Math.abs(z);
        float tangent=az>ax?ax/az:az/ax;
        float scaled=Math.min(length*(float)Math.sqrt(1+tangent*tangent),1);
        return new float[]{x*scaled,z*scaled};
    }
    private SwimmingPhysics(){}
}
