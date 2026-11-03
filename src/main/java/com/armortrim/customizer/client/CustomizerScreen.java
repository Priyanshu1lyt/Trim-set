package com.armortrim.customizer.client;

import com.armortrim.customizer.net.ApplyPayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomizerScreen extends Screen {
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {}

    private static final int PW = 400, PH = 250;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFFAAAAAA, GREEN = 0xFF55FF55, RED = 0xFFFF5555, YELLOW = 0xFFFFFF55;

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] SLOT_CODES = {"HEAD", "CHEST", "LEGS", "FEET"};
    private static final String[] SLOT_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};

    private static final Map<String, Item> MATERIAL_ICONS = new HashMap<>();
    static {
        MATERIAL_ICONS.put("quartz", Items.QUARTZ);
        MATERIAL_ICONS.put("iron", Items.IRON_INGOT);
        MATERIAL_ICONS.put("netherite", Items.NETHERITE_INGOT);
        MATERIAL_ICONS.put("redstone", Items.REDSTONE);
        MATERIAL_ICONS.put("copper", Items.COPPER_INGOT);
        MATERIAL_ICONS.put("gold", Items.GOLD_INGOT);
        MATERIAL_ICONS.put("emerald", Items.EMERALD);
        MATERIAL_ICONS.put("diamond", Items.DIAMOND);
        MATERIAL_ICONS.put("lapis", Items.LAPIS_LAZULI);
        MATERIAL_ICONS.put("amethyst", Items.AMETHYST_SHARD);
        MATERIAL_ICONS.put("resin", Items.RESIN_BRICK);
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Identifier> patterns = new ArrayList<>();
    private final List<Identifier> materials = new ArrayList<>();
    private final Map<Identifier, ItemStack> patternIcons = new HashMap<>();
    private final Map<Identifier, ItemStack> materialIcons = new HashMap<>();

    private final Identifier[] indP = new Identifier[4];
    private final Identifier[] indM = new Identifier[4];
    private Identifier sharedP, sharedM;

    private boolean same = false;
    private boolean trimTab = true;
    private int activeSlot = 0;
    private int scroll = 0;
    private int maxScroll = 0;
    private String status = "Pick a trim, then a material.";
    private int statusColor = GRAY;

    private final ItemStack[] savedStack = new ItemStack[4];
    private final ArmorTrim[] savedTrim = new ArmorTrim[4];

    public CustomizerScreen() {
        super(Text.literal("Armor Trim Customizer"));
    }

    @Override
    protected void init() {
        if (patterns.isEmpty() && client != null && client.world != null) {
            Registry<ArmorTrimPattern> pr = client.world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
            patterns.addAll(pr.getIds());
            patterns.sort(Comparator.comparing(Identifier::toString));
            Registry<ArmorTrimMaterial> mr = client.world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);
            materials.addAll(mr.getIds());
            materials.sort(Comparator.comparing(Identifier::toString));
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String pretty(Identifier id) {
        StringBuilder sb = new StringBuilder();
        for (String w : id.getPath().split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private ItemStack patternIcon(Identifier id) {
        return patternIcons.computeIfAbsent(id, k -> {
            Identifier t = Identifier.of(k.getNamespace(), k.getPath() + "_armor_trim_smithing_template");
            return new ItemStack(Registries.ITEM.containsId(t) ? Registries.ITEM.get(t) : Items.PAPER);
        });
    }

    private ItemStack materialIcon(Identifier id) {
        return materialIcons.computeIfAbsent(id, k -> new ItemStack(MATERIAL_ICONS.getOrDefault(k.getPath(), Items.IRON_NUGGET)));
    }

    private RegistryEntry<ArmorTrimPattern> patternEntry(Identifier id) {
        Registry<ArmorTrimPattern> r = client.world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
        return r.getOptional(RegistryKey.of(RegistryKeys.TRIM_PATTERN, id)).orElse(null);
    }

    private RegistryEntry<ArmorTrimMaterial> materialEntry(Identifier id) {
        Registry<ArmorTrimMaterial> r = client.world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);
        return r.getOptional(RegistryKey.of(RegistryKeys.TRIM_MATERIAL, id)).orElse(null);
    }

    private Identifier effP(int slot) { return same ? sharedP : indP[slot]; }
    private Identifier effM(int slot) { return same ? sharedM : indM[slot]; }
    private Identifier curP() { return effP(activeSlot); }
    private Identifier curM() { return effM(activeSlot); }

    private void setPattern(Identifier id) { if (same) sharedP = id; else indP[activeSlot] = id; }
    private void setMaterial(Identifier id) { if (same) sharedM = id; else indM[activeSlot] = id; }

    private static boolean trimmable(ItemStack s) {
        return !s.isEmpty() && s.isIn(ItemTags.TRIMMABLE_ARMOR);
    }

    private void setStatus(String s, int color) { status = s; statusColor = color; }

    private void updatePreview() {
        if (client == null || client.player == null || client.world == null) return;
        for (int i = 0; i < 4; i++) {
            ItemStack st = client.player.getEquippedStack(SLOTS[i]);
            if (!trimmable(st)) continue;
            Identifier p = effP(i), m = effM(i);
            RegistryEntry<ArmorTrimPattern> pe = p == null ? null : patternEntry(p);
            RegistryEntry<ArmorTrimMaterial> me = m == null ? null : materialEntry(m);
            if (pe != null && me != null) {
                if (savedStack[i] != st) {
                    savedStack[i] = st;
                    savedTrim[i] = st.get(DataComponentTypes.TRIM);
                }
                st.set(DataComponentTypes.TRIM, new ArmorTrim(me, pe));
            } else if (savedStack[i] == st) {
                restore(i, st);
            }
        }
    }

    private void restore(int i, ItemStack st) {
        if (savedTrim[i] == null) st.remove(DataComponentTypes.TRIM);
        else st.set(DataComponentTypes.TRIM, savedTrim[i]);
        savedStack[i] = null;
        savedTrim[i] = null;
    }

    @Override
    public void removed() {
        if (client != null && client.player != null) {
            for (int i = 0; i < 4; i++) {
                ItemStack st = client.player.getEquippedStack(SLOTS[i]);
                if (savedStack[i] != null && savedStack[i] == st) restore(i, st);
            }
        }
        super.removed();
    }

    private void toggleMode() {
        if (!same) {
            if (sharedP == null && sharedM == null) {
                sharedP = indP[activeSlot];
                sharedM = indM[activeSlot];
            }
            same = true;
        } else {
            for (int i = 0; i < 4; i++) {
                if (indP[i] == null && indM[i] == null) { indP[i] = sharedP; indM[i] = sharedM; }
            }
            same = false;
        }
        scroll = 0;
        updatePreview();
        setStatus(same ? "One trim + material for all armor." : "Each armor piece is configured separately.", GRAY);
    }

    private void apply() {
        if (client == null || client.player == null) return;
        StringBuilder sb = new StringBuilder();
        int n = 0;
        boolean anyArmor = false;
        for (int i = 0; i < 4; i++) {
            ItemStack st = client.player.getEquippedStack(SLOTS[i]);
            if (st.isEmpty()) continue;
            anyArmor = true;
            Identifier p = effP(i), m = effM(i);
            if (p == null || m == null || !trimmable(st)) continue;
            if (n > 0) sb.append(';');
            sb.append(SLOT_CODES[i]).append(',').append(p).append(',').append(m);
            n++;
        }
        if (!anyArmor) { setStatus("You are not wearing any armor.", RED); return; }
        if (n == 0) { setStatus("Select a trim AND a material first.", YELLOW); return; }
        if (!ClientPlayNetworking.canSend(ApplyPayload.ID)) {
            setStatus("This server does not have the mod installed.", RED);
            return;
        }
        ClientPlayNetworking.send(new ApplyPayload(sb.toString()));
        setStatus("Applying...", GRAY);
    }

    public void onResult(String msg) {
        if (msg.startsWith("ok:")) {
            String[] f = msg.split(":");
            int applied = Integer.parseInt(f[1]);
            for (int i = 0; i < 4; i++) { savedStack[i] = null; savedTrim[i] = null; }
            setStatus("Applied to " + applied + " armor piece" + (applied == 1 ? "" : "s") + "!", GREEN);
        } else if (msg.equals("err:not_op")) {
            setStatus("Only operators can apply trims.", RED);
        } else if (msg.equals("err:none")) {
            setStatus("Nothing could be applied (no trimmable armor).", RED);
        } else {
            setStatus("Invalid request.", RED);
        }
    }

    private void addHit(int x1, int y1, int x2, int y2, Runnable r) {
        hits.add(new Hit(x1, y1, x2, y2, r));
    }

    private void drawCentered(DrawContext ctx, String s, int x, int y, int w, int color) {
        ctx.drawText(textRenderer, s, x + (w - textRenderer.getWidth(s)) / 2, y, color, true);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int bg, int mx, int my, Runnable r) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? lighten(bg) : bg);
        drawCentered(ctx, label, x, y + (h - 8) / 2, w, WHITE);
        addHit(x, y, x + w, y + h, r);
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 24);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 24);
        int b = Math.min(255, (c & 0xFF) + 24);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        ctx.fill(0, 0, width, height, 0xA0000000);

        int px = (width - PW) / 2, py = (height - PH) / 2;
        ctx.fill(px - 2, py - 2, px + PW + 2, py + PH + 2, 0xFF000000);
        ctx.fill(px, py, px + PW, py + PH, 0xFF3A3A3A);
        ctx.fill(px + 2, py + 2, px + PW - 2, py + PH - 2, 0xFF2A2A2A);

        drawCentered(ctx, "ARMOR TRIM & ORE CUSTOMIZER", px, py + 8, PW, WHITE);

        int ex1 = px + 10, ey1 = py + 24, ex2 = px + 140, ey2 = py + 196;
        ctx.fill(ex1 - 1, ey1 - 1, ex2 + 1, ey2 + 1, 0xFF000000);
        ctx.fill(ex1, ey1, ex2, ey2, 0xFF161616);
        if (client != null && client.player != null) {
            InventoryScreen.drawEntity(ctx, ex1, ey1, ex2, ey2, 68, 0.0625F, (float) mx, (float) my, client.player);
        }
        drawCentered(ctx, same ? "Mode: SAME FOR ALL" : "Mode: INDIVIDUAL", ex1, ey2 + 6, ex2 - ex1, GRAY);

        int rx = px + 150;

        button(ctx, rx, py + 24, 240, 16, "SAME TRIM FOR ALL ARMOR: " + (same ? "ON" : "OFF"),
                same ? 0xFF2E7D32 : 0xFF6B2B2B, mx, my, this::toggleMode);

        if (same) {
            drawCentered(ctx, "Applies to helmet, chestplate, leggings & boots", rx, py + 48, 240, GRAY);
        } else {
            for (int i = 0; i < 4; i++) {
                final int idx = i;
                boolean has = client != null && client.player != null && !client.player.getEquippedStack(SLOTS[i]).isEmpty();
                int bg = i == activeSlot ? 0xFF3C6EA8 : (has ? 0xFF4A4A4A : 0xFF2E2E2E);
                button(ctx, rx + i * 60, py + 44, 60, 16, SLOT_NAMES[i], bg, mx, my, () -> {
                    activeSlot = idx;
                    scroll = 0;
                });
            }
        }

        button(ctx, rx, py + 64, 119, 16, "TRIMS", trimTab ? 0xFF3C6EA8 : 0xFF4A4A4A, mx, my, () -> { trimTab = true; scroll = 0; });
        button(ctx, rx + 121, py + 64, 119, 16, "ORES", !trimTab ? 0xFF3C6EA8 : 0xFF4A4A4A, mx, my, () -> { trimTab = false; scroll = 0; });

        Identifier cp = curP(), cm = curM();
        String who = same ? "All armor" : SLOT_NAMES[activeSlot];
        ctx.drawText(textRenderer, who + ": " + (cp == null ? "-" : pretty(cp)) + " + " + (cm == null ? "-" : pretty(cm)),
                rx, py + 85, WHITE, true);

        List<Identifier> ids = trimTab ? patterns : materials;
        int cols = 2, cw = 118, rh = 20;
        int vx = rx, vy = py + 98, vw = 240, vh = 96;
        int rows = (ids.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * rh - vh);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ctx.fill(vx - 1, vy - 1, vx + vw + 1, vy + vh + 1, 0xFF000000);
        ctx.fill(vx, vy, vx + vw, vy + vh, 0xFF1E1E1E);
        ctx.enableScissor(vx, vy, vx + vw, vy + vh);
        Identifier selected = trimTab ? cp : cm;
        for (int idx = 0; idx < ids.size(); idx++) {
            Identifier id = ids.get(idx);
            int cx = vx + (idx % cols) * (cw + 2);
            int cy = vy + (idx / cols) * rh - scroll;
            if (cy + rh < vy || cy > vy + vh) continue;
            boolean sel = id.equals(selected);
            boolean hover = mx >= cx && mx < cx + cw && my >= Math.max(cy, vy) && my < Math.min(cy + rh, vy + vh);
            ctx.fill(cx, cy, cx + cw, cy + rh - 1, sel ? 0xFF2E7D32 : (hover ? 0xFF505050 : 0xFF353535));
            if (sel) {
                ctx.fill(cx, cy, cx + cw, cy + 1, GREEN);
                ctx.fill(cx, cy + rh - 2, cx + cw, cy + rh - 1, GREEN);
            }
            ctx.drawItem(trimTab ? patternIcon(id) : materialIcon(id), cx + 2, cy + 1);
            ctx.drawText(textRenderer, pretty(id), cx + 22, cy + 6, WHITE, true);

            int hy1 = Math.max(cy, vy), hy2 = Math.min(cy + rh, vy + vh);
            if (hy1 < hy2) {
                addHit(cx, hy1, cx + cw, hy2, () -> {
                    if (trimTab) {
                        setPattern(id);
                        trimTab = false;
                        scroll = 0;
                        setStatus("Now pick a material.", GRAY);
                    } else {
                        setMaterial(id);
                        setStatus(curP() == null ? "Pick a trim too." : "Ready - press Apply.", curP() == null ? YELLOW : GREEN);
                    }
                    updatePreview();
                });
            }
        }
        ctx.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(12, vh * vh / (rows * rh));
            int barY = vy + (int) ((long) (vh - barH) * scroll / maxScroll);
            ctx.fill(vx + vw - 3, barY, vx + vw - 1, barY + barH, 0xFF888888);
        }

        drawCentered(ctx, status, rx, py + 200, 240, statusColor);
        button(ctx, rx, py + 214, 119, 20, "APPLY", 0xFF2E7D32, mx, my, this::apply);
        button(ctx, rx + 121, py + 214, 119, 20, "CANCEL", 0xFF7D2E2E, mx, my, this::close);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (click.x() >= h.x1() && click.x() < h.x2() && click.y() >= h.y1() && click.y() < h.y2()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * 20);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int code = input.getKeycode();
        if (ArmorTrimCustomizerClient.openKey != null
                && code == KeyBindingHelper.getBoundKeyOf(ArmorTrimCustomizerClient.openKey).getCode()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
