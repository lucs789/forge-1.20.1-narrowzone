package net.lucas.narrowzone.item;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NarrowZone.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NARROW_TAB = CREATIVE_MODE_TABS.register("narrow_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.LONG_BOW.get()))
                    .title(Component.translatable("creativetab.narrow_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SHORT_BOW.get());
                        output.accept(Items.BOW);
                        output.accept(ModItems.LONG_BOW.get());
                        output.accept(ModItems.LIGHT_CROSSBOW.get());
                        output.accept(ModItems.REPEATING_CROSSBOW.get());
                        output.accept(Items.CROSSBOW);

                        addEnchantmentBooks(output, ModEnchantments.STEADY_DRAW);
                        addEnchantmentBooks(output, ModEnchantments.QUICKSTRING);
                        addEnchantmentBooks(output, ModEnchantments.DEADEYE);
                        addEnchantmentBooks(output, ModEnchantments.LONGSHOT);
                        addEnchantmentBooks(output, ModEnchantments.CHAIN_BOLT);
                    })
                    .build());

    private static void addEnchantmentBooks(CreativeModeTab.Output output, RegistryObject<Enchantment> enchantment) {
        Enchantment enchantmentValue = enchantment.get();

        for (int level = enchantmentValue.getMinLevel(); level <= enchantmentValue.getMaxLevel(); level++) {
            output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantmentValue, level)));
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
