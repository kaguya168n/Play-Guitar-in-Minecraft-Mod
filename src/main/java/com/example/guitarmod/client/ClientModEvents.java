package com.example.guitarmod.client;

import com.example.guitarmod.GuitarMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Mod 事件总线上的客户端注册（按键绑定）。
 */
@EventBusSubscriber(modid = GuitarMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientSetup.openAudioSettings = new KeyMapping(
                "key.guitarmod.audio_settings",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_G,
                "key.categories.guitarmod");
        event.register(ClientSetup.openAudioSettings);
    }
}
