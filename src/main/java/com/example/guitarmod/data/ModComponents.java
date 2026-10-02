package com.example.guitarmod.data;

import com.example.guitarmod.GuitarMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.21 DataComponent 注册：效果链数据直接以 CompoundTag 存储。
 */
public final class ModComponents {
    private ModComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GuitarMod.MODID);

    /** 结构：{chain: [{id: "guitarmod:overdrive", enabled: 1b, params: {drive: 0.7f}}, ...]} */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> EFFECT_CHAIN =
            COMPONENTS.register("effect_chain", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());
}
