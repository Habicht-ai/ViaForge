package com.viaversion.viaforge.common.compatibility;

/** Vanilla fall-flight integration, in blocks per tick. No creative-flight abilities. */
public final class ElytraPhysics {
    public static double[] step(double x, double y, double z, double lookX, double lookY, double lookZ, float pitch) {
        return step(CompatibilityRegistry.DEFAULT.resolve(107).rules(),x,y,z,lookX,lookY,lookZ,pitch,.08);
    }
    public static double[] step(VersionRules rules,double x,double y,double z,double lookX,double lookY,double lookZ,float pitch,double gravity) {
        double horizontalLook = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double speed = Math.sqrt(x * x + z * z);
        double lookLength = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        float radians = pitch * ((float)Math.PI / 180F);
        boolean precise=rules.enabled(ClientRule.DOUBLE_TRIG_LOOKUP);
        double lift=rules.enabled(ClientRule.JAVA_ELYTRA_LIFT)?Math.cos(radians):OriginalLookMath.cos(radians,precise);
        lift=rules.enabled(ClientRule.JAVA_ELYTRA_LIFT)?lift*lift*Math.min(1,lookLength/.4):(float)(lift*(lift*Math.min(1,lookLength/.4)));
        y += rules.enabled(ClientRule.MODERN_LOOK_VECTOR)?gravity*(-1+lift*.75):-.08+lift*.06;
        if (y < 0 && horizontalLook > 0) {
            double descent = y * -.1 * lift;
            y += descent; x += lookX * descent / horizontalLook; z += lookZ * descent / horizontalLook;
        }
        if (radians < 0 && horizontalLook > 0) {
            double climb = speed * -OriginalLookMath.sin(radians,precise) * .04;
            y += climb * 3.2; x -= lookX * climb / horizontalLook; z -= lookZ * climb / horizontalLook;
        }
        if (horizontalLook > 0) {
            x += (lookX / horizontalLook * speed - x) * .1;
            z += (lookZ / horizontalLook * speed - z) * .1;
        }
        return new double[]{x * (double).99F, y * (double).98F, z * (double).99F};
    }
    public static double boost(double velocity, double look) { return velocity + look * .1 + (look * 1.5 - velocity) * .5; }
    public static boolean usable(int damage, int maximum) { return maximum > 1 && damage < maximum - 1; }
    private ElytraPhysics() { }
}
