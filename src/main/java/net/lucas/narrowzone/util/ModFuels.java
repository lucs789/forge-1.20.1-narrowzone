package net.lucas.narrowzone.util;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarrowZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)

public class ModFuels {

    public static final int RANGED_BURN_TIME = 300;

    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();

        if (item == ModItems.SHORT_BOW.get()) {
            event.setBurnTime(RANGED_BURN_TIME);
            return;
        }
        if (item == ModItems.LONG_BOW.get()) {
            event.setBurnTime(RANGED_BURN_TIME);
            return;
        }
        if (item == ModItems.LIGHT_CROSSBOW.get()) {
            event.setBurnTime(RANGED_BURN_TIME);
            return;
        }
        if (item == ModItems.REPEATING_CROSSBOW.get()) {
            event.setBurnTime(RANGED_BURN_TIME);
            return;
        }

    }

}
