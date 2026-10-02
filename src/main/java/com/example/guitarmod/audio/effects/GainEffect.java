package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

/** 增益：gain 0.0 ~ 4.0。 */
public class GainEffect extends AbstractEffect {

    public GainEffect() {
        params.put("gain", 1.0f);
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "gain");
    }

    @Override
    public float paramMax(String key) {
        return "gain".equals(key) ? 4.0f : 1.0f;
    }

    @Override
    public void process(float[] samples, int length, int sampleRate) {
        float g = params.get("gain");
        for (int i = 0; i < length; i++) {
            float v = samples[i] * g;
            samples[i] = Math.max(-1f, Math.min(1f, v));
        }
    }
}
