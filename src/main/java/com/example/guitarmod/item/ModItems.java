package com.example.guitarmod.item;

import com.example.guitarmod.GuitarMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GuitarMod.MODID);

    public static final DeferredItem<Item> GUITAR = ITEMS.register("guitar",
            () -> new GuitarItem(new Item.Properties().stacksTo(1)));
}
