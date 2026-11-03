package com.armortrim.customizer;

import com.armortrim.customizer.net.ApplyPayload;
import com.armortrim.customizer.net.ResultPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ArmorTrimCustomizer implements ModInitializer {
    public static final String MOD_ID = "armortrimcustomizer";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ApplyPayload.ID, ApplyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResultPayload.ID, ResultPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ApplyPayload.ID,
                (payload, context) -> handle(payload, context.player(), context.server()));
    }

    private static EquipmentSlot slotOf(String code) {
        return switch (code) {
            case "HEAD" -> EquipmentSlot.HEAD;
            case "CHEST" -> EquipmentSlot.CHEST;
            case "LEGS" -> EquipmentSlot.LEGS;
            case "FEET" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static void handle(ApplyPayload payload, ServerPlayerEntity player, MinecraftServer server) {
        // Only operators may use this. (Singleplayer: world must have cheats enabled.)
        boolean op = server.getPlayerManager().isOperator(new PlayerConfigEntry(player.getGameProfile()));
        if (!op) {
            ServerPlayNetworking.send(player, new ResultPayload("err:not_op"));
            return;
        }

        Registry<ArmorTrimPattern> patterns = server.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
        Registry<ArmorTrimMaterial> materials = server.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);

        int applied = 0;
        int skipped = 0;
        boolean[] done = new boolean[4];

        String data = payload.data();
        if (data == null || data.isBlank()) {
            ServerPlayNetworking.send(player, new ResultPayload("err:invalid"));
            return;
        }

        for (String part : data.split(";")) {
            String[] f = part.split(",");
            if (f.length != 3) { skipped++; continue; }
            EquipmentSlot slot = slotOf(f[0]);
            Identifier pid = Identifier.tryParse(f[1]);
            Identifier mid = Identifier.tryParse(f[2]);
            if (slot == null || pid == null || mid == null) { skipped++; continue; }
            int key = slot == EquipmentSlot.HEAD ? 0 : slot == EquipmentSlot.CHEST ? 1 : slot == EquipmentSlot.LEGS ? 2 : 3;
            if (done[key]) { skipped++; continue; }

            RegistryEntry<ArmorTrimPattern> pe = patterns.getOptional(RegistryKey.of(RegistryKeys.TRIM_PATTERN, pid)).orElse(null);
            RegistryEntry<ArmorTrimMaterial> me = materials.getOptional(RegistryKey.of(RegistryKeys.TRIM_MATERIAL, mid)).orElse(null);
            if (pe == null || me == null) { skipped++; continue; }

            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) { skipped++; continue; }

            // Only the trim component changes, in place. Enchantments, durability, name stay untouched.
            stack.set(DataComponentTypes.TRIM, new ArmorTrim(me, pe));
            done[key] = true;
            applied++;
        }

        if (applied == 0) {
            ServerPlayNetworking.send(player, new ResultPayload("err:none"));
        } else {
            ServerPlayNetworking.send(player, new ResultPayload("ok:" + applied + ":" + skipped));
        }
    }
}
