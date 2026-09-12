package com.viaversion.viaforge.gui.account;

/** Small drawing helpers used by the ported account screens. */
final class RenderUtils {
    private RenderUtils() { }

    static int blend(int from, int to, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = from >>> shift & 255;
            int b = to >>> shift & 255;
            result |= Math.round(a + (b - a) * amount) << shift;
        }
        return result;
    }

    static void roundedRect(int x1, int y1, int x2, int y2, int radius, int color) {
        MenuRoundedRenderer.rect(x1, y1, x2 - x1, y2 - y1, radius, color);
    }
}
