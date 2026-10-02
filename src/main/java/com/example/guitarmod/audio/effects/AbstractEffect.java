package com.example.guitarmod.audio.effects;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置效果基类：统一的参数表、启用开关与 NBT 序列化。
 */
public abstract class AbstractEffect implements IEffect {

    protected final Map<String, Float> params = new LinkedHashMap<>();
    private boolean enabled = true;

    @Override
    public Map<String, Float> getParams() {
        return params;
    }

    @Override
    public void setParam(String key, float value) {
        if (params.containsKey(key)) {
            params.put(key, value);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id().toString());
        tag.putBoolean("enabled", enabled);
        CompoundTag p = new CompoundTag();
        params.forEach(p::putFloat);
        tag.put("params", p);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        if (tag.contains("params")) {
            CompoundTag p = tag.getCompound("params");
            for (String key : p.getAllKeys()) {
                setParam(key, p.getFloat(key));
            }
        }
    }
}
