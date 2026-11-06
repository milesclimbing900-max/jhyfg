package com.example.ordersell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Everything is saved to config/ordersell.json. */
public class Config {
    // ---- auto sell ----
    public double threshold = 600_000_000;
    public int delayTicks = 4;
    public int sellClicks = 2;

    // ---- colors ----
    public int theme = 0;            // 0-4 presets, 5 custom
    public float customHue = 0.92f;
    public float saturation = 0.5f;
    public boolean rainbow = false;
    public float animSpeed = 1.0f;
    public float glow = 0.8f;

    // ---- scene ----
    public boolean showBackground = true;
    public boolean showArt = true;
    public float bgDim = 0.45f;
    public float bgMotion = 0.6f;
    public float panelOpacity = 0.6f;

    // ---- sparkles ----
    public int particleMode = 3;     // 0 hearts, 1 stars, 2 sparkles, 3 mixed, 4 off
    public int particleCount = 60;
    public float particleSpeed = 1.0f;
    public float particleOpacity = 0.9f;

    // ---- HUD overlay ----
    public boolean overlayEnabled = true;
    public int overlayCorner = 1;    // 0 TL, 1 TR, 2 BL, 3 BR
    public float overlayOpacity = 0.9f;
    public boolean overlayStats = true;
    public boolean overlayParticles = true;

    private static Config inst;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ordersell.json");

    public static Config get() {
        if (inst == null) load();
        return inst;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) inst = GSON.fromJson(Files.readString(PATH), Config.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inst == null) inst = new Config();
        inst.clamp();
    }

    public static void save() {
        try {
            inst.clamp();
            Files.writeString(PATH, GSON.toJson(inst));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static float c(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int c(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private void clamp() {
        delayTicks = c(delayTicks, 0, 200);
        sellClicks = c(sellClicks, 1, 5);
        theme = c(theme, 0, 5);
        customHue = c(customHue, 0f, 1f);
        saturation = c(saturation, 0f, 1f);
        animSpeed = c(animSpeed, 0.1f, 3f);
        glow = c(glow, 0f, 1f);
        bgDim = c(bgDim, 0f, 0.9f);
        bgMotion = c(bgMotion, 0f, 1f);
        panelOpacity = c(panelOpacity, 0.1f, 1f);
        particleMode = c(particleMode, 0, 4);
        particleCount = c(particleCount, 0, 200);
        particleSpeed = c(particleSpeed, 0.1f, 3f);
        particleOpacity = c(particleOpacity, 0.1f, 1f);
        overlayCorner = c(overlayCorner, 0, 3);
        overlayOpacity = c(overlayOpacity, 0.1f, 1f);
    }
}
