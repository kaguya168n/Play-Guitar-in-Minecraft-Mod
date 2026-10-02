package com.example.guitarmod.client;

import com.example.guitarmod.client.gui.AudioSettingsScreen;
import com.example.guitarmod.client.gui.EffectChainScreen;
import net.minecraft.client.Minecraft;

/**
 * 客户端界面打开入口（仅客户端类，公共代码通过 DistExecutor 调这里）。
 */
public final class ClientScreenOpener {
    private ClientScreenOpener() {}

    public static void openEffectChainScreen() {
        Minecraft.getInstance().setScreen(new EffectChainScreen());
    }

    public static void openAudioSettingsScreen() {
        Minecraft.getInstance().setScreen(new AudioSettingsScreen());
    }
}
