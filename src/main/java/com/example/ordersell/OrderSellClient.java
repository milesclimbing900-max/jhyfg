package com.example.ordersell;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class OrderSellClient implements ClientModInitializer {

    // ---------- keybinds ----------
    private static KeyBinding menuKey, runKey;
    private static boolean openMenuRequested = false;
    private static boolean prevRunDown = false;

    // ---------- runtime state ----------
    private static boolean enabled = false;
    private static String phase = "Open the orders menu";
    private static int lastSyncId = -1;
    private static String lastTitle = "";
    private static int wait = 0;
    private static boolean acted = false;
    private static int idle = 0;
    private static int sold = 0;
    private static int refreshes = 0;
    private static double lastValue = -1;

    private static final Pattern VALUE =
            Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kmbt])?(?![a-z])", Pattern.CASE_INSENSITIVE);

    public static boolean isEnabled() { return enabled; }
    public static String getPhase() { return phase; }
    public static int getSold() { return sold; }
    public static int getRefreshes() { return refreshes; }
    public static double getLastValue() { return lastValue; }

    public static void setEnabled(boolean on) {
        enabled = on;
        lastSyncId = -1;
        lastTitle = "";
        acted = false;
        phase = on ? "Open the orders menu" : "Paused";
        if (on) { sold = 0; refreshes = 0; lastValue = -1; }
    }

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("ordersell", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.ordersell.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        runKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.ordersell.run", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, cat));

        ClientTickEvents.END_CLIENT_TICK.register(OrderSellClient::tick);

        // overlay on the normal HUD ...
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.of("ordersell", "overlay"), (ctx, tickCounter) -> Overlay.render(ctx));
        // ... and on top of the orders / chest GUIs
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GenericContainerScreen) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> Overlay.render(ctx));
            }
        });

        registerCommands();
    }

    // ---------- main loop ----------
    private static void tick(MinecraftClient mc) {
        if (mc.player == null) return;

        if (mc.currentScreen == null) {
            while (menuKey.wasPressed()) mc.setScreen(new ConfigScreen());
            while (runKey.wasPressed()) setEnabled(!enabled);
            if (openMenuRequested) {
                openMenuRequested = false;
                mc.setScreen(new ConfigScreen());
            }
        } else if (mc.currentScreen instanceof GenericContainerScreen) {
            // keybinds don't fire inside GUIs, so poll the start/stop key directly
            InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(runKey);
            if (k.getCategory() == InputUtil.Type.KEYSYM && k.getCode() > 0) {
                boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), k.getCode()) == GLFW.GLFW_PRESS;
                if (down && !prevRunDown) setEnabled(!enabled);
                prevRunDown = down;
            }
        }

        if (!enabled || mc.interactionManager == null) return;
        Config cfg = Config.get();

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            lastSyncId = -1;
            lastTitle = "";
            phase = "Open the orders menu";
            return;
        }

        GenericContainerScreenHandler handler = screen.getScreenHandler();
        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);

        if (handler.syncId != lastSyncId || !title.equals(lastTitle)) {
            lastSyncId = handler.syncId;
            lastTitle = title;
            wait = cfg.delayTicks;
            acted = false;
            idle = 0;
        }

        if (wait > 0) { wait--; return; }

        if (acted) {
            if (++idle > 40) { acted = false; idle = 0; }
            return;
        }

        if (title.contains("confirm sell")) {
            handleConfirm(mc, handler, cfg);
        } else if (title.contains("collect items")) {
            handleCollect(mc, handler);
        } else {
            phase = "Waiting for orders GUI";
        }
    }

    private static void handleCollect(MinecraftClient mc, GenericContainerScreenHandler h) {
        Slot emerald = findSlot(h, s -> s.isOf(Items.EMERALD)
                && s.getName().getString().toLowerCase(Locale.ROOT).contains("sell"));
        if (emerald == null) emerald = findSlot(h, s -> s.isOf(Items.EMERALD));
        if (emerald == null) { phase = "Loading..."; return; }
        phase = "Clicking sell";
        click(mc, h, emerald);
        acted = true;
    }

    private static void handleConfirm(MinecraftClient mc, GenericContainerScreenHandler h, Config cfg) {
        Slot green = findSlot(h, s -> s.isOf(Items.LIME_STAINED_GLASS_PANE)
                || s.isOf(Items.GREEN_STAINED_GLASS_PANE));
        Slot red = findSlot(h, s -> s.isOf(Items.RED_STAINED_GLASS_PANE));
        if (green == null || red == null) { phase = "Loading..."; return; }

        double value = readValue(green.getStack());
        if (value < 0) { phase = "Reading price..."; return; }
        lastValue = value;

        if (value >= cfg.threshold) {
            for (int i = 0; i < Math.max(1, cfg.sellClicks); i++) click(mc, h, green);
            sold++;
            phase = "Sold at " + fmt(value);
        } else {
            click(mc, h, red);
            refreshes++;
            phase = "Too low (" + fmt(value) + "), refreshing";
        }
        acted = true;
    }

    // ---------- helpers ----------
    private static Slot findSlot(GenericContainerScreenHandler h, Predicate<ItemStack> p) {
        int max = Math.min(h.getRows() * 9, h.slots.size());
        for (int i = 0; i < max; i++) {
            Slot slot = h.slots.get(i);
            ItemStack st = slot.getStack();
            if (!st.isEmpty() && p.test(st)) return slot;
        }
        return null;
    }

    private static void click(MinecraftClient mc, GenericContainerScreenHandler h, Slot slot) {
        mc.interactionManager.clickSlot(h.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
    }

    private static double readValue(ItemStack stack) {
        double v = parseValue(stack.getName().getString());
        if (v >= 0) return v;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                v = parseValue(line.getString());
                if (v >= 0) return v;
            }
        }
        return -1;
    }

    /** "595m" -> 595000000, "1.5b" -> 1500000000, "2,500" -> 2500. */
    public static double parseValue(String s) {
        Matcher m = VALUE.matcher(s);
        if (!m.find()) return -1;
        double n;
        try {
            n = Double.parseDouble(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
        String suf = m.group(2);
        if (suf != null) {
            switch (suf.toLowerCase(Locale.ROOT)) {
                case "k" -> n *= 1e3;
                case "m" -> n *= 1e6;
                case "b" -> n *= 1e9;
                case "t" -> n *= 1e12;
                default -> { }
            }
        }
        return n;
    }

    public static String fmt(double v) {
        if (v >= 1e12) return trim(v / 1e12) + "t";
        if (v >= 1e9) return trim(v / 1e9) + "b";
        if (v >= 1e6) return trim(v / 1e6) + "m";
        if (v >= 1e3) return trim(v / 1e3) + "k";
        return trim(v);
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.2f", d);
        return s.replaceAll("\\.?0+$", "");
    }

    // ---------- commands (optional shortcuts) ----------
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("ordersell")
                .executes(ctx -> { openMenuRequested = true; return 1; })
                .then(literal("menu").executes(ctx -> { openMenuRequested = true; return 1; }))
                .then(literal("on").executes(ctx -> { setEnabled(true); return 1; }))
                .then(literal("off").executes(ctx -> { setEnabled(false); return 1; }))
                .then(literal("toggle").executes(ctx -> { setEnabled(!enabled); return 1; }))
                .then(literal("min").then(argument("value", StringArgumentType.word()).executes(ctx -> {
                    double v = parseValue(StringArgumentType.getString(ctx, "value"));
                    if (v < 0) {
                        ctx.getSource().sendFeedback(Text.literal("[OrderSell] Try 600m or 1.2b"));
                        return 0;
                    }
                    Config.get().threshold = v;
                    Config.save();
                    ctx.getSource().sendFeedback(Text.literal("[OrderSell] Minimum price: " + fmt(v)));
                    return 1;
                })))
            ));
    }
}
