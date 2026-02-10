package net.lucas.narrowzone.util;

import net.lucas.narrowzone.NarrowZone;
import net.lucas.narrowzone.item.ModItems;
import net.lucas.narrowzone.item.custom.RepeatingCrossbowItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

import static net.lucas.narrowzone.item.custom.RepeatingCrossbowItem.TAG_ANIM;
import static net.lucas.narrowzone.item.custom.RepeatingCrossbowItem.getMagCount;

public class ModItemProperties {

    public static final ResourceLocation SKIN = new ResourceLocation(NarrowZone.MOD_ID, "skin");

    public static void addCustomItemProperties() {
        makeCustomBow(ModItems.LONG_BOW.get(), 30);
        makeCustomBow(ModItems.SHORT_BOW.get(), 15);
        makeCustomCrossbow((CrossbowItem) ModItems.LIGHT_CROSSBOW.get(), 20);
        makeCustomRep((RepeatingCrossbowItem) ModItems.REPEATING_CROSSBOW.get());

        addCustomSkins(ModItems.LONG_BOW.get());
        addCustomSkins(ModItems.SHORT_BOW.get());
    }

    private static void addCustomSkins (Item item) {
        ItemProperties.register(item, SKIN, (stack, level, entity, seed) -> {
            if (!stack.hasCustomHoverName()) return 0.0F;

            String name = stack.getHoverName().getString().trim(); // trim tira o desnecessário do ‘string’
            if (name.equalsIgnoreCase("Vanilla")) return 1.0F;

            return 0.0F;
        });
    }

    private static void makeCustomBow(Item item, float ticks) {

        ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, entity, i) -> {
            if (entity == null) {
                return 0.0F;
            } else {
                return entity.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / ticks;
            }
        });
        ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, entity, i) -> {
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
        });
    }

    private static void makeCustomCrossbow(CrossbowItem item, float ticks) {
        ItemProperties.register(item, new ResourceLocation("pull"), (ClampedItemPropertyFunction)((stack, level, entity, i) -> {
            if (entity == null) {
                return 0.0F;
            } else {
                return CrossbowItem.isCharged(stack) ? 0.0F : (float)(stack.getUseDuration() - entity.getUseItemRemainingTicks()) / ticks;
            }
        }));

        ItemProperties.register(item, new ResourceLocation("pulling"), (ClampedItemPropertyFunction)((stack, level, entity, i) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F));

        ItemProperties.register(item, new ResourceLocation("charged"), (ClampedItemPropertyFunction)((stack, level, entity, i) ->
                CrossbowItem.isCharged(stack) ? 1.0F : 0.0F));

        ItemProperties.register(item, new ResourceLocation("firework"), (ClampedItemPropertyFunction)((stack, level, entity, i) ->
                CrossbowItem.isCharged(stack) && CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.0F : 0.0F));

    }

    // ResourceLocation da Repeating Crossbow
    private static final ResourceLocation RELOAD = new ResourceLocation(NarrowZone.MOD_ID, "reload");
    private static final ResourceLocation FIRE   = new ResourceLocation(NarrowZone.MOD_ID, "fire");

    private static final ResourceLocation MODE   = new ResourceLocation(NarrowZone.MOD_ID, "mode");
    private static final ResourceLocation TYPE   = new ResourceLocation(NarrowZone.MOD_ID, "type");
    private static final ResourceLocation LOADED = new ResourceLocation(NarrowZone.MOD_ID, "loaded");

    private static boolean usingThis(LivingEntity entity, ItemStack stack) {
        if (entity == null) return false;
        if (!entity.isUsingItem()) return false;

        ItemStack using = entity.getUseItem();
        return ItemStack.isSameItemSameTags(using, stack); // não é por referência
        // return ItemStack.matches(using, stack);
    }

    private static void makeCustomRep(RepeatingCrossbowItem item) {

        ItemProperties.register(item, RELOAD, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (RepeatingCrossbowItem.getMode(stack) != 2) return 0.0F;
            if (!usingThis(entity, stack)) return 0.0F;

            int reloadTicks = RepeatingCrossbowItem.getReloadDurationLoad(stack);
            int usedTicks = stack.getUseDuration() - entity.getUseItemRemainingTicks();
            return Mth.clamp((float) usedTicks / (float) reloadTicks, 0F, 1F);
        });

        ItemProperties.register(item, FIRE, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (RepeatingCrossbowItem.getMode(stack) != 1) return 0.0F;
            if (!usingThis(entity, stack)) return 0.0F;

            int fireTicks = RepeatingCrossbowItem.getReloadDurationFire(stack);
            int anim = stack.getOrCreateTag().getInt(TAG_ANIM);
            return Mth.clamp((float) anim / (float) fireTicks, 0F, 1F);
        });

        ItemProperties.register(item, MODE, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (!usingThis(entity, stack)) return 0.0F;

            return getMagCount(stack) > 0 ? 1.0F : 2.0F; // none/fire/reload
        });

        ItemProperties.register(item, TYPE, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            ItemStack proj = RepeatingCrossbowItem.getNextAmmoType(stack);
            if (proj.isEmpty()) return 0.0F;
            return proj.is(Items.FIREWORK_ROCKET) ? 1.0F : 2.0F;
        });

        ItemProperties.register(item, LOADED, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (usingThis(entity, stack)) return 0.0F;
            return getMagCount(stack) > 0 ? 1.0F : 0.0F;
        });
    }
}