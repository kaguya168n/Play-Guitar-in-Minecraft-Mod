package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

/** 过载/失真：tanh 软削波。drive 0.0 ~ 1.0，level 为输出补偿。 */
public class OverdriveEffect extends AbstractEffect {

    public OverdriveEffect() {
        params.put("drive", 0.5f);
        params.put("level", 0.8f);
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "overdrive");
    }

    @Override
    public void process(float[] samples, int length, int sampleRate) {
        float drive = params.get("drive");
        float level = params.get("level");
        float k = 1.0f + drive * 20.0f;
        float norm = (float) Math.tanh(k); // 归一化，避免 drive 越大越响
        for (int i = 0; i < length; i++) {
            samples[i] = (float) Math.tanh(samples[i] * k) / norm * level;
        }
    }
}
