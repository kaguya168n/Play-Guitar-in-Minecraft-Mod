package com.example.guitarmod;

import com.example.guitarmod.audio.effects.EffectRegistry;
import com.example.guitarmod.config.GuitarConfig;
import com.example.guitarmod.data.ModComponents;
import com.example.guitarmod.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(GuitarMod.MODID)
public class GuitarMod {
    public static final String MODID = "guitarmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GuitarMod(IEventBus bus, ModContainer container) {
        ModItems.ITEMS.register(bus);
        ModComponents.COMPONENTS.register(bus);
        EffectRegistry.bootstrap();
        container.registerConfig(ModConfig.Type.CLIENT, GuitarConfig.SPEC);
        bus.addListener(this::addCreative);
        LOGGER.info("[GuitarMod] initialized");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.GUITAR.get());
        }
    }
}
