package net.lucas.narrowzone.item;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.custom.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, NarrowZone.MOD_ID);

    public static final RegistryObject<Item> LONG_BOW = ITEMS.register("long_bow",
            () -> new LongBowItem(new Item.Properties().durability(384)));

    public static final RegistryObject<Item> SHORT_BOW = ITEMS.register("short_bow",
            () -> new ShortBowItem(new Item.Properties().durability(384)));

    public static final RegistryObject<Item> LIGHT_CROSSBOW = ITEMS.register("light_crossbow",
            () -> new LightCrossbowItem(new Item.Properties().durability(465)));

    public static final RegistryObject<Item> REPEATING_CROSSBOW = ITEMS.register("repeating_crossbow",
            () -> new RepeatingCrossbowItem(new Item.Properties().durability(465)));

public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
    }
}
