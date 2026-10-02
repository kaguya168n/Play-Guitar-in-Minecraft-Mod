package com.example.guitarmod.audio.effects;

import com.example.guitarmod.GuitarMod;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 效果器注册表（id -> 工厂）。公开 API：其他模组依赖本模组后调用
 * {@link #register(ResourceLocation, Supplier)} 即可扩展自己的效果器。
 */
public final class EffectRegistry {
    private EffectRegistry() {}

    private static final Map<ResourceLocation, Supplier<IEffect>> FACTORIES = new LinkedHashMap<>();

    public static synchronized void register(ResourceLocation id, Supplier<IEffect> factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            GuitarMod.LOGGER.warn("[GuitarMod] duplicate effect id ignored: {}", id);
        }
    }

    /** 按 id 创建一个新实例（注册表存的是工厂，每条链持有独立实例）。 */
    public static IEffect create(ResourceLocation id) {
        Supplier<IEffect> f = FACTORIES.get(id);
        return f == null ? null : f.get();
    }

    public static List<ResourceLocation> ids() {
        return Collections.unmodifiableList(new ArrayList<>(FACTORIES.keySet()));
    }

    /** 注册内置效果，在 mod 构造时调用一次。 */
    public static void bootstrap() {
        register(ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "gain"), GainEffect::new);
        register(ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "overdrive"), OverdriveEffect::new);
        register(ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "delay"), DelayEffect::new);
        register(ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "reverb"), ReverbEffect::new);
        register(ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "chorus"), ChorusEffect::new);
    }
}
