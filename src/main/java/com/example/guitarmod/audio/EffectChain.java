package com.example.guitarmod.audio;

import com.example.guitarmod.GuitarMod;
import com.example.guitarmod.audio.effects.EffectRegistry;
import com.example.guitarmod.audio.effects.IEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 效果链容器：有序列表，逐块就地处理。音频线程读、GUI 线程改，故用 CopyOnWriteArrayList。
 */
public class EffectChain {

    private final CopyOnWriteArrayList<IEffect> effects = new CopyOnWriteArrayList<>();

    public List<IEffect> effects() {
        return Collections.unmodifiableList(new ArrayList<>(effects));
    }

    public void add(IEffect effect) {
        effects.add(effect);
    }

    public void remove(int index) {
        if (index >= 0 && index < effects.size()) {
            effects.remove(index);
        }
    }

    public void move(int index, int delta) {
        int target = index + delta;
        if (index >= 0 && index < effects.size() && target >= 0 && target < effects.size()) {
            IEffect e = effects.remove(index);
            effects.add(target, e);
        }
    }

    /** 音频线程主入口：依次过每个启用的效果。 */
    public void process(float[] samples, int length, int sampleRate) {
        for (IEffect effect : effects) {
            if (effect.isEnabled()) {
                effect.process(samples, length, sampleRate);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (IEffect effect : effects) {
            list.add(effect.save());
        }
        tag.put("chain", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        effects.clear();
        ListTag list = tag.getList("chain", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null) continue;
            IEffect effect = EffectRegistry.create(id);
            if (effect == null) {
                GuitarMod.LOGGER.warn("[GuitarMod] unknown effect id in chain: {}", id);
                continue;
            }
            effect.load(entry);
            effects.add(effect);
        }
    }
}
