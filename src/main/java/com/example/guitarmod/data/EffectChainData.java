package com.example.guitarmod.data;

import com.example.guitarmod.audio.EffectChain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 效果链与物品 DataComponent 之间的读写辅助。
 */
public final class EffectChainData {
    private EffectChainData() {}

    /** 从物品组件读取效果链；无组件时返回默认链。 */
    public static EffectChain read(ItemStack stack) {
        CompoundTag tag = stack.get(ModComponents.EFFECT_CHAIN.get());
        EffectChain chain = new EffectChain();
        if (tag != null) {
            chain.load(tag);
        }
        return chain;
    }

    /** 把效果链写回物品组件。 */
    public static void write(ItemStack stack, EffectChain chain) {
        stack.set(ModComponents.EFFECT_CHAIN.get(), chain.save());
    }
}
