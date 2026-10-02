package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

/**
 * 合唱：LFO 调制的短延迟线，线性插值读。rate 为 Hz，depth 为毫秒，mix 为湿声比例。
 */
public class ChorusEffect extends AbstractEffect {

    private static final float BASE_DELAY_MS = 15f;
    private static final float MAX_DEPTH_MS = 10f;

    private float[] buffer = new float[0];
    private int writePos = 0;
    private float lfoPhase = 0f;
    private int initializedRate = 0;

    public ChorusEffect() {
        params.put("rate", 0.8f);
        params.put("depth", 5f);
        params.put("mix", 0.4f);
    }

    @Override
    public float paramMax(String key) {
        return switch (key) {
            case "rate" -> 5.0f;
            case "depth" -> MAX_DEPTH_MS;
            default -> 1.0f;
        };
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "chorus");
    }

    private void ensureInit(int sampleRate) {
        if (initializedRate == sampleRate) return;
        initializedRate = sampleRate;
        int need = (int) ((BASE_DELAY_MS + MAX_DEPTH_MS + 2) * sampleRate / 1000f) + 2;
        buffer = new float[need];
        writePos = 0;
        lfoPhase = 0f;
    }

    @Override
    public void process(float[] samples, int length, int sampleRate) {
        ensureInit(sampleRate);
        float rate = params.get("rate");
        float depthMs = Math.min(MAX_DEPTH_MS, params.get("depth"));
        float mix = params.get("mix");
        float phaseInc = (float) (2.0 * Math.PI * rate / sampleRate);

        for (int i = 0; i < length; i++) {
            float lfo = (float) Math.sin(lfoPhase);
            lfoPhase += phaseInc;
            if (lfoPhase > Math.PI * 2) lfoPhase -= (float) (Math.PI * 2);

            float delayMs = BASE_DELAY_MS + depthMs * (0.5f + 0.5f * lfo);
            float delaySamples = delayMs * sampleRate / 1000f;
            float readPos = writePos - delaySamples;
            while (readPos < 0) readPos += buffer.length;
            int r0 = (int) readPos;
            int r1 = (r0 + 1) % buffer.length;
            float frac = readPos - r0;
            float wet = buffer[r0 % buffer.length] * (1f - frac) + buffer[r1] * frac;

            buffer[writePos] = samples[i];
            writePos = (writePos + 1) % buffer.length;
            samples[i] = samples[i] * (1f - mix) + wet * mix;
        }
    }
}
