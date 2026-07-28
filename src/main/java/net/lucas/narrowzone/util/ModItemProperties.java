package net.lucas.narrowzone.util;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
import net.lucas.narrowzone.enchantment.ModRangedEnchantmentEffects;
import net.lucas.narrowzone.item.custom.RepeatingCrossbowItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ModItemProperties {
    public static final ResourceLocation SKIN = new ResourceLocation(NarrowZone.MOD_ID, "skin");
    private static final ResourceLocation RELOAD = new ResourceLocation(NarrowZone.MOD_ID, "reload");
    private static final ResourceLocation FIRE = new ResourceLocation(NarrowZone.MOD_ID, "fire");
    private static final ResourceLocation MODE = new ResourceLocation(NarrowZone.MOD_ID, "mode");
    private static final ResourceLocation TYPE = new ResourceLocation(NarrowZone.MOD_ID, "type");
    private static final ResourceLocation LOADED = new ResourceLocation(NarrowZone.MOD_ID, "loaded");

    public static void addCustomItemProperties() {
        makeCustomBow(Items.BOW, 20.0F);
        makeCustomBow(ModItems.LONG_BOW.get(), 30.0F);
        makeCustomBow(ModItems.SHORT_BOW.get(), 15.0F);
        makeCustomCrossbow((CrossbowItem) ModItems.LIGHT_CROSSBOW.get(), 20.0F);
        makeCustomRep((RepeatingCrossbowItem) ModItems.REPEATING_CROSSBOW.get());
        addCustomSkins(ModItems.LONG_BOW.get());
        addCustomSkins(ModItems.SHORT_BOW.get());
    }

    private static void addCustomSkins(Item item) {
        ItemProperties.register(item, SKIN, (stack, level, entity, seed) -> {
            if (!stack.hasCustomHoverName()) {
                return 0.0F;
            }
            String name = stack.getHoverName().getString().trim();
            return name.equalsIgnoreCase("Vanilla") ? 1.0F : 0.0F;
        });
    }

    private static void makeCustomBow(Item item, float duration) {
        ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
            if (!usingThis(entity, stack)) {
                return 0.0F;
            }

            int rawUsedTicks = stack.getUseDuration() - entity.getUseItemRemainingTicks();
            return ModRangedEnchantmentEffects.getQuickstringAdjustedPullProgress(stack, rawUsedTicks, duration);
        });

        ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, entity, seed) -> {
            return usingThis(entity, stack) ? 1.0F : 0.0F;
        });
    }

    private static void makeCustomCrossbow(CrossbowItem item, float duration) {
        ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            return CrossbowItem.isCharged(stack) ? 0.0F : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / duration;
        });

        ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, entity, seed) -> {
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F;
        });

        ItemProperties.register(item, new ResourceLocation("charged"), (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);

        ItemProperties.register(item, new ResourceLocation("firework"), (stack, level, entity, seed) -> {
            return CrossbowItem.isCharged(stack) && CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
        });
    }

    private static boolean usingThis(LivingEntity entity, ItemStack stack) {
        if (entity == null || !entity.isUsingItem()) {
            return false;
        }
        ItemStack using = entity.getUseItem();
        return ItemStack.matches(using, stack) || using == stack;
    }

    private static void makeCustomRep(RepeatingCrossbowItem item) {
        ItemProperties.register(item, RELOAD, (stack, level, entity, seed) -> {
            if (entity == null || RepeatingCrossbowItem.getMode(stack) != RepeatingCrossbowItem.MODE_RELOAD || !usingThis(entity, stack)) {
                return 0.0F;
            }
            int duration = RepeatingCrossbowItem.getReloadDurationLoad(stack);
            int used = stack.getUseDuration() - entity.getUseItemRemainingTicks();
            return Mth.clamp((float) used / (float) duration, 0.0F, 1.0F);
        });

        ItemProperties.register(item, FIRE, (stack, level, entity, seed) -> {
            if (entity == null || RepeatingCrossbowItem.getMode(stack) != RepeatingCrossbowItem.MODE_FIRE || !usingThis(entity, stack)) {
                return 0.0F;
            }
            int duration = RepeatingCrossbowItem.getReloadDurationFire(stack);
            int anim = stack.getOrCreateTag().getInt(RepeatingCrossbowItem.TAG_ANIM);
            return Mth.clamp((float) anim / (float) duration, 0.0F, 1.0F);
        });

        ItemProperties.register(item, MODE, (stack, level, entity, seed) -> {
            if (entity == null || !usingThis(entity, stack)) {
                return 0.0F;
            }
            return RepeatingCrossbowItem.getMagCount(stack) > 0 ? 1.0F : 2.0F;
        });

        ItemProperties.register(item, TYPE, (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            ItemStack next = RepeatingCrossbowItem.getNextAmmoType(stack);
            if (next.isEmpty()) {
                return 0.0F;
            }
            return next.is(Items.FIREWORK_ROCKET) ? 1.0F : 2.0F;
        });

        ItemProperties.register(item, LOADED, (stack, level, entity, seed) -> {
            if (entity == null || usingThis(entity, stack)) {
                return 0.0F;
            }
            return RepeatingCrossbowItem.getMagCount(stack) > 0 ? 1.0F : 0.0F;
        });
    }
}
