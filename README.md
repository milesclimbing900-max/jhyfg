# Order Sell (Fabric 1.21.11, client-side)

Auto-sells in `Orders -> Collect Items` / `Orders -> Confirm Sell`:
clicks the emerald, reads the green pane price, then clicks green (2x) if it's >= your minimum,
otherwise clicks red and loops to refresh.

## Controls
- **Right Shift**: open the settings menu (rebind in Controls -> Order Sell)
- **Backslash (\\)**: start / stop selling. Also works while the orders GUI is open.
- `/ordersell` opens the menu; `/ordersell on|off|toggle|min 600m` also work.

## Menu tabs
Sell (min price, delay, clicks) | Colors (themes, hue, saturation, rainbow, speed, glow) |
Scene (background, portrait, dim, camera motion, panel opacity) | Sparkle (style, amount, speed, opacity) |
HUD (status overlay on/off, corner, opacity, stats, sparkles)

All settings save to `config/ordersell.json`.

## Swap the artwork
Replace `assets/ordersell/textures/gui/bg.png` (a resource pack can do this). If your image isn't
425x600, change `TW` / `TH` at the top of `ConfigScreen.java`.

## Build
Push to GitHub; Actions builds it. Grab the jar from the run's Artifacts. Needs Fabric API.
