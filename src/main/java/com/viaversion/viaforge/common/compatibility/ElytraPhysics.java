package com.viaversion.viaforge.common.compatibility;

/** Vanilla fall-flight integration, in blocks per tick. No creative-flight abilities. */
public final class ElytraPhysics {
    public static double[] step(double x, double y, double z, double lookX, double lookY, double lookZ, float pitch) {
        double horizontalLook = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double speed = Math.sqrt(x * x + z * z);
        double lookLength = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double radians = pitch * Math.PI / 180;
        double lift = Math.cos(radians);
        lift = lift * lift * Math.min(1, lookLength / .4);
        y += -.08 + lift * .06;
        if (y < 0 && horizontalLook > 0) {
            double descent = y * -.1 * lift;
            y += descent; x += lookX * descent / horizontalLook; z += lookZ * descent / horizontalLook;
        }
        if (radians < 0 && horizontalLook > 0) {
            double climb = speed * -Math.sin(radians) * .04;
            y += climb * 3.2; x -= lookX * climb / horizontalLook; z -= lookZ * climb / horizontalLook;
        }
        if (horizontalLook > 0) {
            x += (lookX / horizontalLook * speed - x) * .1;
            z += (lookZ / horizontalLook * speed - z) * .1;
        }
        return new double[]{x * .99, y * .98, z * .99};
    }
    public static double boost(double velocity, double look) { return velocity + look * .1 + (look * 1.5 - velocity) * .5; }
    public static boolean usable(int damage, int maximum) { return maximum > 1 && damage < maximum - 1; }
    private ElytraPhysics() { }
}
