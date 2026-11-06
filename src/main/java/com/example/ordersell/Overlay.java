package com.example.ordersell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Small animated status pill. Drawn on the HUD and on top of container screens. */
public final class Overlay {
    private Overlay() {}

    public static void render(DrawContext c) {
        Config cfg = Config.get();
        if (!cfg.overlayEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        Fx.advance();
        float tt = (float) Fx.t;
        float op = cfg.overlayOpacity;

        String title = "Order Sell";
        boolean on = OrderSellClient.isEnabled();
        String state = on ? OrderSellClient.getPhase() : "Paused";
        String stats = "sold " + OrderSellClient.getSold() + "  refresh " + OrderSellClient.getRefreshes()
                + "  last " + (OrderSellClient.getLastValue() < 0 ? "-" : OrderSellClient.fmt(OrderSellClient.getLastValue()));

        int textW = Math.max(tr.getWidth(title), Math.max(tr.getWidth(state), cfg.overlayStats ? tr.getWidth(stats) : 0));
        int w = textW + 34;
        int h = cfg.overlayStats ? 36 : 26;

        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int m = 6;
        int x = (cfg.overlayCorner == 0 || cfg.overlayCorner == 2) ? m : sw - w - m;
        int y = (cfg.overlayCorner == 0 || cfg.overlayCorner == 1) ? m : sh - h - m;

        int acc = Fx.accent(0);
        Fx.glowRect(c, x, y, x + w, y + h, acc, cfg.glow * op);
        c.fillGradient(x, y, x + w, y + h, Fx.a(0x2e1640, 0.88f * op), Fx.a(0x120a1e, 0.88f * op));
        if (cfg.overlayParticles) Fx.particles(c, x + 1, y + 1, x + w - 1, y + h - 1, 7, 1f, op, 3, 1, tt);
        Fx.animatedBorder(c, x, y, x + w, y + h, tt);

        // beating heart
        int hp = Math.sin(tt * 4.0) > 0.65 ? 3 : 2;
        Fx.heart(c, x + 14, y + h / 2, hp, on ? acc : 0x9a8aa8, on ? 1f : 0.7f);

        int tx = x + 28;
        Fx.waveText(c, tr, title, tx, y + 5, tt);
        int stateCol = on ? 0xFFB8FFD9 : 0xFFC9B8D6;
        c.drawText(tr, state, tx, y + 15, stateCol, true);
        if (cfg.overlayStats) c.drawText(tr, stats, tx, y + 25, 0xFFFFE3F4, false);
    }
}
