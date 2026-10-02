package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

/** 延迟：环形缓冲。time 为毫秒，feedback 0~0.9，mix 为湿声比例。 */
public class DelayEffect extends AbstractEffect {

    private static final int MAX_DELAY_MS = 1000;

    private float[] buffer = new float[0];
    private int writePos = 0;

    public DelayEffect() {
        params.put("time", 350f);
        params.put("feedback", 0.35f);
        params.put("mix", 0.3f);
    }

    @Override
    public float paramMax(String key) {
        return switch (key) {
            case "time" -> (float) MAX_DELAY_MS;
            case "feedback" -> 0.9f;
            default -> 1.0f;
        };
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "delay");
    }

    private void ensureCapacity(int sampleRate) {
        int need = sampleRate * MAX_DELAY_MS / 1000;
        if (buffer.length != need) {
            buffer = new float[need];
            writePos = 0;
        }
    }

    @Override
    public void process(float[] samples, int length, int sampleRate) {
        ensureCapacity(sampleRate);
        int delaySamples = Math.min(buffer.length - 1,
                Math.max(1, (int) (params.get("time") * sampleRate / 1000f)));
        float feedback = Math.min(0.9f, params.get("feedback"));
        float mix = params.get("mix");
        for (int i = 0; i < length; i++) {
            int readPos = writePos - delaySamples;
            if (readPos < 0) readPos += buffer.length;
            float delayed = buffer[readPos];
            float dry = samples[i];
            buffer[writePos] = dry + delayed * feedback;
            samples[i] = dry * (1f - mix) + delayed * mix;
            writePos = (writePos + 1) % buffer.length;
        }
    }
}
