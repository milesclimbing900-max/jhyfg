package com.example.ordersell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Shared animation + drawing helpers (pixel sprites, glow, particles, colors). */
public final class Fx {
    private Fx() {}

    private static long last = System.nanoTime();
    public static double t = 0;

    /** Call at the start of every render pass; advances the shared animation clock. */
    public static void advance() {
        long now = System.nanoTime();
        double dt = (now - last) / 1e9;
        last = now;
        if (dt > 0.25) dt = 0.25;
        t += dt * Config.get().animSpeed;
    }

    // ---------- colors ----------
    public static final String[] THEMES = {"Bubblegum", "Lavender", "Mint", "Sky", "Peach", "Custom"};
    private static final float[] TH = {0.93f, 0.74f, 0.42f, 0.57f, 0.04f};
    private static final float[] TS = {0.45f, 0.40f, 0.40f, 0.45f, 0.45f};

    /** Pastel accent color (0xRRGGBB). `offset` shifts hue a little so gradients look dreamy. */
    public static int accent(float offset) {
        Config c = Config.get();
        float h, s;
        if (c.rainbow) {
            h = (float) (t * 0.06) + offset * 0.15f;
            s = Math.max(0.35f, c.saturation);
        } else if (c.theme < 5) {
            h = TH[c.theme] + offset * 0.06f;
            s = TS[c.theme];
        } else {
            h = c.customHue + offset * 0.06f;
            s = c.saturation;
        }
        return hsv(h, s, 1f);
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        h *= 6f;
        int i = (int) h;
        float f = h - i;
        float p = v * (1 - s), q = v * (1 - s * f), u = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = u; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = u; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = u; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /** rgb + alpha(0..1) -> ARGB */
    public static int a(int rgb, float alpha) {
        int al = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float f) {
        int r = (int) (((c1 >> 16) & 255) * (1 - f) + ((c2 >> 16) & 255) * f);
        int g = (int) (((c1 >> 8) & 255) * (1 - f) + ((c2 >> 8) & 255) * f);
        int b = (int) ((c1 & 255) * (1 - f) + (c2 & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double frac(double d) { return d - Math.floor(d); }
    private static double rnd(int i, int k) { return frac(Math.sin(i * 12.9898 + k * 78.233) * 43758.5453); }

    // ---------- pixel sprites ----------
    public static final String[] HEART = {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};
    public static final String[] STAR = {"...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."};
    public static final String[] SPARK = {"..#..", ".###.", "#####", ".###.", "..#.."};

    public static void sprite(DrawContext c, String[] s, int x, int y, int px, int color) {
        for (int r = 0; r < s.length; r++) {
            String row = s[r];
            int col = 0;
            while (col < row.length()) {
                if (row.charAt(col) == '#') {
                    int start = col;
                    while (col < row.length() && row.charAt(col) == '#') col++;
                    c.fill(x + start * px, y + r * px, x + col * px, y + (r + 1) * px, color);
                } else col++;
            }
        }
    }

    public static void heart(DrawContext c, int cx, int cy, int px, int rgb, float alpha) {
        int x = cx - 7 * px / 2, y = cy - 6 * px / 2;
        sprite(c, HEART, x, y, px, a(rgb, alpha));
        c.fill(x + px, y + px, x + 2 * px, y + 2 * px, a(0xFFFFFF, alpha * 0.9f)); // shine
    }

    // ---------- glow / borders ----------
    public static void glowRect(DrawContext c, int x1, int y1, int x2, int y2, int rgb, float strength) {
        if (strength <= 0.01f) return;
        for (int i = 1; i <= 7; i++) {
            c.fill(x1 - i, y1 - i, x2 + i, y2 + i, a(rgb, strength * 0.20f * (1f - i / 8f)));
        }
    }

    public static void border(DrawContext c, int x1, int y1, int x2, int y2, int argb) {
        c.fill(x1, y1, x2, y1 + 1, argb);
        c.fill(x1, y2 - 1, x2, y2, argb);
        c.fill(x1, y1, x1 + 1, y2, argb);
        c.fill(x2 - 1, y1, x2, y2, argb);
    }

    /** Animated border with light streaks running along the top and bottom edges. */
    public static void animatedBorder(DrawContext c, int x1, int y1, int x2, int y2, float tt) {
        border(c, x1, y1, x2, y2, a(accent(0), 0.9f));
        int w = x2 - x1;
        int sx = x1 + (int) (frac(tt * 0.15) * w);
        c.fill(sx, y1, Math.min(sx + 28, x2), y1 + 1, a(0xFFFFFF, 0.95f));
        int bx = x2 - (int) (frac(tt * 0.12) * w);
        c.fill(Math.max(bx - 28, x1), y2 - 1, bx, y2, a(0xFFFFFF, 0.95f));
    }

    // ---------- text ----------
    public static void waveText(DrawContext c, TextRenderer tr, String s, int x, int y, float tt) {
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            int yy = y + (int) Math.round(Math.sin(tt * 3.0 + i * 0.6) * 1.5);
            c.drawText(tr, ch, x, yy, 0xFF000000 | accent(i * 0.7f), true);
            x += tr.getWidth(ch);
        }
    }

    public static int waveWidth(TextRenderer tr, String s) { return tr.getWidth(s); }

    // ---------- particles ----------
    /**
     * Deterministic particle field (no state). mode: 0 hearts rising, 1 twinkling stars,
     * 2 sparkles falling, 3 mixed. maxPx limits sprite scale.
     */
    public static void particles(DrawContext c, int x1, int y1, int x2, int y2,
                                 int count, float speed, float opacity, int mode, int maxPx, float tt) {
        if (mode >= 4 || count <= 0) return;
        float w = x2 - x1, h = y2 - y1;
        for (int i = 0; i < count; i++) {
            double rx = rnd(i, 1), ry = rnd(i, 2), rs = rnd(i, 3), rp = rnd(i, 4), rz = rnd(i, 5);
            int type = mode < 3 ? mode : i % 3;
            double px, py;
            if (type == 0) {
                py = 1.0 - frac(ry + tt * (0.03 + 0.05 * rs) * speed);
                px = rx + 0.025 * Math.sin(tt * (0.6 + rs) + rp * 6.283);
            } else if (type == 1) {
                py = ry + 0.015 * Math.sin(tt * 0.5 + rp * 6.283);
                px = rx + 0.015 * Math.cos(tt * 0.4 + rp * 6.283);
            } else {
                py = frac(ry + tt * (0.02 + 0.03 * rs) * speed);
                px = frac(rx - py * 0.10);
            }
            double tw = Math.abs(Math.sin(tt * (1.2 + rs * 2.0) + rp * 6.283));
            float alpha = (float) (0.35 + 0.65 * tw) * opacity;
            alpha *= (float) Math.min(1.0, Math.min(py, 1.0 - py) * 6.0);
            if (alpha < 0.04f) continue;
            int size = rz < 0.55 ? 1 : (rz < 0.88 ? 2 : 3);
            if (size > maxPx) size = maxPx;
            int sx = x1 + (int) (px * w), sy = y1 + (int) (py * h);
            if (type == 0) {
                heart(c, sx, sy, Math.max(1, size), accent((float) rp * 4f), alpha);
            } else if (type == 1) {
                int col = mix(accent((float) rp * 3f), 0xFFFFFF, 0.55f);
                sprite(c, STAR, sx - 7 * size / 2, sy - 7 * size / 2, size, a(col, alpha));
            } else {
                int col = mix(accent((float) rp * 3f), 0xFFFFFF, 0.75f);
                sprite(c, SPARK, sx - 5 * size / 2, sy - 5 * size / 2, size, a(col, alpha));
            }
        }
    }

    public static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }
}
