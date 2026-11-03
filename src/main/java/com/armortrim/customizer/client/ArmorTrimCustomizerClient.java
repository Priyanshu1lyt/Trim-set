package com.armortrim.customizer.client;

import com.armortrim.customizer.ArmorTrimCustomizer;
import com.armortrim.customizer.net.ResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ArmorTrimCustomizerClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(ArmorTrimCustomizer.MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.armortrimcustomizer.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new CustomizerScreen());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ResultPayload.ID, (payload, context) -> {
            if (context.client().currentScreen instanceof CustomizerScreen screen) {
                screen.onResult(payload.message());
            }
        });
    }
}
