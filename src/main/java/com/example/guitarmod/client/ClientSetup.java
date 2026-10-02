package com.example.guitarmod.client;

import com.example.guitarmod.GuitarMod;
import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.audio.EffectChain;
import com.example.guitarmod.audio.playback.PlayerSoundTracker;
import com.example.guitarmod.data.EffectChainData;
import com.example.guitarmod.item.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 客户端主联动：手持吉他自动启停采集、音源跟随、按键、动画触发、退出世界释放资源。
 */
@EventBusSubscriber(modid = GuitarMod.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    /** 由 ClientModEvents 注入。 */
    static KeyMapping openAudioSettings;

    private static ItemStack lastChainSource = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        while (openAudioSettings != null && openAudioSettings.consumeClick()) {
            ClientScreenOpener.openAudioSettingsScreen();
        }

        if (player == null || mc.level == null) {
            return;
        }

        boolean holding = player.getMainHandItem().is(ModItems.GUITAR.get());

        // 手持吉他 -> 确保采集已启动；放下 -> 停采集
        if (holding && !AudioCaptureManager.INSTANCE.isRunning()) {
            AudioCaptureManager.INSTANCE.start();
        } else if (!holding && AudioCaptureManager.INSTANCE.isRunning() && mc.screen == null) {
            AudioCaptureManager.INSTANCE.stop();
        }

        // 当前吉他的效果链 -> 喂给采集线程（换物品时重载一次）
        if (holding) {
            ItemStack stack = player.getMainHandItem();
            if (stack != lastChainSource) {
                lastChainSource = stack;
                EffectChain chain = EffectChainData.read(stack);
                AudioCaptureManager.INSTANCE.setChain(chain);
            }
        } else {
            lastChainSource = null;
        }

        // 播放层：音源跟随 + 暂停处理
        PlayerSoundTracker.INSTANCE.tick();

        // 动画：电平超阈值时触发弹奏动画
        GuitarAnimationController.tick(player);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PlayerSoundTracker.INSTANCE.shutdown();
        AudioCaptureManager.INSTANCE.stop();
        lastChainSource = null;
    }
}
