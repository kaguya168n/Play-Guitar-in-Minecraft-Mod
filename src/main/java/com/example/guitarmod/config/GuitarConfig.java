package com.example.guitarmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置（TOML）：采样率、缓冲、电平阈值、输入设备名等。
 */
public final class GuitarConfig {
    private GuitarConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SAMPLE_RATE;
    public static final ModConfigSpec.IntValue BUFFER_FRAMES;
    public static final ModConfigSpec.ConfigValue<String> DEVICE_NAME;
    public static final ModConfigSpec.DoubleValue LEVEL_THRESHOLD;
    public static final ModConfigSpec.DoubleValue PLAYBACK_GAIN;
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("audio");
        SAMPLE_RATE = b.comment("Capture sample rate (Hz).")
                .defineInRange("sampleRate", 48000, 8000, 96000);
        BUFFER_FRAMES = b.comment("Frames per capture block (lower = less latency, more CPU).")
                .defineInRange("bufferFrames", 512, 128, 4096);
        DEVICE_NAME = b.comment("Preferred input device name substring. Empty = system default.")
                .define("deviceName", "");
        PLAYBACK_GAIN = b.comment("Master playback gain multiplier.")
                .defineInRange("playbackGain", 1.0, 0.0, 4.0);
        b.pop();
        b.push("animation");
        LEVEL_THRESHOLD = b.comment("RMS level threshold that triggers the strumming animation.")
                .defineInRange("levelThreshold", 0.02, 0.001, 1.0);
        ENABLE_ANIMATION = b.comment("Play playerAnimator strumming animation while playing (requires playerAnimator mod).")
                .define("enableAnimation", true);
        b.pop();
        SPEC = b.build();
    }
}
