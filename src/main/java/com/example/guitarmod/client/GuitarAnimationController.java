package com.example.guitarmod.client;

import com.example.guitarmod.client.integration.PlayerAnimatorBridge;
import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.config.GuitarConfig;
import com.example.guitarmod.item.ModItems;
import net.minecraft.client.player.LocalPlayer;

/**
 * 动画触发：手持吉他且 RMS 电平超阈值 -> 播弹奏动画；静音 1.5s -> 停止。
 * playerAnimator 未安装时整体静默跳过。
 */
public final class GuitarAnimationController {

    private static final long SILENCE_TIMEOUT_MS = 1500;

    private static long lastActiveMs = 0L;
    private static boolean playing = false;

    private GuitarAnimationController() {}

    public static void tick(LocalPlayer player) {
        if (!GuitarConfig.ENABLE_ANIMATION.get() || !PlayerAnimatorBridge.isAvailable()) {
            if (playing) playing = false;
            return;
        }

        boolean holding = player.getMainHandItem().is(ModItems.GUITAR.get());
        float meter = AudioCaptureManager.INSTANCE.meter();
        float threshold = GuitarConfig.LEVEL_THRESHOLD.get().floatValue();
        long now = System.currentTimeMillis();

        if (holding && meter > threshold) {
            lastActiveMs = now;
            if (!playing) {
                PlayerAnimatorBridge.play(player);
                playing = true;
            }
        } else if (playing && now - lastActiveMs > SILENCE_TIMEOUT_MS) {
            PlayerAnimatorBridge.stop(player);
            playing = false;
        }
    }
}
