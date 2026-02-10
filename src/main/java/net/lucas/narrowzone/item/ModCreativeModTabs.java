package net.lucas.narrowzone.item;

import net.lucas.narrowzone.NarrowZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NarrowZone.MOD_ID);

    //cria uma aba nova no criativo
    public static final RegistryObject<CreativeModeTab> NARROW_TAB = CREATIVE_MODE_TABS.register("narrow_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.REPEATING_CROSSBOW.get()))
                    .title(Component.translatable("creativetab.narrow_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.SHORT_BOW.get());
                        pOutput.accept(Items.BOW);
                        pOutput.accept(ModItems.LONG_BOW.get());
                        pOutput.accept(ModItems.LIGHT_CROSSBOW.get());
                        pOutput.accept(ModItems.REPEATING_CROSSBOW.get());
                        pOutput.accept(Items.CROSSBOW);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
