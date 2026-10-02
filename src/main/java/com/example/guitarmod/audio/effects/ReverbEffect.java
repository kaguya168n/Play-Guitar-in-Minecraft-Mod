package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

/**
 * 简易混响：Schroeder 结构 —— 4 个并联梳状滤波 + 2 个串联全通滤波。
 * room 为房间大小（反馈量），mix 为湿声比例。
 */
public class ReverbEffect extends AbstractEffect {

    private static final float[] COMB_MS = {29.7f, 37.1f, 41.1f, 43.7f};
    private static final float[] ALLPASS_MS = {5.0f, 1.7f};
    private static final float ALLPASS_G = 0.5f;

    private final Comb[] combs = new Comb[COMB_MS.length];
    private final Allpass[] allpasses = new Allpass[ALLPASS_MS.length];
    private int initializedRate = 0;

    public ReverbEffect() {
        params.put("room", 0.5f);
        params.put("mix", 0.25f);
    }

    @Override
    public float paramMax(String key) {
        return "room".equals(key) ? 0.95f : 1.0f;
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "reverb");
    }

    private void ensureInit(int sampleRate) {
        if (initializedRate == sampleRate) return;
        initializedRate = sampleRate;
        for (int i = 0; i < combs.length; i++) {
            combs[i] = new Comb((int) (COMB_MS[i] * sampleRate / 1000f));
        }
        for (int i = 0; i < allpasses.length; i++) {
            allpasses[i] = new Allpass((int) (ALLPASS_MS[i] * sampleRate / 1000f));
        }
    }

    @Override
    public void process(float[] samples, int length, int sampleRate) {
        ensureInit(sampleRate);
        float room = Math.min(0.95f, params.get("room"));
        float mix = params.get("mix");
        for (int i = 0; i < length; i++) {
            float dry = samples[i];
            float wet = 0f;
            for (Comb c : combs) wet += c.tick(dry, room);
            wet *= 0.25f;
            for (Allpass a : allpasses) wet = a.tick(wet);
            samples[i] = dry * (1f - mix) + wet * mix;
        }
    }

    private static final class Comb {
        private final float[] buf;
        private int pos;
        private float filterStore;

        Comb(int size) { buf = new float[Math.max(1, size)]; }

        float tick(float in, float feedback) {
            float out = buf[pos];
            filterStore = out * 0.2f + filterStore * 0.8f; // 阻尼低通
            buf[pos] = in + filterStore * feedback;
            pos = (pos + 1) % buf.length;
            return out;
        }
    }

    private static final class Allpass {
        private final float[] buf;
        private int pos;

        Allpass(int size) { buf = new float[Math.max(1, size)]; }

        float tick(float in) {
            float buffered = buf[pos];
            float out = -in + buffered;
            buf[pos] = in + buffered * ALLPASS_G;
            pos = (pos + 1) % buf.length;
            return out;
        }
    }
}
