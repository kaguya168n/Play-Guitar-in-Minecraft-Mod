package com.example.guitarmod.audio.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 效果器扩展点。其他模组通过 EffectRegistry.register 注册自己的实现即可扩展效果器。
 */
public interface IEffect {

    /** 唯一 id，如 "guitarmod:overdrive"，用于注册表和存档。 */
    ResourceLocation id();

    /** 就地处理一块 PCM 采样。samples 为 [-1,1] 浮点单声道数据，length 为有效采样数。 */
    void process(float[] samples, int length, int sampleRate);

    /** 参数表：key -> 当前值（如 "drive"=0.7），GUI 自动生成滑条。 */
    Map<String, Float> getParams();

    void setParam(String key, float value);

    /** 参数的最大值（GUI 滑条量程），最小值恒为 0。 */
    default float paramMax(String key) { return 1.0f; }

    /** 序列化到物品组件/配置。 */
    CompoundTag save();

    void load(CompoundTag tag);

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
