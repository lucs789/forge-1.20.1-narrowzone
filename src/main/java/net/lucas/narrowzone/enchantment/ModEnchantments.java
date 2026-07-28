package net.lucas.narrowzone.enchantment;

import net.lucas.narrowzone.NarrowZone;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Predicate;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, NarrowZone.MOD_ID);

    public static final RegistryObject<Enchantment> STEADY_DRAW = ENCHANTMENTS.register("steady_draw",
            () -> new RangedWeaponEnchantment(Enchantment.Rarity.RARE, 1, ModRangedEnchantmentEffects::isBowOrCrossbow));

    public static final RegistryObject<Enchantment> QUICKSTRING = ENCHANTMENTS.register("quickstring",
            () -> new RangedWeaponEnchantment(Enchantment.Rarity.UNCOMMON, 3, ModRangedEnchantmentEffects::isBow));

    public static final RegistryObject<Enchantment> DEADEYE = ENCHANTMENTS.register("deadeye",
            () -> new RangedWeaponEnchantment(Enchantment.Rarity.RARE, 3, ModRangedEnchantmentEffects::isBow));

    public static final RegistryObject<Enchantment> LONGSHOT = ENCHANTMENTS.register("longshot",
            () -> new RangedWeaponEnchantment(Enchantment.Rarity.UNCOMMON, 3, ModRangedEnchantmentEffects::isBowOrCrossbow));

    public static final RegistryObject<Enchantment> CHAIN_BOLT = ENCHANTMENTS.register("chain_bolt",
            () -> new RangedWeaponEnchantment(Enchantment.Rarity.RARE, 3, ModRangedEnchantmentEffects::isCrossbow));

    private ModEnchantments() {
    }

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }

    private static final class RangedWeaponEnchantment extends Enchantment {
        private final int maxLevel;
        private final Predicate<ItemStack> canEnchant;

        private RangedWeaponEnchantment(Rarity rarity, int maxLevel, Predicate<ItemStack> canEnchant) {
            super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
            this.maxLevel = maxLevel;
            this.canEnchant = canEnchant;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return 8 + (level - 1) * 10;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 25;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return canEnchant.test(stack);
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return canEnchant(stack);
        }
    }
}
