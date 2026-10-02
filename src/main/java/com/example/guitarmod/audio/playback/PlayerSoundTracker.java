package com.example.guitarmod.audio.playback;

import com.example.guitarmod.GuitarMod;
import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.config.GuitarConfig;
import com.example.guitarmod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * 音源位置每 tick 跟随持吉他的玩家，并乘以原版"玩家"音量滑条与配置增益。
 */
public final class PlayerSoundTracker {

    public static final PlayerSoundTracker INSTANCE = new PlayerSoundTracker();

    private final AlStreamPlayer player = new AlStreamPlayer();
    private boolean playbackActive = false;

    private PlayerSoundTracker() {}

    public AlStreamPlayer streamPlayer() {
        return player;
    }

    /** 每客户端 tick 调用。返回当前是否处于"正在演奏"状态（供动画触发）。 */
    public boolean tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            shutdown();
            return false;
        }

        boolean holding = p.getMainHandItem().is(ModItems.GUITAR.get());
        boolean captureRunning = AudioCaptureManager.INSTANCE.isRunning();

        if (mc.isPaused()) {
            // 暂停：停播并丢弃采集数据
            if (playbackActive) {
                player.stopPlayback();
                AudioCaptureManager.INSTANCE.flushQueue();
                playbackActive = false;
            }
            return false;
        }

        if (holding && captureRunning) {
            if (!player.isInitialized() && !player.init()) {
                return false;
            }
            float volume = mc.options.getSoundSourceVolume(SoundSource.PLAYERS)
                    * GuitarConfig.PLAYBACK_GAIN.get().floatValue();
            player.update(p.getEyePosition(), volume);
            playbackActive = true;
            return true;
        }

        if (playbackActive) {
            player.stopPlayback();
            AudioCaptureManager.INSTANCE.flushQueue();
            playbackActive = false;
        }
        return false;
    }

    /** 退出世界/关闭客户端时释放全部资源。 */
    public void shutdown() {
        try {
            player.close();
        } catch (Throwable t) {
            GuitarMod.LOGGER.warn("[GuitarMod] shutdown error", t);
        }
        playbackActive = false;
    }
}
