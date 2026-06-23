package com.palletable.color;

/**
 * Pure color math helpers. No Minecraft dependencies so this can be unit-tested in isolation.
 *
 * <p>Colors are passed around as packed ARGB ints ({@code 0xAARRGGBB}). Perceptual operations
 * (distance, gradients) are done in CIELAB space, which is far closer to human color perception
 * than raw RGB and gives much nicer gradients/sorting.
 */
public final class ColorUtil {
    private ColorUtil() {}

    // D65 reference white used for the XYZ -> Lab step.
    private static final double WHITE_X = 0.95047;
    private static final double WHITE_Y = 1.00000;
    private static final double WHITE_Z = 1.08883;

    public static int alpha(int argb) { return (argb >>> 24) & 0xFF; }
    public static int red(int argb)   { return (argb >> 16) & 0xFF; }
    public static int green(int argb) { return (argb >> 8) & 0xFF; }
    public static int blue(int argb)  { return argb & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgb(int r, int g, int b) {
        return argb(0xFF, r, g, b);
    }

    /** Multiply a base color by a tint color (per-channel, both 0-255). Used for grass/leaves tinting. */
    public static int multiply(int rgb, int tint) {
        int r = red(rgb) * red(tint) / 255;
        int g = green(rgb) * green(tint) / 255;
        int b = blue(rgb) * blue(tint) / 255;
        return rgb(r, g, b);
    }

    private static double srgbToLinear(int c) {
        double cs = c / 255.0;
        return cs <= 0.04045 ? cs / 12.92 : Math.pow((cs + 0.055) / 1.055, 2.4);
    }

    private static double labF(double t) {
        return t > 0.008856 ? Math.cbrt(t) : (7.787 * t + 16.0 / 116.0);
    }

    /** Convert an sRGB color (0-255 per channel) to CIELAB {L, a, b}. */
    public static float[] rgbToLab(int r, int g, int b) {
        double rl = srgbToLinear(r);
        double gl = srgbToLinear(g);
        double bl = srgbToLinear(b);

        double x = rl * 0.4124564 + gl * 0.3575761 + bl * 0.1804375;
        double y = rl * 0.2126729 + gl * 0.7151522 + bl * 0.0721750;
        double z = rl * 0.0193339 + gl * 0.1191920 + bl * 0.9503041;

        double fx = labF(x / WHITE_X);
        double fy = labF(y / WHITE_Y);
        double fz = labF(z / WHITE_Z);

        float l = (float) (116.0 * fy - 16.0);
        float a = (float) (500.0 * (fx - fy));
        float bb = (float) (200.0 * (fy - fz));
        return new float[]{l, a, bb};
    }

    public static float[] rgbToLab(int rgb) {
        return rgbToLab(red(rgb), green(rgb), blue(rgb));
    }

    /** Euclidean distance in Lab space (a.k.a. CIE76 deltaE). */
    public static double deltaE(float[] lab1, float[] lab2) {
        double dl = lab1[0] - lab2[0];
        double da = lab1[1] - lab2[1];
        double db = lab1[2] - lab2[2];
        return Math.sqrt(dl * dl + da * da + db * db);
    }

    /** Chroma (colorfulness) of a Lab color: distance of (a,b) from the neutral axis. */
    public static double chroma(float[] lab) {
        return Math.hypot(lab[1], lab[2]);
    }

    /** Hue angle in degrees [0,360) of a Lab color. */
    public static double labHue(float[] lab) {
        double deg = Math.toDegrees(Math.atan2(lab[2], lab[1]));
        return deg < 0 ? deg + 360.0 : deg;
    }

    /** Hue angle in degrees [0,360) from RGB, for coarse color-family categorization. */
    public static float hue(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        if (d < 1e-6f) return 0f;
        float h;
        if (max == rf)      h = ((gf - bf) / d) % 6f;
        else if (max == gf) h = (bf - rf) / d + 2f;
        else                h = (rf - gf) / d + 4f;
        h *= 60f;
        if (h < 0) h += 360f;
        return h;
    }

    /**
     * Monotonic sort key for spectrum ordering (neutrals by lightness, then hue wheel).
     * Lower values appear earlier; no block-name component is used.
     */
    public static double colorSortKey(int rgb) {
        int r = red(rgb);
        int g = green(rgb);
        int b = blue(rgb);
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float lightness = (max + min) / 2f;

        if (delta < 0.05f) {
            return lightness;
        }
        float hue = hue(r, g, b);
        float saturation = delta / (max < 1e-6f ? 1f : max);
        return 1000.0 + hue + saturation / 1000.0 + lightness / 1_000_000.0;
    }
}
