package net.lucas.narrowzone;

import net.lucas.narrowzone.enchantment.ModEnchantments;
import net.lucas.narrowzone.item.ModCreativeModTabs;
import net.lucas.narrowzone.item.ModItems;
import net.lucas.narrowzone.util.ModItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NarrowZone.MOD_ID)
public class NarrowZone {
    public static final String MOD_ID = "narrowzone";

    public NarrowZone() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModEnchantments.register(modEventBus);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(ModItemProperties::addCustomItemProperties);
        }
    }
}
