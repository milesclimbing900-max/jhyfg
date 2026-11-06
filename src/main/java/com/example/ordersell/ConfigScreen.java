package com.example.ordersell;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ConfigScreen extends Screen {
    private static final Identifier BG = Identifier.of("ordersell", "textures/gui/bg.png");
    private static final int TW = 425, TH = 600;

    private static final String[] TABS = {"Sell", "Colors", "Scene", "Sparkle", "HUD"};
    private static final String[] PMODES = {"Hearts", "Stars", "Sparkles", "Mixed", "Off"};
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};

    private static int tab = 0;
    private int px, py, ph, sx, sw, artX, artW, tabX0, tabW, contentY, rowH;
    private TextFieldWidget priceField;

    public ConfigScreen() {
        super(Text.literal("Order Sell"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    // ------------------------------------------------------------ layout
    @Override
    protected void init() {
        Config cfg = Config.get();
        ph = Math.min(height - 12, 236);
        artW = (cfg.showArt && width >= 330) ? (int) (ph * TW / (float) TH) : 0;
        int gap = artW > 0 ? 8 : 0;
        sw = Math.min(width - 12 - artW - gap, 250);
        int total = artW + gap + sw;
        px = (width - total) / 2;
        py = (height - ph) / 2;
        artX = px;
        sx = px + artW + gap;
        contentY = py + 44;
        int avail = ph - 44 - 30;
        rowH = Math.max(18, Math.min(22, avail / 6));

        // tabs
        tabX0 = sx + 6;
        tabW = (sw - 12) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            final String name = TABS[i];
            addDrawableChild(new CuteButton(tabX0 + i * tabW, py + 22, tabW - 2, 14, () -> name,
                    () -> tab == idx, b -> { tab = idx; clearAndInit(); }));
        }

        // footer
        int fw = (sw - 12 - 4) / 2;
        addDrawableChild(new CuteButton(sx + 6, py + ph - 24, fw, 18,
                () -> OrderSellClient.isEnabled() ? "Stop selling" : "Start selling",
                OrderSellClient::isEnabled,
                b -> OrderSellClient.setEnabled(!OrderSellClient.isEnabled())));
        addDrawableChild(new CuteButton(sx + 6 + fw + 4, py + ph - 24, fw, 18, () -> "Save & close",
                () -> false, b -> close()));

        switch (tab) {
            case 0 -> buildSell(cfg);
            case 1 -> buildColors(cfg);
            case 2 -> buildScene(cfg);
            case 3 -> buildSparkle(cfg);
            default -> buildHud(cfg);
        }
    }

    private int cx() { return sx + 6; }
    private int cw() { return sw - 12; }
    private int row(int i) { return contentY + i * rowH; }

    private void buildSell(Config cfg) {
        priceField = new TextFieldWidget(textRenderer, cx(), contentY + 10, cw(), 16, Text.literal("Minimum price"));
        priceField.setText(OrderSellClient.fmt(cfg.threshold));
        priceField.setChangedListener(s -> {
            double v = OrderSellClient.parseValue(s);
            if (v >= 0) cfg.threshold = v;
        });
        addDrawableChild(priceField);

        String[] presets = {"100m", "250m", "500m", "1b"};
        int bw = (cw() - 3 * 3) / 4;
        for (int i = 0; i < 4; i++) {
            final String p = presets[i];
            addDrawableChild(new CuteButton(cx() + i * (bw + 3), contentY + 30, bw, 14, () -> p, () -> false,
                    b -> priceField.setText(p)));
        }
        addDrawableChild(slider(contentY + 50, "Delay", 0, 40, cfg.delayTicks,
                v -> ((int) Math.round(v)) + " ticks", v -> cfg.delayTicks = (int) Math.round(v)));
        addDrawableChild(slider(contentY + 50 + rowH, "Green clicks", 1, 5, cfg.sellClicks,
                v -> String.valueOf((int) Math.round(v)), v -> cfg.sellClicks = (int) Math.round(v)));
    }

    private void buildColors(Config cfg) {
        addDrawableChild(button(row(0), () -> "Theme: " + Fx.THEMES[cfg.theme], () -> false,
                () -> cfg.theme = (cfg.theme + 1) % 6, false));
        addDrawableChild(slider(row(1), "Custom hue", 0, 1, cfg.customHue,
                v -> (int) (v * 360) + "\u00B0", v -> { cfg.customHue = (float) v; cfg.theme = 5; }));
        addDrawableChild(slider(row(2), "Saturation", 0, 1, cfg.saturation,
                v -> (int) (v * 100) + "%", v -> cfg.saturation = (float) v));
        addDrawableChild(button(row(3), () -> "Rainbow cycle: " + (cfg.rainbow ? "ON" : "OFF"), () -> cfg.rainbow,
                () -> cfg.rainbow = !cfg.rainbow, false));
        addDrawableChild(slider(row(4), "Animation speed", 0.1, 3, cfg.animSpeed,
                v -> String.format("%.1fx", v), v -> cfg.animSpeed = (float) v));
        addDrawableChild(slider(row(5), "Glow", 0, 1, cfg.glow,
                v -> (int) (v * 100) + "%", v -> cfg.glow = (float) v));
    }

    private void buildScene(Config cfg) {
        addDrawableChild(button(row(0), () -> "Background art: " + (cfg.showBackground ? "ON" : "OFF"), () -> cfg.showBackground,
                () -> cfg.showBackground = !cfg.showBackground, false));
        addDrawableChild(button(row(1), () -> "Portrait panel: " + (cfg.showArt ? "ON" : "OFF"), () -> cfg.showArt,
                () -> cfg.showArt = !cfg.showArt, true));
        addDrawableChild(slider(row(2), "Background dim", 0, 0.9, cfg.bgDim,
                v -> (int) (v * 100) + "%", v -> cfg.bgDim = (float) v));
        addDrawableChild(slider(row(3), "Camera motion", 0, 1, cfg.bgMotion,
                v -> (int) (v * 100) + "%", v -> cfg.bgMotion = (float) v));
        addDrawableChild(slider(row(4), "Panel opacity", 0.1, 1, cfg.panelOpacity,
                v -> (int) (v * 100) + "%", v -> cfg.panelOpacity = (float) v));
    }

    private void buildSparkle(Config cfg) {
        addDrawableChild(button(row(0), () -> "Style: " + PMODES[cfg.particleMode], () -> false,
                () -> cfg.particleMode = (cfg.particleMode + 1) % PMODES.length, false));
        addDrawableChild(slider(row(1), "Amount", 0, 200, cfg.particleCount,
                v -> String.valueOf((int) Math.round(v)), v -> cfg.particleCount = (int) Math.round(v)));
        addDrawableChild(slider(row(2), "Speed", 0.1, 3, cfg.particleSpeed,
                v -> String.format("%.1fx", v), v -> cfg.particleSpeed = (float) v));
        addDrawableChild(slider(row(3), "Opacity", 0.1, 1, cfg.particleOpacity,
                v -> (int) (v * 100) + "%", v -> cfg.particleOpacity = (float) v));
    }

    private void buildHud(Config cfg) {
        addDrawableChild(button(row(0), () -> "HUD overlay: " + (cfg.overlayEnabled ? "ON" : "OFF"), () -> cfg.overlayEnabled,
                () -> cfg.overlayEnabled = !cfg.overlayEnabled, false));
        addDrawableChild(button(row(1), () -> "Corner: " + CORNERS[cfg.overlayCorner], () -> false,
                () -> cfg.overlayCorner = (cfg.overlayCorner + 1) % 4, false));
        addDrawableChild(slider(row(2), "Opacity", 0.1, 1, cfg.overlayOpacity,
                v -> (int) (v * 100) + "%", v -> cfg.overlayOpacity = (float) v));
        addDrawableChild(button(row(3), () -> "Stats line: " + (cfg.overlayStats ? "ON" : "OFF"), () -> cfg.overlayStats,
                () -> cfg.overlayStats = !cfg.overlayStats, false));
        addDrawableChild(button(row(4), () -> "Mini sparkles: " + (cfg.overlayParticles ? "ON" : "OFF"), () -> cfg.overlayParticles,
                () -> cfg.overlayParticles = !cfg.overlayParticles, false));
    }

    private CuteButton button(int y, Supplier<String> label, BooleanSupplier lit, Runnable action, boolean relayout) {
        return new CuteButton(cx(), y, cw(), 16, label, lit, b -> {
            action.run();
            if (relayout) clearAndInit();
        });
    }

    private CuteSlider slider(int y, String label, double min, double max, double cur,
                              DoubleFunction<String> fmt, DoubleConsumer onChange) {
        return new CuteSlider(cx(), y, cw(), 16, label, min, max, cur, fmt, onChange);
    }

    // ------------------------------------------------------------ drawing
    private void drawImage(DrawContext c, int x, int y, int w, int h, float zoom, float fx, float fy) {
        float aspect = w / (float) h, texAspect = TW / (float) TH;
        float rw, rh;
        if (aspect < texAspect) { rh = TH / zoom; rw = rh * aspect; }
        else { rw = TW / zoom; rh = rw / aspect; }
        float u = Fx.clamp(fx * TW - rw / 2f, 0, TW - rw);
        float v = Fx.clamp(fy * TH - rh / 2f, 0, TH - rh);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, BG, x, y, u, v, w, h,
                Math.round(rw), Math.round(rh), TW, TH);
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        Fx.advance();
        Config cfg = Config.get();
        float tt = (float) Fx.t;
        int acc = Fx.accent(0);

        // base animated gradient
        int top = Fx.mix(0x1a0b26, Fx.accent(0), 0.12f);
        int bot = Fx.mix(0x0b0614, Fx.accent(3), 0.10f);
        c.fillGradient(0, 0, width, height, 0xFF000000 | top, 0xFF000000 | bot);

        // full-screen art with slow camera drift
        if (cfg.showBackground) {
            float m = cfg.bgMotion;
            float zoom = 1.12f + 0.18f * m * (0.5f + 0.5f * (float) Math.sin(tt * 0.13));
            float fx = 0.5f + 0.06f * m * (float) Math.sin(tt * 0.09);
            float fy = 0.38f + 0.20f * m * (float) Math.sin(tt * 0.11);
            drawImage(c, 0, 0, width, height, zoom, fx, fy);
        }
        // dim + tint
        c.fill(0, 0, width, height, Fx.a(0x120818, cfg.bgDim));
        c.fillGradient(0, 0, width, height, Fx.a(acc, 0.10f), Fx.a(Fx.accent(4), 0.16f));

        // aurora bands
        for (int k = 0; k < 3; k++) {
            int bandY = (int) (height * (0.2f + 0.3f * k) + Math.sin(tt * 0.3 + k * 2.0) * height * 0.08);
            int col = Fx.accent(k * 2f);
            c.fillGradient(0, bandY - 40, width, bandY, Fx.a(col, 0f), Fx.a(col, 0.10f));
            c.fillGradient(0, bandY, width, bandY + 40, Fx.a(col, 0.10f), Fx.a(col, 0f));
        }
        // vignette
        c.fillGradient(0, 0, width, height / 4, Fx.a(0x000000, 0.55f), Fx.a(0x000000, 0f));
        c.fillGradient(0, height * 3 / 4, width, height, Fx.a(0x000000, 0f), Fx.a(0x000000, 0.6f));
        // shimmer sweep
        int sy = (int) ((tt * 0.05 % 1.0) * (height + 80)) - 40;
        c.fillGradient(0, sy, width, sy + 20, Fx.a(0xFFFFFF, 0f), Fx.a(0xFFFFFF, 0.06f));
        c.fillGradient(0, sy + 20, width, sy + 40, Fx.a(0xFFFFFF, 0.06f), Fx.a(0xFFFFFF, 0f));

        // background particles
        Fx.particles(c, 0, 0, width, height, cfg.particleCount, cfg.particleSpeed, cfg.particleOpacity,
                cfg.particleMode, 3, tt);

        // ---------------- portrait panel ----------------
        if (artW > 0) {
            Fx.glowRect(c, artX, py, artX + artW, py + ph, acc, cfg.glow);
            float z = 1f + 0.05f * cfg.bgMotion * (0.5f + 0.5f * (float) Math.sin(tt * 0.7));
            drawImage(c, artX, py, artW, ph, z, 0.5f + 0.02f * (float) Math.sin(tt * 0.4),
                    0.5f + 0.02f * (float) Math.cos(tt * 0.3));
            c.fillGradient(artX, py + ph - 52, artX + artW, py + ph, Fx.a(0x14081c, 0f), Fx.a(0x14081c, 0.85f));
            Fx.animatedBorder(c, artX, py, artX + artW, py + ph, tt);
            Fx.border(c, artX + 3, py + 3, artX + artW - 3, py + ph - 3, Fx.a(0xFFFFFF, 0.25f));
            // twinkling corner stars
            float tw = 0.5f + 0.5f * (float) Math.sin(tt * 2.2);
            Fx.sprite(c, Fx.SPARK, artX - 2, py - 2, 2, Fx.a(0xFFFFFF, 0.4f + 0.6f * tw));
            Fx.sprite(c, Fx.SPARK, artX + artW - 8, py + ph - 8, 2, Fx.a(0xFFFFFF, 1f - 0.6f * tw));
            Fx.particles(c, artX, py, artX + artW, py + ph, Math.min(cfg.particleCount / 3, 30),
                    cfg.particleSpeed, cfg.particleOpacity, cfg.particleMode, 2, tt + 40f);
            // caption
            boolean on = OrderSellClient.isEnabled();
            String cap = on ? "selling..." : "ready when you are";
            int capW = textRenderer.getWidth(cap);
            c.drawText(textRenderer, cap, artX + (artW - capW) / 2, py + ph - 14,
                    on ? 0xFFB8FFD9 : 0xFFFFE3F4, true);
            String name = "Order Sell";
            Fx.waveText(c, textRenderer, name, artX + (artW - textRenderer.getWidth(name)) / 2, py + ph - 28, tt);
        }

        // ---------------- settings panel ----------------
        Fx.glowRect(c, sx, py, sx + sw, py + ph, acc, cfg.glow);
        c.fillGradient(sx, py, sx + sw, py + ph, Fx.a(0x2e1640, cfg.panelOpacity), Fx.a(0x120a1e, cfg.panelOpacity));
        Fx.particles(c, sx, py, sx + sw, py + ph, Math.min(cfg.particleCount / 4, 20),
                cfg.particleSpeed, cfg.particleOpacity * 0.6f, cfg.particleMode, 1, tt + 90f);
        Fx.animatedBorder(c, sx, py, sx + sw, py + ph, tt + 3f);

        // title
        String title = "Order Sell";
        int tw2 = textRenderer.getWidth(title);
        int titleX = sx + (sw - tw2) / 2;
        Fx.waveText(c, textRenderer, title, titleX, py + 8, tt);
        int hb = Math.sin(tt * 4.0) > 0.65 ? 2 : 1;
        Fx.heart(c, titleX - 10, py + 12, hb, acc, 1f);
        Fx.heart(c, titleX + tw2 + 10, py + 12, hb, Fx.accent(2), 1f);

        if (tab == 0) {
            c.drawText(textRenderer, "Minimum price to sell (600m, 1.5b ...)", cx(), contentY, 0xFFFFE3F4, true);
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        // tab underline
        int x = tabX0 + tab * tabW;
        c.fill(x, py + 37, x + tabW - 2, py + 39, 0xFF000000 | Fx.accent(1));
    }

    // ------------------------------------------------------------ widgets
    static class CuteButton extends ButtonWidget {
        private final Supplier<String> label;
        private final BooleanSupplier lit;

        CuteButton(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier lit, PressAction onPress) {
            super(x, y, w, h, net.minecraft.text.Text.literal(label.get()), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
            this.lit = lit;
        }

        @Override
        public void drawIcon(DrawContext c, int mx, int my, float delta) {
            Config cfg = Config.get();
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = lit.getAsBoolean();
            boolean hov = isHovered();
            int acc = Fx.accent(0);
            if (hov) Fx.glowRect(c, x, y, x + w, y + h, acc, cfg.glow * 0.8f);
            int topC = on ? Fx.mix(acc, 0xFFFFFF, 0.25f) : Fx.mix(0x3a2150, acc, hov ? 0.35f : 0.12f);
            int botC = on ? acc : Fx.mix(0x24132f, acc, hov ? 0.25f : 0.05f);
            c.fillGradient(x, y, x + w, y + h, 0xFF000000 | topC, 0xFF000000 | botC);
            Fx.border(c, x, y, x + w, y + h, hov ? 0xFFFFFFFF : Fx.a(acc, 0.8f));
            String s = label.get();
            var tr = Fx.tr();
            int col = on ? 0xFF2A1030 : 0xFFFFE9F7;
            c.drawText(tr, s, x + (w - tr.getWidth(s)) / 2, y + (h - 8) / 2 + 1, col, !on);
        }
    }

    static class CuteSlider extends SliderWidget {
        private final String label;
        private final double min, max;
        private final DoubleConsumer onChange;
        private final DoubleFunction<String> fmt;

        CuteSlider(int x, int y, int w, int h, String label, double min, double max, double cur,
                   DoubleFunction<String> fmt, DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.fmt = fmt;
            this.onChange = onChange;
            updateMessage();
        }

        private double val() { return min + (max - min) * value; }

        @Override
        protected void updateMessage() {
            if (fmt == null) return;
            setMessage(Text.literal(label + ": " + fmt.apply(val())));
        }

        @Override
        protected void applyValue() { onChange.accept(val()); }

        @Override
        public void renderWidget(DrawContext c, int mx, int my, float delta) {
            Config cfg = Config.get();
            float tt = (float) Fx.t;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hov = isHovered() || isFocused();
            int acc = Fx.accent(0);
            if (hov) Fx.glowRect(c, x, y, x + w, y + h, acc, cfg.glow * 0.6f);
            c.fillGradient(x, y, x + w, y + h, 0xFF2A1838, 0xFF160C20);
            int fillW = (int) (value * (w - 2));
            if (fillW > 0) {
                c.fillGradient(x + 1, y + 1, x + 1 + fillW, y + h - 1,
                        Fx.a(Fx.mix(acc, 0xFFFFFF, 0.2f), 0.85f), Fx.a(Fx.accent(2), 0.85f));
                int sh = x + 1 + (int) (((tt * 0.4f) % 1f) * fillW);
                c.fill(sh, y + 1, Math.min(sh + 6, x + 1 + fillW), y + h - 1, Fx.a(0xFFFFFF, 0.25f));
                c.fill(x + fillW - 1, y, x + fillW + 3, y + h, 0xFFFFFFFF); // knob
            }
            Fx.border(c, x, y, x + w, y + h, hov ? 0xFFFFFFFF : Fx.a(acc, 0.8f));
            String s = getMessage().getString();
            var tr = Fx.tr();
            c.drawText(tr, s, x + (w - tr.getWidth(s)) / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF, true);
        }
    }
}
